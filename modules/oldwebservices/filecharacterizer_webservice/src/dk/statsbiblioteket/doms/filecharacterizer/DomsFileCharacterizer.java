/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The DOMS project.
 * Copyright (C) 2007  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.statsbiblioteket.doms.filecharacterizer;

import dk.statsbiblioteket.util.Checksums;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.axis.types.URI;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * This is a dummy implementation of the <code>FileCharacterizer</code>
 * interface for the DOMS testbed, which is capable of downloading a file for
 * characterisation and returning a hard-coded response when receiving a
 * characterization request. That is, no actual characterisation is perfomed.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_OK,
        author = "tsh",
        reviewers = {"jrg"})
public class DomsFileCharacterizer implements FileCharacterizer {

    private static final int BUFFER_SIZE_32KB = 32768;

    /**
     * Temporary directory to download remote files to. Currently hard-coded to
     * "/tmp".
     */
    private static final String TMP_DIR = "/tmp";

    /** Hard-coded characterisation output string: "The caek is a lie ;-)" */
    static final String CHARACTERIZATION_OUTPUT = "<The_caek_is_a_lie/>";

    /**
     * Download the file specified by the URL <code>fileName</code> and return a
     * fake characterisation response, containing the hard-coded
     * characterisation message defined by the
     * <code>CHARACTERIZATION_OUTPUT</code>
     * constant and the file suffix of the file as the format ID.
     *
     * @param fileURI URL to the file to download for characterisation.
     * @return A <code>FileCharacterization</code> object containing bogus
     *         information.
     * @throws IOException              If the characteriser failed downloading
     *                                  the file.
     * @throws NoSuchAlgorithmException FIXME! This exception should never be
     *                                  thrown, and if it does it should be
     *                                  properly logged in the production
     *                                  system, but this is OK for now.
     */
    @QAInfo(level = QAInfo.Level.NORMAL,
            state = QAInfo.State.QA_OK,
            author = "tsh",
            reviewers = {"jrg"})
    public FileCharacterization characterizeFile(URI fileURI)
            throws IOException, NoSuchAlgorithmException {

        final String fileURIString = fileURI.toString();
        final URL fileURL = new URL(fileURIString);

        File downloadedFile = getRemoteFile(fileURL);
        try {

            String remoteFileExtension = fileURIString.substring(
                    fileURIString.lastIndexOf('.') + 1);

            FileCharacterization fileCharacterization
                    = new FileCharacterization();

            final String pronomID = getPronomID(remoteFileExtension);
            fileCharacterization.setPronomID(pronomID);
            fileCharacterization.setCharacterizationOutput(
                    CHARACTERIZATION_OUTPUT.getBytes());


            final FileInputStream fis = new FileInputStream(downloadedFile);
            final byte[] md5sum = Checksums.md5(fis);
            fileCharacterization.setMd5CheckSum(toHex(md5sum));

            if (pronomID.isEmpty()) {
                //  FIXME! The production system should use an enumeration type
                //  in the wsdl for the status!
                fileCharacterization.setValidationStatus("INVALID");
            } else {
                fileCharacterization.setValidationStatus("VALID");
            }
            return fileCharacterization;
        } finally {
            downloadedFile.delete();
        }
    }


    /**
     * Will return an empty string for uidentified files. FIXME! Improve doc.
     * @param fileExtension
     * @return
     */
    private String getPronomID(final String fileExtension) {
        String lowerCaseExtension = fileExtension.toLowerCase();

        String[][] pronomMap = new String[][]{{"html", "fmt/100"},
                                              {"tiff", "fmt/10"},
                                              {"tif", "fmt/10"},
                                              {"jpeg", "fmt/44"},
                                              {"jpg", "fmt/44"},
                                              {"pdf", "fmt/20"}};

        for (String[] pronomEntry : pronomMap) {
            if (pronomEntry[0].equals(lowerCaseExtension)) {
                return pronomEntry[1];
            }
        }
        return "";
    }


    /**
     * Download the remote file specified by <code>fileURL</code> and store it
     * locally in the temporary directory specified by the TMP_DIR constant of
     * this class. The downloaded file will be renamed to a <code>UUID</code>
     * and the full path and name will be returned as a <code>File</code>
     * object.
     *
     * @param fileURL URL to the remote file to download.
     * @return File object representing the local version of the remote file.
     * @throws IOException if any network or file errors occurs.
     */
    @QAInfo(level = QAInfo.Level.NORMAL,
            state = QAInfo.State.QA_OK,
            author = "tsh",
            reviewers = {"jrg"})
    private File getRemoteFile(URL fileURL) throws IOException {

        URLConnection connection = fileURL.openConnection();

        File outputFile = new File(TMP_DIR, UUID.randomUUID().toString());
        FileOutputStream fos = new FileOutputStream(outputFile);

        try {
            BufferedInputStream bis = new BufferedInputStream(
                    connection.getInputStream());


            final byte[] readBuffer = new byte[BUFFER_SIZE_32KB];
            int bytesRead = bis.read(readBuffer, 0, readBuffer.length - 1);

            while (bytesRead >= 0) {
                fos.write(readBuffer, 0, bytesRead);
                bytesRead = bis.read(readBuffer, 0, readBuffer.length - 1);
            }
        } finally {
            fos.close();
        }
        return outputFile;
    }

    //FIXME! This should be in SBUtils ...

    private static final byte MAGIC_INTEGER_4 = 4;
    private static final byte MAGIC_INTEGER_OxOF = 0x0f;
    /**
     * Converts a byte array to a string of hexadecimal characters.
     * @param ba the bytearray to be converted
     * @return ba converted to a hexstring
     */
    private static String toHex(final byte[] ba) {
        char[] hexdigit = {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a',
                'b', 'c',
                'd', 'e', 'f'
        };

        StringBuffer sb = new StringBuffer("");
        int ba_len = ba.length;

        for (int i = 0; i < ba_len; i++) {
            sb.append(hexdigit[(ba[i] >> MAGIC_INTEGER_4) & MAGIC_INTEGER_OxOF]
                );
            sb.append(hexdigit[ba[i] & MAGIC_INTEGER_OxOF]);
        }
        return sb.toString();
    }

}
