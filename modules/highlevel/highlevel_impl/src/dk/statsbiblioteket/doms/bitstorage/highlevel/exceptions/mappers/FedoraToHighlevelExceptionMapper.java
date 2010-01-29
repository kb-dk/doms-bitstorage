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

import dk.statsbiblioteket.doms.bitstorage.highlevel.exceptions.HighlevelException;
import dk.statsbiblioteket.doms.bitstorage.highlevel.fedora.FedoraException;
import dk.statsbiblioteket.doms.webservices.exceptions.ExceptionMapper;

/**
 * Created by IntelliJ IDEA.
 * User: abr
 * Date: Jan 20, 2010
 * Time: 6:00:30 PM
 * To change this template use File | Settings | File Templates.
 */
public class FedoraToHighlevelExceptionMapper extends ExceptionMapper<HighlevelException,FedoraException> {


    public HighlevelException convert(FedoraException ce) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
