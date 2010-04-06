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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.statsbiblioteket.doms.bitstorage.characteriser.Characterisation;
import dk.statsbiblioteket.doms.bitstorage.characteriser.CharacteriseSoapWebservice;
import dk.statsbiblioteket.doms.bitstorage.characteriser.CommunicationException;
import dk.statsbiblioteket.doms.bitstorage.characteriser.FileNotAvailableException;
import dk.statsbiblioteket.doms.webservices.ConfigCollection;

import javax.jws.WebParam;
import javax.jws.WebService;


import eu.planets_project.services.identify.Identify;
import eu.planets_project.services.identify.IdentifyResult;
import eu.planets_project.services.datatypes.DigitalObject;
import eu.planets_project.ifr.core.storage.api.DigitalObjectManager;
import eu.planets_project.fedora.FedoraObjectManager;
import eu.planets_project.fedora.connector.FedoraConnectionException;


import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: abr
 * Date: Dec 1, 2009
 * Time: 2:39:13 PM
 * To change this template use File | Settings | File Templates.
 */
@WebService(endpointInterface = "dk.statsbiblioteket.doms.bitstorage.characteriser.CharacteriseSoapWebservice")
public class CharacteriseSoapWebserviceImpl implements
        CharacteriseSoapWebservice {
    /**
     * The log for this class.
     */
    Log log = LogFactory.getLog(getClass());
/*    String fedoraUrl = "";
    String fedoraUser = "";
    String fedoraUserPassword = "";
*/
    private Identify identifier;

    private DigitalObjectManager fedora;

    private List<Identify> identifiers; //TODO get identifiers from config file. Perhaps in the Initialization?

    private boolean initialised = false;

//    @PostConstruct

    //>>>>>>> .r190
    private void initialise() {
        if (initialised) {
            return;
        }
//<<<<<<< .mine
//        ServletContext servletContext =
//                (ServletContext) webServiceContext.getMessageContext().get(
//                        MessageContext.SERVLET_CONTEXT);
//        String testparam = servletContext.getInitParameter("testParam");
//        initialised = true;
//
//        Service service = Service.create(wsdlLocation, interfaceName);
//        /* Enable streaming, if supported by the service: */
//        @SuppressWarnings("unchecked") T ids = (T) service.getPort(implementationClass.getInterfaces()[0],
//                new MTOMFeature());
//        ((BindingProvider) ids).getRequestContext().put(JAXWSProperties.HTTP_CLIENT_STREAMING_CHUNK_SIZE, 8096);
//        SOAPBinding binding = (SOAPBinding) ((BindingProvider) ids).getBinding();
//        binding.setMTOMEnabled(true);
//


//=======

        String testparam = ConfigCollection.getProperties().getProperty("testParam");
        initialised = true;

//>>>>>>> .r190
    }

    /**
     *
     * @param pid
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
        returnCharacterisation.setMd5CheckSum(""); // TODO Explain why; because it's not needed.
        DigitalObject studiedObject = null;
        URI uri = null;
        List<IdentifyResult> results = new ArrayList<IdentifyResult>();
        try {
            uri = new URI(pid);
            studiedObject = fedora.retrieve(uri);
        } catch (URISyntaxException e) {
            String message = "URISyntax failex with exception: "+e;
            log.error(message, e);
            // TODO Error Handling
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (DigitalObjectManager.DigitalObjectNotFoundException e) {
            String message = "Digital object not found: "+e;
            log.error(message, e);
            // TODO Error Handling
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        for (Identify identifier : identifiers){
                results.add(identifier.identify(studiedObject, null));
        }

        Map<IdentifyResult.Method, List<IdentifyResult>> resultMap = null;
        for (IdentifyResult identifiedResult : results){
            resultMap.put(identifiedResult.getMethod(), results);
        }



//TODO        results = identifiedResult

        for (IdentifyResult result: results){
            List<IdentifyResult> item = resultMap.get(result.getMethod());
            item.add(result);
        }
        Set<URI> pronomIDs = new HashSet<URI>();
        List<IdentifyResult> full_list = resultMap.get(IdentifyResult.Method.FULL_PARSE);
        List<IdentifyResult> partial_list = resultMap.get(IdentifyResult.Method.PARTIAL_PARSE);
        List<IdentifyResult> magic_list = resultMap.get(IdentifyResult.Method.MAGIC);
        List<IdentifyResult> meta_list = resultMap.get(IdentifyResult.Method.METADATA);
        List<IdentifyResult> ext_list = resultMap.get(IdentifyResult.Method.EXTENSION);
        if(full_list.size() > 0){

            for(IdentifyResult result: full_list){
                pronomIDs.addAll(result.getTypes());
            }
        }else if (partial_list.size() > 0){

            for(IdentifyResult result: partial_list){
                pronomIDs.addAll(result.getTypes());
            }
        }else if(magic_list.size() > 0){

            for(IdentifyResult result: magic_list){
                pronomIDs.addAll(result.getTypes());
            }
        }else if(meta_list.size() > 0){

            for(IdentifyResult result: meta_list){
                pronomIDs.addAll(result.getTypes());
            }
        }else if(ext_list.size() > 0){

            for(IdentifyResult result: ext_list){
                pronomIDs.addAll(result.getTypes());
            }
        }
        if(pronomIDs.size()<1){
            //here there be dragons
            //file is not identified
        }
        //Characterisation characterization = new Characterisation();
        List<String> characterization = outPutListAsStrings(pronomIDs);


        //characterise()
        returnCharacterisation.getPronomID().addAll((Collection) pronomIDs);
        if (returnCharacterisation.getPronomID().size() > 0){
            // TODO omskriv denne OG characterization til at have en fornuftig validationStatus evt til en boolean og en liste af valideringer
            returnCharacterisation.setValidationStatus("Validated");
        }
        return returnCharacterisation;  //To change body of implemented methods use File | Settings | File Templates.
    }

/*

    private List<String> outPutListAsStrings(Characterisation list) {
        ArrayList<String> outputList = new ArrayList<String>();
        for(Object o:list){
            outputList.add(o.toString());
        }
        return outputList;
    }
*/


    private List<String> outPutListAsStrings(Set<URI> inputList) {
        ArrayList<String> outputList = new ArrayList<String>();
        for (Object o : inputList) {
            outputList.add(o.toString());
        }
        return outputList;
    }
}
