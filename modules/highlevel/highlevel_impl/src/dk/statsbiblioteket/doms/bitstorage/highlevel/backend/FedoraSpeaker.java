package dk.statsbiblioteket.doms.bitstorage.highlevel.backend;

import dk.statsbiblioteket.doms.bitstorage.characteriser.Characterisation;

import java.net.URL;

/**
 * Created by IntelliJ IDEA.
 * User: abr
 * Date: Dec 1, 2009
 * Time: 2:15:44 PM
 * To change this template use File | Settings | File Templates.
 */
public interface FedoraSpeaker {

    public void createContentDatastream(String pid, URL url, String checksum);

    public void replaceContentDatastream(String pid, URL url, String checksum);

    public String getFormatURI(String pid, String datastream);

    public void storeCharacterization(String pid, Characterisation characterisation);

    public boolean datastreamExists(String pid, String datastream);

    public boolean datastreamHasContent(String pid, String datastream);
}
