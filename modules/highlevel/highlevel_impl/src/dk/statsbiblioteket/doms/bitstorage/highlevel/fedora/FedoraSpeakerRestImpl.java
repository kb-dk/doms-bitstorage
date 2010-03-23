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

package dk.statsbiblioteket.doms.bitstorage.highlevel.fedora;

import dk.statsbiblioteket.doms.bitstorage.characteriser.Characterisation;
import dk.statsbiblioteket.doms.bitstorage.highlevel.UrlProvider;
import dk.statsbiblioteket.doms.bitstorage.highlevel.fedora.exceptions.*;
import dk.statsbiblioteket.doms.bitstorage.highlevel.fedora.generated.*;
import org.apache.http.HttpRequest;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * Created by IntelliJ IDEA.
 * User: abr
 * Date: Jan 20, 2010
 * Time: 9:48:14 AM
 * To change this template use File | Settings | File Templates.
 */
public class FedoraSpeakerRestImpl implements FedoraSpeaker {

    private String contentDatastreamName;
    private String characterisationDatastreamName;


    private Unmarshaller unmarshaller;
    private Marshaller marshaller;

    private FedoraBasicRestSpeaker rest;


    public FedoraSpeakerRestImpl(String contentDatastreamName,
                                 String characterisationDatastreamName,
                                 String username,
                                 String password,
                                 String server,
                                 int port) {
        rest = new FedoraBasicRestSpeaker(username, password, server, port);
        this.contentDatastreamName = contentDatastreamName;
        this.characterisationDatastreamName = characterisationDatastreamName;

    }

    public void createContentDatastream(String pid,
                                        String url,
                                        String checksum)
            throws
            FedoraObjectNotFoundException,
            FedoraDatastreamAlreadyExistException,
            FedoraCommunicationException,
            FedoraChecksumFailedException {

        rest.createExternalDatastream(pid,
                contentDatastreamName,
                url, checksum
        );
    }


    /**
     * Get the list of allowed format uris for this datastram
     *
     * @param pid
     * @param datastream
     * @return
     * @throws FedoraObjectNotFoundException
     * @throws FedoraDatastreamNotFoundException
     *
     * @throws FedoraCommunicationException
     */
    public Collection<String> getFormatURI(String pid,
                                           String datastream)
            throws
            FedoraObjectNotFoundException,
            FedoraDatastreamNotFoundException,
            FedoraCommunicationException {
        ObjectProfile profile = rest.getObjectProfile(pid);
        List<String> cmodels = profile.getObjModels().getModel();

        Set<String> uris = new HashSet<String>();
        for (String cmodel : cmodels) {


            try {
                HttpEntity entity =
                        rest.getDatastreamContents(cmodel,
                                "DS-COMPOSITE-MODEL");
                InputStream stream = entity.getContent();

                Object temp = null;
                try {
                    temp = unmarshaller.unmarshal(stream);
                } catch (JAXBException e) {
                    throw new FedoraCommunicationException(e);
                }
                if (temp instanceof DsCompositeModel) {
                    DsCompositeModel dsCompositeModel = (DsCompositeModel) temp;
                    uris.addAll(extractFormatURIs(dsCompositeModel,
                            datastream));
                }
                entity.consumeContent();
            } catch (IOException e) {
                throw new FedoraCommunicationException(e);
            }
        }
        return uris;

    }

    private Set<String> extractFormatURIs(DsCompositeModel dsCompositeModel,
                                          String datastream) {
        Set<String> uris = new HashSet<String>();
        List<DsTypeModel> typemodels = dsCompositeModel.getDsTypeModel();
        for (DsTypeModel dsTypeModel : typemodels) {
            if (datastream.equals(dsTypeModel.getID())) {
                List<Form> forms = dsTypeModel.getForm();
                for (Form form : forms) {
                    String formaturi = form.getFORMATURI();
                    if (formaturi != null && !formaturi.isEmpty()) {
                        uris.add(formaturi);
                    }
                }
            }
        }
        return uris;
    }


    public void storeCharacterization(String pid,
                                      Characterisation characterisation)
            throws
            FedoraObjectNotFoundException,
            FedoraCommunicationException,
            JAXBException {
        //marshall the charac to a string or url
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        //marshall charac to inputstream
        marshaller.marshal(characterisation, out);
        InputStream blob = new ByteArrayInputStream(out.toByteArray());


        //make blob available as URL
        String characurl;
        try {
            characurl = UrlProvider.registerBlob(blob,
                    characterisationDatastreamName,
                    "text/xml");
        } catch (IOException e) {//not gonna happen
            throw new RuntimeException(
                    "Could not read from ByteArrayInputStream somehow",
                    e);
        }
        rest.createInlineDatastream(pid,
                characterisationDatastreamName,
                characurl
        );
    }


    public boolean datastreamExists(String pid,
                                    String datastream)
            throws FedoraObjectNotFoundException, FedoraCommunicationException {
        pid = FedoraBasicRestSpeaker.sanitize(pid);
        datastream = FedoraBasicRestSpeaker.sanitize(datastream);
        rest.objectExists(pid);

        HttpRequest getDatastream = new HttpGet(
                "/fedora/objects/" + pid + "/datastreams/" + datastream);
        try {
            rest.isOK(getDatastream);
        } catch (ResourceNotFoundException e) {
            return false;
        }
        return true;
    }


    /**
     * TODO what should this method actually do?
     *
     * @param pid
     * @param datastream
     * @return
     * @throws FedoraObjectNotFoundException
     * @throws FedoraDatastreamNotFoundException
     *
     * @throws FedoraCommunicationException
     */
    public boolean datastreamHasContent(String pid,
                                        String datastream)
            throws
            FedoraObjectNotFoundException,
            FedoraDatastreamNotFoundException,
            FedoraCommunicationException {
        HttpRequest getDatastreamContents = new HttpGet(
                "/fedora/objects/" + pid + "/datastreams/" +
                        datastream + "/contents");
        rest.objectExists(pid);
        if (!datastreamExists(pid, datastream)) {
            return false;
        }

        try {
            HttpEntity entity = rest.invoke(getDatastreamContents);
            try {
                entity.consumeContent();
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        } catch (ResourceNotFoundException e) {//Catchall, since the resource MUST exist, except for race conditions
            throw new FedoraCommunicationException(e);
        }
        return true;
    }

    public void deleteDatastream(String pid,
                                 String ds)
            throws
            FedoraObjectNotFoundException,
            FedoraDatastreamNotFoundException,
            FedoraCommunicationException {

        rest.objectExists(pid);
        datastreamExists(pid, ds);
        HttpRequest delete =
                new HttpPost("/fedora/objects/" + pid + "/datastreams/" +
                        ds + "?dsState=D");
        try {
            HttpEntity entity = rest.invoke(delete);
            try {
                entity.consumeContent();
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        } catch (ResourceNotFoundException e) {//Catchall, since the resource MUST exist, except for race conditions
            throw new FedoraCommunicationException(e);
        }
    }

    public String getContentDatastreamName() {
        return contentDatastreamName;
    }

    public String getCharacterisationDatastreamName() {
        return characterisationDatastreamName;
    }

    public String getFileUrl(String pid)
            throws
            FedoraObjectNotFoundException,
            FedoraDatastreamNotFoundException,
            FedoraCommunicationException {
        DatastreamProfile profile =
                rest.getDatastreamProfile(pid, contentDatastreamName);
        return profile.getDsLocation();

    }

    public String getFileChecksum(String pid)
            throws
            FedoraObjectNotFoundException,
            FedoraDatastreamNotFoundException,
            FedoraCommunicationException {

        DatastreamProfile profile =
                rest.getDatastreamProfile(pid, contentDatastreamName);
        return profile.getDsChecksum();
    }


}
