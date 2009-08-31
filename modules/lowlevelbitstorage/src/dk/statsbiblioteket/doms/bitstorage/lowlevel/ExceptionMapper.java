package dk.statsbiblioteket.doms.bitstorage.lowlevel;

import dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.exceptions.BitstorageException;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.exceptions.ChecksumFailedException;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.exceptions.FileAlreadyApprovedException;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.exceptions.FileNotFoundException;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.exceptions.InvalidFilenameException;
import dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.exceptions.NotEnoughFreeSpaceException;

import javax.xml.ws.WebServiceException;
import java.net.MalformedURLException;

public class ExceptionMapper {


    public static WebServiceException convert(BitstorageException e) {
        return new WebServiceException(e);
    }

    public static CommunicationException convert(dk.statsbiblioteket.doms.bitstorage.lowlevel.backend.exceptions.CommunicationException e) {
        return new CommunicationException("", e.getMessage(), e);
    }

    public static dk.statsbiblioteket.doms.bitstorage.lowlevel.ChecksumFailedException convert(
            ChecksumFailedException e) {
        return new dk.statsbiblioteket.doms.bitstorage.lowlevel.ChecksumFailedException(
                "",
                e.getMessage(),
                e);
    }

    public static dk.statsbiblioteket.doms.bitstorage.lowlevel.FileAlreadyApprovedException convert(
            FileAlreadyApprovedException e) {
        return new dk.statsbiblioteket.doms.bitstorage.lowlevel.FileAlreadyApprovedException(
                "",
                e.getMessage(),
                e);
    }

    public static dk.statsbiblioteket.doms.bitstorage.lowlevel.FileNotFoundException convert(
            FileNotFoundException e) {
        return new dk.statsbiblioteket.doms.bitstorage.lowlevel.FileNotFoundException(
                "",
                e.getMessage(),
                e);
    }

    public static dk.statsbiblioteket.doms.bitstorage.lowlevel.InvalidFilenameException convert(
            InvalidFilenameException e) {
        return new dk.statsbiblioteket.doms.bitstorage.lowlevel.InvalidFilenameException(
                "",
                e.getMessage(),
                e);
    }

    public static dk.statsbiblioteket.doms.bitstorage.lowlevel.NotEnoughFreeSpaceException convert(
            NotEnoughFreeSpaceException e) {
        return new dk.statsbiblioteket.doms.bitstorage.lowlevel.NotEnoughFreeSpaceException(
                "",
                e.getMessage(),
                e);
    }

    public static dk.statsbiblioteket.doms.bitstorage.lowlevel.FileNotFoundException convertToFileNotFound(
            MalformedURLException e) {
        return new dk.statsbiblioteket.doms.bitstorage.lowlevel.FileNotFoundException(
                "",
                e.getMessage(),
                e);

    }
}
