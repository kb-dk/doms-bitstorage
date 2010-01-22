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

package dk.statsbiblioteket.doms.bitstorage.lowlevel;

import dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.BitstorageSshImpl;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.exceptions.ChecksumFailedException;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.exceptions.CommunicationException;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.exceptions.FileAlreadyApprovedException;
import dk.statsbiblioteket.util.Checksums;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;

/**
 * TODO abr forgot to document this class
 */
public class BitstorageSshImplTest {

    BitstorageSshImpl ssh;
    File testdata = new File("modules/lowlevelbitstorage/test/data/testfile");


    @Before
    public void setUp() {
        ssh = new BitstorageSshImpl();

    }

    @After
    public void tearDown() {
        // Add your code here
    }

    @Test
    public void testUpload() throws IOException, FileAlreadyApprovedException,
                                    CommunicationException,
                                    ChecksumFailedException {
        System.out.println(testdata.getAbsolutePath());
        byte[] md5bytes = Checksums.md5(testdata);
        String md5 = ByteString.toHex(md5bytes);
        URL url = ssh.upload("data/testfile",
                             new FileInputStream(testdata),
                             md5,testdata.length());

        System.out.println(url);

    }

    @Test

    public void testDisapprove() {
        // Add your code here
    }

    @Test
    public void testApprove() {

    }

    @Test
    public void testSpaceleft() throws CommunicationException {
        System.out.println(ssh.spaceleft());// Add your code here
    }
}
