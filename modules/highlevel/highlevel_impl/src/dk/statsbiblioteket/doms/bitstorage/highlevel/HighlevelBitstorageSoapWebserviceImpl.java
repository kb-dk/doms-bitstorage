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
import dk.statsbiblioteket.doms.bitstorage.highlevel.exceptions.HighlevelException;
import dk.statsbiblioteket.doms.bitstorage.highlevel.exceptions.mappers.CharacteriseToHighlevelExceptionMapper;
import dk.statsbiblioteket.doms.bitstorage.highlevel.exceptions.mappers.FedoraToHighlevelExceptionMapper;
import dk.statsbiblioteket.doms.bitstorage.highlevel.exceptions.mappers.HighlevelExceptionsToSoapFaultsMapper;
import dk.statsbiblioteket.doms.bitstorage.highlevel.exceptions.mappers.LowlevelToHighlevelExceptionMapper;
import dk.statsbiblioteket.doms.bitstorage.highlevel.fedora.*;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.LowlevelSoapException;
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
import javax.xml.ws.WebServiceException;
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

    private HighlevelExceptionsToSoapFaultsMapper highlevelMapper;
    private CharacteriseToHighlevelExceptionMapper
            characMapper;
    private FedoraToHighlevelExceptionMapper fedoraMapper;
    private LowlevelToHighlevelExceptionMapper lowlevelMapper;


    private static final String CONTENTS = "CONTENTS";
    private static final String GOOD = null;
    private static final String CHARACTERISATION = "CHARAC";

    private Logger log;

    private List<Operation> threads;
    private DatatypeFactory dataTypeFactory;


    private synchronized void initialise() throws ConfigException {

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


    public  void  uploadFileToObject(
            @WebParam(name = "pid", targetNamespace = "") String pid,
            @WebParam(name = "filename", targetNamespace = "") String filename,
            @WebParam(name = "filedata", targetNamespace = "")
            DataHandler filedata,
            @WebParam(name = "md5string", targetNamespace = "")
            String md5String,
            @WebParam(name = "filelength", targetNamespace = "")
            long filelength) throws
                             HighlevelSoapException  {
        initialise();
        Operation op = initOperation("Upload");
        try {
            String uploadedURL;
            try {//No rollback here, we have not reached first checkpoint
                op.setFedoraPid(pid);
                op.setFileSize(filelength);

                String message ="Entered uploadFileToObject with params: '"+pid+"', '"+filename+"', '"+md5String+"', '"+filelength+"'.";
                log.trace(message);
                event(op,message);

                uploadedURL = uploadFile(filename, filedata, md5String, filelength, op);
                message = "First checkpoint reached. File is uploaded to lowlevel bitstorage with this url '"+uploadedURL+"'";

                log.debug(message);
                event(op,message);
            } catch (HighlevelException he){
                throw he.convert(highlevelMapper);
            } catch (RuntimeException re){//unexpected error
                throw new WebServiceException(re);
            }
            //Checkpoint here
            try{ //Stuff put in bitstorage, so this must be rolled back
                updateFedora(pid, md5String, uploadedURL);

                log.debug("Fedora datastream created");
                log.debug("Second checkpoint reached. File is in lowlevel and the datastream is in fedora");
            } catch (HighlevelException he){
                rollbackUploadToLowlevel(pid,uploadedURL);
                throw he.convert(highlevelMapper);
            } catch (RuntimeException re){//unexpected error
                rollbackUploadToLowlevel(pid,uploadedURL);
                throw new WebServiceException(re);
            }

            //checkpoint here, fedora updated

            try{
                Characterisation characterisation = characterise(pid);

                log.debug("Get list of formatURIs from Fedora");

                Collection<String> formatURIs =
                        getAllowedFormatURIs(pid, uploadedURL);

                evaluateCharacterisation(pid,
                                         uploadedURL,
                                         characterisation,
                                         formatURIs);
            } catch (HighlevelException he){
                rollbackObjectContentsUpdated(pid,uploadedURL);
                throw he.convert(highlevelMapper);
            } catch (RuntimeException re){//unexpected error
                rollbackObjectContentsUpdated(pid,uploadedURL);
                throw new WebServiceException(re);

            }
            String message = "Third Checkpoint reached. File stored, file object updated. Charac info stored";
            log.debug(message);

            //checkpoint here, charac info stored
        } finally {
            endOperation(op);
        }
    }

    private void evaluateCharacterisation(String pid,
                                          String uploadedURL,
                                          Characterisation characterisation,
                                          Collection<String> formatURIs) throws HighlevelException {
        String message;
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
            String error = "File not accepted by the characteriser. Characterisator output: '"+characterisation.toString()+"'";
            throw new dk.statsbiblioteket.doms.bitstorage.highlevel.exceptions.CharacterisationFailedException(error);
        }else{
            try {
                fedora.storeCharacterization(pid,characterisation);
            } catch (FedoraException e) {
                throw e.convert(fedoraMapper);
            }
        }
    }

    private Collection<String> getAllowedFormatURIs(String pid,
                                                    String uploadedURL) throws

                                                                        HighlevelException {
        Collection<String> formatURIs = null;
        try {
            formatURIs = fedora.getFormatURI(pid, CONTENTS);
        } catch (FedoraException e) {
            throw e.convert(fedoraMapper);
        }
        log.debug(formatURIs);
        return formatURIs;
    }

    private Characterisation characterise(String pid) throws HighlevelException{
        Characterisation characterisation;
        try {
            log.debug("Begin characterisation");
            characterisation = charac.characterise(pid);
            log.debug("File characterised");
        } catch (CharacteriseSoapException e) {
            throw e.convert(characMapper);
        }
        return characterisation;
    }

    private void updateFedora(String pid, String md5String, String uploadedURL)
            throws HighlevelException  {
        try {
            if (fedora.datastreamExists(pid, CONTENTS)) {

                if (fedora.datastreamHasContent(pid, CONTENTS)) {
                    log.error("Fedora object '"+pid+"' already has a '"+CONTENTS+"' datastream with content. Aborting operation rolling back");
                    rollbackUploadToLowlevel(pid,uploadedURL);

                    throw new dk.statsbiblioteket.doms.bitstorage.highlevel.exceptions.FileObjectAlreadyInUseException(
                            "The file object '"+pid+"' is already in use. Pick another file object to upload a file for");
                } else {//no content. Use modifyDatastream
                    log.debug("Fedora object '"+pid+"' alreary has a '"+CONTENTS+"' datastream, but without content so it is replaced.");
                    log.debug("Begin replacing datastream");

                    fedora.replaceContentDatastream(pid,uploadedURL,md5String);
                }
            } else {//Not exist, use createDatastream
                log.debug("Begin creating fedora datastream");
                fedora.createContentDatastream(pid,uploadedURL,md5String);
            }
        } catch (FedoraException e){
            throw e.convert(fedoraMapper);
        }
    }

    private String uploadFile(String filename,
                              DataHandler filedata,
                              String md5String, long filelength, Operation op)
            throws
            HighlevelException {
        String message;
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
        } catch (LowlevelSoapException e) {
            throw e.convert(lowlevelMapper);
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

            message = "Getting fileURL from fedora for object '"+pid+"'";
            log.trace(message);
            event(op,message);
            url = fedora.getFileUrl(pid);
            message = "Gotten url '"+url+"' from fedora";
            log.trace(message);
            event(op,message);
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
                throw new dk.statsbiblioteket.doms.bitstorage.highlevel.exceptions.FileAlreadyApprovedException(message);
            }
        } catch (FedoraException e){
            throw e.convert(fedoraMapper).convert(highlevelMapper);
        } catch (LowlevelSoapException e){
            throw e.convert(lowlevelMapper).convert(highlevelMapper);
        } catch (RuntimeException e){
            throw new WebServiceException(e);
        } catch (HighlevelException e){
            throw e.convert(highlevelMapper);
        }
        finally {
            endOperation(op);
        }

    }

    public void publish(
            @WebParam(name = "pid", targetNamespace = "") String pid)
            throws HighlevelSoapException {


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

            url = fedora.getFileUrl(pid);
            checksum = fedora.getFileChecksum(pid);

            if (!lowlevel.isApproved(url)){
                lowlevel.approve(url,checksum);
            } else{
                //File is already approved
                String message = "The file '"+url+"' is already approved. The object cannot be deleted";
                log.debug(message);
                event(op,message);
            }

        } catch (FedoraException e) {
            throw e.convert(fedoraMapper).convert(highlevelMapper);
        } catch (LowlevelSoapException e){
            throw e.convert(lowlevelMapper).convert(highlevelMapper);
        } catch (RuntimeException e){
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
            event(op,message);
            log.trace(message);

            StatusInformation status = new StatusInformation();

            status.setFreeSpace(lowlevel.spaceleft());
            status.getOperations().addAll(threads);

            return status;
        } catch (LowlevelSoapException e){
            throw e.convert(lowlevelMapper).convert(highlevelMapper);
        } catch (RuntimeException e){
            throw new WebServiceException(e);
        }
        finally {
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
        } catch (LowlevelSoapException e) {
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