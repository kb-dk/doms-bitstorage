package dk.statsbiblioteket.doms.filecharacterizer;

import java.io.Serializable;

/**
 * The return value of the characterizeFile method of the FileCharacterizer
 * API.
 */
public class FileCharacterization implements Serializable {

    // FIXME! The production system should use an enumeration type for the
    // status!

//    private ValidationStatus validationStatus;
    private String validationStatus;
    private byte[] characterizationOutput;
    private String pronomID;
    private String md5CheckSum;

    /**
     * Returns the output of the characterization tool.
     *
     * @return The output of the characterization tool.
     */
    public byte[] getCharacterizationOutput() {
        return characterizationOutput;
    }

    /**
     * Set the output of the characterization tool.
     *
     * @param characterizationOutput The output of the characterization tool.
     */
    public void setCharacterizationOutput(byte[] characterizationOutput) {
        this.characterizationOutput = characterizationOutput;
    }

    /**
     * Returns the PRONOM file format ID.
     *
     * @return The PRONOM file format ID.
     */
    public String getPronomID() {
        return pronomID;
    }

    /**
     * Set the PRONOM file format ID.
     *
     * @param pronomID The PRONOM file format ID.
     */
    public void setPronomID(String pronomID) {
        this.pronomID = pronomID;
    }

//  FIXME! The production system should use an enumeration type for the status!
//    public ValidationStatus getValidationStatus() {
//        return validationStatus;
//    }
//
//    public void setValidationStatus(ValidationStatus validationStatus) {
//        this.validationStatus = validationStatus;
//    }
    
    public String getValidationStatus() {
        return validationStatus;
    }

    public void setValidationStatus(String validationStatus) {
        this.validationStatus = validationStatus;
    }

    public String getMd5CheckSum() {
        return md5CheckSum;
    }

    public void setMd5CheckSum(String md5CheckSum) {
        this.md5CheckSum = md5CheckSum;
    }
}
