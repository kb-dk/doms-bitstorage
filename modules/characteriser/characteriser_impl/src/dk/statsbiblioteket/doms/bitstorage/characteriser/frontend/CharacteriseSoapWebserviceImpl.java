package dk.statsbiblioteket.doms.bitstorage.characteriser.frontend;

import dk.statsbiblioteket.doms.bitstorage.characteriser.Characterisation;
import dk.statsbiblioteket.doms.bitstorage.characteriser.CharacteriseSoapWebservice;
import dk.statsbiblioteket.doms.bitstorage.characteriser.CommunicationException;
import dk.statsbiblioteket.doms.bitstorage.characteriser.FileNotAvailableException;

import javax.annotation.Resource;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.servlet.ServletContext;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

/**
 * Created by IntelliJ IDEA.
 * User: abr
 * Date: Dec 1, 2009
 * Time: 2:39:13 PM
 * To change this template use File | Settings | File Templates.
 */
@WebService(endpointInterface = "dk.statsbiblioteket.doms.bitstorage.characteriser.CharacteriseSoapWebservice")
public class CharacteriseSoapWebserviceImpl implements
                                            CharacteriseSoapWebservice {

    @Resource
    private WebServiceContext webServiceContext;

    private boolean initialised = false;

    private void initialise(){
        if (initialised){
            return;
        }
        ServletContext servletContext =
                (ServletContext) webServiceContext.getMessageContext().get(
                        MessageContext.SERVLET_CONTEXT);
        String testparam = servletContext.getInitParameter("testParam");
        initialised = true;
        
    }


    public Characterisation characterise(
            @WebParam(name = "pid",
                      targetNamespace = "http://characteriser.bitstorage.doms.statsbiblioteket.dk/",
                      partName = "characteriseReturn")
            String characteriseReturn)
            throws CommunicationException, FileNotAvailableException {
        initialise();
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
