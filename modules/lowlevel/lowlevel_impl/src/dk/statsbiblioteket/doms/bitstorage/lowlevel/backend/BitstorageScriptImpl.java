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

package dk.statsbiblioteket.doms.bitstorage.lowlevel.backend;

import dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.exceptions.*;
import dk.statsbiblioteket.doms.webservices.ConfigCollection;
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

/*
 TODO
 * javadoc for all
 * operations does not lock files
 * check for idempotent against scripts, but probably ok
 * Cannot reserve space
 */
@QAInfo(author = "abr",
        reviewers = "kfc",
        level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT)
public class BitstorageScriptImpl
        implements Bitstorage {


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
     * @throws InvalidFilenameException     If the filename is not valid
     */
    public URL upload(String filename, InputStream data, String md5, long filelength)
            throws CommunicationException,
            ChecksumFailedException, FileAlreadyApprovedException, InvalidFilenameException {
        //TODO locking?
        String output;
        try {
            output = runcommand(data, UPLOAD_COMMAND, filename);
        } catch (ContingencyException e) {//something went wrong
            String stdout = e.getStdout();
            if (stdout.contains(ALREADY_STORED_REPLY)) {
                throw new FileAlreadyApprovedException(
                        "File '" + filename + "' has already been approved,"
                                + "and so cannot be uploaded again.");
            } else {
                throw new CommunicationException("Unrecognized script failure"
                        + " during upload of '" + filename + "'", e);
            }
        }
        output = output.trim();

        if (!isChecksum(output)) {//script returned normally, but output is not checksum
            throw new CommunicationException("Script returned unrecognized blob" 
                    + " for checksum: '" + output + "' while uploading file '"
                    + filename + "'");

        }

        if (output.equalsIgnoreCase(md5)) {
            return createURL(filename);
        } else {
            throw new ChecksumFailedException(
                    "Given checksum for file '" + filename + "' was '" + md5
                            + "' but server calculated '" + output + "'");
        }
    }

    private boolean isLong(String output) {
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
        return output != null && output.matches("[a-fA-F0-9]*");
    }


    /**
     * Disapprove the file, ie delete it from temporary bitstorage. Will not
     * do anything if the file is not in temporary bitstorage, so is idempotent.
     *
     * @param file The url to the file (in bitstorage)
     * @throws CommunicationException is there was some problem with the script
     *                                command
     */
    public void disapprove(URL file)
            throws CommunicationException, InvalidFilenameException {

        String datafile = getFileNameFromURL(file);
        String output;
        try {
            output = runcommand(DISAPPROVE_COMMAND, datafile);
        } catch (ContingencyException e) {
            output = e.getStdout();
            if (output.contains(FILE_NOT_FOUND_REPLY)) {
                //ok, not a problem
            } else {
                throw new CommunicationException("Unrecognized script failure"
                        + " while disapproving '" + file + "'", e);
            }
        }
        if (output.trim().length() > 0) {//non empty reply
            throw new CommunicationException("Expected empty reply, got '"
                    + output + "' while disapproving '" + file + "'");
        }

    }

    /**
     * Disapprove the file, ie delete it from temporary bitstorage. Will not
     * do anything if the file is not in temporary bitstorage, so is idempotent.
     *
     * @param file The url to the file (in bitstorage)
     * @param md5  the md5sum of the file. Is currently not used.  TODO let approve use md5
     * @throws CommunicationException      is there was some problem with the script
     *                                     command
     * @throws InvalidFilenameException    if the url is not of the expected format
     * @throws NotEnoughFreeSpaceException if there is not enough free space in
     *                                     bitstorage
     */
    public String approve(URL file, String md5)
            throws NotEnoughFreeSpaceException,
            CommunicationException, InvalidFilenameException {

        String datafile = getFileNameFromURL(file);
        String output;
        try {//TODO call getChecksum or let the script handle this
            output = runcommand(APPROVE_COMMAND, datafile);
        } catch (ContingencyException e) {
            output = e.getStdout();
            if (output.contains(FILE_NOT_FOUND_REPLY)) {
                //ok, not a problem
            } else if (output.contains(NO_SPACE_LEFT_REPLY)) {
                throw new NotEnoughFreeSpaceException(
                        "Not enough free space for file '" + file + "'");
            } else {
                throw new CommunicationException(
                        "Unrecognized script failure for approve of '" + file
                                + "'", e);
            }
        }
        if (!isChecksum(output)) {//script returned normally, but output is not checksum
            throw new CommunicationException(
                    "Script returned unrecognized blob for checksum: '"
                            + output + "' while approving '" + file +  "'");

        }
        return output;

    }


    //TODO javadoc
    public long spaceLeft() throws CommunicationException {
        String output;
        try {
            output = runcommand(SPACELEFT_COMMAND);
        } catch (ContingencyException e) {
            throw new CommunicationException("Unrecognized script failure"
                    + " while checking free space", e);
        }
        //TODO defensive code
        int index = output.indexOf(FREE_SPACE_REPLY);
        String longstring = output.substring(
                index + FREE_SPACE_REPLY.length()).trim();


        try {
            return Long.parseLong(longstring);
        } catch (NumberFormatException e) {
            throw new CommunicationException("Script did not return a long,"
                    + " but '" + longstring + "' while checking free space", e);
        }

    }

    //TODO javadoc
    public String getMd5(URL file)
            throws CommunicationException, FileNotFoundException, InvalidFilenameException {
        String datafile = getFileNameFromURL(file);
        String output;
        try {
            output = runcommand(GETMD5_COMMAND, datafile);
        } catch (ContingencyException e) {
            output = e.getStdout();
            if (output.trim().isEmpty()) {
                throw new FileNotFoundException("File not found '" + file
                        + "' while trying to get checksum for file",
                                                e);
            } else {
                throw new CommunicationException("Unrecognized script failure"
                        + " while getting checksum for '" + file + "'", e);
            }
        }
        return output;
    }

    //TODO javadoc
    public boolean isApproved(URL file)
            throws FileNotFoundException, CommunicationException, InvalidFilenameException {
        String datafile = getFileNameFromURL(file);
        String output;
        try {
            output = runcommand(GETSTATE_COMMAND, datafile);
            if (output.contains(FILE_IN_STAGE)) {
                return false;
            } else if (output.contains(FILE_IN_STORAGE)) {
                return true;
            }
            throw new CommunicationException("Unrecognized script return: '"
                    + output + "' while checking whether '" + file
                    + "' is approved");
        } catch (ContingencyException e) {
            output = e.getStdout();
            if (output.contains(FILE_NOT_FOUND)) {
                throw new FileNotFoundException("File not found '" + file + "'"
                        + "while checking whether it was approved.");
            } else {
                throw new CommunicationException("Unrecognized script failure "
                        + "while checking whether '" + file + "' is approved",
                                                 e);
            }
        }
    }

    //TODO javadoc
    public long getMaxFileSize() throws CommunicationException {
        String output;
        try {
            output = runcommand(SPACELEFT_COMMAND);
        } catch (ContingencyException e) {
            throw new CommunicationException("Unrecognized script failure while"
                    + " getting max file size", e);
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
        throw new CommunicationException("Unrecognized script return: '"
                + output + "' while getting max file size");


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
    private String runcommand(String... command) throws CommunicationException, ContingencyException {
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
        List<String> arrayList = new ArrayList<String>();
        String scriptblob = ConfigCollection.getProperties().getProperty("Script");
        String[] scriptlist = scriptblob.split(" ");

        arrayList.addAll(Arrays.asList(scriptlist));
        arrayList.addAll(Arrays.asList(command));

        ProcessRunner nr = new ProcessRunner(arrayList);

        nr.setInputStream(input);
        nr.run();
        if (nr.isTimedOut()) {
            throw new CommunicationException(
                    "Communication with Bitstorage timed out while running"
                            + " command '" + arrayList + "'");
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
     * @throws InvalidFilenameException if the url is not of the expected format
     */
    private String getFileNameFromURL(URL file) throws InvalidFilenameException {
        if (file.toString().startsWith(bitfinder)) {
            return file.toString().substring(bitfinder.length());
        } else {
            throw new InvalidFilenameException("The url '" + file 
                    + "' did not start with required prefix '"
                    + bitfinder + "'");
        }
    }

    /**
     * Simple method for constructing the url from a filename
     *
     * @param filename the filename
     * @return an url
     * @throws InvalidFilenameException if the filename cannot be converted to
     *                                  an url
     */
    private URL createURL(String filename) throws InvalidFilenameException {
        try {
            return new URL(bitfinder + filename);
        } catch (MalformedURLException e) {
            throw new InvalidFilenameException("The provided filename '"
                    + filename + "' cannot be turned into a URL", e);
        }
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
        public ContingencyException(int returncode, String stdout, String stderr) {
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
