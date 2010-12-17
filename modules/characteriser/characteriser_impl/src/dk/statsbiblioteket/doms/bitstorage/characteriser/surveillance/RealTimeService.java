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

package dk.statsbiblioteket.doms.bitstorage.characteriser.surveillance;

import dk.statsbiblioteket.doms.domsutil.surveyable.Severity;
import dk.statsbiblioteket.doms.domsutil.surveyable.Status;
import dk.statsbiblioteket.doms.domsutil.surveyable.StatusMessage;
import dk.statsbiblioteket.doms.webservices.configuration.ConfigCollection;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Properties;


/**
 * Class that exposes real time system info for the characteriser as
 * surveyable messages.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "jrg",
        reviewers = {""})
public class RealTimeService {
    private Log log = LogFactory.getLog(getClass());


    /**
     * The name of the system being surveyed by through this class.
     */
    private static final String SURVEYEE_NAME = "Characteriser";

    /**
     * Common prefix of those parameters in web.xml which are used in this
     * class.
     */
    private static final String PACKAGE_NAME
            = "dk.statsbiblioteket.doms.bitstorage.characteriser";

    /**
     * Parameter in web.xml describing where the wsdl file for the surveyee is
     * located.
     */
    private static final String PARAMETER_NAME_FOR_SURVEYEE_WSDL_URL
            = PACKAGE_NAME + ".location";

    /**
     * The URL describing where the wsdl file for the surveyee is located.
     */
    private String location;

    /**
     * The namespace of the service
     */
    private static final String SERVICE_NAMESPACE_URI = "http://"
                                                        + "characteriser.bitstorage.doms.statsbiblioteket.dk/";

    /**
     * The name of the service
     */
    private static final String SERVICE_NAME =
            "CharacteriserSoapWebserviceService";


    public RealTimeService() {
        log.trace("Entered constructor RealTimeService()");
        Properties props = ConfigCollection.getProperties();

        location = props.getProperty(
                PARAMETER_NAME_FOR_SURVEYEE_WSDL_URL);
        log.debug("Location of wsdl for surveyee now set to '"
                  + PARAMETER_NAME_FOR_SURVEYEE_WSDL_URL + "'");
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
     * Returns real time info about the current state of the characteriser.
     * This method serves as fault barrier. All exceptions are caught and turned
     * into a status message.
     *
     * @return A status containing list of status messages.
     */
    public Status getStatus() {
        log.trace("Entered method getStatus()");
        Status status;

        try {
            status = checkCharacteriserForCurrentState();
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
     * Checks the status of the characteriser
     *
     * @return A status containing list of status messages.
     */
    private Status checkCharacteriserForCurrentState() {
        log.trace("Entered method checkCharacteriserForCurrentState()");

        Status returnStatus = new Status();

//        try {
//            Bitstorage singeltonBitstorageInstance
//                    = BitstorageFactory.getInstance();
//            spaceLeftInBitstorage = singeltonBitstorageInstance.spaceLeft();
//
//        } catch (CommunicationException e) {
//            log.error("Lowlevel bitstorage webservice"
//                    + " was called but it couldn't communicate with"
//                    + " backend ssh-server.", e);
//            // Report no comms with backend ssh-server
//            return makeStatus(Severity.RED,
//                    "Lowlevel bitstorage webservice"
//                            + " was called but it couldn't communicate with"
//                            + " backend ssh-server."
//                            + " Exception thrown with name: '"
//                            + e.getClass().getName()
//                            + "' and message: ["
//                            + e.getMessage()
//                            + "]"
//            );
//        } catch (Exception e) {
//            log.error("Something went wrong calling"
//                    + " the lowlevel bitstorage.", e);
//            // Report something unknown went wrong
//            return makeStatus(Severity.RED,
//                    "Something went wrong calling"
//                            + " the lowlevel bitstorage."
//                            + " Exception thrown with name: '"
//                            + e.getClass().getName()
//                            + "' and message: ["
//                            + e.getMessage()
//                            + "]"
//            );
//        }
//
//
//        if (spaceLeftInBitstorage < requiredSpaceInBitstorage) {
//            // Report too little space
//            return makeStatus(Severity.RED,
//                    "Not enough space" +
//                            "in bitstorage. Remaining size must be atleast "
//                            + requiredSpaceInBitstorage + " bytes.");
//        } else if (spaceLeftInBitstorage < preferredSpaceInBitstorage) {
//            // Report close to too little space
//            return makeStatus(Severity.YELLOW,
//                    "Space left in bitstorage is getting"
//                            + " dangerously close to the lower limit in"
//                            + " bitstorage. Remaining size should be atleast "
//                            + preferredSpaceInBitstorage + " bytes.");
//        } else {
//            // Report everything ok
//            return makeStatus(Severity.GREEN,
//                    "Lowlevel bitstorage is up, and there is enough space."
//                            + " Currently " + spaceLeftInBitstorage
//                            + " bytes left.");
//        }
        return returnStatus;           // TODO: must be set
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
