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

package dk.statsbiblioteket.doms.bitstorage.lowlevel.backend;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.scriptimpl.BitstorageScriptImpl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is the factory for interacting with the backend bitstorage system. It
 * creates threadsafe BitStorage implementations.
 */
@QAInfo(author = "abr",
        reviewers = "kfc",
        state = QAInfo.State.IN_DEVELOPMENT,
        level = QAInfo.Level.NORMAL)
public class BitstorageFactory {

    static Log log = LogFactory.getLog(BitstorageFactory.class);

    static Bitstorage bs = null;

    /**
     * Factory method for Bitstorage implementations. This method provides
     * a singleton object for the bitstorage system.
     *
     * @return a Bitstorage Object, initialised and ready to use
     */
    public synchronized static Bitstorage getInstance() {
        log.trace("Getting an instance of Bitstorage");
        if (bs == null) {//TODO look into the surveilance factory
            bs = new BitstorageScriptImpl();
        }
        return bs;


    }

}
