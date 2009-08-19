package dk.statsbiblioteket.doms.filecharacterizer;

import org.apache.axis.types.URI;

/**
 * Interface for characterisation tools.
 */
public interface FileCharacterizer {

    /**
     * Perform a characterization of a file identified by the URI provided by
     * the caller.</p>
     *
     * @param fileURI URI for the file to charaterise.
     * @return <code>FileCharacterization</code> object containing the
     *         identified characteristica of the file.
     * @throws Exception if anything goes wrong.
     * @see dk.statsbiblioteket.doms.filecharacterizer.FileCharacterization
     */
    public abstract FileCharacterization characterizeFile(URI fileURI)
            throws Exception;
}
