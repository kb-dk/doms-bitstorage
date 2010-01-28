/* $Id$

The State and University DOMS project.
Copyright (C) 2006  The State and University Library

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
*/
package dk.statsbiblioteket.doms.bitstorage;

import dk.statsbiblioteket.doms.ByteString;
import dk.statsbiblioteket.util.XProperties;
import dk.statsbiblioteket.util.console.ProcessRunner;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class that handles the interface to the bitstorage. <p> Files are first
 * uploaded with the {@link #upload} method. This method checks if the files
 * have been transferred correctly, by comparing md5-sums. Files are held in a
 * temporary staging area in the bitstorage, until the {@link #commit} method
 * are called, which causes them to be transferred to the permanent storage.<br>
 * If the files should not be committed, call the {@link #discard} method
 * instead. This method deletes the files from the staging area.<br> Committed
 * files cannot be discarded, or otherwise removed. This require special access
 * to the bitstorage.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "abr, tsh",
        reviewers = {""})
public class BitstorageUtil {

    private static Log log = LogFactory.getLog(Bitstorage.class);

    private final XProperties xProperties;

    /**
     * Constructor for the <code>BitStorageUtil</code>.
     *
     * @param properties Properties that among other things identifies the
     *                   server running the bit arcive to operate on.
     */
    public BitstorageUtil(BitstorageProperties properties) {
        xProperties = properties.getXProperties();

        log.info(
                "Creating BitstorageUtil object with '" + xProperties
                        .getString(BitstorageProperties.SSH_COMMAND) + " "
                        + xProperties.getString(BitstorageProperties.SERVER)
                        + " "
                        + xProperties.getString(BitstorageProperties.SCRIPT)
                        + "' command");
    }

    /**
     * Commit previously transfered data-objects to Bitstorage. See {@link
     * #upload upload}
     *
     * @param dataIDs id's for previously transferred data-objects.
     * @throws java.io.IOException if the commit could not be completed.
     */
    public void commit(List<String> dataIDs) throws IOException {
        log.trace("commit called for " + dataIDs.size() + " pids");

        //For each file
        for (String datafile : dataIDs) {
            List<String> command = new ArrayList<String>(10);

            command.add(
                    xProperties.getString(
                            BitstorageProperties.SSH_COMMAND));
            command.add(xProperties.getString(BitstorageProperties.SERVER));
            final String script = xProperties
                    .getString(BitstorageProperties.SCRIPT).trim();

            if (!script.isEmpty()) {
                command.add(script);
            }
            command.add(
                    xProperties.getString(
                            BitstorageProperties.APPROVE_COMMAND));
            command.add(datafile);

            ProcessRunner nr = new ProcessRunner(command);
            try {
                nr.run();
            } catch (Exception e) {
                //potentially a very big exception
                throw new IOException(
                        "Error while commiting the datafile '" + datafile
                                + "'. The error output was '"
                                + nr.getProcessErrorAsString() + "' and"
                                + "the output was '"
                                + nr.getProcessOutputAsString() + "'", e);
            }
            log.debug("Committed '" + datafile + "' to Bitstorage");
        }
    }

    /**
     * Delete previously transfered data-objects from Bitstorage. Only works on
     * non-committed files. Committed files cannot be removed.
     *
     * @param dataIDs id's for the previously transferred files
     * @throws java.io.IOException If the discard could not be completed
     */
    public void discard(List<String> dataIDs) throws IOException {
        //Delete the already uploaded files
        log.trace("discard called for " + dataIDs.size() + " pids");

        for (String file : dataIDs) {

            List<String> command = new ArrayList<String>();
            command.add(
                    xProperties.getString(
                            BitstorageProperties.SSH_COMMAND));
            command.add(xProperties.getString(BitstorageProperties.SERVER));

            final String script = xProperties
                    .getString(BitstorageProperties.SCRIPT).trim();

            if (!script.isEmpty()) {
                command.add(script);
            }
            command.add(
                    xProperties.getString(
                            BitstorageProperties.DELETE_COMMAND));
            command.add(file);

            ProcessRunner nr = new ProcessRunner(command);
            try {
                nr.run();
                log.debug("Deleted '" + file + "' from Bitstorage");
            } catch (Exception e) {
                //potentially a very big exception
                throw new IOException(
                        "The Delete failed for file '" + file
                                + "'. The error output was '"
                                + nr.getProcessErrorAsString() + "' and"
                                + "the output was '"
                                + nr.getProcessOutputAsString() + "'", e);
            }
        }
        log.debug("The uploaded files was discarded");
    }

    /**
     * Transfers all data in the dir to bit-storage. Verifies the returned
     * checksum, but does not commit the files. Instead a list of dataIDs is
     * ureturned. These IDs can later be used by the {@link #commit} method.
     *
     * @param uploadDir        where to get the data from.
     * @param bitstorageFolder The main folder to place the files in, in the
     *                         Bitstorage
     * @return a list of IDs for the data, usabale for later commit or fail.
     *
     * @throws java.io.IOException if the data could not be transferred.
     */
    public List<String> upload(File uploadDir, String bitstorageFolder)
            throws IOException {
        log.trace(
                "upload for '" + uploadDir + "' called to '" + bitstorageFolder
                        + "'");

        List<String> excludeList = new ArrayList();

        try {
            excludeList.addAll(
                    (List<String>) xProperties
                            .getObject(BitstorageProperties.EXCLUDE_LIST));
        } catch (NullPointerException npe) {
            log.info("No excludelist defined in properties");
        }

        List<String> dataIDs = new ArrayList<String>(100);

        if (!uploadDir.exists()) {
            log.debug("No data dir found, no files uploaded");
            return dataIDs;
        }

        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new Error("Could not load default MD5 algorithm from java");
        }

        List<File> datafiles = listDirectory(uploadDir, excludeList);

        for (File datafile : datafiles) {
            //calculate local checksum
            FileInputStream datafilestream = new FileInputStream(datafile);

            //handles larger than 2 GB. TE sayslook at Linereader in sbutil
            for (long i = 0; i < datafile.length(); i += Integer.MAX_VALUE) {
                MappedByteBuffer mbb = datafilestream.getChannel().map(
                        FileChannel.MapMode.READ_ONLY, i,
                        Math.min(datafile.length() - i, Integer.MAX_VALUE));
                md5.update(mbb);
            }
            String local_checksum = ByteString.toHex(md5.digest())
                    .toUpperCase();

            //The relative path, ie. inside the data dir is needed for Bitstorage
            String relativePath = null;
            try {
                relativePath = relativePath(
                        datafile, uploadDir.getCanonicalPath(),
                        bitstorageFolder + "/");

                final String script = xProperties.getString(
                        BitstorageProperties.SCRIPT).trim();

                if (!script.isEmpty()) {
                    //Script is a command, arguments will be interpreted by bash
                    //once again
                    relativePath = "\"" + relativePath + "\"";
                }
                /*
                                relativePath = relativePath.replaceAll("\\","%5C");
                                relativePath = relativePath.replaceAll("'","%27");
                */
            } catch (Exception e) {//wrap any exception in a IO exception.
                datafilestream.close();
                discard(dataIDs);
                throw new IOException(e.getMessage(), e);
            }

            //prepare the upload command
            List<String> command = new ArrayList<String>(10);

            command.add(
                    xProperties.getString(
                            BitstorageProperties.SSH_COMMAND));
            command.add(xProperties.getString(BitstorageProperties.SERVER));

            final String script = xProperties
                    .getString(BitstorageProperties.SCRIPT).trim();

            if (!script.isEmpty()) {
                command.add(script);
            }
            command.add(
                    xProperties.getString(
                            BitstorageProperties.UPLOAD_COMMAND));
            command.add(relativePath);

            //So that TE can sleep calmly. Not needed
            /*
                        datafilestream.close();
                        datafilestream = new FileInputStream(datafile);
            */

            //make the process, and feed it the file in a stream
            ProcessRunner nr = new ProcessRunner(command);
            nr.setInputStream(datafilestream);

            try {
                //Run the upload command
                nr.run();
            } catch (Exception e) {
                discard(dataIDs);
                throw new IOException(
                        "The Upload failed for file '" + datafile
                                + "'. The error output was '"
                                + nr.getProcessErrorAsString() + "'", e);
            } finally {
                datafilestream.close();//no use any longer, free it
            }
            log.debug("Uploaded '" + relativePath + "' to Bitstorage");

            //get remote checksum
            String remote_checksum = nr.getProcessOutputAsString().trim()
                    .toUpperCase();

            //System.out.println(remote_checksum);
            if (remote_checksum.contains("WAS STORED")) {
                //file has already been committed. Just ignore it and log
                log.warn(
                        "The file '" + relativePath
                                + "' has already been committed"
                                + " to Bitstorage. Cannot upload before it has been deleted by"
                                + " and admin. Continuing with next file.");
                continue;
            } else {
                //mark the file as uploaded
                dataIDs.add(relativePath);
            }

            /*
                        System.out.println(remote_checksum);
                        System.out.println(local_checksum);
            */

            //compare checksums
            if (!local_checksum.equalsIgnoreCase(remote_checksum)) {
                discard(dataIDs);
                throw new IOException(
                        "Checksums for file '" + datafile.getPath()
                                + "' locally and in Bitstorage disagree."
                                + " Local '" + local_checksum + "' and "
                                + "remote '" + remote_checksum + "'. The"
                                + "error was '" + nr.getProcessErrorAsString()
                                + "'");
            }
        }
        return dataIDs;
    }

    /**
     * For a file, return the path relative to some parent directory. Allows the
     * adding of some prefix to this path. This method translate the path in the
     * File object into the {@link java.io.File#getCanonicalPath() cannonical
     * path}, and as such translate soft links. So soft links will simply
     * followed, and not end up as part of the path.
     *
     * @param file   The File to extract the relative path from.
     * @param root   the parent dir, that the relative path should begin in.
     * @param prefix the prefix to add.
     * @return the new path.
     *
     * @throws IllegalArgumentException if the file does not use the root
     *                                  prefix. Beware this for soft links.
     * @throws java.io.IOException      if the filesystem cannot understand the
     *                                  path for File.
     */
    private String relativePath(File file, String root, String prefix)
            throws IllegalArgumentException, IOException {

        if (file.getCanonicalPath().startsWith(root)) {
            return prefix + file.getCanonicalPath().substring(
                    root.length() + 1);
        } else {
            throw new IllegalArgumentException(
                    "The file '" + file.getCanonicalPath()
                            + "' does not use the '" + root + "' prefix");
        }
    }

    /**
     * Return a list of all the files in the directory dir, including files in
     * subfolders. Empty directories are not included.
     *
     * @param dir The directory to begin in.
     * @return A list of files. If dir is not a directory, return only dir.
     */
    private List<File> listDirectory(File dir, List<String> excludeList) {

        List<File> files = new ArrayList<File>(100);

        if (excludeList.contains(dir.getName())) {
            return files;
        }

        if (!dir.isDirectory()) {
            files.add(dir);
            return files;
        }

        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                files.addAll(listDirectory(file, excludeList));
            } else {
                if (!excludeList.contains(file.getName())) {
                    files.add(file);
                }
            }
        }
        return files;
    }
}