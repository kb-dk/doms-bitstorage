package dk.statsbiblioteket.doms.bitstorage.lowlevel;

import dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.BitstorageSshImpl;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.exceptions.ChecksumFailedException;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.exceptions.CommunicationException;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.exceptions.FileAlreadyApprovedException;
import dk.statsbiblioteket.util.Checksums;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;

/**
 * TODO abr forgot to document this class
 */
public class BitstorageSshImplTest {

    BitstorageSshImpl ssh;
    File testdata = new File("modules/lowlevelbitstorage/test/data/testfile");


    @Before
    public void setUp() {
        ssh = new BitstorageSshImpl();

    }

    @After
    public void tearDown() {
        // Add your code here
    }

    @Test
    public void testUpload() throws IOException, FileAlreadyApprovedException,
                                    CommunicationException,
                                    ChecksumFailedException {
        System.out.println(testdata.getAbsolutePath());
        byte[] md5bytes = Checksums.md5(testdata);
        String md5 = ByteString.toHex(md5bytes);
        URL url = ssh.upload("data/testfile",
                             new FileInputStream(testdata),
                             md5);

        System.out.println(url);

    }

    @Test

    public void testDisapprove() {
        // Add your code here
    }

    @Test
    public void testApprove() {

    }

    @Test
    public void testSpaceleft() throws CommunicationException {
        System.out.println(ssh.spaceleft());// Add your code here
    }
}
