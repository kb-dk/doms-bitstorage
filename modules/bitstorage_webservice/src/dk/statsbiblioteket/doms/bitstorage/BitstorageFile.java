package dk.statsbiblioteket.doms.bitstorage;

import javax.jws.WebService;
import java.io.Serializable;

import dk.statsbiblioteket.doms.filecharacterizer.FileCharacterization;
import org.apache.axis.types.URI;

/**
 * A bitstorage file object contains characteristics, public url, name and file.
 *
 * A bitstorage file object has methods to obtain public url, file name,
 * characterization output, md5 checksum, pronom ID and validation status.
 *
 */
@WebService public class BitstorageFile implements Serializable{

    private URI fileurl;
    private String fileName;
    private FileCharacterization charac;

    /**
     * Construct a bitstorage file object with given content.
     * @param fileurl public url
     * @param fileName name
     * @param calculated_md5 md5 checksum
     * @param charac file characterization
     */
    public BitstorageFile(URI fileurl, String fileName, String calculated_md5,
                          FileCharacterization charac) {
        //To change body of created methods use File | Settings | File Templates.
        this.fileurl = fileurl;
        this.fileName = fileName;
        this.charac = charac;
    }

    public BitstorageFile() {
    }

    public URI getFileurl() {
        return fileurl;
    }

    public String getFileName() {
        return fileName;
    }

    public byte[] getCharacterizationOutput() {
        return charac.getCharacterizationOutput();
    }

    public String getMd5CheckSum() {
        return charac.getMd5CheckSum();
    }

    public String getPronomID() {
        return charac.getPronomID();
    }

    public String getValidationStatus() {
        return charac.getValidationStatus();
    }


}
