package dk.statsbiblioteket.doms.bitstorage.highlevel.fedora;

import dk.statsbiblioteket.doms.bitstorage.characteriser.Characterisation;

import java.util.Collection;

/**
 * Created by IntelliJ IDEA.
 * User: abr
 * Date: Dec 1, 2009
 * Time: 2:15:44 PM
 * To change this template use File | Settings | File Templates.
 */
public interface FedoraSpeaker {



    public void createContentDatastream(String pid, String url, String checksum) throws
                                                                                 FedoraObjectNotFoundException,
                                                                                 FedoraDatastreamAlreadyExistException,
                                                                                 FedoraCommunicationException,
                                                                                 FedoraChecksumFailedException;

    public void replaceContentDatastream(String pid, String url, String checksum) throws
                                                                                  FedoraObjectNotFoundException,
                                                                                  FedoraDatastreamNotFoundException,
                                                                                  FedoraCommunicationException,
                                                                                  FedoraChecksumFailedException;

    public Collection<String> getFormatURI(String pid, String datastream) throws
                                                                          FedoraObjectNotFoundException,
                                                                          FedoraDatastreamNotFoundException,
                                                                          FedoraCommunicationException;

    public void storeCharacterization(String pid, Characterisation characterisation) throws
                                                                                     FedoraObjectNotFoundException,

                                                                                     FedoraCommunicationException;

    public boolean datastreamExists(String pid, String datastream) throws
                                                                   FedoraObjectNotFoundException,
                                                                   FedoraCommunicationException;

    public boolean datastreamHasContent(String pid, String datastream) throws
                                                                       FedoraObjectNotFoundException,
                                                                       FedoraDatastreamNotFoundException,
                                                                       FedoraCommunicationException;

    void deleteDatastream(String pid, String ds) throws
                                                 FedoraObjectNotFoundException,
                                                 FedoraDatastreamNotFoundException,
                                                 FedoraCommunicationException;

    public String getContentDatastreamName();

    public String getCharacterisationDatastreamName();

    String getFileUrl(String pid)  throws
                                                 FedoraObjectNotFoundException,
                                                 FedoraDatastreamNotFoundException,
                                                 FedoraCommunicationException;

    String getFileChecksum(String pid)  throws
                                                 FedoraObjectNotFoundException,
                                                 FedoraDatastreamNotFoundException,
                                                 FedoraCommunicationException;;
}
