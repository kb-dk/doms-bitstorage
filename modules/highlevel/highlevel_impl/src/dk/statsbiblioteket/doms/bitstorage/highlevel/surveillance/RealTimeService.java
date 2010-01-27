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

import dk.statsbiblioteket.doms.surveillance.status.Status;
import dk.statsbiblioteket.doms.surveillance.status.StatusMessage;
import dk.statsbiblioteket.doms.surveillance.status.StatusMessage.Severity;
import dk.statsbiblioteket.doms.surveillance.status.Surveyable;
import dk.statsbiblioteket.doms.bitstorage.highlevel.*;
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
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.net.URL;
import java.net.MalformedURLException;

/** Class that exposes real time system info for high-level bitstorage as
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
    private static final String SURVEYEE_NAME = "High-level bitstorage";

    /** Common prefix of those parameters in web.xml which are used in this
     * class.*/
    private static final String PACKAGE_NAME
            = "dk.statsbiblioteket.doms.bitstorage.highlevel";

    /** Parameter in web.xml describing where the wsdl file for the surveyee is
     * located.  */
    private static final String PARAMETER_NAME_FOR_SURVEYEE_WSDL_URL
            = PACKAGE_NAME + ".location";

    /** Parameter in web.xml telling the number of free bytes preferred in
     * bitstorage*/
    private static final String PARAMETER_NAME_FOR_PREFERRED_BYTES_LEFT
            = PACKAGE_NAME + ".preferredBytesLeft";

    /** Parameter in web.xml telling the number of free bytes required in
     * bitstorage.*/
    private static final String PARAMETER_NAME_FOR_REQUIRED_BYTES_LEFT
            = PACKAGE_NAME + ".requiredBytesLeft";

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

    /** The fully qualified name of the service to monitor
     * @see #SERVICE_NAME
     * @see #SERVICE_NAMESPACE_URI
     */
    private final QName serviceName;

    /** The namespace of the service */
    private static final String SERVICE_NAMESPACE_URI = "http://"
                + "highlevel.bitstorage.doms.statsbiblioteket.dk/";

    /** The name of the service */
    private static final String SERVICE_NAME =
            "HighlevelBitstorageSoapWebserviceService";


    public RealTimeService() {
        log.trace("Entered constructor RealTimeService()");
        serviceName = new QName(SERVICE_NAMESPACE_URI, SERVICE_NAME);

    }


    private void initialize() {
        log.trace("Entered method initialize()");
        if (servletConfig != null) {
            String preferredBytesLeft;
            String requiredBytesLeft;

            location = servletConfig.getInitParameter(
                    PARAMETER_NAME_FOR_SURVEYEE_WSDL_URL);
            log.debug("Location of wsdl for surveyee now set to '"
                    + PARAMETER_NAME_FOR_SURVEYEE_WSDL_URL + "'");

            preferredBytesLeft = servletConfig.getInitParameter(
                    PARAMETER_NAME_FOR_PREFERRED_BYTES_LEFT);
            try {
                preferredSpaceInBitstorage
                        = Integer.parseInt(preferredBytesLeft);
            } catch (NumberFormatException e) {
                log.error("Couldn't parse the value of web.xml parameter '"
                + PARAMETER_NAME_FOR_PREFERRED_BYTES_LEFT + "'");
            }
            log.debug("Preferred number of bytes in bitstorage now set to '"
                    + preferredSpaceInBitstorage + "'");

            requiredBytesLeft = servletConfig.getInitParameter(
                    PARAMETER_NAME_FOR_REQUIRED_BYTES_LEFT);
            try {
                requiredSpaceInBitstorage = Integer.parseInt(requiredBytesLeft);
            } catch (NumberFormatException e) {
                log.error("Couldn't parse the value of web.xml parameter '"
                + PARAMETER_NAME_FOR_REQUIRED_BYTES_LEFT + "'");
            }
            log.debug("Required number of bytes in bitstorage now set to '"
                    + requiredSpaceInBitstorage + "'");
        } else {
            log.error("Servlet configuration object was not properly"
                    + " initialized (was null), and therefore could not access"
                    + " parameters from web.xml");
        }
    }


    @GET
    @Path("getStatusSince/{date}")
    @Produces("application/xml")
    /** Returns only the current real time info.
     *
     * @param time This given date is ignored.
     * @return A status containing list of status messages.
     */
    public Status getStatusSince(@PathParam("date") long time) {
        log.trace("Entered method getStatusSince('" + time + "')");
        return getStatus();
    }


    @GET
    @Path("getStatus")
    @Produces("application/xml")
    /** Returns real time info about the current state of the highlevel
     * bitstorage webservice.
     * This method serves as fault barrier. All exceptions are caught and turned
     * into a status message.
     *
     * @return A status containing list of status messages.
     */
    public Status getStatus() {
        log.trace("Entered method getStatus()");
        Status status;

        try {
            status = checkHighlevelBitstorageForCurrentState();
        } catch (Exception e) {
            log.debug("Exception caught by fault barrier", e);
            // Create status covering exception
            status = new Status(SURVEYEE_NAME, Arrays.asList(new StatusMessage(
                    "Exception caught by fault barrier: " + e.getMessage(),
                    StatusMessage.Severity.RED, System.currentTimeMillis(),
                    false)));
        }

        return status;
    }


    /** Tries to connect to highlevel-bitstorage and calls the status method
     * there.
     *
     * @return A status containing list of status messages.
     * */
    private Status checkHighlevelBitstorageForCurrentState() {
        log.trace("Entered method checkHighlevelBitstorageForCurrentState()");

        HighlevelBitstorageSoapWebserviceService bitstorageWebserviceFactory;
        URL wsdlLocation;
        HighlevelBitstorageSoapWebservice bitstorageService;
        List<StatusMessage> messageList;
        StatusInformation highlevelBitstorageStatus;

        initialize(); // One could discuss whether this call should be outside
                      // the fault barrier

        try {
            log.debug("Making URL from location: '" + location + "'");
            wsdlLocation = new URL(location);
        } catch (MalformedURLException e) {
            log.error("URL to highlevel bitstorage WSDL is"
                    + " broken. URL is: '" + location + "'", e);
            throw new BrokenURLException("URL to highlevel bitstorage WSDL is"
                    + " broken. URL is: '" + location + "'", e);
        }

        try {
            bitstorageWebserviceFactory
                    = new HighlevelBitstorageSoapWebserviceService(wsdlLocation,
                    serviceName);

            bitstorageService = bitstorageWebserviceFactory
                    .getHighlevelBitstorageSoapWebservicePort();
        } catch (Exception e) {
            // For some reason, highlevel bitstorage webservice is unreachable
            log.error("Highlevel bitstorage webservice is unreachable.", e);
            throw new HighlevelBitstorageUnreachableException(
                    "Highlevel bitstorage webservice is unreachable.", e);
        }

        try {
            highlevelBitstorageStatus = bitstorageService.status();
        } catch (CommunicationException e) {
            log.error("Trying to call method 'status' of the highlevel"
                    + " bitstorage webservice"
                    + " gave a CommunicationException.", e);
            // Report no comms with highlevel bitstorage
            return makeStatus(StatusMessage.Severity.RED,
                    "Trying to call method 'status' of the highlevel"
                            + " bitstorage webservice"
                            + " gave an exception with name: '"
                            + e.getClass().getName()
                            + "' and message: ["
                            + e.getMessage()
                            +"]"
            );
        } catch (HighlevelSoapException e) {
            log.error("Trying to call method 'status' of the highlevel"
                    + " bitstorage webservice"
                    + " gave a HighlevelSoapException.", e);
            // Report no comms with highlevel bitstorage
            return makeStatus(StatusMessage.Severity.RED,
                    "Trying to call method 'status' of the highlevel"
                            + " bitstorage webservice"
                            + " gave an exception with name: '"
                            + e.getClass().getName()
                            + "' and message: ["
                            + e.getMessage()
                            +"]"
            );
        }

        // Now collect status messages from different parts of the status
        // we can get from the highlevel bitstorage

        messageList = new ArrayList<StatusMessage>();

        messageList.add(makeStatusMessageForFreeSpace(
                highlevelBitstorageStatus));

        messageList.add(makeStatusMessageForCurrentOperations(
                highlevelBitstorageStatus));

        return new Status(SURVEYEE_NAME, messageList);
    }


    /** Constructs a status message with the status of free space reported
     * by highlevel bitstorage.
     *
     * @param highlevelBitstorageStatus A StatusInformation object as returned
     * by method 'status' of the highlevel bitstorage web service
     * @return A status message detailing the status of free space reported
     * by highlevel bitstorage
     */
    private StatusMessage makeStatusMessageForFreeSpace(
            StatusInformation highlevelBitstorageStatus) {
        Long spaceLeftInBitstorage; // In bytes.

        spaceLeftInBitstorage = highlevelBitstorageStatus.getFreeSpace();

        if (spaceLeftInBitstorage < requiredSpaceInBitstorage) {
            // Report too little space
            return new StatusMessage("Not enough space in bitstorage. Remaining"
                    + " size must be atleast " + requiredSpaceInBitstorage
                    + " bytes.", StatusMessage.Severity.RED,
                    System.currentTimeMillis(), false);
        } else if (spaceLeftInBitstorage < preferredSpaceInBitstorage) {
            // Report close to too little space
            return new StatusMessage("Space left in bitstorage is getting"
                    + " dangerously close to the lower limit in"
                    + " bitstorage. Remaining size should be atleast "
                    + preferredSpaceInBitstorage + " bytes.",
                    StatusMessage.Severity.YELLOW,
                    System.currentTimeMillis(), false);
        } else {
            // Report everything ok
            return new StatusMessage("Highlevel bitstorage is up, and there is"
                    + " enough space. Currently " + spaceLeftInBitstorage
                    + " bytes left.", StatusMessage.Severity.GREEN,
                    System.currentTimeMillis(), false);
        }
    }


    /** Constructs a status message listing the currently running operations
     * in highlevel bitstorage.
     *
     * @param highlevelBitstorageStatus A StatusInformation object as returned
     * by method 'status' of the highlevel bitstorage web service
     * @return A status message detailing the status of free space reported
     * by highlevel bitstorage
     */
    private StatusMessage makeStatusMessageForCurrentOperations(
            StatusInformation highlevelBitstorageStatus) {
        List<Operation> runningOperations;
        String message = "Currently running operations: ";
        Boolean oneOrMoreOperationsListed;

        runningOperations = highlevelBitstorageStatus.getOperations();

        oneOrMoreOperationsListed = false;
        for (Operation operation : runningOperations) {
            XMLGregorianCalendar whenOperationStarted
                    = operation.getHistory().get(0).getWhen();

            if (oneOrMoreOperationsListed) {
                message += ", ";
            }

            message += "{"
                    + "Operation '" + operation.getHighlevelMethod() + "' "
                    + "with ID '" + operation.getID() + "' "
                    + "started at '"
                    + whenOperationStarted.getEonAndYear()
                    + "-" + whenOperationStarted.getMonth()
                    + "-" + whenOperationStarted.getDay()
                    + " " + whenOperationStarted.getHour()
                    + ":" + whenOperationStarted.getMinute()
                    + ":" + whenOperationStarted.getSecond()
                    + "'. Acting on "
                    + "Fedora PID '" + operation.getFedoraPid() + "', "
                    + "Fedora datastream '" + operation.getFedoraDatastream()
                    + "', "
                    + "File size '" + operation.getFileSize() + "'"
                    + "}";

            oneOrMoreOperationsListed = true;
        }

        if (!oneOrMoreOperationsListed) {
            message += "<none>";
        }

        return new StatusMessage(message, StatusMessage.Severity.GREEN,
                System.currentTimeMillis(), false);
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
                                          System.currentTimeMillis(), false);
        messageList.add(statusMessage);
        return new Status(SURVEYEE_NAME, messageList);
    }

}

