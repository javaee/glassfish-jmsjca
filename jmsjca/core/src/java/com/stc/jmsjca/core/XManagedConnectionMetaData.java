/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 *
 * You can obtain a copy of the license at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * HEADER in each file and include the License file at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * If applicable add the following below this CDDL HEADER,
 * with the fields enclosed by brackets "[]" replaced with
 * your own identifying information: Portions Copyright
 * [year] [name of copyright owner]
 */
/*
 * $RCSfile: XManagedConnectionMetaData.java,v $
 * $Revision: 1.3 $
 * $Date: 2007-01-21 17:51:50 $
 *
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.  
 */

package com.stc.jmsjca.core;


/**
 * Metadata
 * TBD
 *
 * @author Frank Kieviet
 * @version $Revision: 1.3 $
 */
public class XManagedConnectionMetaData implements javax.resource.spi.ManagedConnectionMetaData {
//    private static Logger sLog = Logger.getLogger(XManagedConnectionMetaData.class);
    private XManagedConnection mManagedConnection;

    /**
     * XManagedConnectionMetaData
     *
     * @param mc XManagedConnection
     */
    public XManagedConnectionMetaData(XManagedConnection mc) {
        mManagedConnection = mc;
    }

    /**
     * getEISProductName
     *
     * @throws javax.resource.ResourceException failure
     * @return String
     */
    public String getEISProductName() throws javax.resource.ResourceException {
        return "x";
    }

    /**
     * Return the Product Version
     *
     * @return The EIS Product Version
     * @throws javax.resource.ResourceException failure
     */
    public String getEISProductVersion() throws javax.resource.ResourceException {
        return "X";
    }

    /**
     * getMaxConnections
     *
     * @throws javax.resource.ResourceException failure
     * @return int
     */
    public int getMaxConnections() throws javax.resource.ResourceException {
        return 1;
    }

    /**
     * getUserName
     *
     * @throws javax.resource.ResourceException failure
     * @return String
     */
    public String getUserName() throws javax.resource.ResourceException {
        return mManagedConnection.getUserid();
    }
}
