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


import dk.statsbiblioteket.doms.bitstorage.lowlevel.LowlevelBitstorageSoapWebservice;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.LowlevelSoapException;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.Bitstorage;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.BitstorageFactory;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.exceptions.BitstorageException;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.exceptions.BitstorageToLowlevelExceptionMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.activation.DataHandler;
import javax.annotation.Resource;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.servlet.ServletContext;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.soap.MTOM;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

@MTOM
@WebService(endpointInterface = "dk.statsbiblioteket.doms.bitstorage.lowlevel.LowlevelBitstorageSoapWebservice")
public class LowlevelBitstorageSoapWebserviceImpl
        implements LowlevelBitstorageSoapWebservice {

    Bitstorage bs;

    BitstorageToLowlevelExceptionMapper bitstorageMapper = new BitstorageToLowlevelExceptionMapper();
    Log log = LogFactory.getLog(this.getClass());

    @Resource
    private WebServiceContext webServiceContext;

    private void initialise() {
        if (bs != null) {
            return;
        }
        ServletContext servletContext =
                (ServletContext) webServiceContext.getMessageContext().get(
                        MessageContext.SERVLET_CONTEXT);

/*
        Enumeration parameters = servletContext.getInitParameterNames();
        while (parameters.hasMoreElements()) {
            String s = (String) parameters.nextElement();
            System.out.println(s);
        }
*/
        String script = servletContext.getInitParameter("script");
        String server = servletContext.getInitParameter("server");
        String bitfinder = servletContext.getInitParameter("bitfinder");
        bs = BitstorageFactory.getInstance(script,server,bitfinder);
    }

    public String uploadFile(@WebParam(name = "filename",
                                       targetNamespace = "") String filename,
                             @WebParam(name = "filedata",
                                       targetNamespace = "") DataHandler filedata,
                             @WebParam(name = "md5string",
                                       targetNamespace = "") String md5String,
                             @WebParam(name = "filelength",
                                       targetNamespace = "") long filelength)
            throws LowlevelSoapException {
        initialise();
/*        StreamingDataHandler dh = (StreamingDataHandler) filedata;*/
        try {

            return bs.upload(filename,
                             filedata.getInputStream(),
                             md5String,
                             filelength).toString();
        } catch (IOException e) {
            throw new WebServiceException(e);
        } catch (BitstorageException e) {
            throw bitstorageMapper.convertMostApplicable(e);
        }
        catch (Exception e){
            log.error(e);
            throw new WebServiceException(e);
        }

    }

    @WebMethod
    public void disapprove(@WebParam(name = "fileurl",
                                     targetNamespace = "") String fileurl)
            throws LowlevelSoapException {
        initialise();
        try {
            bs.disapprove(new URL(fileurl));
        } catch (BitstorageException e) {
            throw bitstorageMapper.convertMostApplicable(e);
        } catch (MalformedURLException e) {
            throw new dk.statsbiblioteket.doms.bitstorage.lowlevel.FileNotFoundException(
                    e.getMessage(),
                    e.getMessage(),
                    e);

        } catch (Exception e){
            log.error(e);
            throw new WebServiceException(e);
        }


    }

    @WebMethod
    public void approve(@WebParam(name = "fileurl",
                                  targetNamespace = "") String fileurl,
                        @WebParam(name = "md5string",
                                  targetNamespace = "") String md5String)
            throws LowlevelSoapException {
        initialise();
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
            log.error(e);
            throw new WebServiceException(e);
        }


    }

    @WebMethod
    public long spaceleft() throws LowlevelSoapException {
        initialise();


        try {
//            throw new dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.exceptions.CommunicationException("My test exception");
            return bs.spaceleft();
        } catch (BitstorageException e){
            throw bitstorageMapper.convertMostApplicable(e);
        } catch (Exception e){
            log.error(e);
            throw new WebServiceException(e);
        }



    }

    @WebMethod
    public long getMaxFileSize() throws LowlevelSoapException {
        initialise();
        try {
            return bs.getMaxFileSize();
        } catch (BitstorageException e) {
            throw bitstorageMapper.convertMostApplicable(e);
        } catch (Exception e){
            log.error(e);
            throw new WebServiceException(e);
        }

    }

    @WebMethod
    public String getMd5(@WebParam(name = "fileurl",
                                   targetNamespace = "") String fileurl)
            throws LowlevelSoapException{
        initialise();
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
            log.error(e);
            throw new WebServiceException(e);
        }


    }

    @WebMethod
    public boolean isApproved(@WebParam(name = "fileurl",
                                        targetNamespace = "") String fileurl)
            throws LowlevelSoapException{
        initialise();
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
            log.error(e);
            throw new WebServiceException(e);
        }

    }


}