package dk.statsbiblioteket.doms.bitstorage.lowlevel.exceptions;

/**
 * TODO abr forgot to document this class
 */
public abstract class BitstorageException extends Exception{

    public BitstorageException() {
    }

    public BitstorageException(String message) {
        super(message);
    }

    public BitstorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public BitstorageException(Throwable cause) {
        super(cause);
    }
}
