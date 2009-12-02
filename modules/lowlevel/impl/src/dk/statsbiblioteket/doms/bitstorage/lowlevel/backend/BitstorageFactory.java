package dk.statsbiblioteket.doms.bitstorage.lowlevel.backend;

import dk.statsbiblioteket.doms.bitstorage.lowlevel.BitstorageSshConfig;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;

/**
 * TODO abr forgot to document this class
 */
public class BitstorageFactory {


    public static Bitstorage getInstance() {

        try {
            JAXBContext context = JAXBContext.newInstance(BitstorageSshConfig.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            InputStream config = Thread.currentThread().getContextClassLoader().getResourceAsStream(
                    "bitstorageSsh.xml");

            BitstorageSshConfig bitstorageConfig = (BitstorageSshConfig) unmarshaller.unmarshal(
                    config);

            return new BitstorageSshImpl(bitstorageConfig.getServer(),
                                         bitstorageConfig.getScript(),
                                         bitstorageConfig.getBitfinder());

        } catch (JAXBException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            return new BitstorageSshImpl();
        }

    }

}
