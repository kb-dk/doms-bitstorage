package dk.statsbiblioteket.doms.bitstorage.lowlevel;

import dk.statsbiblioteket.util.console.ProcessRunner;

import java.net.URL;
import java.io.InputStream;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

/**
 * TODO abr forgot to document this class
 */
public class BitstorageSshImpl implements Bitstorage{
    private String ssh;
    private String server;
    private String script;
    private String upload_command;
    private String approve_command;
    private String disapprove_command;


    public URL upload(String filename, InputStream data, String md5) {


        //prepare the upload command
        List<String> command = new ArrayList<String>(10);

        command.add(ssh);
        command.add(server);
        if (!script.trim().isEmpty()){
            command.add(script);
        }
        command.add(upload_command);
        command.add(filename);


        //make the process, and feed it the file in a stream
        ProcessRunner nr = new ProcessRunner(command);
        nr.setInputStream(data);

        //Run the upload command
        nr.run();

        try {
            data.close();//no use any longer, free it
        } catch (IOException e1) {
            e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        //get remote checksum
        String remote_checksum = nr.getProcessOutputAsString().trim().toUpperCase();

        //System.out.println(remote_checksum);
        if (remote_checksum.contains("WAS STORED")){
            //file has already been committed. Throw

        }else{
            if (remote_checksum.equalsIgnoreCase(md5)){
                //success
            } else {
                //transmission error
            }
        }

        return createURL(filename);

    }


    public void disapprove(URL file, String md5) {

        List<String> command = new ArrayList<String>(10);

        command.add(ssh);
        command.add(server);
        if (!script.trim().isEmpty()){
            command.add(script);
        }
        String datafile = getFileNameFromURL(file);
        command.add(disapprove_command);
        command.add(datafile);

        ProcessRunner nr = new ProcessRunner(command);

        nr.run();
        
    }

    public void approve(URL file, String md5) {

        List<String> command = new ArrayList<String>(10);

        command.add(ssh);
        command.add(server);
        if (!script.trim().isEmpty()){
            command.add(script);
        }
        String datafile = getFileNameFromURL(file);
        command.add(approve_command);
        command.add(datafile);



        ProcessRunner nr = new ProcessRunner(command);

        nr.run();

    }

    private String getFileNameFromURL(URL file) {
        return null;  //To change body of created methods use File | Settings | File Templates.
    }

    private URL createURL(String filename) {
        return null;  //To change body of created methods use File | Settings | File Templates.
    }


    public long spaceleft() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
