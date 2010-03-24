/*
 * $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The DOMS project.
 * Copyright (C) 2007-2010  The State and University Library
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package dk.statsbiblioteket.doms.bitstorage.highlevel;


import dk.statsbiblioteket.doms.bitstorage.characteriser.Characterisation;
import dk.statsbiblioteket.doms.bitstorage.characteriser.CharacteriseSoapException;
import dk.statsbiblioteket.doms.bitstorage.characteriser.CharacteriseSoapWebservice;
import dk.statsbiblioteket.doms.bitstorage.characteriser.CharacteriseSoapWebserviceService;
import dk.statsbiblioteket.doms.bitstorage.highlevel.exceptions.InternalException;
import dk.statsbiblioteket.doms.bitstorage.highlevel.exceptions.mappers.CharacteriseToInternalExceptionMapper;
import dk.statsbiblioteket.doms.bitstorage.highlevel.exceptions.mappers.FedoraToInternalExceptionMapper;
import dk.statsbiblioteket.doms.bitstorage.highlevel.exceptions.mappers.InternalExceptionsToSoapFaultsMapper;
import dk.statsbiblioteket.doms.bitstorage.highlevel.exceptions.mappers.LowlevelToInternalExceptionMapper;
import dk.statsbiblioteket.doms.bitstorage.highlevel.fedora.*;
import dk.statsbiblioteket.doms.bitstorage.highlevel.fedora.exceptions.*;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.LowlevelSoapException;
import dk.statsbiblioteket.doms.webservices.*;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;


import javax.activation.DataHandler;
import javax.annotation.Resource;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.bind.*;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.soap.MTOM;
import javax.servlet.http.HttpServletRequest;
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
@WebService(endpointInterface = "dk.statsbiblioteket.doms.bitstorage.highlevel.HighlevelBitstorageSoapWebservice")
public class HighlevelBitstorageSoapWebserviceImpl
        implements HighlevelBitstorageSoapWebservice {


    private boolean initialised = false;

    private
    dk.statsbiblioteket.doms.bitstorage.lowlevel.LowlevelBitstorageSoapWebservice
            lowlevel;
    private CharacteriseSoapWebservice charac;

    private FedoraSpeaker fedora;

    private InternalExceptionsToSoapFaultsMapper internalMapper;
    private CharacteriseToInternalExceptionMapper
            characMapper;
    private FedoraToInternalExceptionMapper fedoraMapper;
    private LowlevelToInternalExceptionMapper lowlevelMapper;


    private static final String CONTENTS = "CONTENTS";
    private static final String GOOD = null;
    private static final String CHARACTERISATION = "CHARAC";

    private Log log = LogFactory.getLog(HighlevelBitstorageSoapWebserviceImpl.class);

    private static List<Operation> threads = new ArrayList<Operation>();
    private DatatypeFactory dataTypeFactory;

    @Resource
    WebServiceContext context;


    private synchronized void initialise() throws ConfigException {

        if (initialised) {
            return;
        }
        System.setProperty(
                "com.sun.xml.ws.fault.SOAPFaultBuilder.disableCaptureStackTrace",
                "true");

        lowlevelMapper = new LowlevelToInternalExceptionMapper();
        fedoraMapper = new FedoraToInternalExceptionMapper();
        characMapper = new CharacteriseToInternalExceptionMapper();
        internalMapper = new InternalExceptionsToSoapFaultsMapper();


        initialiseLowLevelConnector();
        initialiseCharacteriserConnector();
        initialiseFedoraSpeaker();


        try {
            dataTypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new ConfigException(e);
        }

        if (threads == null) {
            threads = Collections.synchronizedList(new ArrayList<Operation>());
        }
        initialised = true;

    }


    private void initialiseFedoraSpeaker() {

        String server = ConfigCollection.getProperties().getProperty(
                "dk.statsbiblioteket.doms.bitstorage.fedora.server");
        int port = Integer.decode(ConfigCollection.getProperties().getProperty(
                "dk.statsbiblioteket.doms.bitstorage.fedora.port"));
        String charac = ConfigCollection.getProperties().getProperty(
                "dk.statsbiblioteket.doms.bitstorage.fedora.characstream");
        String contents = ConfigCollection.getProperties().getProperty(
                "dk.statsbiblioteket.doms.bitstorage.fedora.contentstream");
        Credentials creds;
        HttpServletRequest request = (HttpServletRequest) context
                .getMessageContext()
                .get(MessageContext.SERVLET_REQUEST);
        creds = (Credentials) request.getAttribute("Credentials");
        if (creds == null) {
            log.warn("Attempted call at Bitstorage without credentials");
            creds = new Credentials("", "");
        }
        fedora = new FedoraSpeakerRestImpl(contents,
                charac,
                creds.getUsername(),
                creds.getPassword(),
                server,
                port);
    }

    private void initialiseLowLevelConnector() throws ConfigException {


        String wsdlloc = ConfigCollection.getProperties().getProperty(
                "dk.statsbiblioteket.doms.bitstorage.lowlevel.location");
        try {
            lowlevel = new dk.statsbiblioteket.doms.bitstorage.lowlevel.LowlevelBitstorageSoapWebserviceService(
                    new URL(wsdlloc),
                    new QName(
                            "http://lowlevel.bitstorage.doms.statsbiblioteket.dk/",
                            "LowlevelBitstorageSoapWebserviceService")).getLowlevelBitstorageSoapWebservicePort();
        } catch (MalformedURLException e) {
            throw new ConfigException(e);
        }


    }

    private void initialiseCharacteriserConnector() throws ConfigException {

        String wsdlloc = ConfigCollection.getProperties().getProperty(
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
            HighlevelSoapException {
        String message = "Entered uploadFileToObject with params: '"
                + pid + "', '"
                + filename + "', '"
                + md5String
                + "', '"
                + filelength + "'.";
        log.trace(message);
        int checkpoint = 0;
        String uploadedURL = "";
        Operation op = null;
        try {
            op = initOperation("Upload");
            try {
                initialise();
                event(op, message);
                op.setFedoraPid(pid);
                op.setFileSize(filelength);


                //No rollback here, we have not reached first checkpoint
                uploadedURL = uploadFile(filename,
                        filedata,
                        md5String,
                        filelength,
                        op);

                checkpoint = 1;
                //Checkpoint here
                message = "First checkpoint reached. File is uploaded to lowlevel bitstorage with this url '" + uploadedURL + "'";
                log.debug(message);
                event(op, message);


                //Stuff put in bitstorage, so this must be rolled back
                updateFedora(pid, md5String, uploadedURL, op);
                //checkpoint here, fedora updated
                checkpoint = 2;

                message = "Fedora datastream created";
                log.debug(message);
                event(op, message);
                message = "Second checkpoint reached. File is in lowlevel and the datastream is in fedora";
                log.debug(message);
                event(op, message);


                Characterisation characterisation = characterise(pid, op);
                message = "Get list of formatURIs from Fedora";
                log.debug(message);
                event(op, message);
                Collection<String> formatURIs =
                        getAllowedFormatURIs(pid, uploadedURL, op);

                evaluateCharacterisation(pid,
                        uploadedURL,
                        characterisation,
                        formatURIs,
                        op);
                checkpoint = 3;
                message = "Third Checkpoint reached. File stored, file object updated. Charac info stored";
                log.debug(message);
                event(op, message);

                //checkpoint here, charac info stored

            } catch (Exception e) {//something unexpected failed down there
                try {
                    rollback(pid, uploadedURL, op, checkpoint);
                } catch (Exception e2) {//failed in the rollback
                    log.error("Failed in rollback", e2);//TODO more verbatim
                }//e2 is logged and discarded, and we continue with the original exception
                if (e instanceof InternalException) {//an exception we know about
                    throw internalMapper.convertMostApplicable((InternalException) e);
                } //something we do not know about
                throw new WebServiceException(e);
            }
        }
        finally {
            endOperation(op);
        }

    }

    private void rollback(String pid, String url, Operation op, int checkpoint)
            throws
            FedoraException,
            LowlevelSoapException {
        String message = "Rolling back for object '" + pid + "' and file '" + url + "'";
        log.debug(message);
        event(op, message);

        if (checkpoint == 3) {//failed in the end, roll everything back
            rollbackCharacAdded(pid, url, op);
        } else if (checkpoint == 2) {
            rollbackObjectContentsUpdated(pid, url, op);
        } else if (checkpoint == 1) {
            rollbackUploadToLowlevel(pid, url, op);
        } else {//no checkpoint reached, no rollback nessesary

        }

    }

    private void evaluateCharacterisation(String pid,
                                          String uploadedURL,
                                          Characterisation characterisation,
                                          Collection<String> formatURIs,
                                          Operation op)
            throws InternalException {
        String message;
        boolean goodfile = true;
        List<String> objectFormats = characterisation.getPronomID();
        if (formatURIs != null) {
            if (formatURIs.containsAll(objectFormats)) {
                //good, allowed type
                if (characterisation.getValidationStatus().equals(GOOD)) {
                    //good, nothing more to care about

                } else { //bad file, something is wrong
                    message = "Characteriser reported the file to be invalid";
                    log.debug(message);
                    event(op, message);

                    goodfile = false;
                }

            } else {//bad, not allowed type
                message = "File to be uploaded is not identified as allowed type";
                log.debug(message);
                event(op, message);
                log.debug(objectFormats);

                goodfile = false;

            }
        }

        if (!goodfile) {
            String error = "File not accepted by the characteriser. Characterisator output: '" + characterisation.toString() + "'";
            throw new InternalException(error,
                    InternalException.Type.CharacterisationFailed);
        } else {
            try {
                message = "Storing characterisation of '" + uploadedURL + "' in '" + pid + "'";
                log.debug(message);
                event(op, message);

                fedora.storeCharacterization(pid, characterisation);

                message = "Characterisation of '" + uploadedURL + "' stored in '" + pid + "'";
                log.debug(message);
                event(op, message);

            } catch (FedoraException e) {
                throw fedoraMapper.convertMostApplicable(e);
            }
        }
    }

    private Collection<String> getAllowedFormatURIs(String pid,
                                                    String uploadedURL,
                                                    Operation op) throws

            InternalException {
        String message = "Getting allowed Format URIs for '" + pid + "'";
        log.trace(message);
        event(op, message);

        Collection<String> formatURIs = null;
        try {
            formatURIs = fedora.getAllowedFormatURIs(pid, CONTENTS);
        } catch (FedoraException e) {
            throw fedoraMapper.convertMostApplicable(e);
        }
        log.debug(formatURIs);
        return formatURIs;
    }

    private Characterisation characterise(String pid,
                                          Operation op)
            throws InternalException {

        try {
            Characterisation characterisation;
            String message = "Begin characterisation";
            log.debug(message);
            event(op, message);


            characterisation = charac.characterise(pid);
            message = "File characterised";
            log.debug(message);
            event(op, message);
            return characterisation;

        } catch (CharacteriseSoapException e) {
            throw characMapper.convertMostApplicable(e);
        }

    }

    private void updateFedora(String pid,
                              String md5String,
                              String uploadedURL,
                              Operation op)
            throws InternalException {
        String message;
        try {
            try {
                log.debug("Begin creating fedora datastream");
                fedora.createContentDatastream(pid, uploadedURL, md5String);
            } catch (FedoraDatastreamAlreadyExistException e) {
                if (fedora.datastreamHasContent(pid, CONTENTS)) {
                    message = "Fedora object '" + pid + "' already has a '" + CONTENTS + "' datastream with content. Aborting operation.";
                    log.error(message);
                    event(op, message);

                    throw new InternalException("The file object '" + pid + "' " +
                            "is already in use. Pick another file object to " +
                            "upload a file for",
                            InternalException.Type.FileObjectAlreadyInUse);
                } else {//no content
                    message = "Fedora object '" + pid + "' alreary has a '" + CONTENTS + "' datastream, but without content so it is replaced.";
                    log.debug(message);
                    event(op, message);
                    fedora.updateContentDatastream(pid, uploadedURL, md5String);
                }
            }

        } catch (FedoraException e) {
            throw fedoraMapper.convertMostApplicable(e);
        }
    }

    private String uploadFile(String filename,
                              DataHandler filedata,
                              String md5String, long filelength, Operation op)
            throws
            InternalException {
        String message;
        String uploadedURL;
        try {
            message = "Begin upload to bitstorage";
            log.trace(message);
            event(op, message);

            uploadedURL = lowlevel.uploadFile(filename,
                    filedata,
                    md5String,
                    filelength);

            message = "Uploaded file to bitstorage, returned url '" +
                    uploadedURL + "'";
            log.trace(message);
            event(op, message);

        } catch (LowlevelSoapException e) {
            throw lowlevelMapper.convertMostApplicable(e);
        }
        return uploadedURL;
    }


    public void delete(@WebParam(name = "pid", targetNamespace = "") String pid)
            throws HighlevelSoapException

    {
        initialise();
        //This method is invoked as a result of deleting a file object. As such, it should not set the file object to deleted.
        Operation op = initOperation("Delete");
        try {
            op.setFedoraPid(pid);

            String url = null;
            String checksum;
            String message;

            message = "Getting fileURL from fedora for object '" + pid + "'";
            log.trace(message);
            event(op, message);

            url = fedora.getFileUrl(pid);

            message = "Gotten url '" + url + "' from fedora";
            log.trace(message);
            event(op, message);

            if (!lowlevel.isApproved(url)) {
                message = "disapproving file '" + url + "' in lowlevel";
                log.trace(message);
                event(op, message);

                lowlevel.disapprove(url);

                message = "disapproved file '" + url + "' in lowlevel";
                log.trace(message);
                event(op, message);

            } else {
                //File is already approved
                message = "The file '" + url + "' is already approved. The object cannot be deleted";
                log.warn(message);
                event(op, message);
                throw new InternalException(message,
                        InternalException.Type.FileAlreadyApproved);
            }
        } catch (FedoraException e) {
            throw internalMapper.convertMostApplicable(fedoraMapper.convertMostApplicable(
                    e));

        } catch (LowlevelSoapException e) {
            throw internalMapper.convertMostApplicable(lowlevelMapper.convertMostApplicable(
                    e));

        } catch (RuntimeException e) {
            throw new WebServiceException(e);
        } catch (InternalException e) {
            throw internalMapper.convertMostApplicable(e);
        }
        finally {
            endOperation(op);
        }

    }

    public void publish(
            @WebParam(name = "pid", targetNamespace = "") String pid)
            throws HighlevelSoapException {

        initialise();
        /*
       Pseudo kode

         This method is invoked as a result of setting an file object to active. As such, it should not set the file object to active

         1. Call lowlevel to publish the file
        */


        Operation op = initOperation("Publish");
        try {
            op.setFedoraPid(pid);

            String url = null;
            String checksum;

            url = fedora.getFileUrl(pid);
            checksum = fedora.getFileChecksum(pid);

            if (!lowlevel.isApproved(url)) {
                lowlevel.approve(url, checksum);
            } else {
                //File is already approved
                String message = "The file '" + url + "' is already approved. The object cannot be deleted";
                log.debug(message);
                event(op, message);
            }

        } catch (FedoraException e) {

            throw internalMapper.convertMostApplicable(fedoraMapper.convertMostApplicable(
                    e));

        } catch (LowlevelSoapException e) {
            throw internalMapper.convertMostApplicable(lowlevelMapper.convertMostApplicable(
                    e));
        }
        catch (RuntimeException e) {
            throw new WebServiceException(e);
        }
        finally {
            endOperation(op);
        }

    }


    public StatusInformation status() throws HighlevelSoapException {
        initialise();
        Operation op = initOperation("Status");
        try {
            String message = "Invoking status()";
            event(op, message);
            log.trace(message);

            StatusInformation status = new StatusInformation();

            status.getOperations().addAll(threads);

            return status;
        } catch (RuntimeException e) {
            throw new WebServiceException(e);
        }
        finally {
            endOperation(op);
        }
    }

    /*Rollback here*/

    private void rollbackUploadToLowlevel(String pid,
                                          String uploadedURL,
                                          Operation op
    ) throws LowlevelSoapException {
        //disapprove file from lowlevel
        //TODO exception handling
        String message = "Rolling back file '" + uploadedURL + "' from lowlevel";
        event(op, message);
        log.debug(message);

        lowlevel.disapprove(uploadedURL);
    }

    private void rollbackObjectContentsUpdated(String pid,
                                               String uploadedURL,
                                               Operation op) throws
            FedoraException, LowlevelSoapException {
        //TODO exception handling
        //mark content stream as deleted
        String message = "Rolling back content stream from  '" + pid + "' from fedora";
        event(op, message);
        log.debug(message);


        fedora.deleteDatastream(pid, CONTENTS);


        rollbackUploadToLowlevel(pid, uploadedURL, op);
    }


    private void rollbackCharacAdded(String pid,
                                     String uploadedURL,
                                     Operation op
    ) throws
            FedoraException, LowlevelSoapException {
        //TODO exception handling
        //remove charac stream
        String message = "Rolling back characterisation stream from  '" + pid + "' from fedora";
        event(op, message);
        log.debug(message);


        fedora.deleteDatastream(pid, CHARACTERISATION);

        rollbackObjectContentsUpdated(pid, uploadedURL, op);

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
        try {
            log.trace(dumpOperation(op));
        } catch (Exception e) {
            log.warn("Caught exception as we tried to dump operation", e);
        } finally {
            threads.remove(op);
        }
    }

    private String dumpOperation(Operation op) {
        java.io.StringWriter sw = new StringWriter();

        JAXBContext jaxbcontext = null;
        try {
            jaxbcontext = JAXBContext.newInstance(
                    "dk.statsbiblioteket.doms.bitstorage.highlevel");
        } catch (JAXBException e) {
            log.error("Cannot create jaxbcontext", e);
            return "";
        }
        Marshaller marshaller = null;
        try {
            marshaller = jaxbcontext.createMarshaller();
        } catch (JAXBException e) {
            log.error("Cannot create jaxb marshaller", e);
            return "";
        }
        try {
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        } catch (PropertyException e) {
            log.error("Cannot set marshaller property", e);
            return "";
        }
        try {
            JAXBElement<Operation> jaxboperation = new ObjectFactory().createOperation(
                    op);
            marshaller.marshal(jaxboperation, sw);
        } catch (JAXBException e) {
            log.error("Cannot marshall operation", e);
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
