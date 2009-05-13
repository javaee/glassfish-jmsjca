/*
 * The contents of this file are subject to the terms of the Common Development and Distribution License
 * (the "License"). You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the license at https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * See the License for the specific language governing permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each file and include the License file at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html. If applicable add the following below this
 * CDDL HEADER, with the fields enclosed by brackets "[]" replaced with your own identifying
 * information: Portions Copyright [year] [name of copyright owner]
 */
/*
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.
 */

package com.stc.jmsjca.stcms453;

import com.stc.jmsjca.core.RAJMSObjectFactory;

/**
 * <p>From the spec:
 * This represents a resource adapter instance and contains operations for lifecycle
 * management and message endpoint setup. A concrete implementation of this interface
 * is required to be a JavaBean</p>
 *
 * @author Frank Kieviet
 * @version $Revision: 1.5 $
 */
public class RASTCMS453ResourceAdapter extends com.stc.jmsjca.core.RAJMSResourceAdapter {

    /**
     * Default constructor (required by spec)
     */
    public RASTCMS453ResourceAdapter() {
    }

    /**
     * @see com.stc.jmsjca.core.RAJMSResourceAdapter#createObjectFactory(java.lang.String)
     */
    @Override
    public RAJMSObjectFactory createObjectFactory(String urlstr) {
        return new RASTCMS453ObjectFactory();
    }
}
