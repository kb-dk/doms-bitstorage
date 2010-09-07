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

import com.sun.jersey.core.impl.provider.entity.DataSourceProvider;
import dk.statsbiblioteket.doms.webservices.ConfigCollection;

import javax.activation.DataSource;
import javax.servlet.ServletContext;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.GET;
import javax.ws.rs.core.Response;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * TODO ABR forgot to javadoc this class
 */
@Path("")
public class UrlProvider {

    private static Map<String, DataSource> blobs;

    static {
        if (blobs == null) {
            blobs =
                    Collections.synchronizedMap(new HashMap<String, DataSource>());
        }

    }

    public static String registerBlob(InputStream blob,
                                      String name,
                                      String contenttype)
            throws IOException {
        String id = UUID.randomUUID().toString();
        DataSourceProvider.ByteArrayDataSource source = null;

        source = new DataSourceProvider.ByteArrayDataSource(blob, contenttype);
        source.setName(name);
        blobs.put(id, source);
        return id;
    }

    public static void unregisterBlob(String id) {
        blobs.remove(id);
    }

    public static String createURLfromID(String id, WebServiceContext context) {


        MessageContext messagecontext = context.getMessageContext();
        String requestURL = (String) messagecontext.get(
                "com.sun.xml.ws.transport.http.servlet.requestURL");
        //TODO hardcoded knowledge of paths...
        requestURL = requestURL.replace("highlevel/",
                                        "URLprovider/blobs/" + id);
        return requestURL;

    }

    @GET
    @Path("blobs/{id}")
    public Response getBlob(@PathParam("id") String id) {
        DataSource blob = blobs.get(id);
        if (blob != null) {
            return Response.ok(blob).build();
        } else {
            throw new WebApplicationException(404);
        }
    }


}
