package dk.statsbiblioteket.doms.bitstorage.lowlevel.frontend;


import dk.statsbiblioteket.doms.bitstorage.lowlevel.*;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.Bitstorage;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.BitstorageFactory;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.exceptions.BitstorageException;

import javax.activation.DataHandler;
import javax.annotation.Resource;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.servlet.ServletContext;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.soap.MTOM;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

@MTOM
@WebService(endpointInterface = "dk.statsbiblioteket.doms.bitstorage.lowlevel.BitstorageSoapWebservice")
public class BitstorageSoapWebserviceImpl
        implements BitstorageSoapWebservice {

    Bitstorage bs;


    @Resource
    private WebServiceContext webServiceContext;

    private void initialise() {
        if (bs != null) {
            return;
        }
        ServletContext servletContext =
                (ServletContext) webServiceContext.getMessageContext().get(
                        MessageContext.SERVLET_CONTEXT);

/*
        Enumeration parameters = servletContext.getInitParameterNames();
        while (parameters.hasMoreElements()) {
            String s = (String) parameters.nextElement();
            System.out.println(s);
        }
*/
        String script = servletContext.getInitParameter("script");
        String server = servletContext.getInitParameter("server");
        String bitfinder = servletContext.getInitParameter("bitfinder");
        bs = BitstorageFactory.getInstance(script,server,bitfinder);
    }

    public String uploadFile(@WebParam(name = "filename",
                                       targetNamespace = "") String filename,
                             @WebParam(name = "filedata",
                                       targetNamespace = "") DataHandler filedata,
                             @WebParam(name = "md5string",
                                       targetNamespace = "") String md5String,
                             @WebParam(name = "filelength",
                                       targetNamespace = "") long filelength)
            throws ChecksumFailedException, CommunicationException,
                   FileAlreadyApprovedException, InvalidFilenameException,
                   NotEnoughFreeSpaceException {
        initialise();
/*        StreamingDataHandler dh = (StreamingDataHandler) filedata;*/
        try {

            return bs.upload(filename,
                             filedata.getInputStream(),
                             md5String,
                             filelength).toString();
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
        initialise();
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
        initialise();
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
        initialise();
        try {
            return bs.spaceleft();
        } catch (BitstorageException e) {
            throw ExceptionMapper.convert(e);
        }

    }

    @WebMethod
    public long getMaxFileSize() throws CommunicationException {
        initialise();
        try {
            return bs.getMaxFileSize();
        } catch (BitstorageException e) {
            throw ExceptionMapper.convert(e);
        }
    }

    @WebMethod
    public String getMd5(@WebParam(name = "fileurl",
                                   targetNamespace = "") String fileurl)
            throws CommunicationException, FileNotFoundException {
        initialise();
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
        initialise();
        try {
            return bs.isApproved(new URL(fileurl));
        } catch (BitstorageException e) {
            throw ExceptionMapper.convert(e);
        } catch (MalformedURLException e) {
            throw ExceptionMapper.convertToFileNotFound(e);
        }
    }


}