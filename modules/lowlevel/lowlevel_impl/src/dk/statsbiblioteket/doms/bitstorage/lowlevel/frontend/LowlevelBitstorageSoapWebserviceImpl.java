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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.statsbiblioteket.doms.bitstorage.lowlevel.ChecksumFailedException;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.CommunicationException;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.FileAlreadyApprovedException;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.InvalidFilenameException;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.LowlevelBitstorageSoapWebservice;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.LowlevelSoapException;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.NotEnoughFreeSpaceException;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.Bitstorage;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.BitstorageFactory;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.exceptions.BitstorageException;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.exceptions.BitstorageToLowlevelExceptionMapper;
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
public class LowlevelBitstorageSoapWebserviceImpl implements LowlevelBitstorageSoapWebservice {
    /**
     * An exception mapper that maps exceptions from the underlying bitstorage
     * to SOAP faults.
     */
    private BitstorageToLowlevelExceptionMapper bitstorageMapper
            = new BitstorageToLowlevelExceptionMapper();

    /** The logger for this class. */
    private static final Log LOG
            = LogFactory.getLog(LowlevelBitstorageSoapWebserviceImpl.class);

    /**
     * Upload the provided file for later approval, for details, see
     * {@link Bitstorage#upload(String, InputStream, String, long)}.
     *
     * The data for the file should be streamed to the underlying service.
     *
     * This method works as a fault barrier, handling exceptions by converting
     * them to relecant SOAP faults.
     *
     * @param filename The name to give the file to upload.
     * @param filedata The data for the file.
     * @param md5String MD5 checksum of the data.
     * @param filelength Size of the data.
     * @return The checksum calculated by the server.
     * @throws ChecksumFailedException If the server calculated a different
     * checksum than the given checksum
     * @throws CommunicationException On generic trouble communicating with the
     * underlying script.
     * @throws FileAlreadyApprovedException If a file with the given name is
     * already in approved storage
     * @throws InvalidFilenameException If the given filename cannot be used
     * @throws NotEnoughFreeSpaceException If there is not enough space to store
     * the file.
     * @throws LowlevelSoapException On internal errors that are not correctly
     * mapped to SOAP faults. Should never happen.
     * @throws WebServiceException On other unclassified errors. Should never
     * happen.
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
            FileAlreadyApprovedException, InvalidFilenameException,
            NotEnoughFreeSpaceException, LowlevelSoapException,
            WebServiceException {
        LOG.trace("Enter uploadFile('" + filename + "','" + filedata + "','"
                + md5String + "','" + filelength + "')");
        String errorMessage = "Trouble while uploading file '" + filename + "'";
        try {
            Bitstorage bs =  BitstorageFactory.getInstance();
            //TODO: Look into MTOM streaming
            /*        StreamingDataHandler dh = (StreamingDataHandler) filedata;*/
            return bs.upload(
                    filename, filedata.getInputStream(), md5String, filelength)
                    .toString();
        } catch (BitstorageException e) {
            LOG.error(errorMessage, e);
            throw bitstorageMapper.convertMostApplicable(e);
        } catch (Exception e){
            LOG.error(errorMessage, e);
            throw new WebServiceException(errorMessage + ": " + e, e);
        }
    }

    @WebMethod
    public void disapprove(@WebParam(name = "fileurl",
            targetNamespace = "") String fileurl)
            throws LowlevelSoapException {
        Bitstorage bs =
                BitstorageFactory.getInstance();
        try {
            bs.disapprove(new URL(fileurl));
        } catch (BitstorageException e) {
            throw bitstorageMapper.convertMostApplicable(e);
        } catch (MalformedURLException e) {
            throw new InvalidFilenameException(
                    e.getMessage(),
                    e.getMessage(),
                    e);

        } catch (Exception e){
            LOG.error(e);
            throw new WebServiceException(e);
        }


    }

    @WebMethod
    public void approve(@WebParam(name = "fileurl",
            targetNamespace = "") String fileurl,
                        @WebParam(name = "md5string",
                                targetNamespace = "") String md5String)
            throws LowlevelSoapException {
        Bitstorage bs =
                BitstorageFactory.getInstance();
        try {
            bs.approve(new URL(fileurl), md5String);
        } catch (BitstorageException e) {
            throw bitstorageMapper.convertMostApplicable(e);
        } catch (MalformedURLException e) {
            throw new dk.statsbiblioteket.doms.bitstorage.lowlevel.FileNotFoundException(
                    e.getMessage(),
                    e.getMessage(),
                    e);

        } catch (Exception e){
            LOG.error(e);
            throw new WebServiceException(e);
        }


    }

    @WebMethod
    public long spaceLeft() throws LowlevelSoapException {
        Bitstorage bs =
                BitstorageFactory.getInstance();


        try {
//            throw new dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.exceptions.CommunicationException("My test exception");
            return bs.spaceLeft();
        } catch (BitstorageException e) {
            throw bitstorageMapper.convertMostApplicable(e);
        } catch (Exception e){
            LOG.error(e);
            throw new WebServiceException(e);
        }


    }

    @WebMethod
    public long getMaxFileSize() throws LowlevelSoapException {
        Bitstorage bs =
                BitstorageFactory.getInstance();
        try {
            return bs.getMaxFileSize();
        } catch (BitstorageException e) {
            throw bitstorageMapper.convertMostApplicable(e);
        } catch (Exception e){
            LOG.error(e);
            throw new WebServiceException(e);
        }

    }

    @WebMethod
    public String getMd5(@WebParam(name = "fileurl",
            targetNamespace = "") String fileurl)
            throws LowlevelSoapException {
        Bitstorage bs =
                BitstorageFactory.getInstance();
        try {
            return bs.getMd5(new URL(fileurl));
        } catch (BitstorageException e) {
            throw bitstorageMapper.convertMostApplicable(e);
        } catch (MalformedURLException e) {
            throw new dk.statsbiblioteket.doms.bitstorage.lowlevel.FileNotFoundException(
                    e.getMessage(),
                    e.getMessage(),
                    e);

        } catch (Exception e){
            LOG.error(e);
            throw new WebServiceException(e);
        }


    }

    @WebMethod
    public boolean isApproved(@WebParam(name = "fileurl",
            targetNamespace = "") String fileurl)
            throws LowlevelSoapException {
        Bitstorage bs =
                BitstorageFactory.getInstance();
        try {
            return bs.isApproved(new URL(fileurl));
        } catch (BitstorageException e) {
            throw bitstorageMapper.convertMostApplicable(e);
        } catch (MalformedURLException e) {
            throw new dk.statsbiblioteket.doms.bitstorage.lowlevel.FileNotFoundException(
                    e.getMessage(),
                    e.getMessage(),
                    e);

        } catch (Exception e){
            LOG.error(e);
            throw new WebServiceException(e);
        }

    }


}