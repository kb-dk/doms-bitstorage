package hjsdf.lowlevel.exceptions;

/**
 * TODO abr forgot to document this class
 */
public abstract class UploadFailedException extends BitstorageException{
    protected UploadFailedException() {
    }

    protected UploadFailedException(String message) {
        super(message);
    }

    protected UploadFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    protected UploadFailedException(Throwable cause) {
        super(cause);
    }
}
