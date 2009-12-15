package dk.statsbiblioteket.doms.bitstorage.characteriser.frontend;

import dk.statsbiblioteket.doms.bitstorage.characteriser.*;

import javax.jws.WebParam;

/**
 * Created by IntelliJ IDEA.
 * User: abr
 * Date: Dec 1, 2009
 * Time: 2:39:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class CharacteriseSoapWebserviceImpl implements
                                            CharacteriseSoapWebservice {

    public Characterisation characterise(
            @WebParam(name = "pid",
                      targetNamespace = "http://characteriser.bitstorage.doms.statsbiblioteket.dk/",
                      partName = "characteriseReturn")
            String characteriseReturn)
            throws CommunicationException, FileNotAvailableException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
