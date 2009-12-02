package dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.exceptions;

/**
 * TODO abr forgot to document this class
 */
public class InvalidFilenameException
        extends BitstorageException {
    public InvalidFilenameException() {
    }

    public InvalidFilenameException(String message) {
        super(message);
    }

    public InvalidFilenameException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidFilenameException(Throwable cause) {
        super(cause);
    }
}
