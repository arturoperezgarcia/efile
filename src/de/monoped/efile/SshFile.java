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

import com.trilead.ssh2.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Vector;

public class SshFile
        implements EFile {
    private SFTPv3Client client;
    private Connection connection;
    private Path curpath;
    String user, server;

    public SshFile(String server, String user, String path) {
        this.server = server;
        this.user = user;
        curpath = new Path(path);
    }

    public SshFile(String server, String user, String password, String path)
            throws IOException {
        this(server, user, path);
        connection = new Connection(server);
        connection.connect();

        if (!connection.authenticateWithPassword(user, password))
            throw new IOException("Authentication error");

        client = new SFTPv3Client(connection);
    }

    public SshFile(String server, String user, File keyfile, String path)
            throws IOException {
        this(server, user, path);
        connection = new Connection(server);
        connection.connect();

        if (!connection.authenticateWithPublicKey(user, keyfile, null))
            throw new IOException("Authentication error");

        client = new SFTPv3Client(connection);
    }

    public void addPath(String path) {
        curpath.addPath(path);
    }

    public void close() {
        client.close();
        connection.close();
    }

    public void copyFrom(File file)
            throws IOException {
        copyFrom(new FileInputStream(file));
    }

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

    public void delete()
            throws IOException {
        if (curpath.getPath().equals("/"))
            return;

        SFTPv3FileAttributes curfile = client.stat(curpath.getPath());

        if (curfile == null)
            return;

        if (curfile.isDirectory())
            client.rmdir(curpath.getPath());
        else
            client.rm(curpath.getPath());
    }

    public void delete(boolean recursive)
            throws IOException {
        if (!recursive || !isDirectory())
            delete();
        else
            deleteDir(curpath.getPath());
    }

    public boolean exists() {
        try {
            client.stat(curpath.getPath());
            return true;
        } catch (IOException ex) {
        }

        return false;
    }

    public String getBase() {
        return "ssh://" + user + "@" + server;
    }

    public byte[] getBytes()
            throws IOException {
        InputStream in = getInputStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[2048];
        int n;

        while ((n = in.read(buf)) >= 0)
            out.write(buf, 0, n);

        in.close();
        return out.toByteArray();
    }

    public InputStream getInputStream()
            throws IOException {
        SFTPv3FileHandle handle = client.openFileRO(curpath.getPath());

        return new SshInputStream(handle);
    }

    public String getName() {
        return curpath.getName();
    }

    public OutputStream getOutputStream() throws IOException {
        SFTPv3FileHandle handle = client.createFileTruncate(curpath.getPath());

        return new SshOutputStream(handle);
    }

    public String getPath() {
        return curpath.getPath();
    }

    public boolean isDirectory() {
        try {
            return client.stat(curpath.getPath()).isDirectory();
        } catch (IOException ex) {
        }

        return false;
    }

    public Iterator<EFile> iterator()
            throws IOException {
        return new SshFileIterator(list());
    }

    public Iterator<EFile> iterator(FilenameFilter filter)
            throws IOException {
        return new SshFileIterator(list(filter));
    }

    public String[] list()
            throws IOException {
        return list(curpath.getPath());
    }

    public String[] list(FilenameFilter filter)
            throws IOException {
        Vector children = client.ls(curpath.getPath());

        if (children != null) {
            ArrayList<String> show = new ArrayList<String>();

            for (Iterator it = children.iterator(); it.hasNext(); ) {
                SFTPv3DirectoryEntry child = (SFTPv3DirectoryEntry) it.next();

                if (filter.accept(null, child.filename))
                    show.add(child.filename);
            }

            return show.toArray(new String[0]);
        }

        return null;
    }

    public boolean mkdirs()
            throws IOException {
        String[] comps = curpath.getPath().split("/");

        String wd = "/";

        for (int i = 0; i < comps.length; ++i) {
            String comp = comps[i];

            if (comp.length() > 0) {
                String[] files = list(wd);
                boolean found = false;

                for (int j = 0; j < files.length; ++j) {
                    String file = files[j];

                    if (file.equals(comp)) {
                        found = true;
                        break;
                    }
                }

                String wdnew = wd.endsWith("/") ? wd + comp : wd + "/" + comp;

                if (!found)
                    client.mkdir(wdnew, 0755);

                wd = wdnew;
            }
        }

        return true;
    }

    public String getParent() {
        return curpath.getParentPath();
    }

    public EFile parentFile() {
        curpath.parent();
        return this;
    }

    public void putBytes(byte[] bytes)
            throws IOException {
        OutputStream out = getOutputStream();

        out.write(bytes);
        out.close();
    }

    public void setName(String name) {
        curpath.setName(name);
    }

    public void setPath(String path) {
        curpath.setPath(path);
    }

    public String toString() {
        return "ssh://" + user + "@" + server + curpath.getPath();
    }

    private boolean deleteDir(String path)
            throws IOException {
        Vector children = client.ls(path);
        String parent = path + "/";

        for (Iterator it = children.iterator(); it.hasNext(); ) {
            SFTPv3DirectoryEntry child = (SFTPv3DirectoryEntry) it.next();

            if (child.filename.equals(".") || child.filename.equals(".."))
                continue;

            String childpath = parent + child.filename;

            if (child.attributes.isDirectory()) {
                if (!deleteDir(childpath))
                    return false;
            } else
                client.rm(childpath);
        }

        client.rmdir(path);
        return true;
    }

    private String[] list(String path)
            throws IOException {
        Vector children = client.ls(path);

        if (children != null) {
            String[] childArray = new String[children.size()];
            int i = 0;

            for (Iterator it = children.iterator(); it.hasNext(); )
                childArray[i++] = ((SFTPv3DirectoryEntry) it.next()).filename;

            return childArray;
        }

        return null;
    }

    class SshInputStream
            extends InputStream {
        private SFTPv3FileHandle handle;
        private long pos;

        SshInputStream(SFTPv3FileHandle handle) {
            this.handle = handle;
        }

        public void close()
                throws IOException {
            client.closeFile(handle);
        }

        public int read()
                throws IOException {
            byte[] b = new byte[1];

            if (client.read(handle, pos, b, 0, 1) < 0)
                return -1;

            ++pos;
            return b[0];
        }

        public int read(byte[] b, int off, int len)
                throws IOException {
            int n = client.read(handle, pos, b, off, len);

            if (n > 0)
                pos += n;

            return n;
        }

        public int read(byte[] b)
                throws IOException {
            return read(b, 0, b.length);
        }
    }

    class SshOutputStream
            extends OutputStream {
        private SFTPv3FileHandle handle;
        private long pos;

        SshOutputStream(SFTPv3FileHandle handle) {
            this.handle = handle;
        }

        public void close()
                throws IOException {
            client.closeFile(handle);
        }

        public void write(int b)
                throws IOException {
            byte[] buf = {(byte) b};

            write(buf, 0, 1);
        }

        public void write(byte[] b, int off, int len)
                throws IOException {
            client.write(handle, pos, b, off, len);
            pos += len;
        }

        public void write(byte[] b)
                throws IOException {
            write(b, 0, b.length);
        }
    }

    class SshFileIterator
            implements Iterator {
        String[] list;
        int i;

        SshFileIterator(String[] list) {
            i = 0;
            this.list = list;
        }

        public boolean hasNext() {
            return list != null && i < list.length;
        }

        public Object next()
                throws NoSuchElementException {
            if (list == null || i >= list.length)
                throw new NoSuchElementException(String.valueOf(i));

            setName(list[i++]);
            return SshFile.this;
        }

        public void remove()
                throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }
    }
}

