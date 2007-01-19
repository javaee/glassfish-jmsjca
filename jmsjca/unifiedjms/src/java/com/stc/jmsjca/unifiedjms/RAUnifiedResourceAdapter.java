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
 * $RCSfile: RAUnifiedResourceAdapter.java,v $
 * $Revision: 1.1.1.1 $
 * $Date: 2007-01-19 22:28:54 $
 *
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.  
 */

package com.stc.jmsjca.unifiedjms;

import com.stc.jmsjca.util.Logger;
import com.stc.jmsjca.core.RAJMSObjectFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * Specializes the core resource adapter for Spirit Wave Messageserver
 *
 * @version $Revision: 1.1.1.1 $
 * @author misc
 */
public class RAUnifiedResourceAdapter extends com.stc.jmsjca.core.RAJMSResourceAdapter {
    private static Logger sLog = Logger.getLogger(RAUnifiedResourceAdapter.class);
    private Map mObjFactories = Collections.synchronizedMap(new HashMap()); // key: urlstr; value=objfactory
    
    /**
     * @see com.stc.jmsjca.core.RAJMSResourceAdapter#createObjectFactory(java.lang.String)
     */
    public RAJMSObjectFactory createObjectFactory(String url) {
        RAJMSObjectFactory alreadyset = (RAJMSObjectFactory) mObjFactories.get(url);
        if (alreadyset != null) {
            return alreadyset;
        }
        
        if (url == null || url.length() == 0) {
            throw new RuntimeException("URL is not set");
        }
        
        String[] classnames = new String[] {
            "com.stc.jmsjca.stcms.RASTCMSObjectFactory",
            "com.stc.jmsjca.jndi.RAJNDIObjectFactory",
            "com.stc.jmsjca.sunone.RASunOneObjectFactory",
            "com.stc.jmsjca.jboss.RAJBossObjectFactory",
            "com.stc.jmsjca.stcms453.RASTCMS453ObjectFactory",
            "com.stc.jmsjca.wl.RAWLObjectFactory",
            "com.stc.jmsjca.wmq.RAWMQObjectFactory",
        };
        
        for (int i = 0; i < classnames.length; i++) {
            try {
                Class c = Class.forName(classnames[i], true, this.getClass().getClassLoader());
                RAJMSObjectFactory o = (RAJMSObjectFactory) c.newInstance();
                if (o.isUrl(url)) {
                    mObjFactories.put(url, o);
                    return o;
                }
            } catch (Exception e) {
                sLog.warn("Error while matching URL [" + url + "] with classname [" + classnames[i] + "]: " + e, e);
            }
        }
        throw new RuntimeException("The url [" + url + "] cannot be matched with a JMS provider");
    }

    /**
     * Default constructor (required by spec)
     */
    public RAUnifiedResourceAdapter() {
    }
}
