package dk.statsbiblioteket.doms.bitstorage.lowlevel;

import com.sun.xml.internal.ws.developer.StreamingDataHandler;

import javax.activation.DataHandler;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlMimeType;
import javax.xml.ws.soap.MTOM;
import javax.xml.ws.WebServiceException;
import java.net.URL;
import java.io.IOException;

/**
 * TODO abr forgot to document this class
 */

@MTOM
@WebService()
public class SoapWebservice {

    Bitstorage bs;

    @WebMethod
    public URL uploadFile(@WebParam String fileName,
                                     @XmlMimeType("application/octet-stream")
                                     DataHandler data,
                                     String provided_md5){

        StreamingDataHandler dh = (StreamingDataHandler)data;
        try {
            return bs.upload(fileName,dh.readOnce(),provided_md5);
        } catch (IOException e) {
            throw new WebServiceException(e);
        }

    }

    @WebMethod
    public void disapprove(URL fileURL, String md5){
        bs.disapprove(fileURL,md5);

    }

    @WebMethod
    public void approve(URL fileURL, String md5){
        bs.approve(fileURL,md5);
        
    }
}