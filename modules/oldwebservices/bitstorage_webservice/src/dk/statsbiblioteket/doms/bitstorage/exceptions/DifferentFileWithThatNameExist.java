package dk.statsbiblioteket.doms.bitstorage.exceptions;

/**
 * Created by IntelliJ IDEA. User: abr Date: Sep 12, 2008 Time: 4:41:21 PM To
 * change this template use File | Settings | File Templates.
 */
public class DifferentFileWithThatNameExist extends Exception {
    private String message;


    public DifferentFileWithThatNameExist(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
