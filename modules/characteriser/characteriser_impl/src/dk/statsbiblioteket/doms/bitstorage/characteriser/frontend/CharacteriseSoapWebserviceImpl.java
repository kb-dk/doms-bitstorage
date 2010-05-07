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
import eu.planets_project.services.validate.Validate;
import eu.planets_project.services.validate.ValidateResult;
import eu.planets_project.fedora.FedoraObjectManager;
import eu.planets_project.fedora.connector.FedoraConnectionException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.statsbiblioteket.doms.bitstorage.characteriser.*;
import dk.statsbiblioteket.doms.webservices.ConfigCollection;
import dk.statsbiblioteket.util.qa.QAInfo;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.Service;
import javax.xml.namespace.QName;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.*;

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
    private Map<IdentifyResult.Method, List<Identify>> identificationMethods;
    //TODO get identificationMethods from config file. Perhaps in the Initialization?


    private List<Identify> identifiers = new ArrayList<Identify>();
    private boolean initialised = false;
    private List<Validate> validators = new ArrayList<Validate>();
    private Map<URI, List<Validate>> validateMap;

    private void initialise() {
        if (initialised) {
            return;
        }


        Set<Object> keys = ConfigCollection.getProperties().keySet();


        for (Object key : keys) {
            if (key.toString().startsWith(
                    "dk.statsbiblioteket.doms.bitstorage.characteriser.validatorservice")) {
                String validatorwsdl
                        = ConfigCollection.getProperties().getProperty(key.toString());
                try {
                    Validate service = Service.create(new URL(validatorwsdl),
                                                      new QName(
                                                              "http://lowlevel.bitstorage.doms.statsbiblioteket.dk/",
                                                              "LowlevelBitstorageSoapWebserviceService")).getPort(
                            Validate.class);//TODO QNAME, MTOM PROPERTIES
                    validators.add(service);
                } catch (MalformedURLException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }

            }
        }

        for (Object key : keys) {
            if (key.toString().startsWith(
                    "dk.statsbiblioteket.doms.bitstorage.characteriser.identifierservice")) {
                String validatorwsdl
                        = ConfigCollection.getProperties().getProperty(key.toString());
                try {
                    Identify service = Service.create(new URL(validatorwsdl),
                                                      new QName(
                                                              "http://lowlevel.bitstorage.doms.statsbiblioteket.dk/",
                                                              "LowlevelBitstorageSoapWebserviceService")).getPort(
                            Identify.class);//TODO QNAME, MTOM PROPERTIES
                    identifiers.add(service);
                } catch (MalformedURLException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }

            }
        }

        //populate validateMap
        for (Validate validator : validators) {
            ServiceDescription description = validator.describe();
            List<URI> inputformats = description.getInputFormats();
            for (URI informat : inputformats) {
                List<Validate> list = validateMap.get(informat);
                if (list == null) {
                    list = new ArrayList<Validate>();
                    list.add(validator);
                    validateMap.put(informat, list);
                } else {
                    list.add(validator);
                }
            }
        }


        initialised = true;
    }

    /**
     * Retrieve an object and use all available tools in order to identify it.
     * This method degrades on which identification method is used, the
     * following is the ordering that is used, the best method with a result has
     * its result returned.
     * <p/>
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
            @WebParam(name = "pid", targetNamespace = "")
            String pid,
            @WebParam(name = "acceptedFormats",
                      targetNamespace = "")
            List<String> acceptedFormats)
            throws
            CommunicationException,
            FileNotAvailableException,
            CharacteriseSoapException {
        //To change body of implemented methods use File | Settings | File Templates.
        //Design
        // 0. Upon startup, query all validate services about their inputformats
        //Upon invocation
        // 1. Gotten accepted formats as part of invocation
        // 2. Indentify with all identify services.
        // 3. If the file did not identify to one of the accepted formats, return
        // 4. combine the three lists, to a list of validators
        // 5. Validate with the validators
        // 6. return

        // 0. Upon startup, query all validate services about their inputformats
        initialise();

        ArrayList<URI> acceptedFormatsURIs = new ArrayList<URI>();

        for (String acceptedFormat : acceptedFormats) {
            try {
                acceptedFormatsURIs.add(new URI(acceptedFormat));
            } catch (URISyntaxException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }

        String username = null, password = null, server = null;

        FedoraObjectManager fedora = null;
        try {
            fedora = new FedoraObjectManager(username,
                                             password,
                                             server);
        } catch (FedoraConnectionException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        DigitalObject planetsObject = null;
        try {
            planetsObject = fedora.retrieve(pidToURI(pid));
        } catch (DigitalObjectManager.DigitalObjectNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (URISyntaxException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        // 2. Indentify with all identify services.
        List<IdentifyResult> identifyResults = new ArrayList<IdentifyResult>();
        for (Identify identifier : identifiers) {
            IdentifyResult identifyResult = identifier.identify(planetsObject,
                                                                null);
            identifyResults.add(identifyResult);
        }

        // 3. If the file did not identify to one of the accepted formats, return
        Set<URI> formats = new HashSet<URI>();
        for (IdentifyResult result : identifyResults) {
            formats.addAll(result.getTypes());
        }

        formats.retainAll(acceptedFormats);

        if (formats.size() > 0) {//good

        } else {
            return null;
            //TODO return error
        }


        // Get most specific format
        Map<URI, Integer> votesForFormat = null;//TODO

        for (IdentifyResult identifyResult : identifyResults) {
            int increment = identifyResult.getMethod().ordinal() + 1;
            List<URI> identifiedFormats = identifyResult.getTypes();
            for (URI identifiedFormat : identifiedFormats) {
                if (acceptedFormats.contains(identifiedFormat)) {
                    Integer votes = votesForFormat.get(identifiedFormat);
                    if (votes == null) {
                        votes = increment;
                    } else {
                        votes = votes + increment;
                    }
                    votesForFormat.put(identifiedFormat, votes);
                }
            }
        }

        URI bestFormat = null;
        int votes = 0;

        for (Map.Entry<URI, Integer> uriIntegerEntry : votesForFormat.entrySet()) {
            if (uriIntegerEntry.getValue() > votes) {
                votes = uriIntegerEntry.getValue();
                bestFormat = uriIntegerEntry.getKey();
            }
        }


        // 4. combine the three lists, to a list of validators
        // 5. Validate with the validators
        List<ValidateResult> validateResults = new ArrayList<ValidateResult>();
        List<ValidateResult> validateResultsForBestFormat
                = new ArrayList<ValidateResult>();
        for (URI format : formats) {
            List<Validate> validatorsForFormat = validateMap.get(format);
            for (Validate validator : validatorsForFormat) {
                ValidateResult validateResult
                        = validator.validate(planetsObject, format, null);
                validateResults.add(validateResult);
                if (format.equals(bestFormat)) {
                    validateResultsForBestFormat.add(validateResult);
                }
            }
        }


        //Combine results for some return value.

        //1. ServiceDescription from all identify Services
        //2. IdenfifyResults from all identify calls
        //3. ServiceDescription from all Validate services
        //4. ValidateResults from all validate services
        // BestFormat
        // Validate status in regards to this format

        return null;

    }

    private URI pidToURI(String pid) throws URISyntaxException {
        if (pid.startsWith("info:fedora/")) {
            return new URI(pid);
        } else {
            return new URI("info:fedora/" + pid);
        }
    }
}
