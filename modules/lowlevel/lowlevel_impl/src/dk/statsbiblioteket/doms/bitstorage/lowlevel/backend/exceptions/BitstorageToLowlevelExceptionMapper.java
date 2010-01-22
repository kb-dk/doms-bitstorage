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

import dk.statsbiblioteket.doms.bitstorage.lowlevel.LowlevelSoapException;
import dk.statsbiblioteket.doms.exceptions.ExceptionMapper;

import javax.xml.ws.WebServiceException;

/**
 * Created by IntelliJ IDEA.
 * User: abr
 * Date: Jan 20, 2010
 * Time: 6:26:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class BitstorageToLowlevelExceptionMapper extends ExceptionMapper<LowlevelSoapException,BitstorageException>{

    public LowlevelSoapException convert(BitstorageException ce) {
        throw new WebServiceException("Attempted to convert unknown type of exception",ce);
    }

    public LowlevelSoapException convert(ChecksumFailedException ce) {
        return new dk.statsbiblioteket.doms.bitstorage.lowlevel.ChecksumFailedException(ce.getMessage(),ce.getMessage(),ce);
    }

    public LowlevelSoapException convert(dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.exceptions.CommunicationException ce) {
        return new dk.statsbiblioteket.doms.bitstorage.lowlevel.CommunicationException(ce.getMessage(),ce.getMessage(),ce);
    }

    public LowlevelSoapException convert(FileAlreadyApprovedException ce) {
        return new dk.statsbiblioteket.doms.bitstorage.lowlevel.FileAlreadyApprovedException(ce.getMessage(),ce.getMessage(),ce);
    }

    public LowlevelSoapException convert(FileNotFoundException ce) {
        return new dk.statsbiblioteket.doms.bitstorage.lowlevel.FileNotFoundException(ce.getMessage(),ce.getMessage(),ce);
    }

    public LowlevelSoapException convert(InvalidFilenameException ce) {
        return new dk.statsbiblioteket.doms.bitstorage.lowlevel.InvalidFilenameException(ce.getMessage(),ce.getMessage(),ce);
    }

    public LowlevelSoapException convert(NotEnoughFreeSpaceException ce) {
        return new dk.statsbiblioteket.doms.bitstorage.lowlevel.NotEnoughFreeSpaceException(ce.getMessage(),ce.getMessage(),ce);
    }


}
