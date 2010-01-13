package dk.statsbiblioteket.doms.bitstorage.highlevel.frontend;

/**
 * Created by IntelliJ IDEA.
 * User: abr
 * Date: Jan 12, 2010
 * Time: 3:04:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class ConfigException extends RuntimeException{
    public ConfigException() {
    }

    public ConfigException(String message) {
        super(message);
    }

    public ConfigException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConfigException(Throwable cause) {
        super(cause);
    }
}
