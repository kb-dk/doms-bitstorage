package dk.statsbiblioteket.doms.bitstorage.lowlevel;

import java.io.InputStream;
import java.net.URL;

/**
 * TODO abr forgot to document this class
 */
public interface Bitstorage {

    public URL upload(String filename,
                      InputStream data,
                      String md5);

    public void disapprove(URL file, String md5);

    public void approve(URL file, String md5);

    public long spaceleft();
    
}
