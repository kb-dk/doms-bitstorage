package dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.exceptions;

/**
 * TODO abr forgot to document this class
 */
public class FileAlreadyApprovedException
        extends BitstorageException {

    public FileAlreadyApprovedException() {
    }

    public FileAlreadyApprovedException(String message) {
        super(message);
    }

    public FileAlreadyApprovedException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileAlreadyApprovedException(Throwable cause) {
        super(cause);
    }
}
