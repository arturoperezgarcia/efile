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

public class Path {
    private String path;

    //----------------------------------------------------------------------

    public Path() {
        path = "";
    }

    //----------------------------------------------------------------------

    public Path(String path) {
        setPath(path);
    }

    //----------------------------------------------------------------------

    public void addPath(String add) {
        if (add.startsWith("/"))
            throw new IllegalArgumentException(add);

        setPath(path + "/" + add);
    }

    //----------------------------------------------------------------------

    public String getName() {
        int k = path.lastIndexOf("/");

        if (k >= 0)
            return path.substring(k + 1);

        return "";
    }

    //----------------------------------------------------------------------

    public String getParentPath() {
        int k = path.lastIndexOf("/");

        if (k >= 0)
            return path.substring(0, k);

        return "/";
    }

    //----------------------------------------------------------------------

    public String getPath() {
        return path;
    }

    //----------------------------------------------------------------------

    public static String normalizePath(String path) {
        String[] comps = path.split("/");
        String[] ap = new String[comps.length];
        String abspath;
        int j = 0;

        for (int i = 0; i < comps.length; ++i) {
            String s = comps[i];

            if (s.equals(".") || s.length() == 0)
                continue;

            if (s.equals("..")) {
                if (j > 0)
                    --j;
            } else
                ap[j++] = s;
        }

        if (j == 0)
            abspath = "/";
        else {
            abspath = "";

            for (int i = 0; i < j; ++i)
                abspath += "/" + ap[i];
        }

        return abspath;
    }

    //----------------------------------------------------------------------

    public void parent() {
        int k = path.lastIndexOf("/");

        if (k > 0) {
            path = path.substring(0, k);
        } else
            path = "";
    }

    //----------------------------------------------------------------------

    public void setName(String name) {
        if (name.indexOf("/") >= 0)
            throw new IllegalArgumentException(name);

        int k = path.lastIndexOf("/");

        if (k >= 0)
            path = path.substring(0, k + 1) + name;
        else
            path = name;
    }

    //----------------------------------------------------------------------

    public void setPath(String path) {
        this.path = normalizePath(path);
    }

    //----------------------------------------------------------------------

    public String toString() {
        return path;
    }

}

