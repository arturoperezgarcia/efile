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

import java.io.*;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class LocalFile
        implements EFile {
    private Path curpath;

    //----------------------------------------------------------------------

    public LocalFile(String path) {
        curpath = new Path(path);
    }

    //----------------------------------------------------------------------

    public void addPath(String path) {
        curpath.addPath(path);
    }

    //----------------------------------------------------------------------

    public void close() {
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

    public void delete() {
        new File(curpath.getPath()).delete();
    }

    //----------------------------------------------------------------------

    public void delete(boolean recursive) {
        File file = new File(curpath.getPath());

        if (!recursive || !file.isDirectory())
            file.delete();
        else
            deleteDir(file);
    }

    //----------------------------------------------------------------------

    private boolean deleteDir(File dir) {
        File[] files = dir.listFiles();

        if (files != null) {
            for (int i = 0; i < files.length; ++i) {
                File f = files[i];

                if (f.isDirectory()) {
                    if (!deleteDir(f))
                        return false;
                } else if (!f.delete())
                    return false;
            }
        }

        dir.delete();
        return true;
    }

    //----------------------------------------------------------------------

    public boolean exists() {
        return new File(curpath.getPath()).exists();
    }

    //----------------------------------------------------------------------

    public String getBase() {
        return curpath.getPath();
    }

    //----------------------------------------------------------------------

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

    //----------------------------------------------------------------------

    public InputStream getInputStream()
            throws IOException {
        return new FileInputStream(curpath.getPath());
    }

    //----------------------------------------------------------------------

    public String getName() {
        return curpath.getName();
    }

    //----------------------------------------------------------------------

    public String getParent() {
        return curpath.getParentPath();
    }

    //----------------------------------------------------------------------

    public String getPath() {
        return curpath.getPath();
    }

    //----------------------------------------------------------------------

    public OutputStream getOutputStream()
            throws IOException {
        return new FileOutputStream(curpath.getPath());
    }

    //----------------------------------------------------------------------

    public boolean isDirectory() {
        return new File(curpath.getPath()).isDirectory();
    }

    //----------------------------------------------------------------------

    public String[] list() {
        return new File(curpath.getPath()).list();
    }

    //----------------------------------------------------------------------

    public String[] list(FilenameFilter filter) {
        return new File(curpath.getPath()).list(filter);
    }

    //----------------------------------------------------------------------

    public Iterator<EFile> iterator() {
        return new LocalFileIterator(new File(curpath.getPath()).list());
    }

    //----------------------------------------------------------------------

    public Iterator<EFile>  iterator(FilenameFilter filter) {
        return new LocalFileIterator(new File(curpath.getPath()).list(filter));
    }

    //----------------------------------------------------------------------

    public boolean mkdirs() {
        File file = new File(curpath.getPath());

        file.mkdirs();

        return file.exists();
    }

    //----------------------------------------------------------------------

    public void putBytes(byte[] bytes)
            throws IOException {
        OutputStream out = getOutputStream();

        out.write(bytes);
        out.close();
    }

    //----------------------------------------------------------------------

    public void setName(String name) {
        curpath.setName(name);
    }

    //----------------------------------------------------------------------

    public void setPath(String path) {
        curpath.setPath(path);
    }

    //----------------------------------------------------------------------

    public String toString() {
        return curpath.getPath();
    }

    //----------------------------------------------------------------------
    //----------------------------------------------------------------------

    class LocalFileIterator
            implements Iterator {
        private int i;
        private String[] list;

        //----------------------------------------------------------------------

        LocalFileIterator(String[] list) {
            i = 0;
            this.list = list;
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
            return this;
        }

        //----------------------------------------------------------------------

        public void remove()
                throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

    }
}

