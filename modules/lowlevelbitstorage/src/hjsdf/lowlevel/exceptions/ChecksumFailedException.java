package hjsdf.lowlevel.exceptions;

/**
 * TODO abr forgot to document this class
 */
public class ChecksumFailedException extends UploadFailedException{
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
