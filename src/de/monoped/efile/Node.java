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

import java.util.ArrayList;
import java.util.Iterator;

public class Node {
    Node parent;
    String name;
    ArrayList<Node> children;

    Node(Node parent, String name) {
        this.name = name;
        this.parent = parent;
        children = new ArrayList<Node>();
    }

    Node addChild(Node child) {
        children.add(child);
        return child;
    }

    Node getChild(String name) {
        for (Iterator it = children.iterator(); it.hasNext(); ) {
            Node child = (Node) it.next();
            String childName = child.getName();

            if (childName.endsWith("/"))
                childName = childName.substring(0, childName.length() - 1);

            if (childName.equals(name))
                return child;
        }

        return null;
    }

    String getName() {
        return name;
    }

    String getPath() {
        if (parent == null)
            return "/";

        String s = name;
        Node node = this;

        while ((node = node.getParent()) != null)
            s = node.getName() + "/" + s;

        return s;
    }

    Node getParent() {
        return parent;
    }

    boolean isDirectory() {
        return children.size() > 0;
    }

    public Iterator iterator() {
        return children.iterator();
    }

    public String toString() {
        return name;
    }

    void treeToString(StringBuffer s) {
        s.append(getPath()).append("\n");

        if (isDirectory())
            for (Iterator it = children.iterator(); it.hasNext(); )
                ((Node) it.next()).treeToString(s);
    }
}

