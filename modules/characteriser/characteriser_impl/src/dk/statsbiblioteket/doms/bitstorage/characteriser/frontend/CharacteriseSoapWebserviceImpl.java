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

package dk.statsbiblioteket.doms.bitstorage.characteriser.frontend;

import eu.planets_project.ifr.core.storage.api.DigitalObjectManager;
import eu.planets_project.services.datatypes.DigitalObject;
import eu.planets_project.services.datatypes.ServiceDescription;
import eu.planets_project.services.identify.Identify;
import eu.planets_project.services.identify.IdentifyResult;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.statsbiblioteket.doms.bitstorage.characteriser.Characterisation;
import dk.statsbiblioteket.doms.bitstorage.characteriser.CharacteriseSoapWebservice;
import dk.statsbiblioteket.doms.bitstorage.characteriser.CommunicationException;
import dk.statsbiblioteket.doms.bitstorage.characteriser.FileNotAvailableException;
import dk.statsbiblioteket.doms.webservices.ConfigCollection;
import dk.statsbiblioteket.util.qa.QAInfo;

import javax.jws.WebParam;
import javax.jws.WebService;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
@QAInfo(author = "eab", reviewers = "", level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_OK)
@WebService(endpointInterface = "dk.statsbiblioteket.doms.bitstorage.characteriser.CharacteriseSoapWebservice")
public class CharacteriseSoapWebserviceImpl implements
        CharacteriseSoapWebservice {

    Log log = LogFactory.getLog(getClass());    // Log for the class
    private DigitalObjectManager fedora;
    private Map<IdentifyResult.Method, List<Identify>> identificationMethods; //TODO get identificationMethods from config file. Perhaps in the Initialization?
    private boolean initialised = false;


    private void initialise() {
        if (initialised) {
            return;
        }
        
        // TODO actual implementation of getting setup and tools.
        String testparam = ConfigCollection.getProperties().getProperty("testParam");

        initialised = true;
    }

    /**
     * Retrieve an object and use all available tools in order to identify it.
     * This method degrades on which identification method is used, the
     * following is the ordering that is used, the best method with a result has
     * its result returned.
     *
     * The order of identification methods are:
     * 1)  Full Parse
     * 2)  Partial Parse
     * 3)  Magic
     * 4)  Metadata
     * 5)  Extension
     * 6)  Other 
     *  
     * @param pid of the object being identified
     * @return the characterisation of the object pertaining to the PID
     * @throws CommunicationException
     * @throws FileNotAvailableException if there is no file relating to the PID
     */
    public Characterisation characterise(
            @WebParam(name = "pid",
                    targetNamespace = "http://characteriser.bitstorage.doms.statsbiblioteket.dk/",
                    partName = "pid")
            String pid)
            throws CommunicationException, FileNotAvailableException {
        initialise();

        Characterisation returnCharacterisation = new Characterisation();
        returnCharacterisation.setMd5CheckSum(""); // MD5sum is not needed.
        DigitalObject studiedObject = null;
        URI uri = null;

        try {
            uri = new URI(pid);
            studiedObject = fedora.retrieve(uri);
        } catch (URISyntaxException e) {
            String message = "URISyntax failex with exception: " + e;
            log.error(message, e);
            // TODO Error Handling
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (DigitalObjectManager.DigitalObjectNotFoundException e) {
            String message = "Digital object not found: " + e;
            log.error(message, e);
            // TODO Error Handling
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        Map<ServiceDescription, IdentifyResult> identificationResult;
        identificationResult = new HashMap();
        Map<IdentifyResult.Method, Map<ServiceDescription, IdentifyResult>>
                identificationMethodResult = new HashMap();
        Map<IdentifyResult.Method, List<URI>> resultMap = new HashMap();
        Collection methods = identificationMethods.keySet();

        for (Object method : methods) {
            List<Identify> identificationTools
                    = identificationMethods.get((IdentifyResult.Method) method);

            for (Identify identifier : identificationTools) {
                IdentifyResult objectIdentity
                        = identifier.identify(studiedObject, null);
                ServiceDescription toolDescription = identifier.describe();
                identificationResult.put(toolDescription,
                        objectIdentity);
                resultMap.put(objectIdentity.getMethod(),
                              objectIdentity.getTypes());
            }
            identificationMethodResult.put((IdentifyResult.Method) method,
                    identificationResult);
        }

        List<String> pronomIDs;
        String validationStatus;
        /**
         * Use the higest valued method, returning results, to set the values
         * that make up the returnCharacterisation values.
         */
        if (resultMap.get(IdentifyResult.Method.FULL_PARSE).size()>0){
            pronomIDs = convertURI(resultMap.get(
                    IdentifyResult.Method.FULL_PARSE));
            validationStatus = "FULL_PARSE";
            identificationResult = identificationMethodResult.get(
                    IdentifyResult.Method.FULL_PARSE);
        } else if (resultMap.get(IdentifyResult.Method.PARTIAL_PARSE).size()>0){
            pronomIDs = convertURI(resultMap.get(
                    IdentifyResult.Method.PARTIAL_PARSE));
            validationStatus = "PARTIAL_PARSE";
            identificationResult = identificationMethodResult.get(
                    IdentifyResult.Method.PARTIAL_PARSE);
        } else if (resultMap.get(IdentifyResult.Method.MAGIC).size()>0){
            pronomIDs = convertURI(resultMap.get(IdentifyResult.Method.MAGIC));
            validationStatus = "MAGIC";
            identificationResult = identificationMethodResult.get(
                    IdentifyResult.Method.MAGIC);
        } else if (resultMap.get(IdentifyResult.Method.METADATA).size()>0){
            pronomIDs = convertURI(resultMap.get(
                    IdentifyResult.Method.METADATA));
            validationStatus = "METADATA";
            identificationResult = identificationMethodResult.get(
                    IdentifyResult.Method.METADATA);
        } else if (resultMap.get(IdentifyResult.Method.EXTENSION).size()>0){
            pronomIDs = convertURI(resultMap.get(
                    IdentifyResult.Method.EXTENSION));
            validationStatus = "EXTENSION";
            identificationResult = identificationMethodResult.get(
                    IdentifyResult.Method.EXTENSION);
        } else if (resultMap.get(IdentifyResult.Method.OTHER).size()>0){
            pronomIDs = convertURI(resultMap.get(IdentifyResult.Method.OTHER));
            validationStatus = "OTHER";
            identificationResult = identificationMethodResult.get(
                    IdentifyResult.Method.OTHER);
        } else {
            pronomIDs = null;
            validationStatus = "VALIDATION_FAILED"; // TODO verify that this will at the very least throw an error
            identificationResult = null;
        }
        
        returnCharacterisation.getPronomID().addAll(pronomIDs);
        returnCharacterisation.setValidationStatus(validationStatus);
// TODO adapt Characterisation.java to reflect below line 
//        returnCharacterisation.getIdentificationToolResults().putAll(identificationResult);
        return returnCharacterisation;
    }

    private List<String> convertURI(Collection<URI> uris) {
        List<String> retList = new ArrayList<String>();
        for (URI tmpUri : uris){
            retList.add(tmpUri.toString());
        }
        return retList;
    }

}
