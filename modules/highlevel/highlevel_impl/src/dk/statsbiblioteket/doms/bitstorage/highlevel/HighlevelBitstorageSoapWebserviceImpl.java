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
import dk.statsbiblioteket.doms.bitstorage.highlevel.fedora.generated.DatastreamProfile;
import dk.statsbiblioteket.doms.bitstorage.highlevel.fedora.exceptions.*;
import dk.statsbiblioteket.doms.bitstorage.highlevel.status.StaticStatus;
import dk.statsbiblioteket.doms.bitstorage.highlevel.status.Operation;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.LowlevelSoapException;
import dk.statsbiblioteket.doms.webservices.*;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;


import javax.activation.DataHandler;
import javax.annotation.Resource;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.namespace.QName;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.soap.MTOM;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.io.StringWriter;
import java.io.StringReader;

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

    private FedoraSpeakerRestImpl fedora;

    private InternalExceptionsToSoapFaultsMapper internalMapper;
    private CharacteriseToInternalExceptionMapper
            characMapper;
    private FedoraToInternalExceptionMapper fedoraMapper;
    private LowlevelToInternalExceptionMapper lowlevelMapper;


    private static final String GOOD = "valid";


    private static Log log = LogFactory.getLog(
            HighlevelBitstorageSoapWebserviceImpl.class);


    @Resource
    WebServiceContext context;

    private String contents_name;
    private String charac_name;
    private boolean fedoraInitialised = false;
    private boolean lowlevelInitialised = false;


    public HighlevelBitstorageSoapWebserviceImpl() {
        lowlevelMapper = new LowlevelToInternalExceptionMapper();
        fedoraMapper = new FedoraToInternalExceptionMapper();
        characMapper = new CharacteriseToInternalExceptionMapper();
        internalMapper = new InternalExceptionsToSoapFaultsMapper();

        System.setProperty(
                "com.sun.xml.ws.fault.SOAPFaultBuilder.disableCaptureStackTrace",
                "true");


        charac_name = ConfigCollection.getProperties().getProperty(
                "dk.statsbiblioteket.doms.bitstorage.highlevel.fedora.characstream");

        contents_name = ConfigCollection.getProperties().getProperty(
                "dk.statsbiblioteket.doms.bitstorage.highlevel.fedora.contentstream");

    }

    private synchronized void initialiseFedoraSpeaker() throws FedoraException {

        if (fedoraInitialised) {
            return;
        }
        String server;
        server = ConfigCollection.getProperties().getProperty(
                "dk.statsbiblioteket.doms.bitstorage.highlevel.fedora.server");

        Credentials creds;
        HttpServletRequest request = (HttpServletRequest) context
                .getMessageContext()
                .get(MessageContext.SERVLET_REQUEST);
        creds = (Credentials) request.getAttribute("Credentials");
        if (creds == null) {
            log.warn("Attempted call at Bitstorage without credentials");
            creds = new Credentials("", "");
        }
        try {
            fedora = new FedoraSpeakerRestImpl(creds,
                                               server);
        } catch (MalformedURLException e) {
            throw new FedoraException(
                    "Failed to read the fedora location from the config",
                    e);
        }
        fedoraInitialised = true;
    }

    private synchronized void initialiseLowLevelConnector()
            throws ConfigException {

        if (lowlevelInitialised) {
            return;
        }

        String wsdlloc = ConfigCollection.getProperties().getProperty(
                "dk.statsbiblioteket.doms.bitstorage.highlevel.lowlevellocation");
        try {
            lowlevel
                    = new dk.statsbiblioteket.doms.bitstorage.lowlevel.LowlevelBitstorageSoapWebserviceService(
                    new URL(wsdlloc),
                    new QName(
                            "http://lowlevel.bitstorage.doms.statsbiblioteket.dk/",
                            "LowlevelBitstorageSoapWebserviceService")).getLowlevelBitstorageSoapWebservicePort();
        } catch (MalformedURLException e) {
            throw new ConfigException(e);
        }
        lowlevelInitialised = true;

    }

    private void initialiseCharacteriserConnector() throws ConfigException {

        String wsdlloc = ConfigCollection.getProperties().getProperty(
                "dk.statsbiblioteket.doms.bitstorage.highlevel.characteriserlocation");
        try {//TODO credentials
            charac = new CharacteriseSoapWebserviceService(
                    new URL(wsdlloc),
                    new QName(
                            "http://characterise.bitstorage.doms.statsbiblioteket.dk/",
                            "CharacteriseSoapWebserviceService")).getCharacteriseSoapWebservicePort();
        } catch (MalformedURLException e) {
            throw new ConfigException(e);
        }

    }


    public void uploadFileToObjectFromPermanentURLWithCharacterisation(
            @WebParam(name = "pid", targetNamespace = "") String pid,
            @WebParam(name = "filename", targetNamespace = "") String filename,
            @WebParam(name = "permanentURL", targetNamespace = "")
            String permanentURL,
            @WebParam(name = "md5string", targetNamespace = "")
            String md5String,
            @WebParam(name = "filelength", targetNamespace = "")
            long filelength,
            @WebParam(name = "characterisation", targetNamespace = "")
            dk.statsbiblioteket.doms.bitstorage.highlevel.Characterisation characterisation)
            throws
            CharacterisationFailedException,
            ChecksumFailedException,
            CommunicationException,
            FileAlreadyApprovedException,
            FileIsLockedException,
            FileObjectAlreadyInUseException,
            InvalidFilenameException,
            NotEnoughFreeSpaceException,
            ObjectNotFoundException,
            HighlevelSoapException {
        boolean[] checkpoints = createNewCheckpointset();
        String uploadedURL = permanentURL;
        Operation op = null;


        try {
            String message =
                    "Entered uploadFileToObjectFromPermanentURLWithCharacterisation with params: '"
                    + pid + "', '"
                    + filename + "', '"
                    + permanentURL + ", "
                    + md5String
                    + "', '"
                    + filelength + "', "
                    + characterisation.toString() + ".";
            log.trace(message);

            op = StaticStatus.initOperation("Upload");
            try {
                initialiseFedoraSpeaker();
                initialiseLowLevelConnector();

                StaticStatus.event(op, message);
                op.setFedoraPid(pid);
                op.setFileSize(filelength);

                //We have permanent url, so skip checkpoint 1


                //We already have characterisation, so give it to checkpoint2, so that we save a method call later on
                uploadCheckpoint2(pid,
                                  filename,
                                  md5String,
                                  checkpoints,
                                  uploadedURL,
                                  op,
                                  characterisation.getBestFormat());

                //Checkpoint 3
                Characterisation localisedCharac = uploadCheckpoint3(pid,
                                                                     characterisation,
                                                                     checkpoints,
                                                                     uploadedURL,
                                                                     op);
                uploadCheckpoint4(pid, uploadedURL, op, null,
                                  checkpoints);


            } catch (Exception e) {//something unexpected failed down there
                try {
                    rollback(pid, uploadedURL, op, checkpoints);
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
            StaticStatus.endOperation(op);
        }

    }

    private void uploadCheckpoint4(String pid,
                                   String uploadedURL,
                                   Operation op,
                                   Characterisation localisedCharac,
                                   boolean[] checkpoints)
            throws InternalException {

        String message;/* CHECKPOINT 4*/
        message = "Setting the object label to be the URL";
        log.debug(message);
        StaticStatus.event(op, message);
        String message1;
        try {
            initialiseFedoraSpeaker();
            message1 = "Finishing the Fedora object";
            log.debug(message1);
            StaticStatus.event(op, message1);


            message1 = "Setting the object label to be the URL";
            log.debug(message1);
            StaticStatus.event(op, message1);
            fedora.setObjectLabel(pid, uploadedURL);


            if (localisedCharac != null) {
                message1 = "Setting the formatURI of the content datastream";
                log.debug(message1);
                StaticStatus.event(op, message1);

                fedora.setDatastreamFormatURI(pid,
                                              contents_name,
                                              localisedCharac.getBestFormat());
            }
            checkpoints[3] = true;
        } catch (FedoraException e) {
            throw fedoraMapper.convertMostApplicable(e);
        }
    }

    private Characterisation uploadCheckpoint3(String pid,
                                               dk.statsbiblioteket.doms.bitstorage.highlevel.Characterisation characterisation,
                                               boolean[] checkpoints,
                                               String uploadedURL, Operation op)
            throws InternalException {
        String message;/* CHECKPOINT 3*/
        message = "Get list of formatURIs from Fedora";
        log.debug(message);
        StaticStatus.event(op, message);
        Collection<String> formatURIs =
                getAllowedFormatURIs(pid, op);

        Characterisation result;
        try {
            JAXBContext context1
                    = JAXBContext.newInstance(Characterisation.class);
            JAXBContext context2
                    = JAXBContext.newInstance(dk.statsbiblioteket.doms.bitstorage.highlevel.Characterisation.class);
            StringWriter writer = new StringWriter();
            context2.createMarshaller().marshal(characterisation, writer);
            writer.flush();
            StringReader charstring = new StringReader(writer.toString());
            Object convertecChar = context1.createUnmarshaller().unmarshal(
                    charstring);
            result = (Characterisation) convertecChar;
        } catch (JAXBException e) {
            throw new InternalException(e,
                                        InternalException.Type.CharacterisationFailed);

        }

        Characterisation localisedCharac
                = result;
        evaluateCharacterisationAndStore(pid,
                                         uploadedURL,
                                         localisedCharac,
                                         formatURIs,
                                         op);
        checkpoints[2] = true;
        message = "Third Checkpoint reached. File stored, file object"
                  + " updated. Charac info stored";
        log.debug(message);
        StaticStatus.event(op, message);
        //checkpoint here, charac info stored
        return localisedCharac;
    }

    private Characterisation uploadCheckpoint3(String pid,
                                               boolean[] checkpoints,
                                               String uploadedURL, Operation op)
            throws InternalException {
        String message;/* CHECKPOINT 3*/
        message = "Get list of formatURIs from Fedora";
        log.debug(message);
        StaticStatus.event(op, message);
        Collection<String> formatURIs =
                getAllowedFormatURIs(pid, op);


        Characterisation result;

        try {
            initialiseCharacteriserConnector();
            Characterisation characterisation1;
            String message1 = "Begin characterisation";
            log.debug(message1);
            StaticStatus.event(op, message1);

            characterisation1 = charac.characterise(pid,
                                                    null);//TODO not fricking null
            message1 = "File characterised";
            log.debug(message1);
            StaticStatus.event(op, message1);
            result = characterisation1;

        } catch (CharacteriseSoapException e) {
            throw characMapper.convertMostApplicable(e);
        }

        Characterisation characterisation = result;

        evaluateCharacterisationAndStore(pid,
                                         uploadedURL,
                                         characterisation,
                                         formatURIs,
                                         op);
        checkpoints[2] = true;
        message = "Third Checkpoint reached. File stored, file object"
                  + " updated. Charac info stored";
        log.debug(message);
        StaticStatus.event(op, message);
        //checkpoint here, charac info stored
        return characterisation;
    }

    private void uploadCheckpoint2(String pid,
                                   String filename,
                                   String md5String,
                                   boolean[] checkpoints,
                                   String uploadedURL, Operation op,
                                   String formatURI)
            throws InternalException {
        String message;
        try {
            initialiseFedoraSpeaker();

            log.debug("Begin creating fedora datastream");
            fedora.createExternalDatastream(pid,
                                            contents_name,
                                            uploadedURL,
                                            md5String,
                                            filename,
                                            formatURI);
        } catch (ResourceNotFoundException e1) { //TODO
            e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (FedoraAuthenticationException e1) {
            e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (FedoraCommunicationException e1) {
            e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (FedoraException e1) {
            throw fedoraMapper.convertMostApplicable(e1);
        }

        //checkpoint here, fedora updated
        checkpoints[1] = true;
        message = "Fedora datastream created";
        log.debug(message);
        StaticStatus.event(op, message);
        message = "Second checkpoint reached. File is in lowlevel and "
                  + "the datastream is in fedora";
        log.debug(message);
        StaticStatus.event(op, message);
    }

    private String uploadCheckpoint1(String filename,
                                     DataHandler filedata,
                                     String md5String,
                                     long filelength,
                                     boolean[] checkpoints,
                                     Operation op) throws InternalException {

        String uploadedURL1;
        try {
            initialiseLowLevelConnector();
            String message1 = "Begin upload to bitstorage";
            log.trace(message1);
            StaticStatus.event(op, message1);


            uploadedURL1 = lowlevel.uploadFile(filename,
                                               filedata,
                                               md5String,
                                               filelength);

            message1 = "Uploaded file to bitstorage, returned url '" +
                       uploadedURL1 + "'";
            log.trace(message1);
            StaticStatus.event(op, message1);

        } catch (LowlevelSoapException e) {
            throw lowlevelMapper.convertMostApplicable(e);
        }
        String uploadedURL = uploadedURL1;

        checkpoints[0] = true;
        //Checkpoint here
        String message =
                "First checkpoint reached. File is uploaded to lowlevel bitstorage with this url '"
                + uploadedURL + "'";
        log.debug(message);
        StaticStatus.event(op, message);
        return uploadedURL;
    }


    public void uploadFileToObjectWithCharacterisation(
            @WebParam(name = "pid", targetNamespace = "") String pid,
            @WebParam(name = "filename", targetNamespace = "") String filename,
            @WebParam(name = "filedata", targetNamespace = "")
            DataHandler filedata,
            @WebParam(name = "md5string", targetNamespace = "")
            String md5String,
            @WebParam(name = "filelength", targetNamespace = "")
            long filelength,
            @WebParam(name = "characterisation", targetNamespace = "")
            dk.statsbiblioteket.doms.bitstorage.highlevel.Characterisation characterisation)
            throws
            CharacterisationFailedException,
            ChecksumFailedException,
            CommunicationException,
            FileAlreadyApprovedException,
            FileIsLockedException,
            FileObjectAlreadyInUseException,
            InvalidFilenameException,
            NotEnoughFreeSpaceException,
            ObjectNotFoundException,
            HighlevelSoapException {
        boolean[] checkpoints = createNewCheckpointset();
        String uploadedURL = "";
        Operation op = null;
        try {
            String message =
                    "Entered uploadFileToObjectWithCharacterisation with params: '"
                    + pid + "', '"
                    + filename + "', '"
                    + md5String
                    + "', '"
                    + filelength + "', "
                    + characterisation.toString() + ".";
            log.trace(message);

            op = StaticStatus.initOperation("Upload");
            try {

                StaticStatus.event(op, message);
                op.setFedoraPid(pid);
                op.setFileSize(filelength);

                uploadedURL = uploadCheckpoint1(filename,
                                                filedata,
                                                md5String,
                                                filelength,
                                                checkpoints,
                                                op);

                //We have the charac already, so set the FormatURI right away
                uploadCheckpoint2(pid,
                                  filename,
                                  md5String,
                                  checkpoints,
                                  uploadedURL,
                                  op, characterisation.getBestFormat());

                Characterisation localisedCharac = uploadCheckpoint3(pid,
                                                                     characterisation,
                                                                     checkpoints,
                                                                     uploadedURL,
                                                                     op);
                uploadCheckpoint4(pid, uploadedURL, op, null,
                                  checkpoints);

            } catch (Exception e) {//something unexpected failed down there
                try {
                    rollback(pid, uploadedURL, op, checkpoints);
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
            StaticStatus.endOperation(op);
        }


    }

    private boolean[] createNewCheckpointset() {
        return new boolean[]{false, false, false, false};
    }


    public void uploadFileToObjectFromPermanentURL(
            @WebParam(name = "pid", targetNamespace = "") String pid,
            @WebParam(name = "filename", targetNamespace = "") String filename,
            @WebParam(name = "permanentURL", targetNamespace = "")
            String permanentURL,
            @WebParam(name = "md5string", targetNamespace = "")
            String md5String,
            @WebParam(name = "filelength", targetNamespace = "")
            long filelength)
            throws
            CharacterisationFailedException,
            ChecksumFailedException,
            CommunicationException,
            FileAlreadyApprovedException,
            FileIsLockedException,
            FileObjectAlreadyInUseException,
            InvalidFilenameException,
            NotEnoughFreeSpaceException,
            ObjectNotFoundException,
            HighlevelSoapException {
        boolean[] checkpoints = createNewCheckpointset();
        String uploadedURL = permanentURL;
        Operation op = null;
        try {
            String message =
                    "Entered uploadFileToObjectFromPermanentURL with params: '"
                    + pid + "', \n'"
                    + filename + "', \n'"
                    + permanentURL + "', \n'"
                    + md5String + "', \n'"
                    + filelength + "'.\n";
            log.trace(message);
            op = StaticStatus.initOperation("Upload");//TODO?
            try {
                StaticStatus.event(op, message);
                op.setFedoraPid(pid);
                op.setFileSize(filelength);

                uploadCheckpoint2(pid,
                                  filename,
                                  md5String,
                                  checkpoints,
                                  uploadedURL,
                                  op, null);

                Characterisation localisedCharac = uploadCheckpoint3(pid,
                                                                     checkpoints,
                                                                     uploadedURL,
                                                                     op);
                uploadCheckpoint4(pid, uploadedURL, op, localisedCharac,
                                  checkpoints);


            } catch (Exception e) {//something unexpected failed down there
                try {
                    rollback(pid, uploadedURL, op, checkpoints);
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
            StaticStatus.endOperation(op);
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
        boolean[] checkpoints = createNewCheckpointset();
        String uploadedURL = "";
        Operation op = null;
        try {
            String message = "Entered uploadFileToObject with params: '"
                             + pid + "', '"
                             + filename + "', '"
                             + md5String
                             + "', '"
                             + filelength + "'.";
            log.trace(message);

            op = StaticStatus.initOperation("Upload");
            try {

                StaticStatus.event(op, message);
                op.setFedoraPid(pid);
                op.setFileSize(filelength);

                uploadedURL = uploadCheckpoint1(filename,
                                                filedata,
                                                md5String,
                                                filelength,
                                                checkpoints,
                                                op);

                uploadCheckpoint2(pid,
                                  filename,
                                  md5String,
                                  checkpoints,
                                  uploadedURL,
                                  op, null);

                Characterisation localisedCharac = uploadCheckpoint3(pid,
                                                                     checkpoints,
                                                                     uploadedURL,
                                                                     op);
                uploadCheckpoint4(pid, uploadedURL, op, localisedCharac,
                                  checkpoints);

            } catch (Exception e) {//something unexpected failed down there
                try {
                    rollback(pid, uploadedURL, op, checkpoints);
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
            StaticStatus.endOperation(op);
        }

    }

    private void rollback(String pid,
                          String url,
                          Operation op,
                          boolean[] checkpoint)
            throws
            FedoraException,
            LowlevelSoapException {
        String message = "Rolling back for object '" + pid + "' and file '"
                         + url + "'";
        log.debug(message);
        StaticStatus.event(op, message);


        if (checkpoint[2] == true) {
            rollbackCharacAdded(pid, url, op);
        }
        if (checkpoint[1] == true) {
            rollbackObjectContentsUpdated(pid, url, op);
        }
        if (checkpoint[0] == true) {
            rollbackUploadToLowlevel(pid, url, op);
        }
        //no checkpoint reached, no rollback nessesary


    }

    private void evaluateCharacterisationAndStore(String pid,
                                                  String uploadedURL,
                                                  Characterisation characterisation,
                                                  Collection<String> formatURIs,
                                                  Operation op)
            throws InternalException {
        String message;
        boolean goodfile = true;
        List<String> objectFormats = characterisation.getFormatURIs();
        if (formatURIs != null && !formatURIs.isEmpty()) {
            if (formatURIs.containsAll(objectFormats)) {
                //good, allowed type
                if (characterisation.getValidationStatus().equals(GOOD)) {
                    //good, nothing more to care about

                } else { //bad file, something is wrong
                    message = "Characteriser reported the file to be invalid";
                    log.debug(message);
                    StaticStatus.event(op, message);

                    goodfile = false;
                }

            } else {//bad, not allowed type
                message
                        = "File to be uploaded is not identified as allowed type";
                log.debug(message);
                StaticStatus.event(op, message);
                log.debug(objectFormats);

                goodfile = false;

            }
        }

        if (!goodfile) {
            String error =
                    "File not accepted by the characteriser. Characterisator output: '"
                    + characterisation.toString() + "'";
            throw new InternalException(error,
                                        InternalException.Type.CharacterisationFailed);
        } else {
            storeCharacterisation(pid, uploadedURL, characterisation, op);
        }
    }

    private void storeCharacterisation(String pid,
                                       String uploadedURL,
                                       Characterisation characterisation,
                                       Operation op) throws InternalException {
        String message;
        try {
            message = "Storing characterisation of '" + uploadedURL
                      + "' in '" + pid + "'";
            log.debug(message);
            StaticStatus.event(op, message);
            initialiseFedoraSpeaker();

            fedora.createInternalDatastream(pid,
                                            charac_name,
                                            characterisation,
                                            "Characterisation");

            message = "Characterisation of '" + uploadedURL
                      + "' stored in '" + pid + "'";
            log.debug(message);
            StaticStatus.event(op, message);

        } catch (FedoraException e) {
            throw fedoraMapper.convertMostApplicable(e);
        }
    }

    private Collection<String> getAllowedFormatURIs(String pid,
                                                    Operation op) throws

                                                                  InternalException {
        String message = "Getting allowed Format URIs for '" + pid + "'";
        log.trace(message);
        StaticStatus.event(op, message);

        Collection<String> formatURIs = null;
        try {
            initialiseFedoraSpeaker();
            formatURIs = fedora.getAllowedFormatURIs(pid, contents_name);
        } catch (FedoraException e) {
            throw fedoraMapper.convertMostApplicable(e);
        }
        log.debug(formatURIs);
        return formatURIs;
    }


    public void delete(@WebParam(name = "pid", targetNamespace = "") String pid)
            throws HighlevelSoapException

    {
        //This method is invoked as a result of deleting a file object. As such,
        // it should not set the file object to deleted.
        Operation op = StaticStatus.initOperation("Delete");
        try {
            op.setFedoraPid(pid);

            String url = null;
            String checksum;
            String message;

            message = "Getting fileURL from fedora for object '" + pid + "'";
            log.trace(message);
            StaticStatus.event(op, message);

            initialiseFedoraSpeaker();
            url = fedora.getDatastreamProfile(pid,
                                              contents_name).getDsLocation();

            message = "Gotten url '" + url + "' from fedora";
            log.trace(message);
            StaticStatus.event(op, message);

            initialiseLowLevelConnector();
            if (!lowlevel.isApproved(url)) {
                message = "disapproving file '" + url + "' in lowlevel";
                log.trace(message);
                StaticStatus.event(op, message);

                lowlevel.disapprove(url);

                message = "disapproved file '" + url + "' in lowlevel";
                log.trace(message);
                StaticStatus.event(op, message);

            } else {
                //File is already approved
                message = "The file '" + url
                          + "' is already approved. The object cannot be deleted";
                log.warn(message);
                StaticStatus.event(op, message);
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
            StaticStatus.endOperation(op);
        }

    }

    public void publish(
            @WebParam(name = "pid", targetNamespace = "") String pid)
            throws HighlevelSoapException {

        /*
       Pseudo kode

         This method is invoked as a result of setting an file object to active.
          As such, it should not set the file object to active

         1. Call lowlevel to publish the file
        */


        Operation op = StaticStatus.initOperation("Publish");
        try {
            initialiseFedoraSpeaker();
            initialiseLowLevelConnector();

            op.setFedoraPid(pid);
            boolean controlled = fedora.isControlledByLowlevel(pid);
            if (controlled) {
                String url;
                String checksum;

                DatastreamProfile profile = fedora.getDatastreamProfile(pid,
                                                                        contents_name);
                url = profile.getDsLocation();
                checksum = profile.getDsChecksum();


                if (!lowlevel.isApproved(url)) {
                    lowlevel.approve(url, checksum);
                } else {
                    //File is already approved
                    String message = "The file '" + url
                                     + "' is already approved. The object cannot be deleted";
                    log.debug(message);
                    StaticStatus.event(op, message);
                }
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
            StaticStatus.endOperation(op);
        }

    }


    /*Rollback here*/

    private void rollbackUploadToLowlevel(String pid,
                                          String uploadedURL,
                                          Operation op
    ) throws LowlevelSoapException {
        //disapprove file from lowlevel
        //TODO exception handling
        String message = "Rolling back file '" + uploadedURL
                         + "' from lowlevel";
        StaticStatus.event(op, message);
        log.debug(message);

        lowlevel.disapprove(uploadedURL);
    }

    private void rollbackObjectContentsUpdated(String pid,
                                               String uploadedURL,
                                               Operation op) throws
                                                             FedoraException,
                                                             LowlevelSoapException {
        //TODO exception handling
        //mark content stream as deleted
        String message = "Rolling back content stream from  '" + pid
                         + "' from fedora";
        StaticStatus.event(op, message);
        log.debug(message);


        fedora.deleteDatastream(pid, contents_name);

    }


    private void rollbackCharacAdded(String pid,
                                     String uploadedURL,
                                     Operation op
    ) throws
      FedoraException, LowlevelSoapException {
        //TODO exception handling
        //remove charac stream
        String message = "Rolling back characterisation stream from  '" + pid
                         + "' from fedora";
        StaticStatus.event(op, message);
        log.debug(message);


        fedora.deleteDatastream(pid, charac_name);


    }


}
