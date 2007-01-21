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
 * $RCSfile: RASTCMSResourceAdapter.java,v $
 * $Revision: 1.1.1.1 $
 * $Date: 2007-01-21 07:52:12 $
 *
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.  
 */

package com.stc.jmsjca.stcms;

import com.stc.jmsjca.core.RAJMSObjectFactory;
import com.stc.jmsjca.util.UrlParser;

/**
 * <p>From the spec:
 * This represents a resource adapter instance and contains operations for lifecycle
 * management and message endpoint setup. A concrete implementation of this interface
 * is required to be a JavaBean</p>
 *
 * @author Frank Kieviet
 * @version $Revision: 1.1.1.1 $
 */
public class RASTCMSResourceAdapter extends com.stc.jmsjca.core.RAJMSResourceAdapter {
    private RAJMSObjectFactory mFactory;
    
    /**
     * Constructor for derived test classes
     * @param fact factory
     */
    public RASTCMSResourceAdapter(RAJMSObjectFactory fact) {
        mFactory = fact;
    }

    /**
     * Default constructor (required by spec)
     */
    public RASTCMSResourceAdapter() {
        mFactory = new RASTCMSObjectFactory();
    }

    /**
     * setConnectionURL
     *
     * Checks the presence of a port number; if not specified, tries to get it from the
     * environment. This is necessary so that when no port is specified within the
     * application server (and the port is derived from the environment), and the CF
     * is subsequently serialized and deserialized in a different VM, the port number
     * will be available in the other VM as well.
     *
     * @param connectionURL String
     */
    public void setConnectionURL(String connectionURL) {
        UrlParser u = new UrlParser(connectionURL);
        try {
            new RASTCMSObjectFactory().validateAndAdjustURL(u);
            super.setConnectionURL(u.toString());
        } catch (Exception ignore) {
            super.setConnectionURL(connectionURL);
        }
    }

    /**
     * @see com.stc.jmsjca.core.RAJMSResourceAdapter#createObjectFactory(java.lang.String)
     */
    public RAJMSObjectFactory createObjectFactory(String urlstr) {
        return mFactory;
    }
}
