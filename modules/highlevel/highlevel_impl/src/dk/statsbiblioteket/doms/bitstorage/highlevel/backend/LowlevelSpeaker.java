package dk.statsbiblioteket.doms.bitstorage.highlevel.backend;

import javax.activation.DataHandler;
import java.net.URL;

/**
 * Created by IntelliJ IDEA.
 * User: abr
 * Date: Dec 1, 2009
 * Time: 2:15:56 PM
 * To change this template use File | Settings | File Templates.
 */
public interface LowlevelSpeaker {

    public URL uploadFile(String checksum, DataHandler file, long size);
}
