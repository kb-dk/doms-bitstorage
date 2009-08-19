package dk.statsbiblioteket.doms.bitstorage.exceptions;

/**
 * Created by IntelliJ IDEA. User: abr Date: Sep 10, 2008 Time: 4:27:56 PM To
 * change this template use File | Settings | File Templates.
 */
public class CharacterizationFailed extends Exception {

    private String s;

    public CharacterizationFailed(String s) {
        this.s = s;
    }

    public String getS() {
        return s;
    }
}
