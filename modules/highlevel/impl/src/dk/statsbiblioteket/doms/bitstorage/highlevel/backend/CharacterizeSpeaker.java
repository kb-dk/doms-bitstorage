package dk.statsbiblioteket.doms.bitstorage.highlevel.backend;

import dk.statsbiblioteket.doms.bitstorage.characterise.*;

import javax.xml.namespace.QName;
import java.net.URL;

/**
 * Created by IntelliJ IDEA.
 * User: abr
 * Date: Dec 1, 2009
 * Time: 2:16:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class CharacterizeSpeaker {

    CharacteriseSoapWebservice client;
    public CharacterizeSpeaker(URL wsdlLocation) {


        client = new CharacteriseSoapWebserviceService(wsdlLocation,
                                              new QName(
                                                      "http://characterise.bitstorage.doms.statsbiblioteket.dk/",
                                                      "CharacteriseSoapWebserviceService"))
                .getCharacteriseSoapWebservicePort();
    }

    public Characterisation characterise(String pid){
        try {
            client.characterise(pid);
        } catch (CommunicationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (FileNotAvailableException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return null;
    }
}
