package dk.statsbiblioteket.doms.bitstorage.highlevel.frontend;


import dk.statsbiblioteket.doms.bitstorage.characteriser.CharacteriseSoapWebservice;
import dk.statsbiblioteket.doms.bitstorage.characteriser.CharacteriseSoapWebserviceService;
import dk.statsbiblioteket.doms.bitstorage.highlevel.*;
import dk.statsbiblioteket.doms.bitstorage.highlevel.BitstorageSoapWebservice;
import dk.statsbiblioteket.doms.bitstorage.highlevel.ChecksumFailedException;
import dk.statsbiblioteket.doms.bitstorage.highlevel.CommunicationException;
import dk.statsbiblioteket.doms.bitstorage.highlevel.FileAlreadyApprovedException;
import dk.statsbiblioteket.doms.bitstorage.highlevel.FileNotFoundException;
import dk.statsbiblioteket.doms.bitstorage.highlevel.InvalidFilenameException;
import dk.statsbiblioteket.doms.bitstorage.highlevel.NotEnoughFreeSpaceException;
import dk.statsbiblioteket.doms.bitstorage.highlevel.backend.FedoraSpeaker;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.*;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.BitstorageSoapWebserviceService;

import javax.activation.DataHandler;
import javax.annotation.Resource;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.servlet.ServletContext;
import javax.xml.namespace.QName;
import javax.xml.ws.Holder;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.soap.MTOM;
import javax.xml.ws.handler.MessageContext;
import java.net.MalformedURLException;
import java.net.URL;

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

    private
    dk.statsbiblioteket.doms.bitstorage.lowlevel.BitstorageSoapWebservice
            lowlevel;
    private boolean lowlevelinitialised = false;
    private CharacteriseSoapWebservice charac;
    private boolean characinitialised = false;
    private FedoraSpeaker fedora;


    private void initialise() throws CommunicationException, ConfigException {
        if (initialised) {
            return;
        }
        initialiseLowLevelConnector();
        initialiseCharacteriserConnector();
        initialised = true;


    }

    private void initialiseLowLevelConnector() throws ConfigException {
        if (lowlevelinitialised) {
            return;
        }
        ServletContext servletContext =
                (ServletContext) webServiceContext.getMessageContext().get(
                        MessageContext.SERVLET_CONTEXT);
        String wsdlloc = servletContext.getInitParameter(
                "dk.statsbiblioteket.doms.bitstorage.lowlevel.location");
        try {
            lowlevel = new BitstorageSoapWebserviceService(
                    new URL(wsdlloc),
                    new QName(
                            "http://lowlevel.bitstorage.doms.statsbiblioteket.dk/",
                            "BitstorageSoapWebserviceService")).getBitstorageSoapWebservicePort();
        } catch (MalformedURLException e) {
            throw new ConfigException(e);
        }


        lowlevelinitialised = true;

    }

    private void initialiseCharacteriserConnector() throws ConfigException {
        if (characinitialised) {
            return;
        }
        ServletContext servletContext =
                (ServletContext) webServiceContext.getMessageContext().get(
                        MessageContext.SERVLET_CONTEXT);
        String wsdlloc = servletContext.getInitParameter(
                "dk.statsbiblioteket.doms.bitstorage.characteriser.location");
        try {
            charac = new CharacteriseSoapWebserviceService(
                    new URL(wsdlloc),
                    new QName(
                            "http://characterise.bitstorage.doms.statsbiblioteket.dk/",
                            "CharacteriseSoapWebserviceService")).getCharacteriseSoapWebservicePort();
        } catch (MalformedURLException e) {
            throw new ConfigException(e);
        }


        characinitialised = true;

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

        boolean createDatastream = true;
        try {
            String uploadedURL = lowlevel.uploadFile(filename,
                                                     filedata,
                                                     md5String,
                                                     filelength);

            if (fedora.datastreamExists(pid, "CONTENTS")) {
                if (fedora.datastreamHasContent(pid, "CONTENTS")) {
                    //error
                } else {

                }
            } else {

            }
            /*
            Pseudocode
              begin

                upload file,
                if datastream exits
                  if file referenced by datastream exists
                    Already uploaded, cause error
                  else
                    modfify datastream
                else
                  create datastream


                Checkpoint: File stable in stage

                Fedora: modify datastream with url

                Checkpoint: Object stable

                Characteriser: Call characteriser with pid

                Fedora: Get format uri from object content model

                Evaluate if format is okay

                if yes, store charac info in fedora

             */

        } catch (dk.statsbiblioteket.doms.bitstorage.lowlevel.ChecksumFailedException e) {
            throw new ChecksumFailedException(e.getMessage(),
                                              "Lowlevel bitstorage failed in matching the checksums",
                                              e);
        } catch (dk.statsbiblioteket.doms.bitstorage.lowlevel.CommunicationException e) {
            throw new CommunicationException(e.getMessage(),
                                             "Error when communicating with lowlevel bitstorage",
                                             e);
        } catch (dk.statsbiblioteket.doms.bitstorage.lowlevel.FileAlreadyApprovedException e) {
            throw new FileAlreadyApprovedException(e.getMessage(),
                                                   "File is already added to bitstorage, find another name",
                                                   e);
        } catch (dk.statsbiblioteket.doms.bitstorage.lowlevel.InvalidFilenameException e) {
            throw new InvalidFilenameException(e.getMessage(),
                                               "The filename is not valid, as it contains illegal characters",
                                               e);
        } catch (dk.statsbiblioteket.doms.bitstorage.lowlevel.NotEnoughFreeSpaceException e) {
            throw new NotEnoughFreeSpaceException(e.getMessage(),
                                                  "There is not enough free space in the lowlevel bitstorage to complete the upload",
                                                  e);
        }


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

    public StatusInformation status() throws CommunicationException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }


}
