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

package dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.exceptions;

import dk.statsbiblioteket.util.qa.QAInfo;

/** An exception signalling a checksum comparison failed. */
@QAInfo(author = "abr",
        reviewers = "kfc",
        level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_OK)
public class ChecksumFailedException extends BitstorageException {
    /**
     * Initialise a checksum failed exception.
     *
     * @param message A message describing the problem. Should always include
     *                the two checksums, and if possible their origin.
     */
    public ChecksumFailedException(String message) {
        super(message);
    }

    /**
     * Initialise a checksum failed exception.
     *
     * @param message A message describing the problem. Should always include
     *                the two checksums, and if possible their origin.
     * @param cause   An exception that caused the checksum comparison to fail.
     */
    public ChecksumFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
