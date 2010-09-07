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
import dk.statsbiblioteket.doms.bitstorage.highlevel.fedora.exceptions.*;
import dk.statsbiblioteket.doms.bitstorage.highlevel.fedora.generated.DatastreamProfile;
import dk.statsbiblioteket.doms.bitstorage.highlevel.fedora.generated.ObjectProfile;
import org.apache.http.*;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.params.HttpParams;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.AbortableHttpRequest;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;


import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.io.StringReader;
import java.net.URLEncoder;

/**
 * This is the basic REST speaker. It should act sort of like the Fedora Client
 * in that it gives a java interface to Fedora, which other tools can use.
 */
public class FedoraBasicRestSpeaker {

    /**
     * The username under which these operations should be performed
     */
    private String username;

    /**
     * The password associated with the username.
     *
     * @see #username
     */
    private String password;

    /**
     * The fedora host
     */
    HttpHost host;

    private Unmarshaller unmarshaller;
    private HttpClient client;
    private ResponseHandler<String> responsehandler;
    private HttpContext context;


    /**
     * The constructor. Creates a Client for the specified Fedora instance
     *
     * @param username     the username
     * @param password     the password
     * @param server       the server adress
     * @param port         the server port number
     * @param unmarshaller
     */
    public FedoraBasicRestSpeaker(String username,
                                  String password,
                                  String server,
                                  int port,
                                  Unmarshaller unmarshaller) {
        this.username = username;
        this.password = password;
        this.unmarshaller = unmarshaller;

        host = new HttpHost(server, port);
        client = getClient(port);
        responsehandler = new BasicResponseHandler();
        context = new BasicHttpContext();

    }

    private HttpClient getClient(int port) {

        if (client != null) {
            return client;
        }
        // Create and initialize HTTP parameters
        HttpParams params = new BasicHttpParams();
        ConnManagerParams.setMaxTotalConnections(params, 100);
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);

        // Create and initialize scheme registry
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(
                new Scheme("http",
                           PlainSocketFactory.getSocketFactory(),
                           port));

        // Create an HttpClient with the ThreadSafeClientConnManager.
        // This connection manager must be used if more than one thread will
        // be using the HttpClient.
        ClientConnectionManager cm = new ThreadSafeClientConnManager(params,
                                                                     schemeRegistry);
        DefaultHttpClient httpClient = new DefaultHttpClient(cm, params);

        if (username != null) {

            Credentials creds =
                    new UsernamePasswordCredentials(username, password);
            BasicCredentialsProvider provider = new BasicCredentialsProvider();

            provider.setCredentials(AuthScope.ANY, creds);
            httpClient.setCredentialsProvider(provider);
        }

        return httpClient;

    }


    public void deleteDatastream(String pid, String ds)
            throws
            FedoraObjectNotFoundException,
            FedoraCommunicationException,
            FedoraDatastreamNotFoundException {

        objectExists(pid);
        datastreamExists(pid, ds);
        HttpRequest delete =
                new HttpPost("/fedora/objects/" + pid + "/datastreams/" +
                             ds + "?dsState=D");
        try {
            invoke(delete);
        } catch (ResourceNotFoundException e) {//Catchall, since the resource MUST exist, except for race conditions
            throw new FedoraCommunicationException(e);
        }

    }

    /*--------- Statics--------------------*/
    public static String sanitize(String pid) {
        if (pid.startsWith("info:fedora/")) {
            pid = pid.replaceFirst("info:fedora/", "");
        }
        try {
            return URLEncoder.encode(pid, HTTP.DEFAULT_CONTENT_CHARSET);
        } catch (UnsupportedEncodingException e) {//TODO this should never happen
            throw new RuntimeException(e);
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
     * @throws ResourceNotFoundException     if the object was not found
     */
    synchronized void createExternalDatastream(String pid,
                                               String ds,
                                               String url,
                                               String checksum,
                                               String label) throws
                                                             FedoraCommunicationException,
                                                             ResourceNotFoundException,
                                                             FedoraAuthenticationException {
        HttpRequest create =
                new HttpPost("/fedora/objects/" + pid
                             + "/datastreams/" + ds
                             + "?controlGroup=R"//redirect
                             + "&dsLocation=" + url //the content
                             + "&dsLabel=" + label //the filename
                             + "&dsState=A" //active state
                             + "&mimeType=application/octet-stream" //mimetype
                             + "&checksumType=md5" //checksum type
                             + "&checksum=" + checksum  //actual checksum
                );

        objectExists(pid);
        try {
            invoke(create);
        } catch (ResourceNotFoundException e) {
            throw new FedoraCommunicationException(e);
        }

    }


    synchronized void createInternalDatastream(String pid,
                                               String ds,
                                               String characurl,
                                               String label)
            throws
            FedoraCommunicationException,
            FedoraAuthenticationException,
            FedoraObjectNotFoundException,
            FedoraDatastreamAlreadyExistException {
        HttpRequest modify =
                new HttpPost("/fedora/objects/" + pid + "/datastreams/" +
                             ds + "?dsLocation=" + characurl +
                             "&dsLabel=" + label +
                             "&mimetype=text/xml&dsState=A");
        objectExists(pid);
        try {
            datastreamExists(pid, ds);
            //if we get to here, the datastream is found
            throw new FedoraDatastreamAlreadyExistException(
                    "Datastream already exists, cannot create new");
        } catch (FedoraDatastreamNotFoundException e) {//good

        }
        try {
            invoke(modify);
        } catch (ResourceNotFoundException e) {
            throw new FedoraCommunicationException(e);
        }
    }


/*------------------------ Checkers -----------------------------------*/

    void isOK(HttpRequestBase request) throws
                                       FedoraCommunicationException,
                                       ResourceNotFoundException,
                                       FedoraAuthenticationException {
        invokeWithOutRead(request);
    }

    public void datastreamExists(String pid,
                                 String datastream)
            throws
            FedoraCommunicationException,
            FedoraDatastreamNotFoundException,
            FedoraObjectNotFoundException {
        objectExists(pid);
        HttpGet getDatastream = new HttpGet(
                "/fedora/objects/" + pid + "/datastreams/" +
                datastream + "?format=xml");

        try {
            isOK(getDatastream);
        } catch (ResourceNotFoundException e) {
            throw new FedoraDatastreamNotFoundException("Datastream not found",
                                                        e);
        }

    }


    public void objectExists(String pid
    )
            throws FedoraCommunicationException, FedoraObjectNotFoundException {
        HttpGet getObjectProfile = new HttpGet("/fedora/objects/" + pid);

        try {
            isOK(getObjectProfile);
        } catch (ResourceNotFoundException e) {
            throw new FedoraObjectNotFoundException("Object not found", e);
        }

    }


    /* --------------------- Getters -----------------------------*/
    public DatastreamProfile getDatastreamProfile(String pid,
                                                  String datastreamname
    )
            throws
            FedoraCommunicationException,
            FedoraDatastreamNotFoundException, FedoraObjectNotFoundException {
        pid = sanitize(pid);

        objectExists(pid);

        HttpRequest getDatastream = new HttpGet(
                "/fedora/objects/" + pid + "/datastreams/" +
                datastreamname + "?format=xml");

        String datastream;
        try {
            datastream = invoke(getDatastream);
        } catch (ResourceNotFoundException e) {
            throw new FedoraDatastreamNotFoundException(e);
        }

        Object datastreamProfile = null;
        try {
            datastreamProfile = unmarshaller.unmarshal(new StringReader(
                    datastream));
        } catch (JAXBException e) {
            throw new FedoraCommunicationException(e);
        }
        if (datastreamProfile instanceof DatastreamProfile) {
            return (DatastreamProfile) datastreamProfile;
        } else {
            throw new FedoraCommunicationException(datastreamProfile.toString());
        }


    }

    public ObjectProfile getObjectProfile(String pid)
            throws
            FedoraCommunicationException,
            FedoraObjectNotFoundException {
        pid = sanitize(pid);

        objectExists(pid);

        HttpRequest getObject = new HttpGet(
                "/fedora/objects/" + pid + "?format=xml");

        String datastream;
        try {
            datastream = invoke(getObject);
        } catch (ResourceNotFoundException e) {
            throw new FedoraObjectNotFoundException(e);
        }

        Object objectProfile = null;
        try {
            objectProfile
                    = unmarshaller.unmarshal(new StringReader(datastream));
        } catch (JAXBException e) {
            throw new FedoraCommunicationException(e);
        }
        if (objectProfile instanceof ObjectProfile) {
            return (ObjectProfile) objectProfile;
        } else {
            throw new FedoraCommunicationException(objectProfile.toString());
        }

    }

    public String getDatastreamContents(String pid,
                                        String datastream)
            throws
            FedoraCommunicationException,

            FedoraObjectNotFoundException,
            FedoraDatastreamNotFoundException {
        pid = sanitize(pid);
        datastream = sanitize(datastream);
        HttpRequest getDatastreamContents = new HttpGet(
                "/fedora/objects/" + pid + "/datastreams/" +
                datastream + "/content");
        objectExists(pid);
        datastreamExists(pid, datastream);

        try {
            return invoke(getDatastreamContents);
        } catch (ResourceNotFoundException e) {//Catchall, since the resource MUST exist, except for race conditions
            throw new FedoraCommunicationException(e);
        }
    }


    /**
     * Returns true if there is content in the datastream. The datastream has contet
     * if we can read the content without getting an exception
     *
     * @param pid        the pid of the object
     * @param datastream the datastream
     * @return true if as much as one character can be read from the stream
     * @throws FedoraObjectNotFoundException if the object was not found
     * @throws FedoraDatastreamNotFoundException
     *                                       if the datastream was not found
     * @throws FedoraCommunicationException  on anything else
     */
    public boolean datastreamHasContent(String pid,
                                        String datastream)
            throws
            FedoraObjectNotFoundException,
            FedoraDatastreamNotFoundException,
            FedoraCommunicationException {
        HttpGet getDatastreamContents = new HttpGet(
                "/fedora/objects/" + pid + "/datastreams/" +
                datastream + "/contents");
        objectExists(pid);
        datastreamExists(pid, datastream);


        try {
            invokeWithOutRead(getDatastreamContents);
        } catch (ResourceNotFoundException e) {
            return false;
        }
        return true;

    }

/*--------------------------------- Directs ---------------------------------*/

    /**
     * Invoke the given request against the Fedora instance.
     *
     * @param request the request to invoke
     * @return the response as a string
     * @throws FedoraCommunicationException  if something more severe failed in the communication
     * @throws FedoraServerError             if there was an error in the fedora server
     * @throws FedoraClientException         if there was a client error
     * @throws FedoraAuthenticationException if the credentials were not sufficient
     * @throws ResourceNotFoundException     if the resource could not be found
     */
    private String invoke(HttpRequest request
    )
            throws
            FedoraCommunicationException,
            FedoraServerError,
            FedoraClientException,
            FedoraAuthenticationException,
            ResourceNotFoundException {

        String result;
        try {
            result = client.execute(
                    host, request, responsehandler, context);
        } catch (HttpResponseException e) {//thrown if the return code was not in the 200s
            int httpcode;
            httpcode = e.getStatusCode();
            if (httpcode >= 500) { //server error
                throw new FedoraServerError("Fedora server error", e);
            } else if (httpcode >= 400) {
                switch (httpcode) {
                    case 401:
                    case 407:
                    case 403:
                        throw new FedoraAuthenticationException(
                                "Could not authenticate",
                                e);
                    case 404:
                    case 410:
                        throw new ResourceNotFoundException(
                                "Could not find resource",
                                e);
                    default:
                        throw new FedoraClientException(
                                "Unrecognized exception",
                                e);
                }
            }
            throw new FedoraCommunicationException("Unrecognized error code",
                                                   e);

        } catch (ClientProtocolException e) {//thrown if something else failed WITHIN the http protocol
            throw new FedoraCommunicationException(
                    "Exception when communicating with Fedora",
                    e);
        } catch (IOException e) { //Thrown if the failure was not within the http protocol
            throw new FedoraCommunicationException(
                    "IO exception when communicating with Fedora",
                    e);
        }
        return result;


    }

    private void invokeWithOutRead(HttpRequestBase request)
            throws
            FedoraCommunicationException,
            FedoraAuthenticationException,
            ResourceNotFoundException,
            FedoraClientException {

        HttpResponse result;
        try {
            result = client.execute(
                    host, request, context);
            HttpEntity entity = result.getEntity();
            int httpcode = result.getStatusLine().getStatusCode();
            String error = result.getStatusLine().getReasonPhrase();
            request.abort();

            if (httpcode >= 300) {

                if (httpcode >= 500) { //server error
                    throw new FedoraServerError(
                            "Fedora server error: " + error);
                } else if (httpcode >= 400) {
                    switch (httpcode) {
                        case 401:
                        case 407:
                        case 403:
                            throw new FedoraAuthenticationException(
                                    "Could not authenticate: " + error
                            );
                        case 404:
                        case 410:
                            throw new ResourceNotFoundException(
                                    "Could not find resource: " + error
                            );
                        default:
                            throw new FedoraClientException(
                                    "Unrecognized exception: " + error);
                    }
                }
                throw new FedoraCommunicationException(
                        "Unrecognized error code: " + error);
            }
        } catch (ClientProtocolException e) {//thrown if something else failed WITHIN the http protocol
            throw new FedoraCommunicationException(
                    "Exception when communicating with Fedora",
                    e);
        } catch (IOException e) { //Thrown if the failure was not within the http protocol
            throw new FedoraCommunicationException(
                    "IO exception when communicating with Fedora",
                    e);
        }
    }

}
