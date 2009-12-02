package dk.statsbiblioteket.doms.bitstorage.highlevel.frontend;


import dk.statsbiblioteket.doms.bitstorage.highlevel.*;

import javax.activation.DataHandler;
import javax.jws.WebParam;

/**
 * Created by IntelliJ IDEA.
 * User: abr
 * Date: Dec 1, 2009
 * Time: 2:12:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class BitstorageSoapWebserviceImpl implements BitstorageSoapWebservice {
    public void uploadFileToObject(
            @WebParam(name = "pid", targetNamespace = "") String pid,
            @WebParam(name = "filename", targetNamespace = "") String filename,
            @WebParam(name = "filedata", targetNamespace = "")
            DataHandler filedata,
            @WebParam(name = "md5string", targetNamespace = "")
            String md5String,
            @WebParam(name = "filelength", targetNamespace = "")
            long filelength) throws
                             ChecksumFailedException,
                             CommunicationException,
                             FileAlreadyApprovedException,
                             InvalidFilenameException,
                             NotEnoughFreeSpaceException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void delete(@WebParam(name = "pid", targetNamespace = "") String pid)
            throws CommunicationException, FileNotFoundException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void publish(
            @WebParam(name = "pid", targetNamespace = "") String pid)
            throws
            ChecksumFailedException,
            CommunicationException,
            FileNotFoundException,
            NotEnoughFreeSpaceException {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
