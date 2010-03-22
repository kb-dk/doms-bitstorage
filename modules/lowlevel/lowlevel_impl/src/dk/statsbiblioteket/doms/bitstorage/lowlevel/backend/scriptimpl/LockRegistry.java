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

package dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.scriptimpl;

import java.net.URL;
import java.util.Map;
import java.util.HashMap;


/**
 * This is the intermittent lock registry for Bitstorage. The workings are
 * simple, whenever a process must work on an URL, it should attempt to
 * lock it with the lockFile(URL) method. This returns true, if locked, and
 * false if the URL is already locked by another thread. When done, invoke
 * release(URL) to release the url for other threads.
 * <p/>
 * The purpose of this system is to prevent several bitstorage processes from
 * manipulating the same files concurrently. There is no locking control on
 * the backend script, so the webservice needs to do it.
 * <p/>
 * Since bitstorage operations can be very longrunning, these locks are
 * non-blocking. Rather, if the lock cannot be aquired false is returned from
 * lockFile(URL). This allows the webservice to inform the user of the problem
 * rather than waiting for another process to terminate.
 * <p/>
 * This class is a singleton. Get the instance by calling the getInstance()
 * method.
 *
 * @see #lockFile(java.net.URL)
 * @see #release(java.net.URL)
 * @see #getInstance()
 */
public class LockRegistry {

    /**
     * The singleton variable.
     */
    private static LockRegistry registry;

    /**
     * The known locks.
     */
    private Map<URL, Long> locks;

    /**
     * Private constructor.
     */
    private LockRegistry() {
        locks = new HashMap<URL, Long>();

    }

    /**
     * Singleton method. Gets the LockRegistry object.
     *
     * @return a Lockregistry Object
     */
    public synchronized static LockRegistry getInstance() {
        if (registry == null) {
            registry = new LockRegistry();
        }
        return registry;
    }


    /**
     * Attempt to lock the given File. If succesful, the file is locked,
     * and true is returned. If the file is already locked by another thread,
     * false is returned.
     * <p/>
     * A file will remain locked until the release(URL) method is called. There
     * is no timeout for locks, except the lifetime of the jvm.
     * <p/>
     * If you attempt to lock a file you already have locked (ie. locking a file
     * twice), both attempts will give true. You will still only need to unlock
     * it once.
     *
     * @param file the file to lock
     * @return true if the lock could be aquired, false otherwise.
     */
    public synchronized boolean lockFile(URL file) {
        long id = Thread.currentThread().getId();
        Long lock = locks.get(file);
        if (lock == null) {//unregistered
            locks.put(file, id);//lock the file
            return true;//lock aquired
        }

        return lock.equals(id);//if lock == id, locked by me, so true
    }


    /**
     * Release the lock on this file. The lock will only be released if this
     * thread is the owner of the lock. This method will return quietly, even
     * if the file is not locked, or locked by another thread. But the lock
     * is only removed, if the file was locked by this thread.
     *
     * @param file the file to release
     */
    public synchronized void release(URL file) {
        long id = Thread.currentThread().getId();
        Long lock = locks.get(file);
        if (lock == null) {//unregistered, so return quietly
            return;
        }

        if (lock.equals(id)) {//release
            locks.remove(file);
        } else {//locked by another thread....
            //return queitly, do not unlock
            return;
        }

    }

    /**
     * This is a singleton object, we should not allow cline.
     *
     * @return nothing
     * @throws CloneNotSupportedException always
     */
    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }
}
