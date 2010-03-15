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

import org.fcrepo.server.proxy.AbstractInvocationHandler;
import org.fcrepo.server.management.ManagementModule;
import org.fcrepo.server.errors.ModuleInitializationException;
import org.fcrepo.server.errors.ServerInitializationException;
import org.fcrepo.server.Server;
import org.fcrepo.common.Constants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.io.File;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * Created by IntelliJ IDEA.
 * User: abr
 * Date: Mar 15, 2010
 * Time: 3:01:30 PM
 * To change this template use File | Settings | File Templates.
 */
public class HookApprove extends AbstractInvocationHandler {

    /**
     * Logger for this class.
     */
    private static Log LOG = LogFactory.getLog(HookApprove.class);


    public static final String HOOKEDMETHOD = "modifyObject";


    private boolean initialised = false;

    private String username;
    private String password;
    private String webservicelocation;
    private HighlevelBitstorageSoapWebservice bs;
    public static final QName servicename = new QName("http://highlevel.bitstorage.doms.statsbiblioteket.dk/", "HighlevelBitstorageSoapWebserviceService");


    public void init() throws Exception {
        if (initialised) {
            return;
        }
        try {
            Server s_server;

            s_server = Server.getInstance(new File(Constants.FEDORA_HOME), false);


            ManagementModule m_manager = (ManagementModule) s_server.getModule(
                    "fedora.server.management.Management");


            webservicelocation = m_manager.getParameter("bitstorage.webservice.location");
            if (webservicelocation == null) {
                webservicelocation = "http://localhost:8080/ecm/validate/";
                LOG.info("No validator.webservice.location specified, using default location: " + webservicelocation);
            }
            URL wsdl;
            wsdl = new URL(webservicelocation);

            bs = new HighlevelBitstorageSoapWebserviceService(wsdl, servicename).getHighlevelBitstorageSoapWebservicePort();

        } catch (Exception e) {
            LOG.error("Failed to initialise the hookapprove");
            throw e;
        }
        initialised = true;

    }


    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object returnValue = null;
        try {
            if (!initialised) {
                init();
            }
            //before change is committed

            returnValue = method.invoke(target, args);

            //after change is committed

            if (!HOOKEDMETHOD.equals(method.getName())) {
                return returnValue;
            }

            //If we are here, we have modifyObject
            LOG.info("We are hooking method " + method.getName());

            //If the call does not change the state to active, pass through
            String state = (String) args[2];
            if (!(state != null && state.startsWith("A"))) {
                return returnValue;
            }


            String pid = args[1].toString();
            LOG.info("The method was called with the pid " + pid);
            try {
                bs.publish(pid);
            } catch (HighlevelSoapException e) {//something broke, so undo the state operation
                //TODO here we should do rollback
                throw e;
            }

            return returnValue;

        } catch (InvocationTargetException e) {//if the invoke method failed
            throw e.getCause(); //extract the real exception and rethrow
        }
        catch (Exception e) { // something of our own failed
            LOG.error("Failed to publish the file", e); //log it
            throw e;
        }
    }

}
