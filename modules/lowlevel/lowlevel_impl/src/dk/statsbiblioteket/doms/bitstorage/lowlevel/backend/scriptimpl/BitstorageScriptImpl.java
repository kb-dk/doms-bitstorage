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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.Bitstorage;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.exceptions.*;
import dk.statsbiblioteket.doms.webservices.ConfigCollection;
import dk.statsbiblioteket.doms.webservices.Base64;
import dk.statsbiblioteket.util.console.ProcessRunner;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

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
@QAInfo(author = "abr",
        reviewers = "kfc",
        level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_OK)
public class BitstorageScriptImpl implements Bitstorage {


    private Log log = LogFactory.getLog(BitstorageScriptImpl.class);

    /**
     * These are the commands the shell script understands.
     */
    private static enum ScriptCommand {
        UPLOAD_COMMAND("save"),
        APPROVE_COMMAND("approve"),
        DISAPPROVE_COMMAND("delete"),
        SPACELEFT_COMMAND("space-left"),
        GETMD5_COMMAND("get-md5"),
        GETSTATE_COMMAND("get-state");

        private String command;

        ScriptCommand(String command) {
            this.command = command;
        }

        public String getCommand() {
            return command;
        }
    }

    /**
     * This is the server to be used as a prefix for making filenames into urls.
     */
    private String bitfinder;


    /**
     * These are patterns to match for, for parsing output.
     */
    private static final String FREE_SPACE_REPLY = "Free space: ";
    private static final String MAX_FILE_SIZE_REPLY = "Max file size: ";


    public BitstorageScriptImpl() {
        Properties props = ConfigCollection.getProperties();

        this.bitfinder = props.getProperty(
                "dk.statsbiblioteket.doms.bitstorage.lowlevel.bitfinder");
    }


    /**
     * Upload the file to bitstorage. Note that the operation is idempotent, but
     * if an exception is thrown, the file is still present in the temporary
     * bitstorage.
     *
     * @param filename   The filename to store the file by
     * @param data       the inputstream with the file contents
     * @param md5        The locally generated md5sum of the file
     * @param filelength The length of the file. The size to reserve before
     *                   before starting upload.
     * @return the URL to the file
     * @throws NotEnoughFreeSpaceException Disk is full
     * @throws CommunicationException      If the script failed in an
     *                                     unexpected way,
     *                                     or returned something that
     *                                     could not be parsed
     * @throws ChecksumFailedException     If the calculated checksum does not
     *                                     match the provided checksum
     * @throws FileIsLockedException       If the file in bitstorage is
     *                                     already being processed by another
     *                                     process.
     */
    public URL upload(String filename,
                      InputStream data,
                      String md5,
                      long filelength)
            throws NotEnoughFreeSpaceException,
                   CommunicationException,
                   ChecksumFailedException,
                   FileIsLockedException {
        log.trace("Entering upload(" + filename + ", data, " + md5
                  + ", " + filelength + ")");
        String output;

        try {


            log.debug("Starting upload script command");
            output = runcommand(data,
                                ScriptCommand.UPLOAD_COMMAND,
                                filelength + "", md5, filename);
            log.debug("Upload script command exited normally");
            output = output.trim();
            return new URL(output);
        } catch (ContingencyException e) {//something went wrong
            int exitstatus = e.getReturncode();

            switch (exitstatus) {
                case 1:
                    throw new NotEnoughFreeSpaceException("No space, file "
                                                          + "was not saved", e);
                    //break;
                case 2:
                    throw new ChecksumFailedException("Wrong Checksum, file"
                                                      + " not saved", e);
                    //break;
                case 3:
                    throw new FileIsLockedException("File locked", e);
                    //break;
                default:
                    throw new CommunicationException("Unknown exit status "
                                                     + "from bitstorage server script.",
                                                     e);
                    //break;
            }

        }
        catch (MalformedURLException e) {
            throw new CommunicationException(
                    "Script returned unrecognized blob" + " for URL " +
                    " while uploading file '" + filename
                    + "'", e);

        }
        catch (Exception e) {
            log.error("Caught unknown exception", e);
            throw new CommunicationException("Unknown failure", e);
        }
    }

    /**
     * Disapprove the file, ie delete it from temporary bitstorage. Will not
     * do anything if the file is not in temporary bitstorage, so is idempotent.
     *
     * @param file The url to the file (in bitstorage)
     * @throws CommunicationException   is there was some problem with
     *                                  the script
     *                                  command
     * @throws InvalidFileNameException if the url is not of the expected format
     * @throws FileIsLockedException    If the file in bitstorage is
     *                                  already being
     *                                  processed by another process.
     */
    public void disapprove(URL file)
            throws
            CommunicationException {
        log.trace("Entering disapprove(" + file + ")");

        try {

            String datafile = toFile(file);
            String output;

            output = runcommand(ScriptCommand.DISAPPROVE_COMMAND, datafile);
        } catch (ContingencyException e) {
            int exitstatus = e.getReturncode();

            switch (exitstatus) {
                case 1:
                    log.debug("File to disapprove '" + file + "" +
                              "' not found. Not a problem");
                    break;
                default:
                    throw new CommunicationException(e.getMessage(), e);
            }

        } catch (Exception e) {
            log.error("Caught unknown exception", e);
            throw new CommunicationException("Unknown failure", e);
        }

    }


    /**
     * Approve the file, ie moves it from temporary bitstorage to permanent.
     * Will not do anything if the file is not in temporary bitstorage, so is
     * idempotent.
     *
     * @param file The url to the file (in bitstorage)
     * @param md5  the md5sum of the file. Is checked against the
     *             server checksum
     * @throws CommunicationException is there was some problem
     *                                with the script command
     */
    public void approve(URL file, String md5)
            throws CommunicationException,
                   ChecksumFailedException {
        log.trace("Entering approve(" + md5 + ", " + file + ")");


        try {
            String datafile = toFile(file);
            String output;

            output = runcommand(ScriptCommand.APPROVE_COMMAND,
                                md5, datafile);
        } catch (ContingencyException e) {
            int exitstatus = e.getReturncode();

            switch (exitstatus) {
                case 1:
                    log.debug("File to approve '" + file + "" +
                              "' not found in temp, so must be in permanent"
                              + " storage already. Not a problem");
                    break;
                case 2:
                    throw new ChecksumFailedException(
                            "Checksum for file" + file
                            + "does not match the given md5 checksum", e);
                default:
                    throw new CommunicationException(
                            "Unrecognized script failure for approve of '"
                            + file + "'", e);

            }


        } catch (Exception e) {
            log.error("Caught unknown exception", e);
            throw new CommunicationException("Unknown failure", e);

        }
    }


    /**
     * Get the md5sum of the file, whether or not it is in temporary
     * or permanent
     * bitstorage. Currently, the scripts recalculate the checksum when this
     * method is invoked.
     *
     * @param file the url of the file
     * @return the md5sum of the file.
     * @throws CommunicationException   is there was some problem with
     *                                  the script
     *                                  command
     * @throws FileNotFoundException    if the file is in neither temporary or
     *                                  permanent bitstorage
     * @throws InvalidFileNameException if the url is not of the expected format
     * @throws FileIsLockedException    If the file in bitstorage is
     *                                  already being
     *                                  processed by another process.
     */
    public String getMd5(URL file)
            throws
            CommunicationException,
            FileNotFoundException {
        log.trace("Entering getMd5(" + file + ")");

        try {

            String datafile = toFile(file);
            String output;

            output = runcommand(ScriptCommand.GETMD5_COMMAND, datafile);
            return output;
        } catch (ContingencyException e) {
            int exitstatus = e.getReturncode();

            switch (exitstatus) {
                case 1:
                    throw new FileNotFoundException("File not found '" + file
                                                    + "' while trying to get checksum for file",
                                                    e);
                default:
                    throw new CommunicationException(
                            "Unrecognized script failure"
                            + " while getting checksum for '" + file
                            + "'", e);
            }
        } catch (Exception e) {
            log.error("Caught unknown exception", e);
            throw new CommunicationException("Unknown failure", e);
        }


    }

    /**
     * Tests if a file is already approved. If approved, returns true.
     * If in temporary storage, return false. If not in bitstorage, throw
     * FileNotFoundException.
     *
     * @param file the url to check
     * @return true if approved, false if in temporary
     * @throws FileNotFoundException if the file is not in bitstorage
     */
    public boolean isApproved(URL file)
            throws
            FileNotFoundException,
            CommunicationException {
        log.trace("Entering isApproved(" + file + ")");

        try {
            String datafile = toFile(file);
            String output;

            output = runcommand(ScriptCommand.GETSTATE_COMMAND, datafile);
            return true;

            //TODO script does not check temporary.

        } catch (ContingencyException e) {
            int exitstatus = e.getReturncode();

            switch (exitstatus) {
                case 1:
                    throw new FileNotFoundException(
                            "File not found '" + file + "'"
                            + "while checking whether it was approved.", e);
                default:
                    throw new CommunicationException(
                            "Unrecognized script failure"
                            + " while getting checksum for '" + file
                            + "'", e);
            }
        } catch (Exception e) {
            log.error("Caught unknown exception", e);
            throw new CommunicationException("Unknown failure", e);
        }
    }

    private String toFile(URL file) {
        String stringform = file.toString();
        if (stringform.startsWith(bitfinder)) {
            return stringform.substring(bitfinder.length());
        } else {
            return stringform;
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
            output = runcommand(ScriptCommand.SPACELEFT_COMMAND);

            int index1 = output.indexOf(MAX_FILE_SIZE_REPLY);
            int index2 = output.indexOf(FREE_SPACE_REPLY);
            if (index1 > -1 && index2 > -1 && index2 > index1) {
                //this conditional prevents numberformat exceptions
                String longstring = output.substring(
                        index1 + MAX_FILE_SIZE_REPLY.length(), index2).trim();
                if (Utils.isLong(longstring)) {
                    return Long.parseLong(longstring);
                }
            }
            //the output is not of the expected format
            throw new CommunicationException(
                    "Unrecognized script return: '" + output
                    + "' while getting max file size");

        } catch (ContingencyException e) {
            throw new CommunicationException("Unrecognized script failure while"
                                             + " getting max file size", e);
        } catch (Exception e) {
            log.error("Caught unknown exception", e);
            throw new CommunicationException("Unknown failure", e);

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
            output = runcommand(ScriptCommand.SPACELEFT_COMMAND);

            int index = output.indexOf(FREE_SPACE_REPLY);
            String longstring = output;
            if (index > -1) {
                longstring = output.substring(
                        index + FREE_SPACE_REPLY.length()).trim();
            }

            if (Utils.isLong(longstring)) {
                return Long.parseLong(longstring);
            } else {
                throw new CommunicationException(
                        "Script did not return a long," + " but instead '"
                        + longstring + "' while checking free space");
            }
        } catch (ContingencyException e) {
            throw new CommunicationException("Unrecognized script failure"
                                             + " while checking free space", e);
        } catch (Exception e) {
            log.error("Caught unknown exception", e);
            throw new CommunicationException("Unknown failure", e);

        }

    }

    /**
     * Run the specified command in the script. If the script returns normally,
     * the output written on stdout is returned as a string. Equivalent to
     * runcommand(null,command);
     *
     * @param command    the command
     * @param parameters the string parameters
     * @return the standard output
     * @throws CommunicationException If the execution timed out
     * @throws ContingencyException   if the script returned
     *                                abnormally. Contains
     *                                both the return code, the
     *                                standard out and the standard error.
     * @see #runcommand(java.io.InputStream,
     *      dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.scriptimpl.BitstorageScriptImpl.ScriptCommand,
     *      String[])
     */
    private String runcommand(ScriptCommand command, String... parameters)
            throws
            CommunicationException,
            ContingencyException {
        log.trace("Entering runcommand(" + command.getCommand() +
                  ", " + Arrays.deepToString(parameters) + ")");
        return runcommand(null, command, parameters);
    }


    /**
     * Run the specified command in the script. If the script returns normally,
     * the output written on stdout is returned as a string.
     *
     * @param input      the inputstream to feed as stdin
     * @param command    the command
     * @param parameters the string parameters
     * @return the standard output
     * @throws CommunicationException If the execution timed out
     * @throws ContingencyException   if the script returned
     *                                abnormally. Contains
     *                                both the return code, the
     *                                standard out and the standard error.
     */
    private String runcommand(InputStream input,
                              ScriptCommand command,
                              String... parameters)
            throws CommunicationException, ContingencyException {
        log.trace("Entering runcommand(" + "input ," + command.getCommand() +
                  ", " + Arrays.deepToString(parameters) + ")");

        List<String> arrayList = new ArrayList<String>();
        String scriptblob = ConfigCollection.getProperties().getProperty(
                "dk.statsbiblioteket.doms.bitstorage.lowlevel.scriptimpl.script");
        String[] scriptlist = scriptblob.split(" ");

        arrayList.addAll(Arrays.asList(scriptlist));
        arrayList.add(command.getCommand());
        arrayList.addAll(Arrays.asList(parameters));

        ProcessRunner nr = new ProcessRunner(arrayList);

        nr.setInputStream(input);

        log.debug("Starting script parameters '" +
                  Arrays.deepToString(arrayList.toArray()) + "'");
        nr.run();
        log.debug("script parameters '" +
                  Arrays.deepToString(arrayList.toArray()) + "' terminated");
        if (nr.isTimedOut()) {  //default Long.max ms
            throw new CommunicationException(
                    "Communication with Bitstorage timed out while running"
                    + " parameters '" + arrayList + "'");
        }

        if (nr.getReturnCode() != 0) {
            throw new ContingencyException(nr.getReturnCode(),
                                           nr.getProcessOutputAsString(),
                                           nr.getProcessErrorAsString());
        }

        return nr.getProcessOutputAsString();
    }


    /**
     * This is the contingency exception, used by the runcommand
     * method to indicate
     * that the command did not terminate with error code 0.
     * <p/>
     * TODO: Factor out exception in its own file..
     */
    public static class ContingencyException extends Exception {

        /**
         * The actual errorcode received.
         */
        private int returncode;

        /**
         * The stdout from the process.
         */
        private String stdout;

        /**
         * The stderr from the process.
         */
        private String stderr;

        /**
         * Constructor.
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
         * Get the return code of the process.
         *
         * @return the return code, 0 is success
         */
        public int getReturncode() {
            return returncode;
        }

        /**
         * Get the std out from the process as a string.
         *
         * @return a string with the collected stdout
         */
        public String getStdout() {
            return stdout;
        }

        /**
         * Get the stderr from the process as a string.
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