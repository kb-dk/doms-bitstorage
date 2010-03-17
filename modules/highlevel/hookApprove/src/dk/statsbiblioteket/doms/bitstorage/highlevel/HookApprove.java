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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.namespace.QName;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.io.File;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.List;
import java.util.LinkedList;

import fedora.server.proxy.AbstractInvocationHandler;
import fedora.server.management.ManagementModule;
import fedora.server.access.Access;
import fedora.server.access.ObjectProfile;
import fedora.server.Server;
import fedora.server.Context;
import fedora.server.errors.ModuleInitializationException;
import fedora.server.errors.ServerInitializationException;
import fedora.common.Constants;

/**
 * Hooks the ModifyObject method, so that when an file object is set to Active
 * the file is published.
 */
public class HookApprove extends AbstractInvocationHandler {

    /**
     * Logger for this class.
     */
    private static Log LOG = LogFactory.getLog(HookApprove.class);
    public static final QName SERVICENAME =
            new QName(
                    "http://highlevel.bitstorage.doms.statsbiblioteket.dk/",
                    "HighlevelBitstorageSoapWebserviceService");

    public static final String HOOKEDMETHOD = "modifyObject";

    private boolean initialised = false;

    private HighlevelBitstorageSoapWebservice bs;

    private ManagementModule m_manager;
    private Access m_access;

    private Server s_server;

    private List<String> filemodels;


    //synchronized to avoid problems with dual inits
    public synchronized void init() throws ModuleInitializationException, ServerInitializationException, MalformedURLException {
        if (initialised) {
            return;
        }

        s_server = Server.getInstance(new File(Constants.FEDORA_HOME), false);

        filemodels = new LinkedList<String>();

        //get the management module
        m_manager = getManagement();

        //get the access module
        m_access = getAccess();

        //read the parameters from the management module
        String filecmodel = m_manager.getParameter("dk.statsbiblioteket.doms.bitstorage.highlevel.hookapprove.filecmodel");
        if (filecmodel != null) {
            filemodels.add(filecmodel);
        } else {
            LOG.warn("No dk.statsbiblioteket.doms.bitstorage.highlevel.hookapprove.filecmodel specified, disabling hookapprove");
        }

        String webservicelocation = m_manager.getParameter("dk.statsbiblioteket.doms.bitstorage.highlevel.hookapprove.webservicelocation");
        if (webservicelocation == null) {
            webservicelocation = "http://localhost:8080/ecm/validate/";//TODO
            LOG.info("No dk.statsbiblioteket.doms.bitstorage.highlevel.hookapprove.webservicelocation specified, using default location: " + webservicelocation);
        }

        //create the bitstorage client
        URL wsdl;
        wsdl = new URL(webservicelocation);
        bs = new HighlevelBitstorageSoapWebserviceService(wsdl, SERVICENAME)
                .getHighlevelBitstorageSoapWebservicePort();

        initialised = true;

    }

    private Access getAccess() {
        Access module = (Access) s_server.getModule("org.fcrepo.server.access.Access");
        if (module == null) {
            module = (Access) s_server.getModule("fedora.server.access.Access");
        }
        return module;
    }

    private ManagementModule getManagement() {
        ManagementModule module = (ManagementModule) s_server.getModule("org.fcrepo.server.management.Management");
        if (module == null) {
            module = (ManagementModule) s_server.getModule("fedora.server.management.Management");
        }
        return module;

    }

    /**
     * If the Method is ModifyObject AND object to me modified has the specified
     * content model AND is set to the Active state, then attempt to publish
     * file via the bitstorage system. If the file cannot be published, the
     * modification to the object is undone (but will leave a trail in the object
     * log).
     *
     * @param proxy  ?
     * @param method The method to invoke
     * @param args   the arguments to the method
     * @return the method return type
     * @throws Throwable
     */
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        try {
            init();
            //what should happen before change is committed

            if (!HOOKEDMETHOD.equals(method.getName())) {
                return method.invoke(target, args);
            }

            //If the call does not change the state to active, pass through
            String state = (String) args[2];
            if (!(state != null && state.startsWith("A"))) {
                return method.invoke(target, args);
            }

            //so, we have a modify object that change state to A

            Context context = (Context) args[0];//call context
            String pid = args[1].toString();

            //save profile for rollback
            ObjectProfile profile = m_access.getObjectProfile(context, pid, null);

            //Do the change, to see if it was allowed
            Object returnValue = method.invoke(target, args);

            //If we are here, the change committed without exceptions thrown


            try {
                if (isFileObject(profile)) {//is this a file object?
                    bs.publish(pid);//milestone, any fails beyound this must rollback

                }
                return returnValue;

            } catch (Exception e) {//something broke in publishing, so undo the state operation


                //rollback
                String old_state = profile.objectState;
                String old_label = null;
                if (args[3] != null) {
                    //label changed
                    old_label = profile.objectLabel;
                }
                String old_ownerid = null;
                if (args[4] != null) {
                    //ownerid changed
                    old_ownerid = profile.objectOwnerId;
                }
                //commit the rollback. TODO perform this directly on the Management, instead?
                Object new_return = method.invoke(target,
                        context,
                        pid,
                        old_state,
                        old_label,
                        old_ownerid,
                        "Undoing state change because file could not be published");
                //discard rollback returnvalue
                throw new FileCouldNotBePublishedException("The file in '" + pid + "' could not be published. State change rolled back.", e);
            }
        } catch (InvocationTargetException e) {
            //if the invoke method failed, throw the original exception on
            throw e.getCause();
        }//if anything else failed, let it pass
    }

    private boolean isFileObject(ObjectProfile profile) {
        for (String model : profile.objectModels) {
            if (model == null) {
                continue;
            }
            model = ensurePid(model);
            if (filemodels.contains(model)) {
                return true;
            }
        }
        return false;

    }

    private String ensurePid(String model) {
        if (model.startsWith("info:fedora/")) {
            return model.substring("info:fedora/".length());
        }
        return model;
    }

}
