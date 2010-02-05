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

package dk.statsbiblioteket.doms.bitstorage.highlevel.surveillance;

import dk.statsbiblioteket.doms.bitstorage.highlevel.*;
import dk.statsbiblioteket.doms.surveillance.status.Status;
import dk.statsbiblioteket.doms.surveillance.status.StatusMessage;
import dk.statsbiblioteket.doms.surveillance.status.StatusMessage.Severity;
import dk.statsbiblioteket.doms.surveillance.status.Surveyable;
import dk.statsbiblioteket.doms.webservices.ConfigCollection;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.annotation.PostConstruct;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/** Class that exposes real time system info for high-level bitstorage as
 * surveyable messages over REST.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_OK,
        author = "jrg",
        reviewers = {"kfc"})

@Path("/RealTimeService/")  // Part of the url to this webservice, inserted at
//                             * in the relevant url-pattern in web.xml
public class RealTimeService implements Surveyable {
    /** Logger for this class. */
    private Log log = LogFactory.getLog(getClass());

    /** The time when the getStatus method of this class is called. */
    private long timeOfGetStatusCall;

    /** The name of the system being surveyed by through this class. */
    private static final String SURVEYEE_NAME = "High-level bitstorage";

    /** Common prefix of those parameters in web.xml which are used in this
     * class.*/
    private static final String PACKAGE_NAME
            = "dk.statsbiblioteket.doms.bitstorage.highlevel";

    /** Parameter in web.xml describing where the wsdl file for the surveyee is
     * located.  */
    private static final String PARAMETER_NAME_FOR_SURVEYEE_WSDL_URL
            = PACKAGE_NAME + ".location";

    /** The URL describing where the wsdl file for the surveyee is located. */
    private String location;

    /** The namespace of the service */
    private static final String SERVICE_NAMESPACE_URI
            = "http://highlevel.bitstorage.doms.statsbiblioteket.dk/";

    /** The name of the service */
    private static final String SERVICE_NAME =
            "HighlevelBitstorageSoapWebserviceService";

    /** The fully qualified name of the service to monitor
     * @see #SERVICE_NAME
     * @see #SERVICE_NAMESPACE_URI
     */
    private final QName SERVICE_QNAME
            = new QName(SERVICE_NAMESPACE_URI, SERVICE_NAME);


    /** Will be called by the webservice framework after the call of the
     * constructor. Reads parameters from web.xml.
     * This method serves as fault barrier. All exceptions are caught and
     * logged.
     */
    @PostConstruct // Will be called after the call of the constructor
    private void initialize() {
        log.trace("Entered method initialize()");
        Properties props;

        try {
            props = ConfigCollection.getProperties();

            location = props.getProperty(
                    PARAMETER_NAME_FOR_SURVEYEE_WSDL_URL);
            log.info("Location of wsdl for surveyee now set to '"
                    + PARAMETER_NAME_FOR_SURVEYEE_WSDL_URL + "'");
        } catch (Exception e) {
            log.error("Exception caught by fault barrier", e);
        }
    }


    /** Returns only the current real time info.
     *
     * @param time This given date is ignored.
     * @return A status containing list of status messages.
     */
    @GET
    @Path("getStatusSince/{date}")
    @Produces("application/xml")
    public Status getStatusSince(@PathParam("date") long time) {
        log.trace("Entered method getStatusSince(" + time + ")");
        return getStatus();
    }


    /** Returns real time info about the current state of the highlevel
     * bitstorage webservice.
     * This method serves as fault barrier. All exceptions are caught and turned
     * into a status message.
     *
     * @return A status containing list of status messages.
     */
    @GET
    @Path("getStatus")
    @Produces("application/xml")
    public Status getStatus() {
        log.trace("Entered method getStatus()");
        Status status;

        timeOfGetStatusCall = System.currentTimeMillis();

        try {
            status = checkHighlevelBitstorageForCurrentState();
        } catch (Exception e) {
            log.error("Exception caught by fault barrier", e);
            // Create status covering exception
            status = new Status(SURVEYEE_NAME, Arrays.asList(new StatusMessage(
                    "Exception caught by fault barrier: " + e.getMessage(),
                    StatusMessage.Severity.RED, timeOfGetStatusCall,
                    false)));
        }

        return status;
    }


    /** Tries to connect to highlevel-bitstorage and calls the status method
     * there.
     *
     * @return A status containing list of status messages.
     *
     */
    private Status checkHighlevelBitstorageForCurrentState()
            throws BrokenURLException, HighlevelBitstorageUnreachableException,
            BitstorageCommunicationException, BitstorageHighlevelSoapException {
        log.trace("Entered method checkHighlevelBitstorageForCurrentState()");

        HighlevelBitstorageSoapWebserviceService bitstorageWebserviceFactory;
        URL wsdlLocation;
        HighlevelBitstorageSoapWebservice bitstorageService;
        List<StatusMessage> messageList;
        StatusInformation highlevelBitstorageStatus;

        try {
            log.debug("Making URL from location: '" + location + "'");
            wsdlLocation = new URL(location);
        } catch (MalformedURLException e) {
            throw new BrokenURLException("URL to highlevel bitstorage WSDL is"
                    + " broken. URL is: '" + location + "'", e);
        }

        try {
            bitstorageWebserviceFactory
                    = new HighlevelBitstorageSoapWebserviceService(wsdlLocation,
                    SERVICE_QNAME);

            bitstorageService = bitstorageWebserviceFactory
                    .getHighlevelBitstorageSoapWebservicePort();
        } catch (Exception e) {
            // For some reason, highlevel bitstorage webservice is unreachable
            throw new HighlevelBitstorageUnreachableException(
                    "Highlevel bitstorage webservice is unreachable.", e);
        }

        try {
            highlevelBitstorageStatus = bitstorageService.status();
        } catch (CommunicationException e) {
            /* No comms with highlevel bitstorage. Throwing fault exception to
            be caught by fault barrier.*/
            throw new BitstorageCommunicationException("Trying to call method "
                    + "'status' of the highlevel"
                    + " bitstorage webservice"
                    + " gave a CommunicationException.", e);
        } catch (HighlevelSoapException e) {
            /* No comms with highlevel bitstorage. Throwing fault exception to
            be caught by fault barrier.*/
            throw new BitstorageHighlevelSoapException("Trying to call method "
                    + "'status' of the highlevel"
                    + " bitstorage webservice"
                    + " gave a HighlevelSoapException.", e);
        }

        // Now collect status messages from different parts of the status
        // we can get from the highlevel bitstorage

        messageList = new ArrayList<StatusMessage>();

        messageList.add(makeStatusMessageForCurrentOperations(
                highlevelBitstorageStatus));

        return new Status(SURVEYEE_NAME, messageList);
    }


    /** Constructs a status message listing the currently running operations
     * in highlevel bitstorage.
     *
     * @param highlevelBitstorageStatus A StatusInformation object as returned
     * by method 'status' of the highlevel bitstorage web service
     * @return A status message listing the currently running operations in
     * highlevel bitstorage.
     */
    private StatusMessage makeStatusMessageForCurrentOperations(
            StatusInformation highlevelBitstorageStatus) {
        log.trace("Entered method makeStatusMessageForCurrentOperations('"
                + highlevelBitstorageStatus.toString() + "')");
        List<Operation> runningOperations
                = highlevelBitstorageStatus.getOperations();
        String message = "Currently running operations: ";
        Boolean atleastOneOperationListed = false;

        for (Operation operation : runningOperations) {
            XMLGregorianCalendar whenOperationStarted
                    = operation.getHistory().get(0).getWhen();

            if (atleastOneOperationListed) {
                message += ", ";
            }

            message += "Operation '" + operation.getHighlevelMethod()
                    + "'<br />"
                    + "with ID '" + operation.getID() + "'<br />"
                    + "started at '"
                    + whenOperationStarted.toGregorianCalendar().getTime()
                    .toString()
                    + "'.<br />"
                    + "Acting on "
                    + "Fedora PID '" + operation.getFedoraPid() + "',<br />"
                    + "Fedora datastream '" + operation.getFedoraDatastream()
                    + "',<br />"
                    + "File size '" + operation.getFileSize() + "'";

            atleastOneOperationListed = true;
        }

        if (!atleastOneOperationListed) {
            message += "(none)";
        }

        return new StatusMessage(message, StatusMessage.Severity.GREEN,
                                 timeOfGetStatusCall, false);
    }


    /** Constructs a status containing a status message with the input severity,
     * input message, and the current system time.
     *
     * @param severity The severity to be given to the returned status
     * @param message The message to be entered in the returned status
     * @return A status containing a message with the given severity and message
     * text
     */
    private Status makeStatus(Severity severity, String message) {
        log.trace("Entered method makeStatus('" + severity + "', '" + message
                  + "')");
        List<StatusMessage> messageList = new ArrayList<StatusMessage>();
        StatusMessage statusMessage;

        statusMessage = new StatusMessage(message, severity,
                                          timeOfGetStatusCall, false);
        messageList.add(statusMessage);
        return new Status(SURVEYEE_NAME, messageList);
    }

}

