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

import dk.statsbiblioteket.doms.bitstorage.highlevel.fedora.generated.ObjectProfile;
import dk.statsbiblioteket.doms.bitstorage.highlevel.fedora.generated.DatastreamProfile;
import dk.statsbiblioteket.util.caching.TimeSensitiveCache;

/**
 * Created by IntelliJ IDEA.
 * User: abr
 * Date: Oct 21, 2010
 * Time: 3:03:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class Caches {

    TimeSensitiveCache<String, ObjectProfile> objectProfiles;

    TimeSensitiveCache<String, DatastreamProfile> datastreamProfiles;

    TimeSensitiveCache<String, Object> datastreamContents;

    public Caches() {
        long timeToLive = 1000 * 60 * 10;
        objectProfiles = new TimeSensitiveCache<String, ObjectProfile>(
                timeToLive,
                true,
                25);
        datastreamProfiles = new TimeSensitiveCache<String, DatastreamProfile>(
                timeToLive,
                true,
                25);
        datastreamContents = new TimeSensitiveCache<String, Object>(timeToLive,
                                                                    true,
                                                                    25);
    }

    public ObjectProfile getObjectProfile(String pid) {
        return objectProfiles.get(pid);
    }

    public void removeObjectProfile(String pid) {
        objectProfiles.remove(pid);
    }

    public void storeObjectProfile(String pid, ObjectProfile profile) {
        objectProfiles.put(pid, profile);
    }


    public <T> T getDatastreamContents(String pid,
                                       String datastream,
                                       Class<T> returnvalue) {
        Object value = datastreamContents.get(mergeStrings(pid, datastream));
        if (returnvalue.isInstance(value)) {
            return returnvalue.cast(value);
        } else {
            removeDatastreamContents(pid, datastream);
            return null;
        }
    }


    public <T> void storeDatastreamContents(String pid,
                                            String datastream,
                                            Class<T> returnvalue, T contents) {
        datastreamContents.put(mergeStrings(pid, datastream), contents);
    }

    public void removeDatastreamContents(String pid, String ds) {
        datastreamContents.remove(mergeStrings(pid, ds));
    }


    public void removeDatastreamProfile(String pid, String datastream) {
        datastreamProfiles.remove(mergeStrings(pid, datastream));
    }

    public DatastreamProfile getDatastreamProfile(String pid,
                                                  String datastreamname) {
        return datastreamProfiles.get(mergeStrings(pid, datastreamname));
    }

    public void storeDatastreamProfile(String pid,
                                       String datastreamname,
                                       DatastreamProfile profile) {
        datastreamProfiles.put(mergeStrings(pid, datastreamname), profile);
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

}
