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

package dk.statsbiblioteket.doms.bitstorage.lowlevel.frontend;

import com.sun.xml.ws.developer.StreamingDataHandler;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.ChecksumFailedException;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.CommunicationException;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.FileIsLockedException;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.FileNotFoundException;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.NotEnoughFreeSpaceException;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.exceptions.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


import dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.Bitstorage;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.BitstorageFactory;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.*;
import dk.statsbiblioteket.util.qa.QAInfo;

import javax.activation.DataHandler;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.soap.MTOM;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Web service that exposes a low level bitstorage.
 * This class handles communication with SOAP and delegating calls to the
 * underlying bitstorage. Exceptions are logged and delegated as SOAP faults.
 */
@MTOM
@WebService(
        endpointInterface = "dk.statsbiblioteket.doms.bitstorage.lowlevel.LowlevelBitstorageSoapWebservice")
@QAInfo(author = "abr",
        reviewers = "kfc",
        level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_OK)
public class LowlevelBitstorageSoapWebserviceImpl
        implements LowlevelBitstorageSoapWebservice {

    /**
     * The logger for this class.
     */
    private static final Log LOG
            = LogFactory.getLog(LowlevelBitstorageSoapWebserviceImpl.class);

    /**
     * Upload the provided file for later approval, for details, see
     * {@link Bitstorage#upload(String, InputStream, String, long)}.
     * <p/>
     * The data for the file should be streamed to the underlying service.
     * <p/>
     * This method works as a fault barrier, handling exceptions by converting
     * them to relecant SOAP faults.
     *
     * @param filename   The name to give the file to upload.
     * @param filedata   The data for the file.
     * @param md5String  MD5 checksum of the data.
     * @param filelength Size of the data.
     * @return The checksum calculated by the server.
     * @throws ChecksumFailedException     If the server calculated a different
     *                                     checksum than the given checksum
     * @throws CommunicationException      On generic trouble communicating with the
     *                                     underlying script.
     * @throws NotEnoughFreeSpaceException If there is not enough space to store
     *                                     the file.
     * @throws FileIsLockedException       If the file is locked by another operation.
     * @throws WebServiceException         On other unclassified errors. Should never
     *                                     happen.
     */
    @WebMethod
    public String uploadFile(@WebParam(name = "filename",
                                       targetNamespace = "")
                             String filename,
                             @WebParam(name = "filedata",
                                       targetNamespace = "")
                             DataHandler filedata,
                             @WebParam(name = "md5string",
                                       targetNamespace = "")
                             String md5String,
                             @WebParam(name = "filelength",
                                       targetNamespace = "")
                             long filelength)
            throws ChecksumFailedException, CommunicationException,
                   NotEnoughFreeSpaceException, FileIsLockedException, WebServiceException {
        LOG.trace("Enter uploadFile('" + filename + "','" + filedata + "','"
                  + md5String + "','" + filelength + "')");
        String errorMessage = "Trouble while uploading file '" + filename + "'";
        try {
            Bitstorage bs = BitstorageFactory.getInstance();
            InputStream data;
            if (filedata instanceof StreamingDataHandler) {
                LOG.trace("Reading data for file '" + filedata
                          + "' as streaming data");
                data = ((StreamingDataHandler) filedata).readOnce();
            } else {
                LOG.trace("Reading data for file '" + filedata
                          + "' as non-streaming data");
                data = filedata.getInputStream();
            }
            try {
                return bs.upload(filename, data, md5String, filelength)
                        .toString();
            } finally {
                LOG.trace("Function over, returning");
                data.close();
                LOG.trace("data stream closed");
            }
        } catch (BitstorageException ce) {
            LOG.error(errorMessage, ce);

            if (ce instanceof dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.exceptions.ChecksumFailedException) {
                throw new dk.statsbiblioteket.doms.bitstorage.lowlevel.ChecksumFailedException(
                        ce.getMessage(), ce.getMessage(), ce);
            } else if (ce instanceof dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.exceptions.CommunicationException) {
                throw new dk.statsbiblioteket.doms.bitstorage.lowlevel.CommunicationException(
                        ce.getMessage(), ce.getMessage(), ce);

            } else if (ce instanceof dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.exceptions.FileAlreadyApprovedException) {

            } else if (ce instanceof dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.exceptions.FileIsLockedException) {
                throw new dk.statsbiblioteket.doms.bitstorage.lowlevel.FileIsLockedException(
                        ce.getMessage(), ce.getMessage(), ce);

            } else if (ce instanceof dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.exceptions.NotEnoughFreeSpaceException) {
                new dk.statsbiblioteket.doms.bitstorage.lowlevel.NotEnoughFreeSpaceException(
                        ce.getMessage(), ce.getMessage(), ce);
            }
            throw new WebServiceException(errorMessage + ": " + ce, ce);

        } catch (Exception e) {
            LOG.error(errorMessage, e);
            throw new WebServiceException(errorMessage + ": " + e, e);
        }
    }

    /**
     * Remove a file that has not yet been approved, for details see
     * {@link Bitstorage#disapprove(URL)}.
     * <p/>
     * This method works as a fault barrier, handling exceptions by converting
     * them to relecant SOAP faults.
     *
     * @param fileurl The url of the file to disapprove.
     * @throws CommunicationException On generic trouble communicating with the
     *                                underlying script.
     * @throws FileIsLockedException  If the file is locked by another operation.
     * @throws WebServiceException    On other unclassified errors. Should never
     *                                happen.
     */
    @WebMethod
    public void disapprove(@WebParam(name = "fileurl",
                                     targetNamespace = "") String fileurl)
            throws
            CommunicationException,
            WebServiceException {
        LOG.trace("Enter disapprove('" + fileurl + "')");
        String errorMessage = "Trouble while disapproving file '" + fileurl
                              + "'";
        try {
            Bitstorage bs = BitstorageFactory.getInstance();
            bs.disapprove(new URL(fileurl));
        } catch (BitstorageException ce) {
            LOG.error(errorMessage, ce);

            if (ce instanceof dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.exceptions.CommunicationException) {
                throw new dk.statsbiblioteket.doms.bitstorage.lowlevel.CommunicationException(
                        ce.getMessage(), ce.getMessage(), ce);
            }
            throw new WebServiceException(errorMessage + ": " + ce, ce);
        } catch (Exception e) {
            LOG.error(errorMessage, e);
            throw new WebServiceException(errorMessage + ": " + e, e);
        }


    }

    /**
     * Approve a file for permanent storage, for details see
     * {@link Bitstorage#approve(URL, String)}.
     * <p/>
     * This method works as a fault barrier, handling exceptions by converting
     * them to relecant SOAP faults.
     *
     * @param fileurl   The url of the file to approve.
     * @param md5String The md5 checksum of files.
     * @throws CommunicationException      On generic trouble communicating with the
     *                                     underlying script.
     * @throws ChecksumFailedException     If the file on server has a different
     *                                     checksum than the given checksum
     * @throws FileIsLockedException       If the file is locked by another operation.
     * @throws WebServiceException         On other unclassified errors. Should never
     *                                     happen.
     */
    @WebMethod
    public void approve(@WebParam(name = "fileurl",
                                  targetNamespace = "") String fileurl,
                        @WebParam(name = "md5string",
                                  targetNamespace = "") String md5String)
            throws CommunicationException,
                   ChecksumFailedException,
                   WebServiceException {
        LOG.trace("Enter approve('" + fileurl + "', '" + md5String + "')");
        String errorMessage = "Trouble while approving file '" + fileurl + "'";
        try {
            Bitstorage bs = BitstorageFactory.getInstance();
            bs.approve(new URL(fileurl), md5String);
        } catch (BitstorageException ce) {
            LOG.error(errorMessage, ce);

            if (ce instanceof dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.exceptions.ChecksumFailedException) {
                throw new dk.statsbiblioteket.doms.bitstorage.lowlevel.ChecksumFailedException(
                        ce.getMessage(), ce.getMessage(), ce);
            } else if (ce instanceof dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.exceptions.CommunicationException) {
                throw new dk.statsbiblioteket.doms.bitstorage.lowlevel.CommunicationException(
                        ce.getMessage(), ce.getMessage(), ce);
            }
            throw new WebServiceException(errorMessage + ": " + ce, ce);
        } catch (Exception e) {
            LOG.error(errorMessage, e);
            throw new WebServiceException(errorMessage + ": " + e, e);
        }
    }

    /**
     * Get amount of free space in bitstorage, for details see
     * {@link Bitstorage#spaceLeft()}.
     * <p/>
     * This method works as a fault barrier, handling exceptions by converting
     * them to relecant SOAP faults.
     *
     * @return Amount of free space in bytes.
     * @throws CommunicationException On generic trouble communicating with the
     *                                underlying script.
     * @throws WebServiceException    On other unclassified errors. Should never
     *                                happen.
     */
    @WebMethod
    public long spaceLeft() throws CommunicationException, WebServiceException {
        LOG.trace("Enter spaceLeft()");
        String errorMessage = "Trouble while checking free space";
        try {
            Bitstorage bs = BitstorageFactory.getInstance();
            return bs.spaceLeft();
        } catch (BitstorageException ce) {
            LOG.error(errorMessage, ce);

            if (ce instanceof dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.exceptions.CommunicationException) {
                throw new dk.statsbiblioteket.doms.bitstorage.lowlevel.CommunicationException(
                        ce.getMessage(), ce.getMessage(), ce);
            }
            throw new WebServiceException(errorMessage + ": " + ce, ce);

        } catch (Exception e) {
            LOG.error(errorMessage, e);
            throw new WebServiceException(errorMessage + ": " + e, e);
        }
    }

    /**
     * Get amount of free space in bitstorage for a single file, for details see
     * {@link Bitstorage#getMaxFileSize()}.
     * <p/>
     * This method works as a fault barrier, handling exceptions by converting
     * them to relecant SOAP faults.
     *
     * @return Amount of free space in bytes for one single file.
     * @throws CommunicationException On generic trouble communicating with the
     *                                underlying script.
     * @throws WebServiceException    On other unclassified errors. Should never
     *                                happen.
     */
    @WebMethod
    public long getMaxFileSize() throws CommunicationException,
                                        WebServiceException {
        LOG.trace("Enter getMaxFileSize()");
        String errorMessage = "Trouble while checking free space";
        try {
            Bitstorage bs = BitstorageFactory.getInstance();
            return bs.getMaxFileSize();
        } catch (BitstorageException ce) {
            LOG.error(errorMessage, ce);

            if (ce instanceof dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.exceptions.CommunicationException) {
                throw new dk.statsbiblioteket.doms.bitstorage.lowlevel.CommunicationException(
                        ce.getMessage(), ce.getMessage(), ce);

            }
            throw new WebServiceException(errorMessage + ": " + ce, ce);


        } catch (Exception e) {
            LOG.error(errorMessage, e);
            throw new WebServiceException(errorMessage + ": " + e, e);
        }
    }

    /**
     * Get checksum for a file, for details see
     * {@link Bitstorage#getMd5(URL)}.
     * <p/>
     * This method works as a fault barrier, handling exceptions by converting
     * them to relecant SOAP faults.
     *
     * @param fileurl The url of the file to get checksum for.
     * @return Checksum for file.
     * @throws FileNotFoundException  If the file does not exist in any storage.
     * @throws CommunicationException On generic trouble communicating with the
     *                                underlying script.
     * @throws WebServiceException    On other unclassified errors. Should never
     *                                happen.
     */
    @WebMethod
    public String getMd5(@WebParam(name = "fileurl",
                                   targetNamespace = "")
                         String fileurl)
            throws FileNotFoundException, CommunicationException, WebServiceException {
        LOG.trace("Enter getMd5('" + fileurl + "')");
        String errorMessage = "Trouble while getting checksum for fileurl '"
                              + fileurl + "'";
        try {
            Bitstorage bs = BitstorageFactory.getInstance();
            return bs.getMd5(new URL(fileurl));
        } catch (BitstorageException ce) {
            LOG.error(errorMessage, ce);

            if (ce instanceof dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.exceptions.CommunicationException) {
                throw new dk.statsbiblioteket.doms.bitstorage.lowlevel.CommunicationException(
                        ce.getMessage(), ce.getMessage(), ce);

            } else if (ce instanceof dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.exceptions.FileNotFoundException) {
                throw new dk.statsbiblioteket.doms.bitstorage.lowlevel.FileNotFoundException(
                        ce.getMessage(), ce.getMessage(), ce);
            }
            throw new WebServiceException(errorMessage + ": " + ce, ce);


        } catch (Exception e) {
            LOG.error(errorMessage, e);
            throw new WebServiceException(errorMessage + ": " + e, e);
        }
    }

    /**
     * Check whether a file is approved, for details see
     * {@link Bitstorage#isApproved(URL)}.
     * <p/>
     * This method works as a fault barrier, handling exceptions by converting
     * them to relecant SOAP faults.
     *
     * @param fileurl The url of the file to check whether is approved.
     * @return true if exists and is approved, false if exists but is not yet
     *         approved.
     * @throws FileNotFoundException  If the file does not exist in any storage.
     * @throws CommunicationException On generic trouble communicating with the
     *                                underlying script.
     * @throws WebServiceException    On other unclassified errors. Should never
     *                                happen.
     */
    @WebMethod
    public boolean isApproved(@WebParam(name = "fileurl",
                                        targetNamespace = "")
                              String fileurl)
            throws FileNotFoundException, CommunicationException, WebServiceException {
        LOG.trace("Enter getMd5('" + fileurl + "')");
        String errorMessage = "Trouble while checking if fileurl '"
                              + fileurl + "' is approved";
        try {
            Bitstorage bs = BitstorageFactory.getInstance();
            return bs.isApproved(new URL(fileurl));
        } catch (BitstorageException ce) {
            LOG.error(errorMessage, ce);

            if (ce instanceof dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.exceptions.CommunicationException) {
                throw new dk.statsbiblioteket.doms.bitstorage.lowlevel.CommunicationException(
                        ce.getMessage(), ce.getMessage(), ce);
            } else if (ce instanceof dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.exceptions.FileNotFoundException) {
                throw new dk.statsbiblioteket.doms.bitstorage.lowlevel.FileNotFoundException(
                        ce.getMessage(), ce.getMessage(), ce);
            }
            throw new WebServiceException(errorMessage + ": " + ce, ce);

        } catch (Exception e) {
            LOG.error(errorMessage, e);
            throw new WebServiceException(errorMessage + ": " + e, e);
        }
    }
}