package dk.statsbiblioteket.doms.bitstorage.highlevel;


import dk.statsbiblioteket.doms.bitstorage.characteriser.Characterisation;
import dk.statsbiblioteket.doms.bitstorage.characteriser.CharacteriseSoapWebservice;
import dk.statsbiblioteket.doms.bitstorage.characteriser.CharacteriseSoapWebserviceService;
import dk.statsbiblioteket.doms.bitstorage.characteriser.FileNotAvailableException;
import dk.statsbiblioteket.doms.bitstorage.highlevel.fedora.*;
import org.apache.log4j.Logger;

import javax.activation.DataHandler;
import javax.annotation.Resource;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.servlet.ServletContext;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.soap.MTOM;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: abr
 * Date: Dec 1, 2009
 * Time: 2:12:14 PM
 * To change this template use File | Settings | File Templates.
 */
@MTOM
@WebService(endpointInterface = "dk.statsbiblioteket.doms.bitstorage.highlevel.BitstorageSoapWebservice")
public class HighlevelBitstorageSoapWebserviceImpl implements HighlevelBitstorageSoapWebservice {

    @Resource
    private WebServiceContext webServiceContext;


    private boolean initialised = false;

    private
    dk.statsbiblioteket.doms.bitstorage.lowlevel.LowlevelBitstorageSoapWebservice
            lowlevel;
    private CharacteriseSoapWebservice charac;

    private FedoraSpeaker fedora;

    private static final String CONTENTS = "CONTENTS";
    private static final String GOOD = null;
    private static final String CHARACTERISATION = "CHARAC";

    private Logger log;

    private List<Operation> threads;
    private DatatypeFactory dataTypeFactory;


    private synchronized void initialise() throws
                                           CommunicationException, ConfigException {

        if (initialised) {
            return;
        }
        //TODO init logger
        initialiseLowLevelConnector();
        initialiseCharacteriserConnector();
        //TODO init GOOD

        initialiseFedoraSpeaker();
        //TODO init CONTENTS and CHARAC

        try {
            dataTypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new ConfigException(e);
        }

        if (threads == null){
            threads = Collections.synchronizedList(new ArrayList<Operation>());
        }
        initialised = true;

    }

    private void initialiseFedoraSpeaker() {
        //TODO
        //To change body of created methods use File | Settings | File Templates.
    }

    private void initialiseLowLevelConnector() throws ConfigException {

        //TODO
        ServletContext servletContext =
                (ServletContext) webServiceContext.getMessageContext().get(
                        MessageContext.SERVLET_CONTEXT);
        String wsdlloc = servletContext.getInitParameter(
                "dk.statsbiblioteket.doms.bitstorage.lowlevel.location");
        try {
            lowlevel = new dk.statsbiblioteket.doms.bitstorage.lowlevel.LowlevelBitstorageSoapWebserviceService(
                    new URL(wsdlloc),
                    new QName(
                            "http://lowlevel.bitstorage.doms.statsbiblioteket.dk/",
                            "BitstorageSoapWebserviceService")).getLowlevelBitstorageSoapWebservicePort();
        } catch (MalformedURLException e) {
            throw new ConfigException(e);
        }



    }

    private void initialiseCharacteriserConnector() throws ConfigException {

        //TODO
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
                             dk.statsbiblioteket.doms.bitstorage.highlevel.ChecksumFailedException,
                             dk.statsbiblioteket.doms.bitstorage.highlevel.CommunicationException,
                             dk.statsbiblioteket.doms.bitstorage.highlevel.FileAlreadyApprovedException,
                             dk.statsbiblioteket.doms.bitstorage.highlevel.InvalidFilenameException,
                             dk.statsbiblioteket.doms.bitstorage.highlevel.NotEnoughFreeSpaceException,
                             dk.statsbiblioteket.doms.bitstorage.highlevel.FileObjectAlreadyInUseException,
                             dk.statsbiblioteket.doms.bitstorage.highlevel.CharacterisationFailedException,
                             dk.statsbiblioteket.doms.bitstorage.highlevel.ObjectNotFoundException {
        initialise();
        Operation op = initOperation("Upload");
        try {
            op.setFedoraPid(pid);
            op.setFileSize(filelength);

            String message ="Entered uploadFileToObject with params: '"+pid+"', '"+filename+"', '"+md5String+"', '"+filelength+"'.";
            log.trace(message);
            event(op,message);
            String uploadedURL;
            try{
                message = "Begin upload to bitstorage";
                log.trace(message);
                event(op,message);
                uploadedURL = lowlevel.uploadFile(filename,
                                                  filedata,
                                                  md5String,
                                                  filelength);
                message = "Uploaded file to bitstorage, returned url '" +
                          uploadedURL + "'";
                log.trace(message);
                event(op,message);
            } catch (dk.statsbiblioteket.doms.bitstorage.lowlevel.ChecksumFailedException e) {
                log.error("Lowlevel bitstorage failed in matching the checksums",e);
                //TODO events

                throw new dk.statsbiblioteket.doms.bitstorage.highlevel.ChecksumFailedException(e.getMessage(),
                                                                                                "Lowlevel bitstorage failed in matching the checksums",
                                                                                                e);
            } catch (dk.statsbiblioteket.doms.bitstorage.lowlevel.CommunicationException e) {
                log.error("Error when communicating with lowlevel bitstorage",e);
                throw new dk.statsbiblioteket.doms.bitstorage.highlevel.CommunicationException(e.getMessage(),
                                                 "Error when communicating with lowlevel bitstorage",
                                                 e);
            } catch (dk.statsbiblioteket.doms.bitstorage.lowlevel.FileAlreadyApprovedException e) {
                log.error("File is already added to bitstorage, find another name",e);
                throw new dk.statsbiblioteket.doms.bitstorage.highlevel.FileAlreadyApprovedException(e.getMessage(),
                                                       "File is already added to bitstorage, find another name",
                                                       e);
            } catch (dk.statsbiblioteket.doms.bitstorage.lowlevel.InvalidFilenameException e) {
                log.error("The filename is not valid, as it contains illegal characters",e);
                throw new dk.statsbiblioteket.doms.bitstorage.highlevel.InvalidFilenameException(e.getMessage(),
                                                   "The filename is not valid, as it contains illegal characters",
                                                   e);
            } catch (dk.statsbiblioteket.doms.bitstorage.lowlevel.NotEnoughFreeSpaceException e) {
                log.error("There is not enough free space in the lowlevel bitstorage to complete the upload",e);
                throw new dk.statsbiblioteket.doms.bitstorage.highlevel.NotEnoughFreeSpaceException(e.getMessage(),
                                                      "There is not enough free space in the lowlevel bitstorage to complete the upload",
                                                      e);
            }
            message = "First checkpoint reached. File is uploaded to lowlevel bitstorage with this url '"+uploadedURL+"'";

            log.debug(message);
            event(op,message);
            //Checkpoint here

            try {
                if (fedora.datastreamExists(pid, CONTENTS)) {
                    try {
                        if (fedora.datastreamHasContent(pid, CONTENTS)) {
                            log.error("Fedora object '"+pid+"' already has a '"+CONTENTS+"' datastream with content. Aborting operation rolling back");
                            rollbackUploadToLowlevel(pid,uploadedURL);

                            throw new dk.statsbiblioteket.doms.bitstorage.highlevel.FileObjectAlreadyInUseException("FileObjectAlreadyInUse",
                                                                      "The file object '"+pid+"' is already in use. Pick another file object to upload a file for");
                        } else {//no content. Use modifyDatastream
                            log.debug("Fedora object '"+pid+"' alreary has a '"+CONTENTS+"' datastream, but without content so it is replaced.");
                            log.debug("Begin replacing datastream");
                            try {
                                fedora.replaceContentDatastream(pid,uploadedURL,md5String);
                            } catch (FedoraChecksumFailedException e) {
                                String error = "File checksum '"+md5String+"' failed in Fedora, aborting upload. Rolling back";
                                log.error(error,e);
                                rollbackUploadToLowlevel(pid,uploadedURL);
                                throw new dk.statsbiblioteket.doms.bitstorage.highlevel.ChecksumFailedException(e.getMessage(),error,e);
                            }
                        }
                    } catch (FedoraDatastreamNotFoundException e) {
                        //We just checked that the datastream existed....
                        //TODO
                    }
                } else {//Not exist, use createDatastream
                    log.debug("Begin creating fedora datastream");
                    try {
                        fedora.createContentDatastream(pid,uploadedURL,md5String);
                    } catch (FedoraDatastreamAlreadyExistException e) {
                        //we just checked that the datastream did not exist....
                        //TODO
                    } catch (FedoraChecksumFailedException e) {
                        String error = "File checksum '"+md5String+"' failed in Fedora, aborting upload. Rolling back";
                        log.error(error,e);
                        rollbackUploadToLowlevel(pid,uploadedURL);
                        throw new dk.statsbiblioteket.doms.bitstorage.highlevel.ChecksumFailedException(e.getMessage(),error,e);
                    }
                }
            } catch (FedoraObjectNotFoundException e) {
                String error = "Attempted to add file to object '"+pid+"' that does not exist. Rolling back";
                log.error(error);
                rollbackUploadToLowlevel(pid,uploadedURL);
                throw new dk.statsbiblioteket.doms.bitstorage.highlevel.ObjectNotFoundException(e.getMessage(),error,e);
            } catch (FedoraCommunicationException e) {
                String error = "Problem communicating with Fedora. Aborting ingest. Rolling back";
                log.error(error);
                rollbackUploadToLowlevel(pid,uploadedURL);
                throw new dk.statsbiblioteket.doms.bitstorage.highlevel.CommunicationException(e.getMessage(),error,e);
            }
            log.debug("Fedora datastream created");
            log.debug("Second checkpoint reached. File is in lowlevel and the datastream is in fedora");
            //checkpoint here, fedora updated



            Characterisation characterisation;
            try {
                log.debug("Begin characterisation");
                characterisation = charac.characterise(pid);
                log.debug("File characterised");
            } catch (dk.statsbiblioteket.doms.bitstorage.characteriser.CommunicationException e) {
                log.error("Problem communicating with the characteriser. Rolling back",e);
                rollbackObjectContentsUpdated(pid,uploadedURL);
                throw new dk.statsbiblioteket.doms.bitstorage.highlevel.CommunicationException(e.getMessage(),"Error communicating with the characterisation service, file is removed from bitststorage",e);
            } catch (FileNotAvailableException e) {
                log.error("Characteriser cannot get file from Fedora. Rolling back",e);
                rollbackObjectContentsUpdated(pid,uploadedURL);
                throw new dk.statsbiblioteket.doms.bitstorage.highlevel.CommunicationException(e.getMessage(),"Characteriser cannot get file from Fedora. Rolling back",e);
            }

            log.debug("Get list of formatURIs from Fedora");
            Collection<String> formatURIs = null;
            try {
                formatURIs = fedora.getFormatURI(pid, CONTENTS);
            } catch (FedoraObjectNotFoundException e) {
                String error = "Attempted to add file to object '"+pid+"' that does not exist. Somewhat weird, as previous calls worked on the object. Perhaps someone deleted it while it was used?. Rolling back";
                log.error(error,e);
                rollbackObjectContentsUpdated(pid,uploadedURL);
                throw new dk.statsbiblioteket.doms.bitstorage.highlevel.ObjectNotFoundException(e.getMessage(),error,e);
            } catch (FedoraDatastreamNotFoundException e) {//We have just uploaded the file to this datastream. How can it not be there?
                //TODO
            } catch (FedoraCommunicationException e) {
                String error = "Problem communicating with Fedora. Aborting ingest. Rolling back";
                log.error(error,e);
                rollbackObjectContentsUpdated(pid,uploadedURL);
                throw new dk.statsbiblioteket.doms.bitstorage.highlevel.CommunicationException(e.getMessage(),error,e);

            }
            log.debug(formatURIs);

            boolean goodfile = true;
            List<String> objectFormats = characterisation.getPronomID();
            if (formatURIs != null) {
                if (formatURIs.containsAll(objectFormats)){
                    //good, allowed type
                    if (characterisation.getValidationStatus().equals(GOOD)){
                        //good, nothing more to care about

                    } else { //bad file, something is wrong
                        log.debug("Characteriser reported the file to be invalid");
                        goodfile = false;
                    }

                } else {//bad, not allowed type
                    log.debug("File to be uploaded is not identified as allowed type");
                    log.debug(objectFormats);
                    goodfile = false;

                }
            }

            if (!goodfile){
                String error = "File not accepted by the characteriser. Rolling back. Characterisator output: '"+characterisation.toString()+"'";
                log.error(error);
                rollbackObjectContentsUpdated(pid,uploadedURL);
                throw new dk.statsbiblioteket.doms.bitstorage.highlevel.CharacterisationFailedException(error,error);
            }else{
                try {
                    fedora.storeCharacterization(pid,characterisation);
                } catch (FedoraObjectNotFoundException e) {
                    String error = "Attempted to add file to object '"+pid+"' that does not exist. Somewhat weird, as previous calls worked on the object. Perhaps someone deleted it while it was used?. Rolling back";
                    log.error(error,e);
                    rollbackObjectContentsUpdated(pid,uploadedURL);
                    throw new dk.statsbiblioteket.doms.bitstorage.highlevel.ObjectNotFoundException(e.getMessage(),error,e);
                } catch (FedoraCommunicationException e) {
                    String error = "Problem communicating with Fedora. Aborting ingest. Rolling back";
                    log.error(error,e);
                    rollbackObjectContentsUpdated(pid,uploadedURL);
                    throw new dk.statsbiblioteket.doms.bitstorage.highlevel.CommunicationException(e.getMessage(),error,e);
                }
            }
            message = "Third Checkpoint reached. File stored, file object updated. Charac info stored";
            log.debug(message);
            //checkpoint here, charac info stored
        } finally {
            endOperation(op);
        }
    }








    public void delete(@WebParam(name = "pid", targetNamespace = "") String pid)
            throws
            dk.statsbiblioteket.doms.bitstorage.highlevel.CommunicationException,
            dk.statsbiblioteket.doms.bitstorage.highlevel.FileNotFoundException,
            dk.statsbiblioteket.doms.bitstorage.highlevel.ObjectNotFoundException,
            dk.statsbiblioteket.doms.bitstorage.highlevel.FileAlreadyApprovedException {
        initialise();
        //This method is invoked as a result of deleting a file object. As such, it should not set the file object to deleted.
        Operation op = initOperation("Delete");
        try {
            op.setFedoraPid(pid);

            String url = null;
            String checksum;
            String message;
            try {
                message = "Getting fileURL from fedora for object '"+pid+"'";
                log.trace(message);
                event(op,message);
                url = fedora.getFileUrl(pid);
                message = "Gotten url '"+url+"' from fedora";
                log.trace(message);
                event(op,message);
            } catch (FedoraObjectNotFoundException e) {
                message = "Object '"+pid+"' to publish is not found";
                log.error(message,e);
                event(op,message);
                throw new dk.statsbiblioteket.doms.bitstorage.highlevel.ObjectNotFoundException(e.getMessage(),message,e);
            } catch (FedoraDatastreamNotFoundException e) {
                message = "Object '"+pid+"' to publish has no "+CONTENTS+" datastream";
                log.error(message,e);
                event(op,message);
                throw new dk.statsbiblioteket.doms.bitstorage.highlevel.ObjectNotFoundException(e.getMessage(),message,e);

            } catch (FedoraCommunicationException e) {
                message = "Problem communicating with Fedora.";
                log.error(message,e);
                event(op,message);
                throw new dk.statsbiblioteket.doms.bitstorage.highlevel.CommunicationException(e.getMessage(),message,e);
            }
            try {
                if (!lowlevel.isApproved(url)){
                    message = "disapproving file '"+url+"' in lowlevel";
                    log.trace(message);
                    event(op,message);
                    lowlevel.disapprove(url);
                    message = "disapproved file '"+url+"' in lowlevel";
                    log.trace(message);
                    event(op,message);
                } else{
                    //File is already approved
                    message = "The file '"+url+"' is already approved. The object cannot be deleted";
                    log.warn(message);
                    event(op,message);
                    throw new dk.statsbiblioteket.doms.bitstorage.highlevel.FileAlreadyApprovedException(message,message);
                }
            }  catch (dk.statsbiblioteket.doms.bitstorage.lowlevel.CommunicationException e) {
                message = "Problem communicating with Lowlevel.";
                log.error(message,e);
                event(op,message);
                throw new dk.statsbiblioteket.doms.bitstorage.highlevel.CommunicationException(e.getMessage(),message,e);
            } catch (dk.statsbiblioteket.doms.bitstorage.lowlevel.FileNotFoundException e) {
                message = "File is not found in lowlevel bitstorage";
                log.error(message,e);
                event(op,message);
                throw new dk.statsbiblioteket.doms.bitstorage.highlevel.FileNotFoundException(e.getMessage(),message,e);
            } catch (dk.statsbiblioteket.doms.bitstorage.lowlevel.FileAlreadyApprovedException e) {
                //TODO should not be possible to happen
                message = "This should not happen";
                log.error(message,e);
                event(op,message);
                throw new dk.statsbiblioteket.doms.bitstorage.highlevel.FileAlreadyApprovedException(message,message,e);
            }
        } finally {
            endOperation(op);
        }

    }

    public void publish(
            @WebParam(name = "pid", targetNamespace = "") String pid)
            throws
            dk.statsbiblioteket.doms.bitstorage.highlevel.ChecksumFailedException,
            dk.statsbiblioteket.doms.bitstorage.highlevel.CommunicationException,
            dk.statsbiblioteket.doms.bitstorage.highlevel.FileNotFoundException,
            dk.statsbiblioteket.doms.bitstorage.highlevel.NotEnoughFreeSpaceException,
            dk.statsbiblioteket.doms.bitstorage.highlevel.ObjectNotFoundException,
            dk.statsbiblioteket.doms.bitstorage.highlevel.FileAlreadyApprovedException {


        /*
       Pseudo kode

         This method is invoked as a result of setting an file object to active. As such, it should not set the file object to active

         1. Call lowlevel to publish the file
        */


        initialise();
        Operation op = initOperation("Publish");
        try {
            op.setFedoraPid(pid);

            String url = null;
            String checksum;
            try {
                url = fedora.getFileUrl(pid);
                checksum = fedora.getFileChecksum(pid);

            } catch (FedoraObjectNotFoundException e) {
                String message = "Object '"+pid+"' to publish is not found";
                log.error(message,e);
                event(op,message);
                throw new dk.statsbiblioteket.doms.bitstorage.highlevel.ObjectNotFoundException(e.getMessage(),message,e);
            } catch (FedoraDatastreamNotFoundException e) {
                String message = "Object '"+pid+"' to publish has no "+CONTENTS+" datastream";
                log.error(message,e);
                event(op,message);
                throw new dk.statsbiblioteket.doms.bitstorage.highlevel.ObjectNotFoundException(e.getMessage(),message,e);

            } catch (FedoraCommunicationException e) {
                String message = "Problem communicating with Fedora.";
                log.error(message,e);
                event(op,message);
                throw new dk.statsbiblioteket.doms.bitstorage.highlevel.CommunicationException(e.getMessage(),message,e);
            }
            try {
                if (!lowlevel.isApproved(url)){
                    lowlevel.approve(url,checksum);
                } else{
                    //File is already approved
                    String message = "The file '"+url+"' is already approved. The object cannot be deleted";
                    log.debug(message);
                    event(op,message);
                }

            } catch (dk.statsbiblioteket.doms.bitstorage.lowlevel.ChecksumFailedException e) {
                String message = "Checksum disagreement between lowlevel and Fedora";
                log.error(message,e);
                event(op,message);
                throw new dk.statsbiblioteket.doms.bitstorage.highlevel.ChecksumFailedException(e.getMessage(),message,e);
            } catch (dk.statsbiblioteket.doms.bitstorage.lowlevel.CommunicationException e) {
                String message = "Problem communicating with Lowlevel.";
                log.error(message,e);
                event(op,message);
                throw new dk.statsbiblioteket.doms.bitstorage.highlevel.CommunicationException(e.getMessage(),message,e);
            } catch (dk.statsbiblioteket.doms.bitstorage.lowlevel.FileNotFoundException e) {
                String message = "File is not found in lowlevel bitstorage";
                log.error(message,e);
                event(op,message);
                throw new dk.statsbiblioteket.doms.bitstorage.highlevel.FileNotFoundException(e.getMessage(),message,e);
            } catch (dk.statsbiblioteket.doms.bitstorage.lowlevel.NotEnoughFreeSpaceException e) {
                String message = "Not enough Free Space in lowlevel to complete the operation";
                log.error(message,e);
                event(op,message);
                throw new dk.statsbiblioteket.doms.bitstorage.highlevel.NotEnoughFreeSpaceException(e.getMessage(),message,e);
            } catch (dk.statsbiblioteket.doms.bitstorage.lowlevel.FileAlreadyApprovedException e) {
                //TODO should not be possible to happen
                String message = "This should not happen";
                log.error(message,e);
                event(op,message);
                throw new dk.statsbiblioteket.doms.bitstorage.highlevel.FileAlreadyApprovedException(message,message,e);

            }
        } finally {
            endOperation(op);
        }

    }


    public StatusInformation status() throws dk.statsbiblioteket.doms.bitstorage.highlevel.CommunicationException {
        initialise();
        Operation op = initOperation("Status");
        try {
            String message = "Invoking status()";
            event(op,message);
            log.trace(message);

            StatusInformation status = new StatusInformation();
            try {
                status.setFreeSpace(lowlevel.spaceleft());
            } catch (dk.statsbiblioteket.doms.bitstorage.lowlevel.CommunicationException e) {
                message = "Communication error when communicating with lowlevel";
                log.error(message,e);
                event(op,message);
                throw new CommunicationException(e.getMessage(),message,e);
            }
            status.getOperations().addAll(threads);

            return status;
        } finally {
            endOperation(op);
        }

    }

    /*Rollback here*/

    private void rollbackUploadToLowlevel(String pid,
                                          String uploadedURL
    ){
        //disapprove file from lowlevel
        //TODO exception handling
        try {
            lowlevel.disapprove(uploadedURL);
        } catch (dk.statsbiblioteket.doms.bitstorage.lowlevel.CommunicationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (dk.statsbiblioteket.doms.bitstorage.lowlevel.FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (dk.statsbiblioteket.doms.bitstorage.lowlevel.FileAlreadyApprovedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private void rollbackObjectContentsUpdated(String pid,
                                               String uploadedURL
    ){
        //TODO exception handling
        //mark content stream as deleted
        try {
            fedora.deleteDatastream(pid,CONTENTS);
        } catch (FedoraObjectNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (FedoraDatastreamNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (FedoraCommunicationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        rollbackUploadToLowlevel(pid,uploadedURL);
    }


    private void rollbackCharacAdded(String pid,
                                     String uploadedURL
    ){
        //TODO exception handling
        //remove charac stream
        try {
            fedora.deleteDatastream(pid,CHARACTERISATION);
        } catch (FedoraObjectNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (FedoraDatastreamNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (FedoraCommunicationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        rollbackObjectContentsUpdated(pid,uploadedURL);

    }




    /*Operation handling below here*/


    private Operation initOperation(String s) {
        Operation op = new Operation();
        op.setID(UUID.randomUUID().toString());
        op.setHighlevelMethod(s);
        threads.add(op);
        return op;
    }

    private void endOperation(Operation op) {
        log.trace(dumpOperation(op));
        threads.remove(op);
    }

    private String dumpOperation(Operation op) {
        java.io.StringWriter sw = new StringWriter();
        JAXBContext jaxbcontext =
                null;
        try {
            jaxbcontext = JAXBContext.newInstance(Operation.class, Event.class);
        } catch (JAXBException e) {
            log.error("Cannot create jaxbcontext",e);
            return "";
        }
        Marshaller marshaller = null;
        try {
            marshaller = jaxbcontext.createMarshaller();
        } catch (JAXBException e) {
            log.error("Cannot create jaxb marshaller",e);
            return "";
        }
        try {
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        } catch (PropertyException e) {
            log.error("Cannot set marshaller property",e);
            return "";
        }
        try {
            marshaller.marshal(op, sw);
        } catch (JAXBException e) {
            log.error("Cannot marshall operation",e);
            return "";
        }
        return sw.toString();
    }

    private void event(Operation operation, String message) {
        Event event = new Event();

        XMLGregorianCalendar now = dataTypeFactory.newXMLGregorianCalendar(new GregorianCalendar());
        event.setWhen(now);
        event.setWhat(message);
        operation.getHistory().add(event);
    }


}
