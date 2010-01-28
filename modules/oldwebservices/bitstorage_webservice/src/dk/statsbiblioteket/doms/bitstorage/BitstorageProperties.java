/* $Id$

The State and University DOMS project.
Copyright (C) 2006  The State and University Library

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
*/
package dk.statsbiblioteket.doms.bitstorage;

import dk.statsbiblioteket.doms.PropAccess;
import dk.statsbiblioteket.util.XProperties;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Common functionality for the ingest-module. The primary use is to give
 * Singleton access to properties.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "tsh")
public class BitstorageProperties extends PropAccess {

    private static Log log = LogFactory.getLog(BitstorageProperties.class);

    private static final String PROPERTIES_FILE_NAME = "bitstorageProperties.xml";

    public static final String DELETE_COMMAND="delete_command";
    public static final String APPROVE_COMMAND="approve_command";
    public static final String UPLOAD_COMMAND="upload_command";
    public static final String SSH_COMMAND="SSH";
    public static final String SERVER="server";
    public static final String SCRIPT="script";
    public static final String EXCLUDE_LIST="excludeList";

    static {   //Default values
        XProperties bitstorageProps = new XProperties();

        bitstorageProps.putDefault(DELETE_COMMAND,"delete");
        bitstorageProps.putDefault(APPROVE_COMMAND,"approve");
        bitstorageProps.putDefault(UPLOAD_COMMAND,"save-md5");
        bitstorageProps.putDefault(SSH_COMMAND,"ssh");
        bitstorageProps.putDefault(SERVER,"doms@halley");
        bitstorageProps.putDefault(SCRIPT,"");

        PropAccess.xprop.putDefault("BitstorageProperties",bitstorageProps);

        PropAccess.load(PROPERTIES_FILE_NAME);
    }

    /**
     * Return the xproperties object, read from the configuration files.
     * @return The loaded properties.
     */
    public XProperties getXProperties() {
        return PropAccess.xprop;
    }
}