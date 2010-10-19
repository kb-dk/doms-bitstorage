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

import com.sun.jersey.api.client.*;
import dk.statsbiblioteket.doms.bitstorage.highlevel.fedora.exceptions.FedoraAuthenticationException;
import dk.statsbiblioteket.doms.bitstorage.highlevel.fedora.exceptions.FedoraCommunicationException;
import dk.statsbiblioteket.doms.bitstorage.highlevel.fedora.exceptions.ResourceNotFoundException;
import dk.statsbiblioteket.doms.bitstorage.highlevel.fedora.generated.*;
import dk.statsbiblioteket.doms.webservices.Base64;
import dk.statsbiblioteket.doms.webservices.Credentials;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.*;


/**
 * Created by IntelliJ IDEA.
 * User: abr
 * Date: Jan 20, 2010
 * Time: 9:48:14 AM
 * To change this template use File | Settings | File Templates.
 */
public class FedoraSpeakerRestImpl {

    private static Log log = LogFactory.getLog(FedoraSpeakerRestImpl.class);

    private Credentials creds;
    private WebResource restApi;

    private static Client client = Client.create();


    public FedoraSpeakerRestImpl(Credentials creds,
                                 String server) throws MalformedURLException {
        this.creds = creds;
        restApi = client.resource(server);
    }


    public Collection<String> getAllowedFormatURIs(String pid,
                                                   String datastream)
            throws
            ResourceNotFoundException,
            FedoraCommunicationException,
            FedoraAuthenticationException {

        //not delegate
        ObjectProfile profile = getObjectProfile(pid);
        List<String> cmodels = profile.getObjModels().getModel();

        Set<String> uris = new HashSet<String>();
        for (String cmodel : cmodels) {
            DsCompositeModel content = null;
            try {
                content = getDatastreamContents(cmodel,
                                                "DS-COMPOSITE-MODEL",
                                                DsCompositeModel.class);
            } catch (ResourceNotFoundException e) {
                // The content model does not exist, or do not have the datastream
                //not technically a problem, so log this instead
                log.debug(
                        "The content model, or the DS-COMPOSITE-MODEL datastream does not exist",
                        e);
            }

            uris.addAll(extractFormatURIs(content,
                                          datastream));
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

    public boolean isControlledByLowlevel(String pid) throws
                                                      ResourceNotFoundException,
                                                      FedoraAuthenticationException,
                                                      FedoraCommunicationException {
        List<String> foundObjects = null;
        try {
            foundObjects = query(URLEncoder.encode(
                    "select $cm from <#ri> "
                    + "where $object <mulgara:is> <info:fedora/" + pid + "> "
                    + "and $object <fedora-model:hasModel> $cm "
                    + "and $cm <http://doms.statsbiblioteket.dk/relations/"
                    + "default/0/1/#isControlledByLowLevelBitstorage> 'false'",
                    "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new ResourceNotFoundException("The server lacks UTF-8 support");
        }
        if (foundObjects.isEmpty()) {
            return true;
        } else {
            return false;
        }
    }


    private String credsAsBase64() {
        String preBase64 = creds.getUsername() + ":" + creds.getPassword();
        String base64 = Base64.encodeBytes(preBase64.getBytes());
        return "Basic " + base64;
    }


    public void deleteDatastream(String pid, String ds)
            throws
            FedoraCommunicationException,
            FedoraAuthenticationException, ResourceNotFoundException {

        try {
            restApi.path("/objects/")
                    .path(sanitize(pid))
                    .path("/datastreams/")
                    .path(sanitize(ds))
                    .queryParam("dsState", "D")
                    .header("Authorization", credsAsBase64())
                    .post();
        } catch (UniformInterfaceException e) {
            if (e.getResponse().getClientResponseStatus().equals(ClientResponse.Status.NOT_FOUND)) {
                throw new ResourceNotFoundException("Resource Not Found", e);
            } else if (e.getResponse().getClientResponseStatus().equals(
                    ClientResponse.Status.UNAUTHORIZED)) {
                throw new ResourceNotFoundException("Resource Not Found", e);
            }
            throw new FedoraCommunicationException("General communication error",
                                                   e);
        }
    }

    /*--------- Statics--------------------*/
    public static String sanitize(String pid) {
        if (pid.startsWith("info:fedora/")) {
            pid = pid.replaceFirst("info:fedora/", "");
        }
        try {
            return URLEncoder.encode(pid, "UTF-8");
        } catch (UnsupportedEncodingException e) {//TODO this should never happen
            throw new Error("UFT8", e);
        }
    }

    /*-------------------Creators--------------------------------*/

    /**
     * Create a new external datastream
     *
     * @param pid      the object pid
     * @param ds       the datastream name
     * @param url      the url to the content
     * @param checksum the checksum of the content
     * @throws FedoraAuthenticationException If the client was not created
     *                                       with sufficient credentials to perform this operation.
     */
    public void createExternalDatastream(String pid,
                                         String ds,
                                         String url,
                                         String checksum,
                                         String label) throws
                                                       FedoraCommunicationException,
                                                       FedoraAuthenticationException,
                                                       ResourceNotFoundException {


        WebResource temp = null;
        try {
            temp = restApi.path("/objects/")
                    .path(sanitize(pid))
                    .path("/datastreams/")
                    .path(sanitize(ds))
                    .queryParam("controlGroup", "R")
                    .queryParam("dsLocation",
                                URLEncoder.encode(url, "UTF-8"))
                    .queryParam("dsLabel", label)
                    .queryParam("dsState", "A")
                    .queryParam("mimeType", "application/octet-stream");


        } catch (UnsupportedEncodingException e) {
            throw new Error("UTF8", e);
        }

        if (checksum == null || checksum.length() == 0) {


        } else {
            temp = temp.queryParam("checksumType", "md5")
                    .queryParam("checksum", checksum);

        }
        try {
            temp.header("Authorization", credsAsBase64()).post();
        } catch (UniformInterfaceException e) {
            if (e.getResponse().getClientResponseStatus().equals(ClientResponse.Status.NOT_FOUND)) {
                throw new ResourceNotFoundException("Resource Not Found", e);
            } else if (e.getResponse().getClientResponseStatus().equals(
                    ClientResponse.Status.UNAUTHORIZED)) {
                throw new FedoraAuthenticationException("Invalid credentials",
                                                        e);
            }
            throw new FedoraCommunicationException("General communication error",
                                                   e);

        }

    }


    public <T> void createInternalDatastream(String pid,
                                             String ds,
                                             T contents,
                                             String label)
            throws
            FedoraCommunicationException,
            FedoraAuthenticationException,
            ResourceNotFoundException {

        WebResource temp = null;

        temp = restApi.path("/objects/")
                .path(sanitize(pid))
                .path("/datastreams/")
                .path(sanitize(ds))
                .queryParam("dsLabel", label)
                .queryParam("dsState", "A")
                .queryParam("mimeType", "text/xml");

        try {
            temp.header("Authorization", credsAsBase64()).post(contents);
        } catch (UniformInterfaceException e) {
            if (e.getResponse().getClientResponseStatus().equals(ClientResponse.Status.NOT_FOUND)) {
                throw new ResourceNotFoundException("Resource Not Found", e);
            } else if (e.getResponse().getClientResponseStatus().equals(
                    ClientResponse.Status.UNAUTHORIZED)) {
                throw new FedoraAuthenticationException("Invalid credentials",
                                                        e);
            }
            throw new FedoraCommunicationException("General communication error",
                                                   e);

        }

    }

    public DatastreamProfile getDatastreamProfile(String pid,
                                                  String datastreamname
    )
            throws
            FedoraCommunicationException,
            FedoraAuthenticationException,
            ResourceNotFoundException {
        try {
            DatastreamProfile profile = restApi.path("/objects/")
                    .path(sanitize(pid))
                    .path("/datastreams/")
                    .path(sanitize(datastreamname))
                    .queryParam("format", "xml")
                    .header("Authorization", credsAsBase64())
                    .get(DatastreamProfile.class);
            return profile;
        } catch (UniformInterfaceException e) {
            if (e.getResponse().getClientResponseStatus().equals(ClientResponse.Status.NOT_FOUND)) {
                throw new ResourceNotFoundException("Resource Not Found", e);
            } else if (e.getResponse().getClientResponseStatus().equals(
                    ClientResponse.Status.UNAUTHORIZED)) {
                throw new FedoraAuthenticationException("Invalid credentials",
                                                        e);
            }
            throw new FedoraCommunicationException("General communication error",
                                                   e);
        }
    }

    public ObjectProfile getObjectProfile(String pid)
            throws
            FedoraCommunicationException,
            ResourceNotFoundException,
            FedoraAuthenticationException {
        try {
            ObjectProfile profile = restApi.path("/objects/")
                    .path(sanitize(pid))
                    .queryParam("format", "xml")
                    .header("Authorization", credsAsBase64())
                    .get(ObjectProfile.class);
            return profile;
        } catch (UniformInterfaceException e) {
            if (e.getResponse().getClientResponseStatus().equals(ClientResponse.Status.NOT_FOUND)) {
                throw new ResourceNotFoundException("Resource Not Found", e);
            } else if (e.getResponse().getClientResponseStatus().equals(
                    ClientResponse.Status.UNAUTHORIZED)) {
                throw new FedoraAuthenticationException("Invalid credentials",
                                                        e);
            }
            throw new FedoraCommunicationException("General communication error",
                                                   e);
        }
    }

    public <T> T getDatastreamContents(String pid,
                                       String datastream,
                                       Class<T> returnvalue)
            throws
            FedoraCommunicationException,
            FedoraAuthenticationException,
            ResourceNotFoundException {
        try {
            T contents = restApi.path("/objects/")
                    .path(sanitize(pid))
                    .path("/datastreams/")
                    .path(sanitize(datastream))
                    .path("/content")
                    .header("Authorization", credsAsBase64())
                    .get(returnvalue);
            return contents;
        } catch (UniformInterfaceException e) {
            if (e.getResponse().getClientResponseStatus().equals(ClientResponse.Status.NOT_FOUND)) {
                throw new ResourceNotFoundException("Resource Not Found", e);
            } else if (e.getResponse().getClientResponseStatus().equals(
                    ClientResponse.Status.UNAUTHORIZED)) {
                throw new FedoraAuthenticationException("Invalid credentials",
                                                        e);
            }
            throw new FedoraCommunicationException("General communication error",
                                                   e);
        }
    }


    public void setObjectLabel(String pid, String label) throws
                                                         ResourceNotFoundException,
                                                         FedoraAuthenticationException,
                                                         FedoraCommunicationException {
        try {
            restApi.path("/objects/")
                    .path(sanitize(pid))
                    .queryParam("label", label)
                    .header("Authorization", credsAsBase64())
                    .put();
        } catch (UniformInterfaceException e) {
            if (e.getResponse().getClientResponseStatus().equals(ClientResponse.Status.NOT_FOUND)) {
                throw new ResourceNotFoundException("Resource Not Found", e);
            } else if (e.getResponse().getClientResponseStatus().equals(
                    ClientResponse.Status.UNAUTHORIZED)) {
                throw new FedoraAuthenticationException("Invalid credentials",
                                                        e);
            }
            throw new FedoraCommunicationException("General communication error",
                                                   e);
        }
    }

    public void setDatastreamFormatURI(String pid,
                                       String datastream,
                                       String formatURI)
            throws FedoraCommunicationException,
                   FedoraAuthenticationException,
                   ResourceNotFoundException {
        try {
            restApi.path("/objects/")
                    .path(sanitize(pid))
                    .path("/datastreams/")
                    .path(sanitize(datastream))
                    .queryParam("formatURI", formatURI)
                    .header("Authorization", credsAsBase64())
                    .put();
        } catch (UniformInterfaceException e) {
            if (e.getResponse().getClientResponseStatus().equals(ClientResponse.Status.NOT_FOUND)) {
                throw new ResourceNotFoundException("Resource Not Found", e);
            } else if (e.getResponse().getClientResponseStatus().equals(
                    ClientResponse.Status.UNAUTHORIZED)) {
                throw new FedoraAuthenticationException("Invalid credentials",
                                                        e);
            }
            throw new FedoraCommunicationException("General communication error",
                                                   e);
        }
    }

    public List<String> query(String query) throws
                                            FedoraCommunicationException,
                                            ResourceNotFoundException,
                                            FedoraAuthenticationException {
        try {
            String objects = restApi.path("/risearch")
                    .queryParam("type", "tuples")
                    .queryParam("lang", "iTQL")
                    .queryParam("stream", "on")
                    .queryParam("flush", "true")
                    .queryParam("format", "CSV")
                    .queryParam("query", query)
                    .header("Authorization", credsAsBase64())
                    .post(String.class);

            String[] lines = objects.split("\n");
            List<String> foundobjects = new ArrayList<String>();
            for (String line : lines) {
                if (line.startsWith("\"")) {
                    continue;
                }
                line = line.replaceAll("info:fedora/", "");

                foundobjects.add(line);
            }
            return foundobjects;

        } catch (UniformInterfaceException e) {
            if (e.getResponse().getClientResponseStatus().equals(ClientResponse.Status.NOT_FOUND)) {
                throw new ResourceNotFoundException("Resource Not Found", e);
            } else if (e.getResponse().getClientResponseStatus().equals(
                    ClientResponse.Status.UNAUTHORIZED)) {
                throw new FedoraAuthenticationException("Invalid credentials",
                                                        e);
            }
            throw new FedoraCommunicationException("General communication error",
                                                   e);
        }
    }

}
