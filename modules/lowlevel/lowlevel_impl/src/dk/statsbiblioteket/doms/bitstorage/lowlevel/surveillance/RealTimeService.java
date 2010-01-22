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

package dk.statsbiblioteket.doms.bitstorage.lowlevel.surveillance;

import dk.statsbiblioteket.doms.bitstorage.lowlevel.CommunicationException;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.LowlevelBitstorageSoapWebservice;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.LowlevelBitstorageSoapWebserviceService;
import dk.statsbiblioteket.doms.surveillance.status.Status;
import dk.statsbiblioteket.doms.surveillance.status.StatusMessage;
import dk.statsbiblioteket.doms.surveillance.status.Surveyable;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.ServletConfig;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.xml.namespace.QName;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;


/** Class that exposes real time system info for low-level bitstorage as
 * surveyable messages over REST.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "jrg",
        reviewers = {"kfc"})
@Path("/RealTimeService/")      // Part of the url to this webservice, inserted
// at * in the relevant url-pattern in web.xml
public class RealTimeService implements Surveyable {
    private Log log = LogFactory.getLog(getClass());

    /** Servlet configuration object through which we receive various
     * parameters that have been entered into the web.xml file. */
    @Context
    private ServletConfig servletConfig;

    /** The name of the system being surveyed by through this class. */
    private static final String SURVEYEE_NAME = "Low-level bitstorage";

    /** The URL describing where the wsdl file for the surveyee is located. */
    private String location;

    /** The number of free bytes preferred in bitstorage. If less than this
     * amount is left, surveillance will report it with a yellow stop-light
     * severity.*/
    private int preferredSpaceInBitstorage;

    /** The number of free bytes required in bitstorage. If less than this
     * amount is left, surveillance will report it with a red stop-light
     * severity.*/
    private int requiredSpaceInBitstorage;
    private final QName serviceName;

    public RealTimeService() {
        serviceName = new QName("http://"
                  + "lowlevel.bitstorage.doms.statsbiblioteket.dk/",
                  "LowlevelBitstorageSoapWebserviceService");
    }


    private void initialize() {
        log.trace("Entered method initialize()");
        if (servletConfig != null) {
            String preferredBytesLeft;
            String requiredBytesLeft;
            String packagename = "dk.statsbiblioteket.doms.bitstorage.lowlevel";

            location = servletConfig.getInitParameter(
                    packagename +".location");

            preferredBytesLeft = servletConfig.getInitParameter(
                    packagename
                    + ".preferredBytesLeft");
            requiredBytesLeft = servletConfig.getInitParameter(
                    packagename
                    + ".requiredBytesLeft");
            preferredSpaceInBitstorage = Integer.parseInt(preferredBytesLeft);
            requiredSpaceInBitstorage = Integer.parseInt(requiredBytesLeft);
        } else {
        }
    }


    @GET
    @Path("getStatusSince/{date}")
    @Produces("application/xml")
    /** Returns only the current real time info.
     *
     * @param time This given date is ignored.
     * @return A status containing list of log messages.
     */
    public Status getStatusSince(@PathParam("date") long time) {
        log.trace("Entered method getStatusSince('" + time + "')");
        return getStatus();
    }


    @GET
    @Path("getStatus")
    @Produces("application/xml")
    /** Returns real time info about the current state of the lowlevel
     * bitstorage webservice
     *
     * @return A status containing list of log messages.
     */
    public Status getStatus() {
        log.trace("Entered method getStatus()");
        // When tested in a local tomcat, this service is accessible from
        // localhost:8080/lowlevelbitstorage/
        //   lowlevelbitstoragerealtimesurveillanceservice/RealTimeService/
        //   getStatus
        //
        // Or, in one line, for testing-convenience:
        // http://localhost:8080/lowlevelbitstorage/lowlevelbitstoragerealtimesurveillanceservice/RealTimeService/getStatus


        LowlevelBitstorageSoapWebserviceService bitstorageWebserviceFactory;
        URL wsdlLocation = null;
        LowlevelBitstorageSoapWebservice bitstorageService;
        Long spaceLeftInBitstorage;              // In bytes.

        initialize();

        try {
            log.debug("Making URL from location: '" + location + "'");
            wsdlLocation = new URL(location);
        } catch (MalformedURLException e) {
            throw new Error(e);
        }

        try {
            bitstorageWebserviceFactory
                    = new LowlevelBitstorageSoapWebserviceService(wsdlLocation,
                                                                  serviceName);

            bitstorageService = bitstorageWebserviceFactory
                    .getLowlevelBitstorageSoapWebservicePort();
        } catch (Exception e) {
            // Report lowlevel bitstorage webservice is unreachable
            return makeStatus(StatusMessage.Severity.RED,
                              "Lowlevel bitstorage webservice is unreachable."
                              + " Exception thrown with name: '"
                              + e.getClass().getName()
                              + "' and message: ["
                              + e.getMessage()
                              +"]"
            );
        }

        try {
            spaceLeftInBitstorage = bitstorageService.spaceleft();
        } catch (CommunicationException e) {
/*
            // Development note: Have tried to find out how to get the
            // exception which is wrapped in the SOAPFaultException, to no
            // avail. A googling found several people asking about the same
            // problem, but no useful answer.
            if (e.getMessage().startsWith("dk.statsbiblioteket.doms.bitstorage"
                    + ".lowlevel.backend.exceptions.CommunicationException:")) {
*/
            // Report no comms with backend ssh-server
            return makeStatus(StatusMessage.Severity.RED,
                              "Lowlevel bitstorage webservice"
                              + " was called but it couldn't communicate with"
                              + " backend ssh-server."
                              + " Exception thrown with name: '"
                              + e.getClass().getName()
                              + "' and message: ["
                              + e.getMessage()
                              +"]"
            );
        } catch (Exception e) {

            // Report something unknown went wrong
            return makeStatus(StatusMessage.Severity.RED,
                              "Something went wrong calling"
                              + " the lowlevel bitstorage webservice."
                              + " Exception thrown with name: '"
                              + e.getClass().getName()
                              + "' and message: ["
                              + e.getMessage()
                              +"]"
            );
        }



        if (spaceLeftInBitstorage < requiredSpaceInBitstorage) {
            // Report too little space
            return makeStatus(StatusMessage.Severity.RED,
                              "Not enough space" +
                              "in bitstorage. Remaining size must be atleast "
                              + requiredSpaceInBitstorage + " bytes.");
        } else if (spaceLeftInBitstorage < preferredSpaceInBitstorage) {
            // Report close to too little space
            return makeStatus(StatusMessage.Severity.YELLOW,
                              "Space left in bitstorage is getting"
                              + " dangerously close to the lower limit in bitstorage."
                              + " Remaining size should be atleast "
                              + preferredSpaceInBitstorage + " bytes.");
        } else {
            // Report everything ok
            return makeStatus(StatusMessage.Severity.GREEN,
                              "Lowlevel bitstorage is up, and there is enough space."
                              + " Currently " + spaceLeftInBitstorage + " bytes left.");
        }
    }


    /** Constructs a status containing a status message with the input severity,
     * input message, and the current system time.
     *
     * @param severity The severity to be given to the returned status
     * @param message The message to be entered in the returned status
     * @return A status containing a message with the given severity and message
     * text
     */
    private Status makeStatus(
            dk.statsbiblioteket.doms.surveillance.status.StatusMessage.Severity
                    severity,
            String message) {
        log.trace("Entered method makeStatus('" + severity + "', '" + message
                  + "')");
        ArrayList<StatusMessage> messageList = new ArrayList<StatusMessage>();
        StatusMessage statusMessage;

        statusMessage = new StatusMessage(message, severity,
                                          System.currentTimeMillis(), false);
        messageList.add(statusMessage);
        return new Status(SURVEYEE_NAME, messageList);
    }

}


