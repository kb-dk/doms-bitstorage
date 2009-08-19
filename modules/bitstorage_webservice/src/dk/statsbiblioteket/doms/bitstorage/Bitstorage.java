package dk.statsbiblioteket.doms.bitstorage;

import dk.statsbiblioteket.doms.bitstorage.exceptions.*;
import org.apache.axis.types.URI;

import javax.jws.WebMethod;
import javax.jws.WebService;
import java.io.Serializable;
import java.rmi.RemoteException;

/**
 * A webservice for accessing DOMS bitstorage. Working with the Bitstorage goes
 * like this:
 *
 */
@WebService public interface Bitstorage extends Serializable, java.rmi.Remote {

    /**
     * Upload the provided file to the temporary area of bitstorage,
     * giving it the provided file name.
     * Return the MD5 checksum of the file.
     * The file is only uploaded to a temporary approve-area of
     * the bitstorage, and needs to be approved by calling approveFile before
     * it is actually moved to the permanent bitstorage.
     *
     * If you try to upload a file that is already there, it checks the provided
     * md5 against the file of the file on the server. If they match, there is no
     * upload, you just get the return about the file already there.
     * If they do not match, an exception is thrown.
     *
     * @param fileName The filename to store the file by
     * @param localurl The url to where the bitstorage webservice can get the file
     * @param md5 The locally generated md5sum of the file
     * @return A bitstorage object, detailing the characteritics and public url
     * of the uploaded file.
     * @throws java.rmi.RemoteException If anything went wrong
     * @throws dk.statsbiblioteket.doms.bitstorage.exceptions.CannotGetFile If the service cannot get the file from the localurl
     * @throws dk.statsbiblioteket.doms.bitstorage.exceptions.CannotStoreFile If the service cannot store the file on the server
     * @throws dk.statsbiblioteket.doms.bitstorage.exceptions.CharacterizationFailed If the characterization service failed somehow
     * @throws dk.statsbiblioteket.doms.bitstorage.exceptions.InvalidFileName If the provided fileName is invalid
     * @throws dk.statsbiblioteket.doms.bitstorage.exceptions.WrongChecksum If the provided checksum does not match
     * @throws dk.statsbiblioteket.doms.bitstorage.exceptions.DifferentFileWithThatNameExist If there is already a file with that name, but a different checksum
     */
    @WebMethod public BitstorageFile uploadFile(String fileName, URI localurl, String md5)
            throws
            RemoteException, CannotGetFile, InvalidFileName, CannotStoreFile,
            WrongChecksum, CharacterizationFailed,
            DifferentFileWithThatNameExist;


    /**
     * Check the earlier uploaded file against the provided checksum, and if
     * this succeeds, and possibly other criteria are met, move the file
     * from the temporary area of bitstorage to the permanent bitstorage.
     *
     * If you call this method on an already approved file, with the correct checksum, nothing happens.
     * If the checksum is wrong, you get an exception.
     *
     * @param fileurl The url to the file (in bitstorage)
     * @param md5sum The md5sum of the file
     * @throws dk.statsbiblioteket.doms.bitstorage.exceptions.CannotStoreFile If the file cannot be stored in the permanent storage area or if the file is not available in the temporary area
     * @throws dk.statsbiblioteket.doms.bitstorage.exceptions.UnknownURI If the URI is not to this bitstorage.
     * @throws dk.statsbiblioteket.doms.bitstorage.exceptions.WrongChecksum If the provided checksum match
     * @throws java.rmi.RemoteException If anything went wrong with the connection
     */
    @WebMethod public abstract void approveFile(URI fileurl, String md5sum)
            throws RemoteException, UnknownURI, WrongChecksum, CannotStoreFile ;


    /**
     * Delete the named file from bitstorage. Only works for files that have
     * not yet been approved.
     *
     * If the file is not in temporary bitstorage nothing happens.
     * @param fileurl The url to the file (in bitstorage)
     * @param md5sum The md5sum of the file
     * @throws dk.statsbiblioteket.doms.bitstorage.exceptions.UnknownURI If the URI is not to this bitstorage.
     * @throws dk.statsbiblioteket.doms.bitstorage.exceptions.WrongChecksum If the checksum does not match the file in temporary
     * @throws java.rmi.RemoteException If anything went wrong with the connection
     */
    @WebMethod public abstract void disapproveFile(URI fileurl, String md5sum)
            throws RemoteException, UnknownURI, WrongChecksum;


    /**
     * Return the number of bytes left in bitstorage.
     * @throws java.rmi.RemoteException if something unforseen broke while performing the request
     * @return The number of bytes left in bitstorage.
     */
    @WebMethod public abstract long spaceLeft() throws RemoteException;

}
