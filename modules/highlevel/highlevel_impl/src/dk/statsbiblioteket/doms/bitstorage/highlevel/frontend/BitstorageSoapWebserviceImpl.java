package dk.statsbiblioteket.doms.bitstorage.highlevel.frontend;


import dk.statsbiblioteket.doms.bitstorage.highlevel.*;

import javax.activation.DataHandler;
import javax.annotation.Resource;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.servlet.ServletContext;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.soap.MTOM;
import javax.xml.ws.handler.MessageContext;

/**
 * Created by IntelliJ IDEA.
 * User: abr
 * Date: Dec 1, 2009
 * Time: 2:12:14 PM
 * To change this template use File | Settings | File Templates.
 */
@MTOM
@WebService(endpointInterface = "dk.statsbiblioteket.doms.bitstorage.highlevel.BitstorageSoapWebservice")
public class BitstorageSoapWebserviceImpl implements BitstorageSoapWebservice {

    @Resource
    private WebServiceContext webServiceContext;

    private boolean initialised = false;

    private void initialise(){
        if (initialised){
            return;
        }
        ServletContext servletContext =
                (ServletContext) webServiceContext.getMessageContext().get(
                        MessageContext.SERVLET_CONTEXT);
        String testparam = servletContext.getInitParameter("testParam");
        initialised = true;

    }



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
        initialise();
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void delete(@WebParam(name = "pid", targetNamespace = "") String pid)
            throws CommunicationException, FileNotFoundException {
        initialise();
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void publish(
            @WebParam(name = "pid", targetNamespace = "") String pid)
            throws
            ChecksumFailedException,
            CommunicationException,
            FileNotFoundException,
            NotEnoughFreeSpaceException {
        initialise();
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
