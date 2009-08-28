package hjsdf.lowlevel;

import hjsdf.lowlevel.exceptions.CommunicationException;
import hjsdf.lowlevel.exceptions.NotEnoughFreeSpaceException;
import hjsdf.lowlevel.exceptions.FileNotFoundException;
import hjsdf.lowlevel.exceptions.UploadFailedException;
import hjsdf.lowlevel.exceptions.ChecksumFailedException;
import hjsdf.lowlevel.exceptions.FileAlreadyApprovedException;

import java.io.InputStream;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * TODO abr forgot to document this class
 */
public interface Bitstorage {

    public URL upload(String filename,
                      InputStream data,
                      String md5)
            throws MalformedURLException,
                   CommunicationException,
                   NotEnoughFreeSpaceException, UploadFailedException,
                   ChecksumFailedException, FileAlreadyApprovedException;

    public void disapprove(URL file, String md5)
            throws FileNotFoundException,
            CommunicationException;

    public String approve(URL file, String md5)
            throws FileNotFoundException,CommunicationException,NotEnoughFreeSpaceException;

    public long spaceleft() throws CommunicationException;

    public long getMaxFileSize() throws CommunicationException;

    public String getMd5(URL file) throws FileNotFoundException, CommunicationException;

    public boolean isApproved(URL file) throws FileNotFoundException, CommunicationException;


}
