package dk.statsbiblioteket.doms.bitstorage.exceptions;

/**
 * Created by IntelliJ IDEA. User: abr Date: Sep 10, 2008 Time: 1:59:33 PM To
 * change this template use File | Settings | File Templates.
 */
public class InvalidFileName extends Exception {

    private String message;
    public InvalidFileName(String s) {
        message = s;
    }

    public String getMessage() {
        return message;
    }
}
