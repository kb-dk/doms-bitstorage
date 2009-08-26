/**
 * FileCharacterization.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package dk.statsbiblioteket.doms.filecharacterizer;

public class FileCharacterization  implements java.io.Serializable {
    private byte[] characterizationOutput;

    private java.lang.String md5CheckSum;

    private java.lang.String pronomID;

    private java.lang.String validationStatus;

    public FileCharacterization() {
    }

    public FileCharacterization(
           byte[] characterizationOutput,
           java.lang.String md5CheckSum,
           java.lang.String pronomID,
           java.lang.String validationStatus) {
           this.characterizationOutput = characterizationOutput;
           this.md5CheckSum = md5CheckSum;
           this.pronomID = pronomID;
           this.validationStatus = validationStatus;
    }


    /**
     * Gets the characterizationOutput value for this FileCharacterization.
     * 
     * @return characterizationOutput
     */
    public byte[] getCharacterizationOutput() {
        return characterizationOutput;
    }


    /**
     * Sets the characterizationOutput value for this FileCharacterization.
     * 
     * @param characterizationOutput
     */
    public void setCharacterizationOutput(byte[] characterizationOutput) {
        this.characterizationOutput = characterizationOutput;
    }


    /**
     * Gets the md5CheckSum value for this FileCharacterization.
     * 
     * @return md5CheckSum
     */
    public java.lang.String getMd5CheckSum() {
        return md5CheckSum;
    }


    /**
     * Sets the md5CheckSum value for this FileCharacterization.
     * 
     * @param md5CheckSum
     */
    public void setMd5CheckSum(java.lang.String md5CheckSum) {
        this.md5CheckSum = md5CheckSum;
    }


    /**
     * Gets the pronomID value for this FileCharacterization.
     * 
     * @return pronomID
     */
    public java.lang.String getPronomID() {
        return pronomID;
    }


    /**
     * Sets the pronomID value for this FileCharacterization.
     * 
     * @param pronomID
     */
    public void setPronomID(java.lang.String pronomID) {
        this.pronomID = pronomID;
    }


    /**
     * Gets the validationStatus value for this FileCharacterization.
     * 
     * @return validationStatus
     */
    public java.lang.String getValidationStatus() {
        return validationStatus;
    }


    /**
     * Sets the validationStatus value for this FileCharacterization.
     * 
     * @param validationStatus
     */
    public void setValidationStatus(java.lang.String validationStatus) {
        this.validationStatus = validationStatus;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof FileCharacterization)) return false;
        FileCharacterization other = (FileCharacterization) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.characterizationOutput==null && other.getCharacterizationOutput()==null) || 
             (this.characterizationOutput!=null &&
              java.util.Arrays.equals(this.characterizationOutput, other.getCharacterizationOutput()))) &&
            ((this.md5CheckSum==null && other.getMd5CheckSum()==null) || 
             (this.md5CheckSum!=null &&
              this.md5CheckSum.equals(other.getMd5CheckSum()))) &&
            ((this.pronomID==null && other.getPronomID()==null) || 
             (this.pronomID!=null &&
              this.pronomID.equals(other.getPronomID()))) &&
            ((this.validationStatus==null && other.getValidationStatus()==null) || 
             (this.validationStatus!=null &&
              this.validationStatus.equals(other.getValidationStatus())));
        __equalsCalc = null;
        return _equals;
    }

    private boolean __hashCodeCalc = false;
    public synchronized int hashCode() {
        if (__hashCodeCalc) {
            return 0;
        }
        __hashCodeCalc = true;
        int _hashCode = 1;
        if (getCharacterizationOutput() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getCharacterizationOutput());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getCharacterizationOutput(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getMd5CheckSum() != null) {
            _hashCode += getMd5CheckSum().hashCode();
        }
        if (getPronomID() != null) {
            _hashCode += getPronomID().hashCode();
        }
        if (getValidationStatus() != null) {
            _hashCode += getValidationStatus().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(FileCharacterization.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://filecharacterizer.doms.statsbiblioteket.dk", "FileCharacterization"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("characterizationOutput");
        elemField.setXmlName(new javax.xml.namespace.QName("http://filecharacterizer.doms.statsbiblioteket.dk", "characterizationOutput"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "base64Binary"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("md5CheckSum");
        elemField.setXmlName(new javax.xml.namespace.QName("http://filecharacterizer.doms.statsbiblioteket.dk", "md5CheckSum"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("pronomID");
        elemField.setXmlName(new javax.xml.namespace.QName("http://filecharacterizer.doms.statsbiblioteket.dk", "pronomID"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("validationStatus");
        elemField.setXmlName(new javax.xml.namespace.QName("http://filecharacterizer.doms.statsbiblioteket.dk", "validationStatus"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
    }

    /**
     * Return type metadata object
     */
    public static org.apache.axis.description.TypeDesc getTypeDesc() {
        return typeDesc;
    }

    /**
     * Get Custom Serializer
     */
    public static org.apache.axis.encoding.Serializer getSerializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanSerializer(
            _javaType, _xmlType, typeDesc);
    }

    /**
     * Get Custom Deserializer
     */
    public static org.apache.axis.encoding.Deserializer getDeserializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanDeserializer(
            _javaType, _xmlType, typeDesc);
    }

}
