package dk.statsbiblioteket.doms.bitstorage.exceptions;

/**
 * Created by IntelliJ IDEA. User: abr Date: Sep 10, 2008 Time: 2:00:36 PM To
 * change this template use File | Settings | File Templates.
 */
public class WrongChecksum extends Exception {
    private String message;
    public WrongChecksum(String s) {
        message = s;
    }

    public String getMessage() {
        return message;
    }
}
