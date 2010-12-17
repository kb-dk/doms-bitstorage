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

import dk.statsbiblioteket.doms.bitstorage.highlevel.HighlevelSoapException;
import dk.statsbiblioteket.doms.bitstorage.highlevel.status.Operation;
import dk.statsbiblioteket.doms.bitstorage.highlevel.status.StaticStatus;
import dk.statsbiblioteket.doms.bitstorage.highlevel.status.StatusInformation;
import dk.statsbiblioteket.doms.domsutil.surveyable.Severity;
import dk.statsbiblioteket.doms.domsutil.surveyable.Status;
import dk.statsbiblioteket.doms.domsutil.surveyable.StatusMessage;
import dk.statsbiblioteket.doms.domsutil.surveyable.Surveyable;
import dk.statsbiblioteket.doms.webservices.configuration.ConfigCollection;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.PostConstruct;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.List;

/**
 * Class that exposes real time system info for high-level bitstorage as
 * surveyable messages.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_OK,
        author = "jrg",
        reviewers = {"kfc"})
public class RealTimeService implements Surveyable {
    /**
     * Logger for this class.
     */
    private Log log = LogFactory.getLog(getClass());

    /**
     * The time when the getStatus method of this class is called.
     */
    private long timeOfGetStatusCall;

    /**
     * The name of the system being surveyed by through this class.
     */
    private static final String SURVEYEE_NAME = "HighlevelBitstorage";

    /**
     * Will be called by the webservice framework after the call of the
     * constructor. Reads parameters from web.xml.
     * This method serves as fault barrier. All exceptions are caught and
     * logged.
     */
    @PostConstruct
    // Will be called after the call of the constructor
    private void initialize() {

        log.trace("Entered method initialize()");
        try {
            ConfigCollection.getProperties();
        } catch (Exception e) {
            log.error("Exception caught by fault barrier", e);
        }
    }


    /**
     * Returns only the current real time info.
     *
     * @param time This given date is ignored.
     * @return A status containing list of status messages.
     */
    public Status getStatusSince(long time) {
        log.trace("Entered method getStatusSince(" + time + ")");
        return getStatus();
    }


    /**
     * Returns real time info about the current state of the highlevel
     * bitstorage webservice.
     * This method serves as fault barrier. All exceptions are caught and turned
     * into a status message.
     *
     * @return A status containing list of status messages.
     */
    public Status getStatus() {
        log.trace("Entered method getStatus()");
        Status status;

        timeOfGetStatusCall = System.currentTimeMillis();

        try {
            status = checkHighlevelBitstorageForCurrentState();
        } catch (Exception e) {
            log.error("Exception caught by fault barrier", e);
            // Create status covering exception
            status = makeStatus(Severity.RED,
                                "Exception caught by fault barrier: "
                                + e.getMessage());
        }

        return status;
    }


    /**
     * Tries to connect to highlevel-bitstorage and calls the status method
     * there.
     *
     * @return A status containing list of status messages.
     * @throws BitstorageHighlevelSoapException
     *          on trouble calling status
     *          message.
     */
    private Status checkHighlevelBitstorageForCurrentState()
            throws BitstorageHighlevelSoapException {
        log.trace("Entered method checkHighlevelBitstorageForCurrentState()");

        Status status;
        StatusInformation highlevelBitstorageStatus;

        try {
            highlevelBitstorageStatus = StaticStatus.status();
        } catch (HighlevelSoapException e) {
            /* No comms with highlevel bitstorage. Throwing fault exception to
            be caught by fault barrier.*/
            throw new BitstorageHighlevelSoapException("Trying to call method "
                                                       + "'status' of the highlevel"
                                                       + " bitstorage webservice"
                                                       + " gave a HighlevelSoapException.",
                                                       e);
        }


        // Now collect status messages from different parts of the status
        // we can get from the highlevel bitstorage

        status = new Status();
        status.setName(SURVEYEE_NAME);
        status.getMessages().add(makeStatusMessageForCurrentOperations(
                highlevelBitstorageStatus));
        return status;
    }


    /**
     * Constructs a status message listing the currently running operations
     * in highlevel bitstorage.
     *
     * @param highlevelBitstorageStatus A StatusInformation object as returned
     *                                  by method 'status' of the highlevel bitstorage web service
     * @return A status message listing the currently running operations in
     *         highlevel bitstorage.
     */
    private StatusMessage makeStatusMessageForCurrentOperations(
            StatusInformation highlevelBitstorageStatus) {
        log.trace("Entered method makeStatusMessageForCurrentOperations('"
                  + highlevelBitstorageStatus.toString() + "')");
        List<Operation> runningOperations
                = highlevelBitstorageStatus.getOperations();
        String message = "Currently running operations: ";
        Boolean atleastOneOperationListed = false;
        StatusMessage statusMessage = new StatusMessage();

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

        statusMessage.setMessage(message);
        statusMessage.setSeverity(Severity.GREEN);
        statusMessage.setTime(timeOfGetStatusCall);
        statusMessage.setLogMessage(false);
        return statusMessage;
    }


    /**
     * Constructs a status containing a status message with the input severity,
     * input message, and the current system time.
     *
     * @param severity The severity to be given to the returned status
     * @param message  The message to be entered in the returned status
     * @return A status containing a message with the given severity and message
     *         text
     */
    private Status makeStatus(Severity severity, String message) {
        log.trace("Entered method makeStatus('" + severity + "', '" + message
                  + "')");
        StatusMessage statusMessage = new StatusMessage();
        Status status = new Status();

        statusMessage.setMessage(message);
        statusMessage.setSeverity(severity);
        statusMessage.setTime(timeOfGetStatusCall);

        status.setName(SURVEYEE_NAME);
        status.getMessages().add(statusMessage);
        return status;
    }

}

