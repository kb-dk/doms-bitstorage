package dk.statsbiblioteket.doms.objectmanipulation;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.axis.types.URI;

/** Interface for manipulating existing objects in the DOMS. */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "",
        reviewers = {""})
public interface ObjectManipulation {

    /**
     * Upload file to bitstorage and attach to and update given file object. The
     * URL and the characterization information received when uploading file to
     * bitstorage is appended to the file object identified by the given PID
     *
     * @param username        Name of the user adding the file.
     * @param password        Password for the user adding the file.
     * @param fileInformation Information about the file to add.
     * @param pid             PID of the DOMS object to add the file to.
     */
    //FIXME! Remember adding @throws to javadoc when all exceptions are known.
    public void addFile(String username, String password,
                        FileInformation fileInformation, URI pid)
            throws Exception;
}
