/**
 * DomsFileCharacterizerServiceLocator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package dk.statsbiblioteket.doms.filecharacterizer;

public class DomsFileCharacterizerServiceLocator extends org.apache.axis.client.Service implements dk.statsbiblioteket.doms.filecharacterizer.DomsFileCharacterizerService {

    public DomsFileCharacterizerServiceLocator() {
    }


    public DomsFileCharacterizerServiceLocator(org.apache.axis.EngineConfiguration config) {
        super(config);
    }

    public DomsFileCharacterizerServiceLocator(java.lang.String wsdlLoc, javax.xml.namespace.QName sName) throws javax.xml.rpc.ServiceException {
        super(wsdlLoc, sName);
    }

    // Use to get a proxy class for FileCharacterizer
    private java.lang.String FileCharacterizer_address = "http://pc214:7910/filecharacterizer/services/FileCharacterizer";

    public java.lang.String getFileCharacterizerAddress() {
        return FileCharacterizer_address;
    }

    // The WSDD service name defaults to the port name.
    private java.lang.String FileCharacterizerWSDDServiceName = "FileCharacterizer";

    public java.lang.String getFileCharacterizerWSDDServiceName() {
        return FileCharacterizerWSDDServiceName;
    }

    public void setFileCharacterizerWSDDServiceName(java.lang.String name) {
        FileCharacterizerWSDDServiceName = name;
    }

    public dk.statsbiblioteket.doms.filecharacterizer.DomsFileCharacterizer getFileCharacterizer() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(FileCharacterizer_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getFileCharacterizer(endpoint);
    }

    public dk.statsbiblioteket.doms.filecharacterizer.DomsFileCharacterizer getFileCharacterizer(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            dk.statsbiblioteket.doms.filecharacterizer.FileCharacterizerSoapBindingStub _stub = new dk.statsbiblioteket.doms.filecharacterizer.FileCharacterizerSoapBindingStub(portAddress, this);
            _stub.setPortName(getFileCharacterizerWSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setFileCharacterizerEndpointAddress(java.lang.String address) {
        FileCharacterizer_address = address;
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (dk.statsbiblioteket.doms.filecharacterizer.DomsFileCharacterizer.class.isAssignableFrom(serviceEndpointInterface)) {
                dk.statsbiblioteket.doms.filecharacterizer.FileCharacterizerSoapBindingStub _stub = new dk.statsbiblioteket.doms.filecharacterizer.FileCharacterizerSoapBindingStub(new java.net.URL(FileCharacterizer_address), this);
                _stub.setPortName(getFileCharacterizerWSDDServiceName());
                return _stub;
            }
        }
        catch (java.lang.Throwable t) {
            throw new javax.xml.rpc.ServiceException(t);
        }
        throw new javax.xml.rpc.ServiceException("There is no stub implementation for the interface:  " + (serviceEndpointInterface == null ? "null" : serviceEndpointInterface.getName()));
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(javax.xml.namespace.QName portName, Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        if (portName == null) {
            return getPort(serviceEndpointInterface);
        }
        java.lang.String inputPortName = portName.getLocalPart();
        if ("FileCharacterizer".equals(inputPortName)) {
            return getFileCharacterizer();
        }
        else  {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return _stub;
        }
    }

    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("http://filecharacterizer.doms.statsbiblioteket.dk", "DomsFileCharacterizerService");
    }

    private java.util.HashSet ports = null;

    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("http://filecharacterizer.doms.statsbiblioteket.dk", "FileCharacterizer"));
        }
        return ports.iterator();
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(java.lang.String portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        
if ("FileCharacterizer".equals(portName)) {
            setFileCharacterizerEndpointAddress(address);
        }
        else 
{ // Unknown Port Name
            throw new javax.xml.rpc.ServiceException(" Cannot set Endpoint Address for Unknown Port" + portName);
        }
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(javax.xml.namespace.QName portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        setEndpointAddress(portName.getLocalPart(), address);
    }

}
