package dk.statsbiblioteket.doms.bitstorage.lowlevel.backend;

/**
 * TODO abr forgot to document this class
 */
public class BitstorageFactory {



    public static Bitstorage getInstance(String script, String server, String bitfinder) {
        return new BitstorageSshImpl(server,
                                     script,
                                     bitfinder);


    }

}
