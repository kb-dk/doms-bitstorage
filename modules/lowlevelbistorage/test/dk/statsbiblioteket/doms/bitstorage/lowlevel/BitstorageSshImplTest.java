package dk.statsbiblioteket.doms.bitstorage.lowlevel;

import dk.statsbiblioteket.util.Checksums;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

/**
 * TODO abr forgot to document this class
 */
public class BitstorageSshImplTest {

    BitstorageSshImpl ssh;
    File testdata = new File("data/testfile");


    @Before
    public void setUp() {
        ssh = new BitstorageSshImpl();
    }

    @After
    public void tearDown() {
        // Add your code here
    }

    @Test
    public void testUpload() throws IOException {
        byte[] md5bytes = Checksums.md5(testdata);
        ByteString
    }

    @Test
    public void testDisapprove() {
        // Add your code here
    }

    @Test
    public void testApprove() {
        // Add your code here
    }

    @Test
    public void testSpaceleft() {
        System.out.println(ssh.spaceleft());// Add your code here
    }
}
