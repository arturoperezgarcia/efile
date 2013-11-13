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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.jar.JarFile;

// TODO:    Win testen, Pfadtrenner
//          url = "/" korrekt behandeln?

public class FileFactory {
    static public EFile getResourceFile(URL url)
            throws IOException {
        String protocol = url.getProtocol();

        if (protocol.equals("file"))
            return new LocalFile(url.getPath());

        if (protocol.equals("jar")) {
            String path = url.getPath();

            if (!path.startsWith("file:"))
                throw new UnsupportedOperationException(path);

            int n = path.indexOf("!");

            String filePath = path.substring(n + 2) + "/",
                    jarPath = path.substring(5, n);
            ZipEntryFile zf = new ZipEntryFile(new JarFile(new File(jarPath)), filePath);

            return zf;
        }

        throw new UnsupportedOperationException(url.toString());
    }

    //----------------------------------------------------------------------

    static public EFile getResourceFile(String urlString)
            throws IOException {
        if (urlString.startsWith(File.separator))
            urlString = urlString.substring(1);

        URL url = FileFactory.class.getClassLoader().getResource(urlString);

        if (url == null)
            throw new FileNotFoundException(urlString + " (No such file or directory)");

        String protocol = url.getProtocol(),
                path = url.getPath();

        // Switch on protocol (file: or jar:)

        if (protocol.equals("file"))
            return new LocalFile(path.substring(0, path.length() - urlString.length()) + "/" + urlString);

        if (protocol.equals("jar")) {
            if (!path.startsWith("file:"))
                throw new UnsupportedOperationException(path);

            int n = path.indexOf("!");

            String filePath = path.substring(n + 2) + "/",
                    jarPath = path.substring(5, n);

            return new ZipEntryFile(new JarFile(new File(jarPath)), filePath);
        }

        throw new UnsupportedOperationException(url.toString());
    }
}

