/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The DOMS project.
 * Copyright (C) 2007  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.statsbiblioteket.doms.objectmanipulation;

import org.apache.axis.types.URI;

/**
 * Bean for transport of file name and URI of where the file data can be found.
 */
public class FileInformation {

    private String fileName;
    private URI uri;
    private String md5Sum;

    /**
     * Get the actual name of the file without any path information.
     * @return The name of the file.
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Set the actual name of the file without any path information.
     * @fileName The name of the file.
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Get a URI indicating where the file data can be found. This may have no
     * resemblance with the file name at all.
     * @return URI to where the file data can be found.
     */
    public URI getUri() {
        return uri;
    }

    /**
    * Set a URI indicating where the file data can be found. This may have no
    * resemblance with the file name at all.
    * @return URI to where the file data can be found.
    */
    public void setUri(URI uri) {
        this.uri = uri;
    }

    /**
     * Get the MD5 checksum of the file referenced by this
     * <code>FileInformation</code> object.
     * @return <code>String</code> containing the MD5 checksum.
     */
    public String getMd5Sum() {
        return md5Sum;
    }

    /**
     * Set the MD5 checksum of the file referenced by this
     * <code>FileInformation</code> object.
     * @param md5Sum <code>String</code> containing the MD5 checksum.
     */
    public void setMd5Sum(String md5Sum) {
        this.md5Sum = md5Sum;
    }
}
