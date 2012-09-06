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

import dk.statsbiblioteket.doms.bitstorage.highlevel.fedora.generated.DatastreamProfile;
import dk.statsbiblioteket.doms.bitstorage.highlevel.fedora.generated.ObjectProfile;
import dk.statsbiblioteket.doms.webservices.configuration.ConfigCollection;
import dk.statsbiblioteket.util.caching.TimeSensitiveCache;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Created by IntelliJ IDEA.
 * User: abr
 * Date: Oct 21, 2010
 * Time: 3:03:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class Caches {

    private Log log = LogFactory.getLog(getClass());

    TimeSensitiveCache<String, ObjectProfile> objectProfiles;

    TimeSensitiveCache<String, DatastreamProfile> datastreamProfiles;

    TimeSensitiveCache<String, Object> datastreamContents;

    public Caches() {
        log.trace("Entered constructor Caches()");
        long lifetime
                = Long.parseLong(ConfigCollection.getProperties().getProperty(
                "dk.statsbiblioteket.doms.bitstorage.highlevel.connectors.fedora.cache.lifetime",
                "" + 1000 * 60 * 10));
        int size
                = Integer.parseInt(ConfigCollection.getProperties().getProperty(
                "dk.statsbiblioteket.doms.bitstorage.highlevel.connectors.fedora.cache.size",
                "" + 20));
        objectProfiles = new TimeSensitiveCache<String, ObjectProfile>(
                lifetime,
                true,
                size);
        datastreamProfiles = new TimeSensitiveCache<String, DatastreamProfile>(
                lifetime,
                true,
                size);
        datastreamContents = new TimeSensitiveCache<String, Object>(lifetime,
                                                                    true,
                                                                    size);
    }

    public ObjectProfile getObjectProfile(String pid) {
        if (pidProtection(pid)) {
            return objectProfiles.get(pid);
        }
        return null;
    }

    public void removeObjectProfile(String pid) {
        objectProfiles.remove(pid);
    }

    public void storeObjectProfile(String pid, ObjectProfile profile) {
        if (pidProtection(pid)) {
            objectProfiles.put(pid, profile);
        }
    }


    public <T> T getDatastreamContents(String pid,
                                       String datastream,
                                       Class<T> returnvalue) {
        if (pidProtection(pid)) {
            Object value = datastreamContents.get(mergeStrings(pid,
                                                               datastream));
            if (returnvalue.isInstance(value)) {
                return returnvalue.cast(value);
            } else {
                removeDatastreamContents(pid, datastream);
                return null;
            }
        }
        return null;
    }


    public <T> void storeDatastreamContents(String pid,
                                            String datastream,
                                            Class<T> returnvalue, T contents) {
        if (pidProtection(pid)) {
            datastreamContents.put(mergeStrings(pid, datastream), contents);
        }
    }

    public void removeDatastreamContents(String pid, String ds) {
        datastreamContents.remove(mergeStrings(pid, ds));
    }


    public void removeDatastreamProfile(String pid, String datastream) {
        datastreamProfiles.remove(mergeStrings(pid, datastream));
    }

    public DatastreamProfile getDatastreamProfile(String pid,
                                                  String datastreamname) {
        if (pidProtection(pid)) {
            return datastreamProfiles.get(mergeStrings(pid, datastreamname));
        }
        return null;
    }

    public void storeDatastreamProfile(String pid,
                                       String datastreamname,
                                       DatastreamProfile profile) {
        if (pidProtection(pid)) {
            datastreamProfiles.put(mergeStrings(pid, datastreamname), profile);
        }
    }

    private String mergeStrings(String... strings) {
        String result = "";
        for (int i = 0; i < strings.length; i++) {
            String string = strings[i];
            if (i == 0) {
                result = string;
            } else {
                result = result + "/" + string;
            }

        }
        return result;
    }


    private boolean pidProtection(String pid) {
        pid = pid.replaceAll("info:fedora/", "");
        if (pid.startsWith("doms:")) {
            return true;
        }
        return false;
    }

}
