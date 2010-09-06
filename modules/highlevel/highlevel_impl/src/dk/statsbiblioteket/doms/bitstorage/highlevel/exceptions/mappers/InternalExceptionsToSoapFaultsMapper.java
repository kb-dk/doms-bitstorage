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

package dk.statsbiblioteket.doms.bitstorage.highlevel.exceptions.mappers;


import dk.statsbiblioteket.doms.bitstorage.highlevel.*;
import dk.statsbiblioteket.doms.bitstorage.highlevel.exceptions.InternalException;

import dk.statsbiblioteket.doms.webservices.exceptions.ExceptionMapper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Created by IntelliJ IDEA.
 * User: abr
 * Date: Jan 20, 2010
 * Time: 2:13:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class InternalExceptionsToSoapFaultsMapper
        extends ExceptionMapper<HighlevelSoapException, InternalException> {

    private Log log
            = LogFactory.getLog(InternalExceptionsToSoapFaultsMapper.class);


    public HighlevelSoapException convert(InternalException ce) {
        switch (ce.getType()) {
            case Communication:
                return new CommunicationException(ce.getMessage(),
                                                  ce.getType().toString(),
                                                  ce);
            case ChecksumFailed:
                return new ChecksumFailedException(ce.getMessage(),
                                                   ce.getType().toString(),
                                                   ce);
            case Unknown://TODO
                return new CommunicationException(ce.getMessage(),
                                                  ce.getType().toString(),
                                                  ce);

            //lowlevel types
            case FileAlreadyApproved:
                return new FileAlreadyApprovedException(ce.getMessage(),
                                                        ce.getType().toString(),
                                                        ce);
            case FileIsLocked:
                return new FileIsLockedException(ce.getMessage(),
                                                 ce.getType().toString(),
                                                 ce);
            case FileNotFound://TODO
                return new InvalidFilenameException(ce.getMessage(),
                                                    ce.getType().toString(),
                                                    ce);
            case NotEnoughFreeSpace:
                return new NotEnoughFreeSpaceException(ce.getMessage(),
                                                       ce.getType().toString(),
                                                       ce);
            case InvalidFilename:
                return new InvalidFilenameException(ce.getMessage(),
                                                    ce.getType().toString(),
                                                    ce);

            //Fedora types
            case FileObjectAlreadyInUse:
                return new FileObjectAlreadyInUseException(ce.getMessage(),
                                                           ce.getType().toString(),
                                                           ce);
            case NotAuthorized://TODO
                return new CommunicationException(ce.getMessage(),
                                                  ce.getType().toString(),
                                                  ce);
            case ObjectNotFound:
                return new ObjectNotFoundException(ce.getMessage(),
                                                   ce.getType().toString(),
                                                   ce);

            //Characteriser types
            case CharacterisationFailed:
                return new CharacterisationFailedException(ce.getMessage(),
                                                           ce.getType().toString(),
                                                           ce);
            default:
                return new CommunicationException(ce.getMessage(),
                                                  ce.getType().toString(),
                                                  ce);
        }
    }
}