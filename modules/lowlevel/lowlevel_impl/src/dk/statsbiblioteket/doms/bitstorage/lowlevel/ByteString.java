/*
 * $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The DOMS project.
 * Copyright (C) 2007-2010  The State and University Library
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package dk.statsbiblioteket.doms.bitstorage.lowlevel;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Utillity class for working with bytes, bytearrays, and strings.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_OK,
        author = "kfc",
        reviewers = {"abr"})
public class ByteString {
    /**
     * The number of bits in a nibble (used for shifting).
     */
    private static final byte BITS_IN_NIBBLE = 4;
    /**
     * A bitmask for a nibble (used for "and'ing" out the bits.
     */
    private static final byte BITMASK_FOR_NIBBLE = 0x0f;

    /**
     * Utility class, don't initialise.
     */
    private ByteString() {
    }

    /**
     * Converts a byte array to a hexstring.
     *
     * @param ba the bytearray to be converted
     * @return ba converted to a hexstring
     */
    public static String toHex(final byte[] ba) {
        char[] hexdigit = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

        int baLen = ba.length;
        StringBuffer sb = new StringBuffer(baLen * 2);

        for (int i = 0; i < baLen; i++) {
            sb.append(hexdigit[(ba[i] >> BITS_IN_NIBBLE) & BITMASK_FOR_NIBBLE]);
            sb.append(hexdigit[ba[i] & BITMASK_FOR_NIBBLE]);
        }

        return sb.toString();
    }


}
