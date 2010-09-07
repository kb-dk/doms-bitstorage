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

package dk.statsbiblioteket.doms.bitstorage.highlevel.fedora;

import dk.statsbiblioteket.doms.bitstorage.characteriser.Characterisation;
import dk.statsbiblioteket.doms.bitstorage.highlevel.fedora.exceptions.*;
import dk.statsbiblioteket.util.qa.QAInfo;

import javax.xml.ws.WebServiceContext;
import java.util.Collection;

/**
 * This is the interface to the Fedora Commons Repository System. These methods
 * are quite high level, and are meant to separate the concerns of the bitstorage
 * system from the actual fedora integration.
 * <p/>
 * TODO explain how a Fedora file object works
 */
@QAInfo(author = "abr",
        reviewers = "",
        level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT)
public interface FedoraSpeaker {


    /**
     * Store the file content in the content datastream in an object
     *
     * @param pid      the pid of the object
     * @param url      the URL of the content
     * @param checksum the checksum for the ocntent
     * @throws FedoraObjectNotFoundException if the object does not exist
     * @throws FedoraDatastreamAlreadyExistException
     *                                       If the object already
     *                                       has a content datastream
     * @throws FedoraCommunicationException  if something else failed
     * @throws FedoraChecksumFailedException if the checksum does not match
     *                                       what fedora calculated
     */
    public void createContentDatastream(String pid,
                                        String url,
                                        String checksum,
                                        String filename)
            throws
            FedoraAuthenticationException,
            FedoraObjectNotFoundException,
            FedoraDatastreamAlreadyExistException,
            FedoraCommunicationException,
            FedoraChecksumFailedException;

    /**
     * Updates a content datastream with new content.
     *
     * @param pid      the pid of the object
     * @param url      the URL of the content
     * @param checksum the checksum for the ocntent
     * @param filename
     * @throws FedoraObjectNotFoundException if the object does not exist
     * @throws FedoraCommunicationException  if something else failed
     * @throws FedoraChecksumFailedException if the checksum does not match
     *                                       what fedora calculated
     */
    public void updateContentDatastream(String pid,
                                        String url,
                                        String checksum, String filename)
            throws
            FedoraAuthenticationException,
            FedoraObjectNotFoundException,
            FedoraCommunicationException,
            FedoraDatastreamAlreadyExistException,
            FedoraDatastreamNotFoundException;


    /**
     * Get the allowed format URIs for a datastream, read from the content
     * models of the object
     *
     * @param pid        the pid of the object
     * @param datastream the name of the datastream
     * @return The list of formats uris.
     * @throws FedoraObjectNotFoundException if the object does not exist
     * @throws FedoraDatastreamNotFoundException
     *                                       if the object does not have a
     *                                       datastream with that name
     * @throws FedoraCommunicationException  if something else failed
     */
    public Collection<String> getAllowedFormatURIs(String pid,
                                                   String datastream)
            throws
            FedoraAuthenticationException,
            FedoraObjectNotFoundException,
            FedoraDatastreamNotFoundException,
            FedoraCommunicationException;


    /**
     * Store the characterisation blob in the correct datastream in the object
     *
     * @param pid              the pid of the object
     * @param characterisation the characterisation blob to store
     * @throws FedoraObjectNotFoundException if the object does not exist
     * @throws FedoraCommunicationException  if something else failed
     * @throws FedoraSerializationExcecption if the characterisation blob could
     *                                       not be serialized into a storable format
     */
    public void storeCharacterization(String pid,
                                      Characterisation characterisation,
                                      WebServiceContext context)
            throws
            FedoraAuthenticationException,
            FedoraObjectNotFoundException,
            FedoraSerializationExcecption,
            FedoraCommunicationException,
            FedoraDatastreamAlreadyExistException;

    /**
     * Check if the datastream exists in the object.
     *
     * @param pid        the pid of the object
     * @param datastream the name of the datastream
     * @return true if the datastream exists
     * @throws FedoraObjectNotFoundException if the object does not exist
     * @throws FedoraCommunicationException  if something else failed
     */
    public boolean datastreamExists(String pid,
                                    String datastream)
            throws
            FedoraAuthenticationException,
            FedoraObjectNotFoundException,
            FedoraCommunicationException;

    /**
     * TODO
     * Odd method that checks if the datastream has content.
     *
     * @param pid        the pid of the object
     * @param datastream the name of the datastream
     * @return true if the datastream has content
     * @throws FedoraObjectNotFoundException if the object does not exist
     * @throws FedoraDatastreamNotFoundException
     *                                       if the object does not have a
     *                                       datastream with that name
     * @throws FedoraCommunicationException  if something else failed
     */
    public boolean datastreamHasContent(String pid,
                                        String datastream)
            throws
            FedoraAuthenticationException,
            FedoraObjectNotFoundException,
            FedoraDatastreamNotFoundException,
            FedoraCommunicationException;

    /**
     * Delete a datastream from an object
     *
     * @param pid        the pid of the object
     * @param datastream the name of the datastream
     * @throws FedoraObjectNotFoundException if the object does not exist
     * @throws FedoraDatastreamNotFoundException
     *                                       if the object does not have a
     *                                       datastream with that name
     * @throws FedoraCommunicationException  if something else failed
     */
    void deleteDatastream(String pid,
                          String datastream)
            throws
            FedoraAuthenticationException,
            FedoraObjectNotFoundException,
            FedoraDatastreamNotFoundException,
            FedoraCommunicationException;


    /**
     * Get the URL to the content from a file object.
     *
     * @param pid the pid of the object
     * @return the URL to the stored file
     * @throws FedoraObjectNotFoundException if the object does not exist
     * @throws FedoraDatastreamNotFoundException
     *                                       if the object does not have a
     *                                       content datastream
     * @throws FedoraCommunicationException  if something else failed
     */
    public String getFileUrl(String pid)
            throws
            FedoraAuthenticationException,
            FedoraObjectNotFoundException,
            FedoraDatastreamNotFoundException,
            FedoraCommunicationException;

    /**
     * Get the checksum of the content from a file object
     *
     * @param pid the pid of the object
     * @return the Fedora stored checksum
     * @throws FedoraObjectNotFoundException if the object does not exist
     * @throws FedoraDatastreamNotFoundException
     *                                       if the object does not have a
     *                                       content datastream
     * @throws FedoraCommunicationException  if something else failed
     */
    public String getFileChecksum(String pid)
            throws
            FedoraAuthenticationException,
            FedoraObjectNotFoundException,
            FedoraDatastreamNotFoundException,
            FedoraCommunicationException;


    /**
     * Sets the label of the Fedora object
     *
     * @param pid   the pid of the object
     * @param label the label
     */
    public void setObjectLabel(String pid, String label)
            throws
            FedoraAuthenticationException,
            FedoraObjectNotFoundException,
            FedoraCommunicationException;

}
