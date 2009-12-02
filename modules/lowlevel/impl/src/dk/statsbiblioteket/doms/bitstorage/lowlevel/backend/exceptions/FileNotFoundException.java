package dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.exceptions;

/**
 * TODO abr forgot to document this class
 */
public class FileNotFoundException
        extends BitstorageException {
    public FileNotFoundException() {
    }

    public FileNotFoundException(String message) {
        super(message);
    }

    public FileNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileNotFoundException(Throwable cause) {
        super(cause);
    }
}
