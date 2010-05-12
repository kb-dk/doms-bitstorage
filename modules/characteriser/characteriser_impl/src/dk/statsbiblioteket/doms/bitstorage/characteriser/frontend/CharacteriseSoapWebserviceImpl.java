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
import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.soap.MTOMFeature;
import javax.xml.ws.soap.SOAPBinding;
import javax.xml.namespace.QName;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.*;
import java.io.StringWriter;

import com.sun.xml.ws.developer.JAXWSProperties;

/**
 *
 */
@QAInfo(author = "abr", reviewers = "", level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT)
@WebService(endpointInterface = "dk.statsbiblioteket.doms.bitstorage.characteriser.CharacteriseSoapWebservice")
public class CharacteriseSoapWebserviceImpl implements
                                            CharacteriseSoapWebservice {

    /**
     * The logger for this class.
     */
    private static final Log LOG
            = LogFactory.getLog(CharacteriseSoapWebserviceImpl.class);

    private DigitalObjectManager fedora;
    private String username, password, server;

    private List<Identify> identifiers = new ArrayList<Identify>();
    private boolean initialised = false;
    private List<Validate> validators = new ArrayList<Validate>();
    private Map<URI, List<Validate>> validateMap
            = new TreeMap<URI, List<Validate>>();


    private List<String> identifyServiceDescription = new ArrayList<String>();

    private List<String> validateServiceDescription = new ArrayList<String>();
    private JAXBContext jaxbcontext;

    private static final String PROPERTIES_PREFIX
            = "dk.statsbiblioteket.doms.bitstorage.characteriser.";

    private synchronized void initialise() throws ConfigException {
        if (initialised) {
            return;
        }


        try {

            jaxbcontext = JAXBContext.newInstance(ValidateResult.class,
                                                  IdentifyResult.class);
        } catch (JAXBException e) {
            throw new ConfigException("JAXBProblem", e);
        }

        username = ConfigCollection.getProperties().getProperty(
                PROPERTIES_PREFIX + "fedora.username");
        password = ConfigCollection.getProperties().getProperty(
                PROPERTIES_PREFIX + "fedora.password");
        server = ConfigCollection.getProperties().getProperty(
                PROPERTIES_PREFIX + "fedora.server");


        {/**This is the section for reading the wsdls from the web.xml */
            Set<Object> keys = ConfigCollection.getProperties().keySet();

            for (Object key : keys) {
                if (key.toString().startsWith(
                        PROPERTIES_PREFIX + "validatorservice")) {
                    String validatorwsdl
                            = ConfigCollection.getProperties().getProperty(key.toString());
                    try {
                        Service service;
                        service = Service.create(new URL(validatorwsdl),
                                                 new QName(
                                                         "http://planets-project.eu/services/",
                                                         "Validate"));

                        Validate port = service.getPort(Validate.class,
                                                        new MTOMFeature());

                        ((BindingProvider) port).getRequestContext().put(
                                JAXWSProperties.HTTP_CLIENT_STREAMING_CHUNK_SIZE,
                                8096);
                        SOAPBinding binding
                                = (SOAPBinding) ((BindingProvider) port).getBinding();
                        binding.setMTOMEnabled(true);

                        validators.add(port);
                        validateServiceDescription.add(port.describe().toXml());
                    } catch (MalformedURLException e) {
                        throw new ConfigException("Malformed validator urls",
                                                  e);
                    }

                }
            }

            for (Object key : keys) {
                if (key.toString().startsWith(
                        "dk.statsbiblioteket.doms.bitstorage.characteriser.identifierservice")) {
                    String indentifyWSDL
                            = ConfigCollection.getProperties().getProperty(key.toString());
                    try {
                        Service service;
                        service = Service.create(new URL(indentifyWSDL),
                                                 new QName(
                                                         "http://planets-project.eu/services/",
                                                         "Identify"));

                        Identify port = service.getPort(Identify.class,
                                                        new MTOMFeature());

                        ((BindingProvider) port).getRequestContext().put(
                                JAXWSProperties.HTTP_CLIENT_STREAMING_CHUNK_SIZE,
                                8096);
                        SOAPBinding binding
                                = (SOAPBinding) ((BindingProvider) port).getBinding();
                        binding.setMTOMEnabled(true);
                        identifiers.add(port);
                        identifyServiceDescription.add(port.describe().toXml());
                    } catch (MalformedURLException e) {
                        throw new ConfigException("Malformed identifier urls",
                                                  e);
                    }

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

    public Characterisation characterise(
            @WebParam(name = "pid", targetNamespace = "")
            String pid,
            @WebParam(name = "acceptedFormats",
                      targetNamespace = "")
            List<String> acceptedFormats)
            throws
            CharacteriseSoapException,
            FileNotAvailableException {
        String errorMessage = "Trouble while characterising object '" + pid
                              + "'";
        try {

            Characterisation characterisation = new Characterisation();
            characterisation.getIdentifyServiceDescriptions().addAll(
                    identifyServiceDescription);
            characterisation.getValidateServiceDescriptions().addAll(
                    validateServiceDescription);

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

                acceptedFormatsURIs.add(new URI(acceptedFormat));
            }


            FedoraObjectManager fedora = null;
            try {
                fedora = new FedoraObjectManager(username,
                                                 password,
                                                 server);
            } catch (FedoraConnectionException e) {
                throw new CommunicationException("Problem with Fedora",
                                                 "Problem with Fedora",
                                                 e);
            }

            DigitalObject planetsObject = null;
            try {
                planetsObject = fedora.retrieve(pidToURI(pid));
            } catch (DigitalObjectManager.DigitalObjectNotFoundException e) {
                throw new ObjectNotFoundException("Object not found in Fedora",
                                                  "Object not found in Fedora",
                                                  e);
            }

            Marshaller marshaller = jaxbcontext.createMarshaller();

            // 2. Indentify with all identify services.
            List<IdentifyResult> identifyResults
                    = new ArrayList<IdentifyResult>();
            for (Identify identifier : identifiers) {
                IdentifyResult identifyResult = identifier.identify(
                        planetsObject,
                        null);
                identifyResults.add(identifyResult);
                StringWriter writer = new StringWriter();
                marshaller.marshal(identifyResult, writer);
                writer.flush();
                characterisation.getIdentifyServiceReports().add(writer.toString());
            }

            // 3. If the file did not identify to one of the accepted formats, return
            Set<URI> formats = new HashSet<URI>();
            for (IdentifyResult result : identifyResults) {
                formats.addAll(result.getTypes());
            }

            for (URI format : formats) {
                characterisation.getFormatURIs().add(format.toString());
            }

            formats.retainAll(acceptedFormats);

            if (formats.size() > 0) {//good

            } else {
                characterisation.setValidationStatus(
                        "File not identified to be of any of the accepted formats");
                return characterisation;
            }


            // Get most specific format
            Map<URI, Integer> votesForFormat = new HashMap<URI, Integer>();

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
                }//what about ties?
            }

            characterisation.setBestFormat(bestFormat.toString());


            // 4. combine the three lists, to a list of validators
            // 5. Validate with the validators
            List<ValidateResult> validateResults
                    = new ArrayList<ValidateResult>();
            List<ValidateResult> validateResultsForBestFormat
                    = new ArrayList<ValidateResult>();
            for (URI format : formats) {
                List<Validate> validatorsForFormat = validateMap.get(format);
                for (Validate validator : validatorsForFormat) {
                    ValidateResult validateResult
                            = validator.validate(planetsObject, format, null);
                    validateResults.add(validateResult);
                    StringWriter writer = new StringWriter();
                    marshaller.marshal(validateResult, writer);
                    writer.flush();
                    characterisation.getValidateServiceReports().add(writer.toString());
                    if (format.equals(bestFormat)) {
                        validateResultsForBestFormat.add(validateResult);
                    }
                }
            }


            boolean allValid = true;
            for (ValidateResult validateResult : validateResultsForBestFormat) {
                allValid = validateResult.isOfThisFormat()
                           && validateResult.isValidInRegardToThisFormat();
                if (!allValid) {
                    break;
                }
            }
            if (allValid) {
                characterisation.setValidationStatus("Valid");
            } else {
                characterisation.setValidationStatus("Invalid");
            }
            return characterisation;

        } catch (CharacteriseSoapException e) {
            //Rethrow, just so that they are not wrapped as a WebserviceException
            throw e;
        }
        catch (Exception e) {
            LOG.error(errorMessage, e);
            throw new WebServiceException(errorMessage + ": " + e, e);
        }

    }

    private URI pidToURI(String pid) throws URISyntaxException {
        if (pid.startsWith("info:fedora/")) {
            return new URI(pid);
        } else {
            return new URI("info:fedora/" + pid);
        }
    }
}
