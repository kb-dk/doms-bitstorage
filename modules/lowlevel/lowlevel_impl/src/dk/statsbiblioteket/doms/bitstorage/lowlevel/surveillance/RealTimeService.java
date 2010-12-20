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

import dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.Bitstorage;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.BitstorageFactory;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.exceptions.CommunicationException;
import dk.statsbiblioteket.doms.domsutil.surveyable.Severity;
import dk.statsbiblioteket.doms.domsutil.surveyable.Status;
import dk.statsbiblioteket.doms.domsutil.surveyable.StatusMessage;
import dk.statsbiblioteket.doms.domsutil.surveyable.Surveyable;
import dk.statsbiblioteket.doms.webservices.configuration.ConfigCollection;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Properties;


/**
 * Class that exposes real time system info for low-level bitstorage as
 * surveyable messages.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "jrg",
        reviewers = {"kfc"})
public class RealTimeService implements Surveyable {
    private Log log = LogFactory.getLog(getClass());


    /**
     * The name of the system being surveyed by through this class.
     */
    private static final String SURVEYEE_NAME = "DomsLowlevelBitstorage";

    /**
     * Common prefix of those parameters in web.xml which are used in this
     * class.
     */
    private static final String PACKAGE_NAME
            = "dk.statsbiblioteket.doms.bitstorage.lowlevel";

    /**
     * Parameter in web.xml telling the number of free bytes preferred in
     * bitstorage
     */
    private static final String PARAMETER_NAME_FOR_PREFERRED_BYTES_LEFT
            = PACKAGE_NAME + ".preferredBytesLeft";

    /**
     * Parameter in web.xml telling the number of free bytes required in
     * bitstorage.
     */
    private static final String PARAMETER_NAME_FOR_REQUIRED_BYTES_LEFT
            = PACKAGE_NAME + ".requiredBytesLeft";

    /**
     * The number of free bytes preferred in bitstorage. If less than this
     * amount is left, surveillance will report it with a yellow stop-light
     * severity.
     */
    private int preferredSpaceInBitstorage;

    /**
     * The number of free bytes required in bitstorage. If less than this
     * amount is left, surveillance will report it with a red stop-light
     * severity.
     */
    private int requiredSpaceInBitstorage;

    public RealTimeService() {
        log.trace("Entered constructor RealTimeService()");
        log.trace("Entered method initialize()");
        Properties props = ConfigCollection.getProperties();

        String preferredBytesLeft;
        String requiredBytesLeft;

        preferredBytesLeft = props.getProperty(
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

        requiredBytesLeft = props.getProperty(
                PARAMETER_NAME_FOR_REQUIRED_BYTES_LEFT);
        try {
            requiredSpaceInBitstorage = Integer.parseInt(requiredBytesLeft);
        } catch (NumberFormatException e) {
            log.error("Couldn't parse the value of web.xml parameter '"
                      + PARAMETER_NAME_FOR_REQUIRED_BYTES_LEFT + "'");
        }
        log.debug("Required number of bytes in bitstorage now set to '"
                  + requiredSpaceInBitstorage + "'");
    }


    /**
     * Returns only the current real time info.
     *
     * @param time This given date is ignored.
     * @return A status containing list of status messages.
     */
    public Status getStatusSince(long time) {
        log.trace("Entered method getStatusSince('" + time + "')");
        return getStatus();
    }


    /**
     * Returns real time info about the current state of the lowlevel
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
            status = checkLowlevelBitstorageForCurrentState();
        } catch (Exception e) {
            log.debug("Exception caught by fault barrier", e);
            // Create status covering exception
            status = makeStatus(Severity.RED,
                                "Exception caught by fault barrier: "
                                + e.getMessage());
        }

        return status;
    }


    /**
     * Tries to connect to lowlevel-bitstorage and checks the amount of space
     * left there.
     *
     * @return A status containing list of status messages.
     */
    private Status checkLowlevelBitstorageForCurrentState() {
        log.trace("Entered method checkLowlevelBitstorageForCurrentState()");

        Long spaceLeftInBitstorage; // In bytes.


        try {
            Bitstorage singeltonBitstorageInstance
                    = BitstorageFactory.getInstance();
            spaceLeftInBitstorage = singeltonBitstorageInstance.spaceLeft();

        } catch (CommunicationException e) {
            log.error("Lowlevel bitstorage webservice"
                      + " was called but it couldn't communicate with"
                      + " backend ssh-server.", e);
            // Report no comms with backend ssh-server
            return makeStatus(Severity.RED,
                              "Lowlevel bitstorage webservice"
                              + " was called but it couldn't communicate with"
                              + " backend ssh-server."
                              + " Exception thrown with name: '"
                              + e.getClass().getName()
                              + "' and message: ["
                              + e.getMessage()
                              + "]"
            );
        } catch (Exception e) {
            log.error("Something went wrong calling"
                      + " the lowlevel bitstorage.", e);
            // Report something unknown went wrong
            return makeStatus(Severity.RED,
                              "Something went wrong calling"
                              + " the lowlevel bitstorage."
                              + " Exception thrown with name: '"
                              + e.getClass().getName()
                              + "' and message: ["
                              + e.getMessage()
                              + "]"
            );
        }


        if (spaceLeftInBitstorage < requiredSpaceInBitstorage) {
            // Report too little space
            return makeStatus(Severity.RED,
                              "Not enough space" +
                              "in bitstorage. Remaining size must be atleast "
                              + requiredSpaceInBitstorage + " bytes.");
        } else if (spaceLeftInBitstorage < preferredSpaceInBitstorage) {
            // Report close to too little space
            return makeStatus(Severity.YELLOW,
                              "Space left in bitstorage is getting"
                              + " dangerously close to the lower limit in"
                              + " bitstorage. Remaining size should be atleast "
                              + preferredSpaceInBitstorage + " bytes.");
        } else {
            // Report everything ok
            return makeStatus(Severity.GREEN,
                              "Lowlevel bitstorage is up, and there is enough space."
                              + " Currently " + spaceLeftInBitstorage
                              + " bytes left.");
        }
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
        Status status = new Status();
        StatusMessage statusMessage = new StatusMessage();

        statusMessage.setMessage(message);
        statusMessage.setSeverity(severity);
        statusMessage.setTime(System.currentTimeMillis());
        statusMessage.setLogMessage(false);

        status.setName(SURVEYEE_NAME);
        status.getMessages().add(statusMessage);
        return status;
    }

}



