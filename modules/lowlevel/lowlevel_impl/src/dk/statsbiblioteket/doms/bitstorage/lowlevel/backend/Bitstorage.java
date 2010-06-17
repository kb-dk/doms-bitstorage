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

import dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.exceptions.*;

import java.io.InputStream;
import java.net.URL;

/**
 * Java interface to the bitstorage system. The DOMS bitstorage system is a
 * tiered storage. There are two tiers, temporary storage, and permanent storage
 * All uploads happen to temporary storage. There is no real preservation security
 * in the temporary storage. When a file has been inspected and deemed ready for
 * preservation, it can be "approved". By approving it, the file is moved to the
 * permanent storage. Here, it will be backed up, and the preservation is guaranteed.
 * <p/>
 * While a file is still in the temporary storage, it can be "disapproved", which
 * causes it to be deleted. Files moved to permanent storage cannot be  deleted.
 * <p/>
 * There are no provisions for changing files in either of the storages. If
 * you want to change a file in temporary storage, deleted it and upload the
 * changed version. Permanent storage cannot be changed.
 * <p/>
 * Implementations if this interface are thread safe, so the same object can
 * be used for multiple concurrent operations on the bitstorage.
 * <p/>
 * Files in bitstorage, both the temporary and the permanent storage, will always
 * be accessible by an URL. This URL is assigned when first the file is uploaded.
 * While the file is in temporary storage, the URL is only resolvable on the
 * local network. When the file is approved, the URL becomes permanent, and publically
 * resolvable. The URL does not change when the file is approved.
 */
public interface Bitstorage {

    /**
     * Upload the provided file to the temporary area of bitstorage, giving it
     * the provided file name. Return the MD5 checksum of the file. The file is
     * only uploaded to the temporary bitstorage, and needs to
     * be approved by calling approve before it is moved to the
     * permanent bitstorage.
     * <p/>
     * This method is Idempotent, meaning that multiple uploads of the same
     * file will have the same result. Implementations do not have to
     * perform the reupload, if the provided checksum match the already uploaded
     * file.
     * <p/>
     * While the file is being uploaded, it is locked for other processes. This
     * prevents to persons uploading the same file simultaniously.
     *
     * @param filename   The filename to store the file by
     * @param data       the inputstream with the file contents
     * @param md5        The locally generated md5sum of the file
     * @param filelength The length of the file. The size to reserve before
     *                   before starting upload.
     * @return the resolvable URL to the uploaded file
     * @throws ChecksumFailedException      if the provided checksum does not
     *                                      match the one calculated on the
     *                                      server
     * @throws CommunicationException       on problems communicating with
     *                                      bitstorage
     * @throws FileAlreadyApprovedException if a file with that name has
     *                                      already been added to the
     *                                      bitstorage
     * @throws NotEnoughFreeSpaceException  if there is not enough space for
     *                                      the file to be uploaded
     * @throws InvalidFileNameException     If the filename could not be
     *                                      transformed into a valid URL
     * @throws FileIsLockedException        If the file in bitstorage is already being
     *                                      processed by another process. If you try to upload to a filename that
     *                                      already exist in temporary store, and this file is being worked on
     *                                      by another upload or other function, this exception will be thrown.
     * @see #approve(java.net.URL, String)
     */
    public URL upload(String filename,
                      InputStream data,
                      String md5,
                      long filelength)
            throws NotEnoughFreeSpaceException, 
            InvalidFileNameException, CommunicationException,
            //NotEnoughFreeSpaceException,
            ChecksumFailedException,
            FileAlreadyApprovedException,
            FileIsLockedException;


    /**
     * Delete the named file from bitstorage. Only works for files that have not
     * yet been approved, ie. are in the temporary storage. If the file is approved
     * or not available, nothing is done, and noting is returned. As such, this
     * method is idempotent.
     *
     * @param file The url to the file (in bitstorage)
     * @throws CommunicationException   on problems communicating with bitstorage
     * @throws InvalidFileNameException if the url is not of the format of
     *                                  this bitstorage
     * @throws FileIsLockedException    If the file in bitstorage is already being
     *                                  processed by another process.
     */
    public void disapprove(URL file)
            throws
            CommunicationException,
            InvalidFileNameException,
            FileIsLockedException;


    /**
     * Check the earlier uploaded file against the provided checksum, and if
     * this succeeds, move the file from
     * the temporary bitstorage to the permanent bitstorage.
     * <p/>
     * If you call this method on an already approved file, with the correct
     * checksum, nothing happens, so the method is Idempotent.
     *
     * @param file The url to the file (in bitstorage)
     * @param md5  The md5sum of the file
     * @return the calculated md5sum of the file
     * @throws CommunicationException      on problems communicating with
     *                                     bitstorage
     * @throws FileNotFoundException       if the file was not found in the
     *                                     temporary or the permanent storage
     * @throws NotEnoughFreeSpaceException if there was not enough free space in
     *                                     the permanent storage
     * @throws ChecksumFailedException     if the supplied checksum does not
     *                                     match the checksum of the file
     * @throws InvalidFileNameException    if the url is not of the format of
     *                                     this bitstorage
     * @throws FileIsLockedException       If the file in bitstorage is already being
     *                                     processed by another process.
     */
    public String approve(URL file, String md5)
            throws FileNotFoundException, CommunicationException,
            NotEnoughFreeSpaceException,
            ChecksumFailedException,
            InvalidFileNameException,
            FileIsLockedException;

    /**
     * Return the number of bytes left in permanent bitstorage.
     * <p/>
     * Side effect free.
     *
     * @return The number of bytes left in bitstorage.
     * @throws CommunicationException on problems communicating with bitstorage
     */
    public long spaceLeft() throws CommunicationException;

    /**
     * Return the number of bytes that is allocatable for a single file, ie. the
     * minimum of what is available in temporary storage and permanent storage.
     * <p/>
     * Side effect free.
     *
     * @return The number of bytes left
     * @throws CommunicationException on problems communicating with bitstorage
     */
    public long getMaxFileSize() throws CommunicationException;

    /**
     * Get the server-calculated md5 of a file. Implementations can provide
     * a precalculated checksum or recalculate when this method is invoked.
     * <p/>
     * Aside from the recalculation, there should be no side effects of this methdo.
     *
     * @param file the url of the file
     * @return the md5sum
     * @throws FileNotFoundException    if the file is not found in either
     *                                  permanent or temporary bitstorage.
     * @throws CommunicationException   on problems communicating with bitstorage
     * @throws InvalidFileNameException if the url is not of the format of
     *                                  this bitstorage
     * @throws FileIsLockedException    If the file in bitstorage is already being
     *                                  processed by another process.
     */
    public String getMd5(URL file)
            throws
            FileNotFoundException,
            CommunicationException,
            InvalidFileNameException,
            FileIsLockedException;


    /**
     * Check if the url is of a approved file
     *
     * @param file the url to check
     * @return true if is an approved file, false if it is an temporary file
     * @throws FileNotFoundException    if the file is not found in bitstorage
     * @throws CommunicationException   on problems communicating with bitstorage
     * @throws InvalidFileNameException if the url is not of the format of
     *                                  this bitstorage
     * @throws FileIsLockedException    If the file in bitstorage is already being
     *                                  processed by another process.
     */
    public boolean isApproved(URL file)
            throws
            FileNotFoundException,
            CommunicationException,
            InvalidFileNameException,
            FileIsLockedException;


}
