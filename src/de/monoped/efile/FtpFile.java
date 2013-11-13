/* This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * monoped@users.sourceforge.net
 */

package de.monoped.efile;

import com.enterprisedt.net.ftp.FTPClient;
import com.enterprisedt.net.ftp.FTPException;
import com.enterprisedt.net.ftp.FTPFile;
import com.enterprisedt.net.ftp.FTPTransferType;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class FtpFile
        implements EFile {
    private FTPClient client;
    private String ftpdir;
    private String server;
    private String user;
    private Path currentPath;
    private HashMap<String, FTPFile[]> dirmap;

    //----------------------------------------------------------------------

    public FtpFile(String server, String user, String password, String path)
            throws IOException, FTPException {
        this.server = server;
        this.user = user;
        currentPath = new Path(path);
        dirmap = new HashMap<String, FTPFile[]>();
        client = new FTPClient();
        client.setRemoteHost(server);
        client.connect();
        client.user(user);
        client.password(password);
        client.setType(FTPTransferType.BINARY);
    }

    //----------------------------------------------------------------------

    public void addPath(String path) {
        currentPath.addPath(path);
    }

    //----------------------------------------------------------------------

    public void close()
            throws IOException {
        try {
            client.quit();
        } catch (FTPException ex) {
            throw new IOException(ex);
        }
    }

    //----------------------------------------------------------------------

    public void copyFrom(File file)
            throws IOException {
        copyFrom(new FileInputStream(file));
    }

    //----------------------------------------------------------------------

    public void copyFrom(InputStream in)
            throws IOException {
        OutputStream out = getOutputStream();
        byte[] buf = new byte[4096];
        int n;

        while ((n = in.read(buf)) >= 0)
            out.write(buf, 0, n);

        in.close();
        out.close();
    }

    //----------------------------------------------------------------------

    public void delete()
            throws IOException {
        if (currentPath.getPath().equals("/"))
            return;

        try {
            FTPFile curfile = getFtpFile();

            if (curfile == null)
                return;

            if (curfile.isDir())
                client.rmdir(currentPath.getPath());
            else
                client.delete(currentPath.getPath());
        } catch (Exception ex) {
            throw new IOException(ex);
        }

        dirmap.remove(currentPath.getParentPath());
    }

    //----------------------------------------------------------------------

    public void delete(boolean recursive)
            throws IOException {
        if (!recursive || !isDirectory())
            delete();
        else
            deleteDir(currentPath.getPath());
    }

    //----------------------------------------------------------------------

    public boolean exists()
            throws IOException {
        try {
            return currentPath.getPath().equals("/") || getFtpFile() != null;
        } catch (Exception ex) {
            throw new IOException(ex.getMessage());
        }
    }

    //----------------------------------------------------------------------

    public String getBase() {
        return "ftp://" + user + "@" + server;
    }

    //----------------------------------------------------------------------

    public byte[] getBytes()
            throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        try {
            client.get(stream, currentPath.getPath());
        } catch (FTPException ex) {
            throw new IOException(ex.getMessage());
        }

        return stream.toByteArray();
    }

    //----------------------------------------------------------------------

    public InputStream getInputStream()
            throws IOException {
        final PipedOutputStream pout = new PipedOutputStream();

        Thread t = new Thread() {
            public void run() {
                try {
                    client.get(pout, currentPath.getPath());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };

        FtpInputStream pin = new FtpInputStream(pout, t);

        t.start();
        return pin;
    }

    //----------------------------------------------------------------------

    public String getName() {
        return currentPath.getName();
    }

    //----------------------------------------------------------------------

    public OutputStream getOutputStream()
            throws IOException {
        final PipedInputStream pin = new PipedInputStream();

        Thread t = new Thread() {
            public void run() {
                try {
                    client.put(pin, currentPath.getPath());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };

        FtpOutputStream pout = new FtpOutputStream(pin, t);

        t.start();
        return pout;
    }

    //----------------------------------------------------------------------

    public String getParent() {
        return currentPath.getParentPath();
    }

    //----------------------------------------------------------------------

    public String getPath() {
        return currentPath.getPath();
    }

    //----------------------------------------------------------------------

    public boolean isDirectory()
            throws IOException {
        try {
            FTPFile curfile = getFtpFile();

            return curfile != null && curfile.isDir();
        } catch (Exception ex) {
            throw new IOException(ex.getMessage());
        }
    }

    //----------------------------------------------------------------------

    public Iterator<EFile> iterator()
            throws IOException {
        return new FtpFileIterator(list());
    }

    //----------------------------------------------------------------------

    public Iterator<EFile> iterator(FilenameFilter filter)
            throws IOException {
        return new FtpFileIterator(list(filter));
    }

    //----------------------------------------------------------------------

    public String[] list()
            throws IOException {
        try {
            FTPFile[] children = getChildren(currentPath.getPath());

            if (children != null) {
                String[] names = new String[children.length];

                for (int i = 0; i < children.length; ++i)
                    names[i] = children[i].getName();

                return names;
            }
        } catch (FTPException ex) {
            throw new IOException(ex.getMessage());
        }

        return null;
    }

    //----------------------------------------------------------------------

    public String[] list(FilenameFilter filter)
            throws IOException {
        try {
            FTPFile[] children = getChildren(currentPath.getPath());

            if (children != null) {
                ArrayList<String> show = new ArrayList<String>();

                for (int i = 0; i < children.length; ++i) {
                    String name = children[i].getName();

                    if (filter.accept(null, name))
                        show.add(name);
                }

                return show.toArray(new String[0]);
            }
        } catch (FTPException ex) {
            throw new IOException(ex.getMessage());
        }

        return null;
    }

    //----------------------------------------------------------------------

    public boolean mkdirs()
            throws IOException {
        String[] comps = currentPath.getPath().split("/");

        String wd = "/";

        try {
            for (int i = 0; i < comps.length; ++i) {
                String comp = comps[i];

                if (comp.length() > 0) {
                    FTPFile[] files = getChildren(wd);
                    boolean found = false;

                    for (int j = 0; j < files.length; ++j) {
                        FTPFile file = files[j];

                        if (file.getName().equals(comp)) {
                            found = true;
                            break;
                        }
                    }

                    String wdnew = wd.endsWith("/") ? wd + comp : wd + "/" + comp;

                    if (!found) {
                        client.mkdir(wdnew);
                        dirmap.remove(wd);
                    }

                    wd = wdnew;
                }
            }
        } catch (FTPException ex) {
            throw new IOException(ex.getMessage());
        }

        return true;
    }

    //----------------------------------------------------------------------

    public EFile parentFile() {
        currentPath.parent();
        return this;
    }

    //----------------------------------------------------------------------

    public void putBytes(byte[] bytes)
            throws IOException {
        try {
            client.put(bytes, currentPath.getPath());
        } catch (FTPException ex) {
            throw new IOException(ex.getMessage());
        }
    }

    //----------------------------------------------------------------------

    public void setName(String name) {
        if (!Utils.isName(name))
            throw new IllegalArgumentException(name);

        currentPath.setName(name);
    }

    //----------------------------------------------------------------------

    public void setPath(String path) {
        if (!path.startsWith("/"))
            path = "/" + path;

        currentPath.setPath(path);
    }

    //----------------------------------------------------------------------

    public String toString() {
        return "ftp://" + user + "@" + server + getPath();
    }

    //----------------------------------------------------------------------
    //----------------------------------------------------------------------

    private void chdir(String path)
            throws IOException, FTPException {
        if (!path.equals(ftpdir))
            client.chdir(ftpdir = path);
    }

    //----------------------------------------------------------------------

    private FTPFile[] getChildren(String path)
            throws IOException, FTPException {
        FTPFile[] chn = dirmap.get(path);

        if (chn == null)
            try {
                chn = client.dirDetails(path);
                dirmap.put(path, chn);
            } catch (java.text.ParseException ex) {
                throw new IOException(ex.getMessage());
            }

        return chn;
    }

    //----------------------------------------------------------------------

    private void deleteDir(String path)
            throws IOException {
        try {
            FTPFile[] children = getChildren(path);
            String parent = path + "/";

            for (int i = 0; i < children.length; ++i) {
                FTPFile child = children[i];
                String childpath = parent + child.getName();

                if (child.isDir())
                    deleteDir(childpath);
                else
                    client.delete(childpath);
            }

            chdir("/");
            client.delete(path);
        } catch (FTPException ex) {
            throw new IOException(ex);
        }

    }

    //----------------------------------------------------------------------

    private void dumpChildren(String text, FTPFile[] children) {
        System.out.println(text);

        if (children == null)
            System.out.println("null");
        else if (children.length == 0)
            System.out.println("- keine -");
        else
            for (int i = 0; i < children.length; ++i) {
                FTPFile child = children[i];
                System.out.println("child: " + child.getName() + " dir: " + child.isDir() + " date: " + child.lastModified());
            }
    }

    //----------------------------------------------------------------------

    private FTPFile getFtpFile()
            throws FTPException, IOException, java.text.ParseException {
        FTPFile[] children = dirmap.get(currentPath.getParentPath());

        if (children == null)
            dirmap.put(currentPath.getParentPath(), children = client.dirDetails(currentPath.getParentPath()));

        for (int i = 0; i < children.length; ++i) {
            FTPFile child = children[i];

            if (child.getName().equals(currentPath.getName()))
                return child;
        }

        return null;
    }

    //----------------------------------------------------------------------
    //----------------------------------------------------------------------

    class FtpInputStream
            extends PipedInputStream {
        private Thread thread;

        //----------------------------------------------------------------------

        FtpInputStream(PipedOutputStream pout, Thread thread)
                throws IOException {
            super(pout);
            this.thread = thread;
        }

        //----------------------------------------------------------------------

        public void close()
                throws IOException {
            try {
                thread.join();
            } catch (InterruptedException ie) {
            }

            super.close();
        }
    }

    //----------------------------------------------------------------------

    class FtpOutputStream
            extends PipedOutputStream {
        private Thread thread;

        //----------------------------------------------------------------------

        FtpOutputStream(PipedInputStream pin, Thread thread)
                throws IOException {
            super(pin);
            this.thread = thread;
        }

        //----------------------------------------------------------------------

        public void close()
                throws IOException {
            super.close();

            try {
                thread.join();
            } catch (InterruptedException ie) {
            }
        }
    }

    //----------------------------------------------------------------------
    //----------------------------------------------------------------------

    class FtpFileIterator
            implements Iterator {
        String[] list;
        int i;

        //----------------------------------------------------------------------

        FtpFileIterator(String[] list) {
            this.list = list;
            i = 0;
        }

        //----------------------------------------------------------------------

        public boolean hasNext() {
            return list != null && i < list.length;
        }

        //----------------------------------------------------------------------

        public Object next()
                throws NoSuchElementException {
            if (list == null || i >= list.length)
                throw new NoSuchElementException(String.valueOf(i));

            setName(list[i++]);
            return FtpFile.this;
        }

        //----------------------------------------------------------------------

        public void remove()
                throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

    }

}

