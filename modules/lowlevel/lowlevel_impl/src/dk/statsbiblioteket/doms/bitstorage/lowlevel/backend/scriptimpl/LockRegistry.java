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

import dk.statsbiblioteket.doms.webservices.ConfigCollection;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.net.URL;
import java.util.Map;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


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
 * Since bitstorage operations can be very longrunning, these locks will only
 * block for a certain amount of time.  If the lock cannot be aquired
 * false is returned from
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
@QAInfo(author = "abr",
        reviewers = "",
        level = QAInfo.Level.FINE,
        state = QAInfo.State.QA_NEEDED)
public class LockRegistry {

    private static Log log = LogFactory.getLog(LockRegistry.class);

    /**
     * The singleton variable.
     */
    private static LockRegistry registry;

    /**
     * The known locks.
     */
    private Map<URL, Long> locks;

    /**
     * The interval in which to check if a lock has become available
     */
    private long pollinterval;

    /**
     * The total time to wait for a lock
     */
    private long timeout;

    /**
     * Private constructor. Uses the two configCollection values
     * dk.statsbiblioteket.doms.bitstorage.lowlevel.locking.pollInterval and
     * dk.statsbiblioteket.doms.bitstorage.lowlevel.locking.timeout.
     * <p/>
     * PollInterval controls how often the threads should check to get the lock
     * of a file.
     * <p/>
     * Timeout controls the total time a thread should wait to get the lock of
     * a file.
     */
    private LockRegistry() {
        log.trace("Invoking LockRegistry Constructor");
        locks = new HashMap<URL, Long>();
        String pollinterval_string = ConfigCollection.getProperties().getProperty(
                "dk.statsbiblioteket.doms.bitstorage.lowlevel.locking.pollInterval");
        if (Utils.isLong(pollinterval_string)) {
            pollinterval = Long.parseLong(pollinterval_string);
        } else {
            pollinterval = 1000;//default value, 1 sec
        }
        log.trace("pollinterval is set to '" + pollinterval + "'");
        String timeout_string = ConfigCollection.getProperties().getProperty(
                "dk.statsbiblioteket.doms.bitstorage.lowlevel.locking.timeout");
        if (Utils.isLong(timeout_string)) {
            timeout = Long.parseLong(timeout_string);
        } else {
            timeout = 1000 * 60 * 10;//default value, 10 min
        }
        log.trace("timeout is set to '" + timeout + "'");
    }

    /**
     * Singleton method. Gets the LockRegistry object.
     *
     * @return a Lockregistry Object
     */
    public synchronized static LockRegistry getInstance() {
        log.trace("Entering getInstance");
        if (registry == null) {
            log.trace("No registry, creating new one");
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
     * <p/>This method will, at most, wait the timeout duration to get the
     * lock. If the lock is not available within that time, the method will
     * return false.
     *
     * @param file the file to lock
     * @return true if the lock could be aquired, false otherwise.
     */
    public synchronized boolean lockFile(URL file) {
        log.trace("Entering lockFile(" + file.toString() + ")");

        boolean lockbool = getLock(file);//attemp to get the lock
        if (lockbool) {
            log.trace("We have aquired the log, returning");
        } else {
            log.trace("File is locked by another thread, starting wait-while");
        }
        long startTime = System.currentTimeMillis();

        while (!lockbool) {//only if we have not gotten it yet
            try {
                wait(pollinterval); //wait for a while
            } catch (InterruptedException e) {
                //not a problem, just continue
            }
            lockbool = getLock(file); //is it available now?
            if (System.currentTimeMillis() - startTime > timeout) {//we have waited to long
                log.trace("Timeout reached, we will wait no more for the lock");
                break;
            }

        }
        return lockbool; //return if we have the lock or not

    }

    /**
     * Attempts to get the lock for a file. If the lock is not currently assigned
     * it is given to the thread and true is returned. If it is assigned to this
     * thread, true is returned. If it is given to another thread, false is
     * returned.
     *
     * @param file the file to lock
     * @return true if the lock was aquired, false otherwise.
     */
    private synchronized boolean getLock(URL file) {
        log.trace("Entering getLock(" + file.toString() + ")");
        long id = Thread.currentThread().getId();
        Long lock = locks.get(file);
        if (lock == null) {//unregistered
            locks.put(file, id);//lock the file
            return true;//lock aquired
        }
        if (lock.equals(id)) {//we already have the Lock
            return true;
        } else {
            return false;
        }
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
        log.trace("Entering release(" + file.toString() + ")");
        long id = Thread.currentThread().getId();
        Long lock = locks.get(file);
        if (lock == null) {//unregistered, so return quietly
            log.trace("Lock is not registered, returning");
            return;
        }

        if (lock.equals(id)) {//release
            log.trace("File is locked by this thread, unlocking");
            locks.remove(file);
        } else {//locked by another thread....
            //return queitly, do not unlock
            log.trace("File is locked by another thread. Returning without" +
                    "unlocking");
            return;
        }

    }

    /**
     * This is a singleton object, we should not allow clone.
     *
     * @return nothing
     * @throws CloneNotSupportedException always
     */
    @Override
    protected Object clone() throws CloneNotSupportedException {
        log.trace("Entering clone(). This is not allowed, throwing exception");
        throw new CloneNotSupportedException();
    }
}
