package dk.statsbiblioteket.doms.bitstorage.lowlevel;

import dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.BitstorageSshImpl;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.Bitstorage;

/**
 * TODO abr forgot to document this class
 */
public class BitstorageFactory {


    public static Bitstorage getInstance(){
        return new BitstorageSshImpl();
    }

}
