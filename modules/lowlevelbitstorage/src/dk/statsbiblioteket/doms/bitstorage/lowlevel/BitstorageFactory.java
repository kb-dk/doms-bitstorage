package dk.statsbiblioteket.doms.bitstorage.lowlevel;

/**
 * TODO abr forgot to document this class
 */
public class BitstorageFactory {


    public static Bitstorage getInstance(){
        return new BitstorageSshImpl();
    }
}
