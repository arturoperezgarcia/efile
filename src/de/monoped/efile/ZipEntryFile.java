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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipEntryFile
        implements EFile, Cloneable {
    private ZipFile zip;
    private ZipNode rootNode, node;
    private Path path;

    public ZipEntryFile(ZipFile zip) {
        this(zip, "/");
    }

    public ZipEntryFile(ZipFile zip, String path) {
        this.zip = zip;
        this.path = new Path(path);

        rootNode = new ZipNode(null, null, "");

        // build tree

        for (Enumeration entries = zip.entries(); entries.hasMoreElements(); ) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            String[] comps = entry.getName().split("/");
            Node base = rootNode;
            int icomp;

            boolean found = false;

            for (icomp = 0; icomp < comps.length; ++icomp) {
                // Find path component in current base's entries

                String comp = comps[icomp];

                found = false;

                for (Iterator it = base.iterator(); it.hasNext(); ) {
                    Node child = (Node) it.next();
                    String childName = child.getName();

                    if (childName.equals(comp)) {
                        base = child;
                        found = true;
                        break;
                    }
                }

                if (!found)
                    break;
            }

            if (!found) {
                // Not yet present, add dir parts

                for (; icomp < comps.length - 1; ++icomp)
                    base = base.addChild(new ZipNode(null, base, comps[icomp]));

                // and file part

                base.addChild(new ZipNode(entry, base, comps[comps.length - 1]));
            }
        }

        node = getNode();
    }

    public ZipEntryFile(Path path) {
        this.path = path;
    }

    public Object clone() {
        Object f = null;

        try {
            f = super.clone();
            node = getNode();
        } catch (CloneNotSupportedException ex) {
        }

        return f;
    }

    public void close() {
    }

    @Override
    public void copyFrom(InputStream in) throws IOException {
        throw new IOException(zip.getName() + ": can't write");
    }

    public void copyFrom(File src)
            throws IOException {
        throw new IOException(zip.getName() + ": can't write");
    }

    public void delete() {
    }

    public void delete(boolean recursive) {
    }

    public boolean exists() {
        return getNode() != null;
    }

    public String getBase() {
        return zip.getName();
    }

    public String getAbsolutePath() {
        return "/" + path;
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
        if (node == null)
            throw new FileNotFoundException(path + " (No such file or directory)");

        if (node.isDirectory())
            throw new FileNotFoundException(path + " (Is a directory)");

        return zip.getInputStream(node.getEntry());
    }

    public String getName() {
        return path.getName();
    }

    public void setName(String name) {
        if (!Utils.isName(name))
            throw new IllegalArgumentException(name);

        path.setName(name);
    }

    private ZipNode getNode(String path) {
        String[] components = path.split("/");
        ZipNode dir = rootNode;

        if (components.length == 0)
            return rootNode;

        for (int i = 0; i < components.length; ++i) {
            String comp = components[i];

            if (comp.length() > 0) {
                ZipNode child = (ZipNode) dir.getChild(comp);

                if (child == null)
                    return null;

                dir = child;
            }
        }

        return dir;
    }

    private ZipNode getNode() {
        return getNode(path.getPath());
    }

    public OutputStream getOutputStream()
            throws IOException {
        throw new IOException(zip.getName() + ": can't write");
    }

    public String getParent() {
        return path.getParentPath();
    }

    public String getPath() {
        return path.getPath();
    }

    public void setPath(String path) {
        this.path = new Path(path);
    }

    public boolean isDirectory() {
        return node != null && node.isDirectory();
    }

    public ZipEntryFileIterator iterator() {
        return new ZipEntryFileIterator();
    }

    public ZipEntryFileIterator iterator(FilenameFilter filter) {
        return new ZipEntryFileIterator(filter);
    }

    public String[] list() {
        if (node == null)
            return null;

        int n = node.children.size();
        String[] names = new String[n];
        int i = 0;

        for (Iterator it = node.iterator(); it.hasNext(); ) {
            Node nod = (Node) it.next();

            names[i++] = nod.getName();
        }

        return names;
    }

    public String[] list(FilenameFilter filter) {
        if (node == null)
            return null;

        File parent = new File(path.getParentPath());

        ArrayList<String> names = new ArrayList<String>();

        for (Iterator<Node> it = node.iterator(); it.hasNext(); ) {
            Node child = it.next();

            if (filter.accept(parent, child.getName()))
                names.add(child.getName());
        }

        return names.toArray(new String[0]);
    }

    public boolean mkdirs() {
        return false;
    }

    public void putBytes(byte[] bytes)
            throws IOException {
        throw new IOException(path + ": can't write");
    }

    public void addPath(String path) {
        this.path.addPath(path);
    }

    public String toString() {
        return "ZIP (" + zip.getName() + ") " + path;
    }

    class ZipEntryFileIterator
            implements Iterator {
        String[] list;
        int i;

        ZipEntryFileIterator() {
            i = 0;
            this.list = list();
        }

        ZipEntryFileIterator(FilenameFilter filter) {
            this.list = list(filter);
            i = 0;
        }

        public boolean hasNext() {
            return list != null && i < list.length;
        }

        public EFile next()
                throws NoSuchElementException {
            if (list == null || i >= list.length)
                throw new NoSuchElementException(String.valueOf(i));

            path.addPath(list[i++]);
            return new ZipEntryFile(path);
        }

        public void remove()
                throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }
    }
}

