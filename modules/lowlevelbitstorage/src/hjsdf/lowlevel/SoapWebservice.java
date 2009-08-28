package hjsdf.lowlevel;


import com.sun.xml.ws.developer.StreamingDataHandler;
import hjsdf.lowlevel.exceptions.CommunicationException;
import hjsdf.lowlevel.exceptions.FileNotFoundException;
import hjsdf.lowlevel.exceptions.NotEnoughFreeSpaceException;
import hjsdf.lowlevel.exceptions.UploadFailedException;

import javax.activation.DataHandler;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlMimeType;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.soap.MTOM;
import java.io.IOException;
import java.net.URL;

/**
 * TODO abr forgot to document this class
 */

@MTOM
@WebService()
public class SoapWebservice {

    Bitstorage bs;

    public SoapWebservice() {
        bs = BitstorageFactory.getInstance();
    }

    @WebMethod
    public URL uploadFile(@WebParam String fileName,
                                     @XmlMimeType("application/octet-stream")
                                     DataHandler data,
                                     String provided_md5) throws
                                                          UploadFailedException,
                                                          CommunicationException,
                                                          NotEnoughFreeSpaceException {

        StreamingDataHandler dh = (StreamingDataHandler)data;
        try {
            return bs.upload(fileName,dh.readOnce(),provided_md5);
        } catch (IOException e) {
            throw new WebServiceException(e);
        }

    }

    @WebMethod
    public void disapprove(URL fileURL, String md5) throws
                                                    CommunicationException,
                                                    FileNotFoundException {
        bs.disapprove(fileURL,md5);

    }

    @WebMethod
    public void approve(URL fileURL, String md5) throws CommunicationException,
                                                        FileNotFoundException,
                                                        NotEnoughFreeSpaceException {
        bs.approve(fileURL,md5);
        
    }
}