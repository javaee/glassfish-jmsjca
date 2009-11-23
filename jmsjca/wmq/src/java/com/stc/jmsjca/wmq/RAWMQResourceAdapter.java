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

package com.stc.jmsjca.wmq;

import com.stc.jmsjca.core.RAJMSActivationSpec;
import com.stc.jmsjca.core.RAJMSObjectFactory;
import com.stc.jmsjca.core.RAJMSResourceAdapter;
import com.stc.jmsjca.core.XManagedConnectionFactory;
import com.stc.jmsjca.util.Exc;
import com.stc.jmsjca.util.UrlParser;

/**
 * Specializes the core resource adapter for IBM WebSphere Message Server
 *
 * @author cye
 * @version $Revision: 1.7 $
 */
public class RAWMQResourceAdapter extends com.stc.jmsjca.core.RAJMSResourceAdapter {
    private static final Localizer LOCALE = Localizer.get();    

    /**
     * Default constructor (required by spec)
     */
    public RAWMQResourceAdapter() {
    }
    
    /**
     * @param connectionURL String
     */
    @Override
    public void setConnectionURL(String connectionURL) {
        UrlParser u = new UrlParser(connectionURL);
        try {
            new RAWMQObjectFactory().validateAndAdjustURL(u);
        } catch (Exception ex) {
            throw Exc.rtexc(LOCALE.x("E310: Invalid connectionURL [{0}]: {1}", connectionURL, ex), ex);
        }
        super.setConnectionURL(u.toString());        
    }
    //TODO: add equals and hashcode

    /**
     * @see com.stc.jmsjca.core.RAJMSResourceAdapter#createObjectFactory(java.lang.String)
     */
    @Override
    public RAJMSObjectFactory createObjectFactory(RAJMSResourceAdapter ra, 
        RAJMSActivationSpec spec, XManagedConnectionFactory fact) {
        return new RAWMQObjectFactory();
    }
}
