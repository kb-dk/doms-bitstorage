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

import dk.statsbiblioteket.doms.bitstorage.characteriser.Characterisation;
import dk.statsbiblioteket.doms.bitstorage.characteriser.CharacteriseSoapWebservice;
import dk.statsbiblioteket.doms.bitstorage.characteriser.CommunicationException;
import dk.statsbiblioteket.doms.bitstorage.characteriser.FileNotAvailableException;

import javax.annotation.Resource;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.servlet.ServletContext;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

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

    @Resource
    private WebServiceContext webServiceContext;

    private boolean initialised = false;

    private void initialise(){
        if (initialised){
            return;
        }
        ServletContext servletContext =
                (ServletContext) webServiceContext.getMessageContext().get(
                        MessageContext.SERVLET_CONTEXT);
        String testparam = servletContext.getInitParameter("testParam");
        initialised = true;
        
    }


    public Characterisation characterise(
            @WebParam(name = "pid",
                      targetNamespace = "http://characteriser.bitstorage.doms.statsbiblioteket.dk/",
                      partName = "characteriseReturn")
            String characteriseReturn)
            throws CommunicationException, FileNotAvailableException {
        initialise();
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
