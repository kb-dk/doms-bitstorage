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

package dk.statsbiblioteket.doms.bitstorage.highlevel.status;

import dk.statsbiblioteket.doms.bitstorage.highlevel.status.Event;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "operation", propOrder = {
        "id",
        "fedoraPid",
        "fedoraDatastream",
        "fileSize",
        "highlevelMethod",
        "history"
})
public class Operation {

    @XmlElement(name = "ID", required = true)
    protected String id;
    @XmlElement(required = true)
    protected String fedoraPid;
    @XmlElement(required = true)
    protected String fedoraDatastream;
    protected Long fileSize;
    @XmlElement(required = true)
    protected String highlevelMethod;

    protected List<Event> history;

    /**
     * Gets the value of the id property.
     *
     * @return possible object is
     *         {@link String }
     */
    public String getID() {
        return id;
    }

    /**
     * Sets the value of the id property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setID(String value) {
        this.id = value;
    }

    /**
     * Gets the value of the fedoraPid property.
     *
     * @return possible object is
     *         {@link String }
     */
    public String getFedoraPid() {
        return fedoraPid;
    }

    /**
     * Sets the value of the fedoraPid property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setFedoraPid(String value) {
        this.fedoraPid = value;
    }

    /**
     * Gets the value of the fedoraDatastream property.
     *
     * @return possible object is
     *         {@link String }
     */
    public String getFedoraDatastream() {
        return fedoraDatastream;
    }

    /**
     * Sets the value of the fedoraDatastream property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setFedoraDatastream(String value) {
        this.fedoraDatastream = value;
    }

    /**
     * Gets the value of the fileSize property.
     *
     * @return possible object is
     *         {@link Long }
     */
    public Long getFileSize() {
        return fileSize;
    }

    /**
     * Sets the value of the fileSize property.
     *
     * @param value allowed object is
     *              {@link Long }
     */
    public void setFileSize(Long value) {
        this.fileSize = value;
    }

    /**
     * Gets the value of the highlevelMethod property.
     *
     * @return possible object is
     *         {@link String }
     */
    public String getHighlevelMethod() {
        return highlevelMethod;
    }

    /**
     * Sets the value of the highlevelMethod property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setHighlevelMethod(String value) {
        this.highlevelMethod = value;
    }

    /**
     * Gets the value of the history property.
     * <p/>
     * <p/>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the history property.
     * <p/>
     * <p/>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getHistory().add(newItem);
     * </pre>
     * <p/>
     * <p/>
     * <p/>
     * Objects of the following type(s) are allowed in the list
     * {@link Event }
     */
    public List<Event> getHistory() {
        if (history == null) {
            history = new ArrayList<Event>();
        }
        return this.history;
    }

}
