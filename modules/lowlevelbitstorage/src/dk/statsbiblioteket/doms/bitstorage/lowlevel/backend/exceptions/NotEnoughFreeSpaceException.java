package dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.exceptions;

/**
 * TODO abr forgot to document this class
 */
public class NotEnoughFreeSpaceException extends BitstorageException{

    public NotEnoughFreeSpaceException() {
    }

    public NotEnoughFreeSpaceException(String message) {
        super(message);
    }

    public NotEnoughFreeSpaceException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotEnoughFreeSpaceException(Throwable cause) {
        super(cause);
    }
}
