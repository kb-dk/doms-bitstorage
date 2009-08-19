package dk.statsbiblioteket.doms.bitstorage;

import dk.statsbiblioteket.doms.bitstorage.exceptions.*;
import dk.statsbiblioteket.doms.filecharacterizer.DomsFileCharacterizer;
import dk.statsbiblioteket.doms.filecharacterizer.DomsFileCharacterizerService;
import dk.statsbiblioteket.doms.filecharacterizer.DomsFileCharacterizerServiceLocator;
import dk.statsbiblioteket.doms.filecharacterizer.FileCharacterization;
import dk.statsbiblioteket.util.Checksums;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.axis.types.URI;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.xml.rpc.ServiceException;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


/**
 * Created by IntelliJ IDEA. User: abr Date: Sep 4, 2008 Time: 3:37:47 PM To
 * change this template use File | Settings | File Templates.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "",
        reviewers = {""})
@WebService public class TestBedBitstorage implements Bitstorage{


    private URI server;
    private String storedir;
    private static final Log log
            = LogFactory.getLog(TestBedBitstorage.class);


    public TestBedBitstorage() {
        log.debug("entering constructor");
        this.storedir = "webapps/Bitstorage/storedir/";
        new File(storedir).mkdirs();
        try {
            server = new URI("http://alhena.statsbiblioteket.dk:7910/Bitstorage/storedir/");
        } catch (URI.MalformedURIException e) {
            throw new Error("default server invalid");
        }
        log.debug("Ending constructor");
    }


    @WebMethod public BitstorageFile uploadFile(String fileName, URI localurl, String provided_md5)
            throws RemoteException, CannotGetFile, InvalidFileName,
            CannotStoreFile, WrongChecksum, CharacterizationFailed,
            DifferentFileWithThatNameExist {

        log.debug("Entering upload with parameters:\nfileName: '" + fileName +
                  "'\nlocalurl: '" + localurl + "'\nmd5: '" +
                  provided_md5 + "'");

        System.out.println("BitStorage: Entering upload with parameters:\nfileName: '" + fileName +
                  "'\nlocalurl: '" + localurl + "'\nmd5: '" +
                  provided_md5 + "'");

        URI fileurl;
        try {
            fileurl = new URI(server + fileName);
        } catch (URI.MalformedURIException e) {
            throw new InvalidFileName("the filename was invalid");
        }


        File targetFile;
        try{
            targetFile = new File(storedir,fileName);
        } catch (NullPointerException e){
            throw new InvalidFileName("The filename was null");
        }


        if (targetFile.exists()){
            try {
                byte[] bytehash = Checksums.md5(targetFile);
                String hash = toHex(bytehash);
                if (hash.equalsIgnoreCase(provided_md5)){//the same file
                    FileCharacterization charac = characterize(fileurl);
                    return new BitstorageFile(fileurl, fileName,provided_md5,charac);
                } else{//not the same file
                    throw new DifferentFileWithThatNameExist("There is already a file like that");
                }
            } catch (IOException e) {
                throw new CannotStoreFile("File exist already, but cannot be read");
            }
        } //file does not exist already

        log.debug("Target file:" + targetFile.getAbsolutePath());
        String calculated_md5 = fetch(localurl,targetFile);



        log.debug("Comparing checksums");
        if (!calculated_md5.equalsIgnoreCase(provided_md5)){
            throw new WrongChecksum("The provided checksum does not match the calculated");
        }
        log.debug("Checksums match");

        FileCharacterization charac = characterize(fileurl);

        return new BitstorageFile(fileurl, fileName,calculated_md5,charac);

    }

    private FileCharacterization characterize(URI file)
            throws CharacterizationFailed {
        DomsFileCharacterizerService service =
                new DomsFileCharacterizerServiceLocator();
        try {

            DomsFileCharacterizer fc
                    = service.getFileCharacterizer(new URL("http://localhost:7910/filecharacterizer_webservice/services/FileCharacterizer"));
            return fc.characterizeFile(file);

        } catch (ServiceException e) {
            log.error("charac call failed",e);
            throw new CharacterizationFailed("The characterizer failed somehow");

        } catch (RemoteException e) {
            log.error("charac call failed",e);
            throw new Error("Charac call failed");
        } catch (MalformedURLException e) {
            log.error("charac call failed",e);
            throw new CharacterizationFailed("The characterizer failed somehow");
        }
    }

    /**
     * Fetches the file at the source, and writes it in target. Returns the md5
     * of the file
     * @param source the URI for the file
     * @param target the File to store the bytes in
     * @return The dynamically calculated md5
     * @throws CannotGetFile if The file cannot be accessed
     * @throws dk.statsbiblioteket.doms.bitstorage.exceptions.CannotStoreFile
     * The target file could not be completely written
     */
    private String fetch(URI source, File target)
            throws CannotGetFile, CannotStoreFile {
        log.debug("Entering fetch");
        int bufSize = 4*1024;
        try {
            if (source == null){
                throw new CannotGetFile("URL was null");
            }
            URL url = new URL(source.toString());
            InputStream in = url.openStream();
            DigestInputStream in2 =
                    new DigestInputStream(in,
                                          MessageDigest.getInstance("MD5"));
            target.getParentFile().mkdirs();
            if (target.createNewFile()){
                OutputStream out = new FileOutputStream(target);


                byte[] buf = new byte[bufSize];
                int len;
                log.debug("Starting upload");
                while ((len = in2.read(buf)) > 0) {
                    try {
                        out.write(buf, 0, len);
                    } catch (IOException e) {
                        throw new CannotStoreFile("Cannot store the file in "
                                                  + "the temp store");
                    }
                }
                log.debug("upload done");
                MessageDigest md5 = in2.getMessageDigest();
                return toHex(md5.digest());
            }else {//file could not be created
                target.delete();
                throw new CannotStoreFile("The file in temp storage could not "
                                          + "be created");
            }


        }catch (MalformedURLException e) {
            throw new CannotGetFile("The input URI is invalid");
        } catch (IOException e) {
            throw new CannotGetFile("The file cannot be read");
        } catch (NoSuchAlgorithmException e) {
            throw new Error("MD5 not known by this system");
        }
    }

    @WebMethod public void approveFile(URI fileurl, String md5sum)
            throws RemoteException, UnknownURI, WrongChecksum, CannotStoreFile {
        byte[] temphash = new byte[0];
        try {
            temphash = Checksums.md5(new URL(fileurl.toString()).openStream());
        } catch (IOException e) {
            throw new CannotStoreFile("Could not read teh file from temporary storage");
        }
        if (!toHex(temphash).equalsIgnoreCase(md5sum)){
            throw new WrongChecksum("The provided checksum is wrong");
        }

    }


    @WebMethod public void disapproveFile(URI fileurl, String md5sum)
            throws RemoteException, UnknownURI, WrongChecksum {
        String fileName = storedir+fileurl.toString().substring(server.toString().length());
        File file = new File(fileName);
        if (file.exists()){
            byte[] temphash = new byte[0];
            try {
                temphash = Checksums.md5(new URL(fileurl.toString()).openStream());
            } catch (IOException e) {
                log.error("Could not checksum existing file in storage",e);
                return;
            }
            if (!toHex(temphash).equalsIgnoreCase(md5sum)){
                throw new WrongChecksum("The provided checksum is wrong");
            }
            file.delete();
        }
    }


    private static final byte MAGIC_INTEGER_4 = 4;
    private static final byte MAGIC_INTEGER_OxOF = 0x0f;
    /**
     * Converts a byte array to a string of hexadecimal characters.
     * @param ba the bytearray to be converted
     * @return ba converted to a hexstring
     */
    private static String toHex(final byte[] ba) {
        char[] hexdigit = {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a',
                'b', 'c',
                'd', 'e', 'f'
        };

        StringBuffer sb = new StringBuffer("");
        int ba_len = ba.length;

        for (int i = 0; i < ba_len; i++) {
            sb.append(hexdigit[(ba[i] >> MAGIC_INTEGER_4) & MAGIC_INTEGER_OxOF]);
            sb.append(hexdigit[ba[i] & MAGIC_INTEGER_OxOF]);
        }
        return sb.toString();
    }




    @WebMethod public long spaceLeft() throws RemoteException {
        return new File("khjkl").getUsableSpace();
    }
}
