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

import dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.exceptions.*;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.Bitstorage;
import dk.statsbiblioteket.doms.webservices.ConfigCollection;
import dk.statsbiblioteket.util.console.ProcessRunner;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is an implementation of the Bitstorage system, based on the DOMS
 * bitstorage system, as provided by JHJL. It is based on a shell script,
 * invokable from the command line. This class provides a java interface
 * to this shell script.
 * <p/>
 * This class expects the ConfigCollection class is available. It requests these
 * parameters from it:
 * <ul>
 * <li>Bitfinder
 * <li>Script
 * </ul>
 */

/*
 TODO
 * check for idempotent against scripts, but probably ok
 * Cannot reserve space
 */
@QAInfo(author = "abr",
        reviewers = "kfc",
        level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_OK)
public class BitstorageScriptImpl
        implements Bitstorage {


    private Log log = LogFactory.getLog(BitstorageScriptImpl.class);
    /**
     * These are the commands the shell script understands
     */
    private static final String UPLOAD_COMMAND = "save-md5";
    private static final String APPROVE_COMMAND = "approve";
    private static final String DISAPPROVE_COMMAND = "delete";
    private static final String SPACELEFT_COMMAND = "space-left";
    private static final String GETMD5_COMMAND = "get-md5";
    private static final String GETSTATE_COMMAND = "get-state";

    /**
     * This is the server to be used as a prefix for making filenames into urls
     */
    private String bitfinder;


    /**
     * These are patterns to match for, for parsing output
     */
    private static final String ALREADY_STORED_REPLY = "was stored!";
    private static final String FILE_NOT_FOUND_REPLY = "not found";
    private static final String NO_SPACE_LEFT_REPLY = "No space left for file";
    private static final String FREE_SPACE_REPLY = "Free space: ";
    private static final String MAX_FILE_SIZE_REPLY = "Max file size: ";

    /* These are the three replies for Getstate*/
    private static final String FILE_IN_STAGE = "File in stage";
    private static final String FILE_IN_STORAGE = "File in storage";
    private static final String FILE_NOT_FOUND = "File not found";


    public BitstorageScriptImpl() {
        Properties props = ConfigCollection.getProperties();

        this.bitfinder = props.getProperty("bitfinder");
    }


    /**
     * Upload the file to bitstorage. Note that the operation is idempotent, but
     * if an exception is thrown, the file is still present in the temporary
     * bitstorage.
     * <p/>
     * This method is supposed to use file locking. It delegates this task
     * to the script, which at the moment, does not do it.
     *
     * @param filename   The filename to store the file by
     * @param data       the inputstream with the file contents
     * @param md5        The locally generated md5sum of the file
     * @param filelength The length of the file. The size to reserve before
     *                   before starting upload.
     * @return the URL to the file
     * @throws CommunicationException       If the script failed in an unexpected way,
     *                                      or returned something that could not be parsed as an checksum
     * @throws ChecksumFailedException      If the calculated checksum does not
     *                                      match the provided checksum
     * @throws FileAlreadyApprovedException If the filename is already used
     *                                      by an already approved file
     * @throws InvalidFileNameException     If the filename is not valid
     * @throws FileIsLockedException        If the file in bitstorage is already being
     *                                      processed by another process.
     */
    public URL upload(String filename,
                      InputStream data,
                      String md5,
                      long filelength)
            throws CommunicationException,
            ChecksumFailedException,
            FileAlreadyApprovedException,
            InvalidFileNameException,
            FileIsLockedException {
        log.trace("Entering upload(" + filename + ", data, " + md5 + ", " + filelength + ")");
        String output;
        URL url = createURL(filename);
        log.debug("Locking file '" + url + "'");
        if (!LockRegistry.getInstance().lockFile(url)) {//could not lock the file
            throw new FileIsLockedException("The file " + url + " is locked by another process");
        }

        try {
            try {
                log.debug("Starting upload script command");
                output = runcommand(data, UPLOAD_COMMAND, filename);
                log.debug("Upload script command exited normally");
            } catch (ContingencyException e) {//something went wrong
                String stdout = e.getStdout();
                if (stdout.contains(ALREADY_STORED_REPLY)) {
                    throw new FileAlreadyApprovedException(
                            "File '" + filename + "' have already been approved");
                } else {
                    throw new CommunicationException(
                            "Unrecognized script failure",
                            e);
                }
            }
            output = output.trim();

            if (!isChecksum(output)) {//script returned normally, but output is not checksum
                throw new CommunicationException(
                        "Script returned unrecognized blob for checksum: '" + output + "'");

            }

            if (output.equalsIgnoreCase(md5)) {
                return url;
            } else {
                throw new ChecksumFailedException(
                        "Given checksum '" + md5 + "' but server calculated '" +
                                output + "'");
            }
        } finally {
            log.debug("Releasing lock on file '" + url + "'");
            LockRegistry.getInstance().release(url);//however we got here, release the lock
        }
    }


    /**
     * Disapprove the file, ie delete it from temporary bitstorage. Will not
     * do anything if the file is not in temporary bitstorage, so is idempotent.
     *
     * @param file The url to the file (in bitstorage)
     * @throws CommunicationException   is there was some problem with the script
     *                                  command
     * @throws InvalidFileNameException if the url is not of the expected format
     * @throws FileIsLockedException    If the file in bitstorage is already being
     *                                  processed by another process.
     */
    public void disapprove(URL file)
            throws
            CommunicationException,
            InvalidFileNameException,
            FileIsLockedException {
        log.trace("Entering disapprove(" + file + ")");
        if (!LockRegistry.getInstance().lockFile(file)) {//could not lock the file
            throw new FileIsLockedException("The file " + file + " is locked by another process");
        }

        try {

            String datafile = getFileNameFromURL(file);
            String output;
            try {
                output = runcommand(DISAPPROVE_COMMAND, datafile);
            } catch (ContingencyException e) {
                output = e.getStdout();
                if (output.contains(FILE_NOT_FOUND_REPLY)) {
                    log.debug("File to disapprove '" + file + "' not found. Not a problem");
                    //ok, not a problem
                } else {
                    throw new CommunicationException(
                            "Unrecognized script failure",
                            e);
                }
            }
            if (output.trim().length() > 0) {//non empty reply
                throw new CommunicationException("Expected empty reply, got '" + output + "'");
            }
        } finally {
            LockRegistry.getInstance().release(file);
        }

    }

    /**
     * Approve the file, ie moves it from temporary bitstorage to permanent.
     * Will not do anything if the file is not in temporary bitstorage, so is
     * idempotent.
     *
     * @param file The url to the file (in bitstorage)
     * @param md5  the md5sum of the file. Is checked against the server checksum
     * @throws CommunicationException      is there was some problem with the script
     *                                     command
     * @throws InvalidFileNameException    if the url is not of the expected format
     * @throws NotEnoughFreeSpaceException if there is not enough free space in
     *                                     bitstorage
     * @throws FileIsLockedException       If the file in bitstorage is already being
     *                                     processed by another process.
     */
    public String approve(URL file, String md5)
            throws NotEnoughFreeSpaceException,
            CommunicationException,
            InvalidFileNameException,
            FileIsLockedException,
            FileNotFoundException,
            ChecksumFailedException {
        log.trace("Entering approve(" + file + ", " + md5 + ")");
        if (!LockRegistry.getInstance().lockFile(file)) {//could not lock the file
            throw new FileIsLockedException("The file " + file + " is locked by another process");
        }

        try {

            //TODO remove this when/if the scripts take checksums on approve
            String serverchecksum = getMd5(file);
            if (!serverchecksum.equalsIgnoreCase(md5)) {//checksums doesn't match
                throw new ChecksumFailedException("The provided checksum was '" + md5 + "' but the server calculated '" + serverchecksum + "'");
            }

            String datafile = getFileNameFromURL(file);
            String output;
            try {
                output = runcommand(APPROVE_COMMAND, datafile);
            } catch (ContingencyException e) {
                output = e.getStdout();
                if (output.contains(FILE_NOT_FOUND_REPLY)) {
                    log.debug("File to approve '" + file + "' not found in temp, so must be in permanent storage already. Not a problem");
                } else if (output.contains(NO_SPACE_LEFT_REPLY)) {
                    throw new NotEnoughFreeSpaceException(
                            "Not enough free space for file '" + file + "'");
                } else {
                    throw new CommunicationException(
                            "Unrecognized script failure",
                            e);
                }
            }
            if (!isChecksum(output)) {//script returned normally, but output is not checksum
                throw new CommunicationException(
                        "Script returned unrecognized blob for checksum: '" + output + "'");

            }
            return output;
        } finally {
            LockRegistry.getInstance().release(file);
        }

    }


    /**
     * Give the number of bytes left in the permanent bitstorage.
     *
     * @return number of bytes
     * @throws CommunicationException is there was some problem with the script
     *                                command
     */
    public long spaceLeft() throws CommunicationException {
        log.trace("Entering spaceLeft()");
        String output;
        try {
            output = runcommand(SPACELEFT_COMMAND);
        } catch (ContingencyException e) {
            throw new CommunicationException("Unrecognized script failure", e);
        }

        int index = output.indexOf(FREE_SPACE_REPLY);
        String longstring = output;
        if (index > -1) {
            longstring = output.substring(
                    index + FREE_SPACE_REPLY.length()).trim();
        }

        if (isLong(longstring)) {
            return Long.parseLong(longstring);
        } else {
            throw new CommunicationException(
                    "Script did not return a long, but instead '" + longstring + "'");
        }
    }

    /**
     * Get the md5sum of the file, whether or not it is in temporary or permanent
     * bitstorage. Currently, the scripts recalculate the checksum when this
     * method is invoked.
     *
     * @param file the url of the file
     * @return the md5sum of the file.
     * @throws CommunicationException   is there was some problem with the script
     *                                  command
     * @throws FileNotFoundException    if the file is in neither temporary or
     *                                  permanent bitstorage
     * @throws InvalidFileNameException if the url is not of the expected format
     * @throws FileIsLockedException    If the file in bitstorage is already being
     *                                  processed by another process.
     */
    public String getMd5(URL file)
            throws
            CommunicationException,
            FileNotFoundException,
            InvalidFileNameException,
            FileIsLockedException {
        log.trace("Entering getMd5(" + file + ")");
        if (!LockRegistry.getInstance().lockFile(file)) {//could not lock the file
            throw new FileIsLockedException("The file " + file + " is locked by another process");
        }

        try {


            String datafile = getFileNameFromURL(file);
            String output;
            try {
                output = runcommand(GETMD5_COMMAND, datafile);
            } catch (ContingencyException e) {
                output = e.getStdout();
                if (output.trim().isEmpty()) {
                    throw new FileNotFoundException("File not found '" + file + "'",
                            e);
                } else {
                    throw new CommunicationException(
                            "Unrecognized script failure",
                            e);
                }
            }
            return output;
        } finally {
            LockRegistry.getInstance().release(file);
        }
    }

    /**
     * Tests if a file is already approved. If approved, returns true.
     * If in temporary storage, return false. If not in bitstorage, throw
     * FileNotFoundException.
     *
     * @param file the url to check
     * @return true if approved, false if in temporary
     * @throws FileNotFoundException    if the file is not in bitstorage
     * @throws InvalidFileNameException if the url is not of the expected format
     * @throws FileIsLockedException    If the file in bitstorage is already being
     *                                  processed by another process.
     */
    public boolean isApproved(URL file)
            throws
            FileNotFoundException,
            CommunicationException,
            InvalidFileNameException,
            FileIsLockedException {
        log.trace("Entering isApproved(" + file + ")");
        if (!LockRegistry.getInstance().lockFile(file)) {//could not lock the file
            throw new FileIsLockedException("The file " + file + " is locked by another process");
        }

        try {

            String datafile = getFileNameFromURL(file);
            String output;
            try {
                output = runcommand(GETSTATE_COMMAND, datafile);
                if (output.contains(FILE_IN_STAGE)) {
                    return false;
                } else if (output.contains(FILE_IN_STORAGE)) {
                    return true;
                }
                throw new CommunicationException("Unrecognized script return: '" + output + "'");
            } catch (ContingencyException e) {
                output = e.getStdout();
                if (output.contains(FILE_NOT_FOUND)) {
                    throw new FileNotFoundException("File not found");
                } else {
                    throw new CommunicationException(
                            "Unrecognized script failure",
                            e);
                }
            }
        } finally {
            LockRegistry.getInstance().release(file);
        }
    }

    /**
     * Get the maximum filesize allocatable for a single file. This is the
     * minimum of the space in temporary store and in permanent store.
     *
     * @return the maximum size for an upload
     */
    public long getMaxFileSize() throws CommunicationException {
        log.trace("Entering getMaxFileSize()");
        String output;
        try {
            output = runcommand(SPACELEFT_COMMAND);
        } catch (ContingencyException e) {
            throw new CommunicationException("Unrecognized script failure", e);
        }

        int index1 = output.indexOf(MAX_FILE_SIZE_REPLY);
        int index2 = output.indexOf(FREE_SPACE_REPLY);
        if (index1 > -1 && index2 > -1 && index2 > index1) {
            //this conditional prevents numberformat exceptions
            String longstring = output.substring(
                    index1 + MAX_FILE_SIZE_REPLY.length(), index2).trim();
            if (isLong(longstring)) {
                return Long.parseLong(longstring);
            }
        }
        //the output is not of the expected format
        throw new CommunicationException("Unrecognized script return: '" + output + "'");


    }

    /**
     * Run the specified command in the script. If the script returns normally,
     * the output written on stdout is returned as a string. Equivalent to
     * runcommand(null,command);
     *
     * @param command the command and paramenters
     * @return the standard output
     * @throws CommunicationException If the execution timed out
     * @throws ContingencyException   if the script returned abnormally. Contains
     *                                both the return code, the standard out and the standard error.
     * @see #runcommand(java.io.InputStream, String[])
     */
    private String runcommand(String... command) throws
            CommunicationException,
            ContingencyException {
        log.trace("Entering runcommand(" + Arrays.deepToString(command) + ")");
        return runcommand(null, command);
    }


    /**
     * Run the specified command in the script. If the script returns normally,
     * the output written on stdout is returned as a string.
     *
     * @param input   the inputstream to feed as stdin
     * @param command the command and paramenters
     * @return the standard output
     * @throws CommunicationException If the execution timed out
     * @throws ContingencyException   if the script returned abnormally. Contains
     *                                both the return code, the standard out and the standard error.
     */
    private String runcommand(InputStream input, String... command)
            throws CommunicationException, ContingencyException {
        log.trace("Entering runcommand(" + "input ," + Arrays.deepToString(
                command) + ")");

        List<String> arrayList = new ArrayList<String>();
        String scriptblob = ConfigCollection.getProperties().getProperty(
                "Script");
        String[] scriptlist = scriptblob.split(" ");

        arrayList.addAll(Arrays.asList(scriptlist));
        arrayList.addAll(Arrays.asList(command));

        ProcessRunner nr = new ProcessRunner(arrayList);

        nr.setInputStream(input);

        log.debug("Starting script command '" + Arrays.deepToString(arrayList.toArray()) + "'");
        nr.run();
        log.debug("script command '" + Arrays.deepToString(arrayList.toArray()) + "' terminated");
        if (nr.isTimedOut()) {  //default 1000 ms
            throw new CommunicationException(
                    "Communication with Bitstorage timed out");
        }

        if (nr.getReturnCode() != 0) {
            throw new ContingencyException(nr.getReturnCode(),
                    nr.getProcessOutputAsString(),
                    nr.getProcessErrorAsString());
        }

        return nr.getProcessOutputAsString();
    }

    /**
     * Attempts to aquire the local filename from a public url.
     *
     * @param file the url
     * @return the localfilename
     * @throws InvalidFileNameException if the url is not of the expected format
     */
    private String getFileNameFromURL(URL file) throws
            InvalidFileNameException {
        if (file.toString().startsWith(bitfinder)) {
            return file.toString().substring(bitfinder.length());
        } else {
            throw new InvalidFileNameException("The url '" + file + "' was not recognized");
        }
    }

    /**
     * Simple method for constructing the url from a filename
     *
     * @param filename the filename
     * @return an url
     * @throws InvalidFileNameException if the filename cannot be converted to
     *                                  an url
     */
    private URL createURL(String filename) throws InvalidFileNameException {
        log.trace("Entering createURL(" + filename + ")");
        try {
            return new URL(bitfinder + filename);
        } catch (MalformedURLException e) {
            throw new InvalidFileNameException(
                    "The provided filename cannot be turned into an url",
                    e);
        }
    }

    /**
     * Simple little method for checking if the message contain only 0-9 chars
     *
     * @param output the message
     * @return true if only 0-9 chars
     */
    private boolean isLong(String output) {
        log.trace("Entering isLong(" + output + ")");
        return output != null && output.matches("[0-9]*");
    }

    /**
     * Simple little method for checking if the message can be regarded as a
     * checksum.
     *
     * @param output the message
     * @return true if the message contain only a-f A-F and 0-9
     */
    private boolean isChecksum(String output) {
        log.trace("Entering isChecksum(" + output + ")");
        return output != null && output.matches("[a-fA-F0-9]*");
    }


    /**
     * This is the contingency exception, used by the runcommand method to indicate
     * that the command did not terminate with error code 0
     */
    public static class ContingencyException extends Exception {

        /**
         * The actual errorcode received
         */
        private int returncode;

        /**
         * The stdout from the process
         */
        private String stdout;

        /**
         * The stderr from the process
         */
        private String stderr;

        /**
         * Constructor
         *
         * @param returncode the return code
         * @param stdout     the stdout string
         * @param stderr     the std err string
         */
        public ContingencyException(int returncode,
                                    String stdout,
                                    String stderr) {
            this.returncode = returncode;
            this.stdout = stdout;
            this.stderr = stderr;
        }

        /**
         * Get the return code of the process
         *
         * @return the return code, 0 is success
         */
        public int getReturncode() {
            return returncode;
        }

        /**
         * Get the std out from the process as a string
         *
         * @return a string with the collected stdout
         */
        public String getStdout() {
            return stdout;
        }

        /**
         * Get the stderr from the process as a string
         *
         * @return a string with the collected stderr
         */
        public String getStderr() {
            return stderr;
        }

        @Override
        public String getMessage() {
            return "Unexpected return from bitstorage script: \n " +
                    "Return code was '" + returncode + "'\n" +
                    "Stdout was '" + stdout + "'\n" +
                    "Stderr was '" + stderr + "'\n";
        }
    }
}
