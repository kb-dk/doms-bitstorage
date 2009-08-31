package dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.exceptions;

/**
 * TODO abr forgot to document this class
 */
public class ChecksumFailedException extends BitstorageException{
    public ChecksumFailedException() {
    }

    public ChecksumFailedException(String message) {
        super(message);
    }

    public ChecksumFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public ChecksumFailedException(Throwable cause) {
        super(cause);
    }
}
