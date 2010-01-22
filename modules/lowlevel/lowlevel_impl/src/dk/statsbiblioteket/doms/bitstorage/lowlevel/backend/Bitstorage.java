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

import dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.exceptions.ChecksumFailedException;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.exceptions.CommunicationException;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.exceptions.FileAlreadyApprovedException;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.exceptions.FileNotFoundException;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.exceptions.NotEnoughFreeSpaceException;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Java interface the bitstorage system.
 */
public interface Bitstorage {

    /**
     * Upload the provided file to the temporary area of bitstorage, giving it
     * the provided file name. Return the MD5 checksum of the file. The file is
     * only uploaded to a temporary approve-area of the bitstorage, and needs to
     * be approved by calling approveFile before it is actually moved to the
     * permanent bitstorage.
     *
     * @param filename The filename to store the file by
     * @param data     the inputstream with the file contents
     * @param md5      The locally generated md5sum of the file
     * @return the resolvable URL to the uploaded file
     * @throws ChecksumFailedException        if the provided checksum does not
     *                                        match the one calculated on the
     *                                        server
     * @throws CommunicationException         on problems communicating with
     *                                        bitstorage
     * @throws FileAlreadyApprovedException   if a file with that name has
     *                                        already been added to the
     *                                        bitstorage
     * @throws NotEnoughFreeSpaceException    if there is not enough space for
     *                                        the file to be uploaded
     * @throws java.net.MalformedURLException if the filename cannot be made
     *                                        into an url
     */
    public URL upload(String filename, InputStream data, String md5, long filelength)
            throws MalformedURLException, CommunicationException,
                   NotEnoughFreeSpaceException, ChecksumFailedException,
                   FileAlreadyApprovedException;


    /**
     * Delete the named file from bitstorage. Only works for files that have not
     * yet been approved.
     * <p/>
     * If the file is not in temporary bitstorage nothing happens.
     *
     * @param file The url to the file (in bitstorage)
     * @throws CommunicationException on problems communicating with bitstorage
     * @throws FileNotFoundException  if the file was not found in the temporary
     *                                storage
     */
    public void disapprove(URL file)
            throws FileNotFoundException, CommunicationException;


    /**
     * Check the earlier uploaded file against the provided checksum, and if
     * this succeeds, and possibly other criteria are met, move the file from
     * the temporary area of bitstorage to the permanent bitstorage.
     * <p/>
     * If you call this method on an already approved file, with the correct
     * checksum, nothing happens. If the checksum is wrong, you get an
     * exception.
     *
     * @param file The url to the file (in bitstorage)
     * @param md5  The md5sum of the file
     * @return the calculated md5sum of the file
     * @throws CommunicationException      on problems communicating with
     *                                     bitstorage
     * @throws FileNotFoundException       if the file was not found in the
     *                                     temporary storage
     * @throws NotEnoughFreeSpaceException if there was not enough free space in
     *                                     the storage
     * @throws ChecksumFailedException     if the supplied checksum does not
     *                                     match the checksum of the file
     */
    public String approve(URL file, String md5)
            throws FileNotFoundException, CommunicationException,
                   NotEnoughFreeSpaceException, ChecksumFailedException;

    /**
     * Return the number of bytes left in bitstorage.
     *
     * @return The number of bytes left in bitstorage.
     * @throws CommunicationException on problems communicating with bitstorage
     */
    public long spaceleft() throws CommunicationException;

    /**
     * Return the number of bytes that is allocatable for a single file
     *
     * @return The number of bytes left
     * @throws CommunicationException on problems communicating with bitstorage
     */
    public long getMaxFileSize() throws CommunicationException;

    /**
     * Get the server-calculated md5 of a file
     *
     * @param file the url of the file
     * @return the md5sum
     * @throws FileNotFoundException  if the file is not found in bitstorage
     * @throws CommunicationException on problems communicating with bitstorage
     */
    public String getMd5(URL file)
            throws FileNotFoundException, CommunicationException;


    /**
     * Check if the url is of a approved file
     *
     * @param file the url to check
     * @return true if is an approved file, false if it is an temporary file
     * @throws FileNotFoundException  if the file is not found in bitstorage
     * @throws CommunicationException on problems communicating with bitstorage
     */
    public boolean isApproved(URL file)
            throws FileNotFoundException, CommunicationException;


}
