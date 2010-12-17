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

import dk.statsbiblioteket.doms.bitstorage.highlevel.exceptions.InternalException;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.*;
import dk.statsbiblioteket.doms.webservices.exceptionhandling.ExceptionMapper;

public class LowlevelToInternalExceptionMapper
        extends ExceptionMapper<InternalException, LowlevelSoapException> {

    public InternalException convert(LowlevelSoapException ce) {
        return new InternalException(ce, InternalException.Type.Unknown);
    }


    public InternalException convert(
            ChecksumFailedException ce) {
        return new InternalException(ce.getMessage(),
                                     ce,
                                     InternalException.Type.ChecksumFailed);
    }

    public InternalException convert(
            CommunicationException ce) {
        return new InternalException(ce.getMessage(),
                                     ce,
                                     InternalException.Type.Communication);
    }


    public InternalException convert(FileIsLockedException ce) {
        return new InternalException(ce.getMessage(),
                                     ce,
                                     InternalException.Type.FileIsLocked);
    }


    public InternalException convert(
            FileNotFoundException ce) {
        return new InternalException(ce.getMessage(),
                                     ce,
                                     InternalException.Type.FileNotFound);
    }


    public InternalException convert(NotEnoughFreeSpaceException ce) {
        return new InternalException(ce.getMessage(),
                                     ce,
                                     InternalException.Type.NotEnoughFreeSpace);
    }


}
