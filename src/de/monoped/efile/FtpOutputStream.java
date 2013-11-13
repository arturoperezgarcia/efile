package de.monoped.efile;

import com.enterprisedt.net.ftp.FTPClient;
import com.enterprisedt.net.ftp.FTPException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

class FtpOutputStream
        extends ByteArrayOutputStream {
    private FTPClient ftp;
    private String path;

    //----------------------------------------------------------------------

    FtpOutputStream(FTPClient ftp, String path) {
        this.ftp = ftp;
        this.path = path;
    }

    //----------------------------------------------------------------------

    public void close()
            throws IOException {
        try {
            ftp.put(toByteArray(), path);
        } catch (FTPException ex) {
            throw new IOException(ex.getMessage());
        }
    }

}

