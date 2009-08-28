package dk.statsbiblioteket.doms.bitstorage.lowlevel;

import dk.statsbiblioteket.doms.bitstorage.lowlevel.exceptions.ChecksumFailedException;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.exceptions.CommunicationException;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.exceptions.FileAlreadyApprovedException;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.exceptions.FileNotFoundException;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.exceptions.NotEnoughFreeSpaceException;
import dk.statsbiblioteket.util.console.ProcessRunner;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * TODO abr forgot to document this class
 */
public class BitstorageSshImpl implements Bitstorage{


    private String SSH = "ssh";
    private String SERVER = "domstest@halley";
    private String SCRIPT = "bin/server.sh";
    
    private String UPLOAD_COMMAND = "save-md5";
    private String APPROVE_COMMAND = "approve";
    private String DISAPPROVE_COMMAND = "delete";
    private String SPACELEFT_COMMAND = "space-left";
    private String GETMD5_COMMAND = "get-md5";

    public String BITFINDER = "http://bitfinder.statsbiblioteket.dk/";


    private static final String ALREADY_STORED_REPLY = "was stored!";
    private static final String FILE_NOT_FOUND_REPLY = "not found";
    private static final String NO_SPACE_LEFT_REPLY = "No space left for file";
    private static final String FREE_SPACE_REPLY = "Free space: ";
    private static final String MAX_FILE_SIZE_REPLY = "Max file size: ";


    public URL upload(String filename, InputStream data, String md5) throws
                                                                     MalformedURLException,
                                                                     CommunicationException,
                                                                     ChecksumFailedException,
                                                                     FileAlreadyApprovedException {

        String output = runcommand(data, UPLOAD_COMMAND, filename);

        if (output.contains(ALREADY_STORED_REPLY)){
            throw new FileAlreadyApprovedException("File '"+filename+"' have already been approved");
        }else{
            //output should be the checksum
            if (output.equalsIgnoreCase(md5)){
                return createURL(filename);
            } else {
                throw new ChecksumFailedException("Given checksum '"+md5+"' but server calculated '"+output+"'");
            }
        }

    }


    public void disapprove(URL file, String md5) throws CommunicationException,
                                                        FileNotFoundException {

        String datafile = getFileNameFromURL(file);
        String output = runcommand(DISAPPROVE_COMMAND, datafile);
        if (output.contains(FILE_NOT_FOUND_REPLY)){
            throw new FileNotFoundException();
        } else{
            //ok
        }
    }

    public String approve(URL file, String md5) throws FileNotFoundException,
                                                       NotEnoughFreeSpaceException,
                                                       CommunicationException {

        String datafile = getFileNameFromURL(file);
        String output = runcommand(APPROVE_COMMAND, datafile);
        if (output.contains(FILE_NOT_FOUND_REPLY)){
            throw new FileNotFoundException("File '"+file.toString()+"' not found");
        } else if (output.contains(NO_SPACE_LEFT_REPLY)){
            throw new NotEnoughFreeSpaceException("Not enough free space for file '"+file+"'");
        } else {
            return output;
        }
    }

    private String getFileNameFromURL(URL file) {
        return file.toString().substring(BITFINDER.length());
    }

    private URL createURL(String filename) throws MalformedURLException {
        return new URL(BITFINDER +filename);
    }


    public long spaceleft() throws CommunicationException {
        String output = runcommand(SPACELEFT_COMMAND);
        int index = output.indexOf(FREE_SPACE_REPLY);
        String longstring = output.substring(index + FREE_SPACE_REPLY.length()).trim();

        //TODO defensive code?
        return Long.parseLong(longstring);

    }


    public String getMd5(URL file) throws CommunicationException,
                                          FileNotFoundException {
        String output = runcommand(GETMD5_COMMAND);
        if (output.trim().isEmpty()){
            throw new FileNotFoundException("File not found");
        } else {
            return output;
        }

    }

    public boolean isApproved(URL file)
            throws FileNotFoundException, CommunicationException {
        //TODO TALK TO jens henrik about getting this method.
        return false;
    }

    public long getMaxFileSize() throws CommunicationException {
        String output = runcommand(SPACELEFT_COMMAND);
        int index1 = output.indexOf(MAX_FILE_SIZE_REPLY);
        int index2 = output.indexOf(FREE_SPACE_REPLY);
        String longstring = output.substring(index1 + MAX_FILE_SIZE_REPLY.length(),index2).trim();
        return Long.parseLong(longstring);


    }

    private String runcommand(String... command)
            throws CommunicationException {
        return runcommand(null,command);
    }

    private String runcommand(InputStream input, String... command)
            throws CommunicationException{
        List<String> arrayList = new ArrayList<String>(10);

        arrayList.add(SSH);
        arrayList.add(SERVER);
        if (!SCRIPT.trim().isEmpty()){
            arrayList.add(SCRIPT);
        }
        arrayList.addAll(Arrays.asList(command));

        ProcessRunner nr = new ProcessRunner(arrayList);

        nr.run();
        if (nr.isTimedOut()){
            throw new CommunicationException("Communication with Bitstorage timed out");
        }

        if (nr.getReturnCode() != 0){
            throw new CommunicationException("Return code "+nr.getReturnCode()+"\n"
                                             +"output '"+nr.getProcessOutputAsString()+"'\n"+
                                             "erroroutput '"+nr.getProcessErrorAsString()+"'\n");
        }

        return nr.getProcessOutputAsString();
    }
}
