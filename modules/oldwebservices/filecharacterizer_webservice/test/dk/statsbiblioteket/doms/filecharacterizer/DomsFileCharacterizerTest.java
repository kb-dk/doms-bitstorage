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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.axis.types.URI;
import org.apache.axis.types.URI.MalformedURIException;

import java.util.Arrays;

/**
 * DomsFileCharacterizer Tester.
 *
 * @author tsh
 * @version 1.0
 * @since <pre>08/07/2008</pre>
 */
public class DomsFileCharacterizerTest extends TestCase {

    private static URI TEST_FILE_URI;
    private static final String TEST_FILE_FORMAT_ID = "fmt/100";
    private static final String EXPECTED_VALIDATION_STATUS = "VALID";

    public DomsFileCharacterizerTest(String name) throws MalformedURIException {
        super(name);
        TEST_FILE_URI = new URI("http://statsbiblioteket.dk/index.html");
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    //TODO: Enhance this test
    public void testCharacterizeFile() throws Exception {
        FileCharacterizer fileCharacterizer = new DomsFileCharacterizer();
        FileCharacterization fileCharacterization =
                fileCharacterizer.characterizeFile(TEST_FILE_URI);

        assertNotNull("FileCharacterizer.characterizeFile() returned null",
                      fileCharacterization);

        assertTrue("Unexpected characterisation output.", Arrays.equals(
                DomsFileCharacterizer.CHARACTERIZATION_OUTPUT.getBytes(),
                fileCharacterization.getCharacterizationOutput()));

        assertEquals(TEST_FILE_FORMAT_ID,
                     fileCharacterization.getPronomID());

        assertEquals(EXPECTED_VALIDATION_STATUS,
                     fileCharacterization.getValidationStatus());

        //FIXME! Test the MD5 checksum!
    }

    public static Test suite() {
        return new TestSuite(DomsFileCharacterizerTest.class);
    }
}
