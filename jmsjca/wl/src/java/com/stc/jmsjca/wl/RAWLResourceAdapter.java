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
 * $RCSfile: RAWLResourceAdapter.java,v $
 * $Revision: 1.3 $
 * $Date: 2007-01-21 17:52:24 $
 *
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.  
 */

package com.stc.jmsjca.wl;

import com.stc.jmsjca.core.RAJMSObjectFactory;

/**
 * Specializes the core resource adapter for Spirit Wave Messageserver
 *
 * @version $Revision: 1.3 $
 * @author misc
 */
public class RAWLResourceAdapter extends com.stc.jmsjca.core.RAJMSResourceAdapter {
    
    /**
     * Property name of connection factory jndi name
     */
    public static final String PROP_XACF = "xacf";
    
    /**
     * Property name of jndi name prefix 
     */
    public static final String PROP_PREFIX = "prefix";

    /**
     * Default constructor (required by spec)
     */
    public RAWLResourceAdapter() {
    }

    /**
     * @see com.stc.jmsjca.core.RAJMSResourceAdapter#createObjectFactory(java.lang.String)
     */
    public RAJMSObjectFactory createObjectFactory(String urlstr) {
        return new RAWLObjectFactory();
    }
}
