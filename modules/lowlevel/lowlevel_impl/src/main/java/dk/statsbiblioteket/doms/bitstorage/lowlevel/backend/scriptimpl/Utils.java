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

package dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.scriptimpl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.net.URL;
import java.net.MalformedURLException;

/**
 * Small utility class for lowlevel scriptimpl. Methods will probably move to
 * domsutils at a later date.
 */
@QAInfo(author = "abr",
        reviewers = "",
        level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED)
public class Utils {

    private static Log log = LogFactory.getLog(Utils.class);

    /**
     * Simple little method for checking if the message contain only 0-9 chars
     *
     * @param output the message
     * @return true if only 0-9 chars
     */
    public static boolean isLong(String output) {
        log.trace("Entering isLong(" + output + ")");
        return output != null && output.matches("[0-9]*");
    }

    /**
     * Simple little method for checking if the message can be regarded as a
     * checksum.
     *
     * @param output the message
     * @return true if the message contain only a-f A-F and 0-9
     */
    public static boolean isChecksum(String output) {
        log.trace("Entering isChecksum(" + output + ")");
        return output != null && output.matches("[a-fA-F0-9]*");
    }

    public static boolean isURL(String output) {
        log.trace("Entering isURL(" + output + ")");
        try {
            new URL(output);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }
}
