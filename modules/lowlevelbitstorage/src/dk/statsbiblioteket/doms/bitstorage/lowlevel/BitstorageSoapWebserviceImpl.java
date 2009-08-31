package dk.statsbiblioteket.doms.bitstorage.lowlevel;


import com.sun.xml.ws.developer.StreamingDataHandler;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.Bitstorage;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.exceptions.BitstorageException;

import javax.activation.DataHandler;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.WebMethod;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.soap.MTOM;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * TODO abr forgot to document this class
 */

@MTOM
@WebService()
public class BitstorageSoapWebserviceImpl
        implements BitstorageSoapWebservice {

    Bitstorage bs;


    public BitstorageSoapWebserviceImpl() {
        bs = BitstorageFactory.getInstance();
    }

    @WebMethod
    public String uploadFile(@WebParam(name = "filename",
                                       targetNamespace = "") String filename,
                             @WebParam(name = "filedata",
                                       targetNamespace = "") DataHandler filedata,
                             @WebParam(name = "md5string",
                                       targetNamespace = "") String md5String)
            throws ChecksumFailedException, CommunicationException,
                   FileAlreadyApprovedException, InvalidFilenameException,
                   NotEnoughFreeSpaceException {
        StreamingDataHandler dh = (StreamingDataHandler) filedata;
        try {
            return bs.upload(filename, dh.readOnce(), md5String).toString();
        } catch (IOException e) {
            throw new WebServiceException(e);
        } catch (BitstorageException e) {
            throw ExceptionMapper.convert(e);
        }

    }

    @WebMethod
    public void disapprove(@WebParam(name = "fileurl",
                                     targetNamespace = "") String fileurl,
                           @WebParam(name = "md5string",
                                     targetNamespace = "") String md5String)
            throws CommunicationException, FileNotFoundException {
        try {
            bs.disapprove(new URL(fileurl), md5String);
        } catch (BitstorageException e) {
            throw ExceptionMapper.convert(e);
        } catch (MalformedURLException e) {
            throw ExceptionMapper.convertToFileNotFound(e);
        }


    }

    @WebMethod
    public void approve(@WebParam(name = "fileurl",
                                  targetNamespace = "") String fileurl,
                        @WebParam(name = "md5string",
                                  targetNamespace = "") String md5String)
            throws ChecksumFailedException, CommunicationException,
                   FileNotFoundException, NotEnoughFreeSpaceException {
        try {
            bs.approve(new URL(fileurl), md5String);
        } catch (BitstorageException e) {
            throw ExceptionMapper.convert(e);
        } catch (MalformedURLException e) {
            throw ExceptionMapper.convertToFileNotFound(e);
        }

    }

    @WebMethod
    public long spaceleft() throws CommunicationException {
        try {
            return bs.spaceleft();
        } catch (BitstorageException e) {
            throw ExceptionMapper.convert(e);
        }

    }

    @WebMethod
    public long getMaxFileSize() throws CommunicationException {
        try {
            return bs.spaceleft();
        } catch (BitstorageException e) {
            throw ExceptionMapper.convert(e);
        }
    }

    @WebMethod
    public String getMd5(@WebParam(name = "fileurl",
                                   targetNamespace = "") String fileurl)
            throws CommunicationException, FileNotFoundException {
        try {
            return bs.getMd5(new URL(fileurl));
        } catch (BitstorageException e) {
            throw ExceptionMapper.convert(e);
        } catch (MalformedURLException e) {
            throw ExceptionMapper.convertToFileNotFound(e);
        }

    }

    @WebMethod
    public boolean isApproved(@WebParam(name = "fileurl",
                                        targetNamespace = "") String fileurl)
            throws CommunicationException, FileNotFoundException {
        try {
            return bs.isApproved(new URL(fileurl));
        } catch (BitstorageException e) {
            throw ExceptionMapper.convert(e);
        } catch (MalformedURLException e) {
            throw ExceptionMapper.convertToFileNotFound(e);
        }
    }
}