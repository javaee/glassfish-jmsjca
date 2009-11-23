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

package com.stc.jmsjca.unifiedjms;

import com.stc.jmsjca.core.RAJMSActivationSpec;
import com.stc.jmsjca.core.RAJMSObjectFactory;
import com.stc.jmsjca.core.RAJMSResourceAdapter;
import com.stc.jmsjca.core.XManagedConnectionFactory;
import com.stc.jmsjca.util.Exc;
import com.stc.jmsjca.util.Logger;
import com.stc.jmsjca.util.Str;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * Specializes the core resource adapter for Spirit Wave Messageserver
 *
 * @version $Revision: 1.8 $
 * @author misc
 */
public class RAUnifiedResourceAdapter extends GJRResourceAdapterLayer {
    private static Logger sLog = Logger.getLogger(RAUnifiedResourceAdapter.class);
    private Map<String, RAJMSObjectFactory> mObjFactories 
    = Collections.synchronizedMap(new HashMap<String, RAJMSObjectFactory>());
    private static final Localizer LOCALE = Localizer.get();
    
    /**
     * @see com.stc.jmsjca.core.RAJMSResourceAdapter#createObjectFactory(java.lang.String)
     */
    @Override
    public RAJMSObjectFactory createObjectFactory(RAJMSResourceAdapter ra, 
        RAJMSActivationSpec spec, XManagedConnectionFactory fact) {
        
        // Locate factory using the ConnectionURL
        String url = ra.getConnectionURL();
        if (spec != null && !Str.empty(spec.getConnectionURL())) {
            url = spec.getConnectionURL();
        }
        if (fact != null && !Str.empty(fact.getConnectionURL())) {
            url = fact.getConnectionURL();
        }
        
        if (!Str.empty(url)) {
            RAJMSObjectFactory alreadyset = mObjFactories.get(url);
            if (alreadyset != null) {
                return alreadyset;
            }
            
            if (url == null || url.length() == 0) {
                throw Exc.rtexc(LOCALE.x("E701: URL is not set"));
            }
            
            String[] classnames = new String[] {
                "com.stc.jmsjca.stcms.RASTCMSObjectFactory",
                "com.stc.jmsjca.jndi.RAJNDIObjectFactory",
                "com.stc.jmsjca.sunone.RASunOneObjectFactory",
                "com.stc.jmsjca.wave.RAWaveObjectFactory",
                "com.stc.jmsjca.jboss.RAJBossObjectFactory",
                "com.stc.jmsjca.stcms453.RASTCMS453ObjectFactory",
                "com.stc.jmsjca.wl.RAWLObjectFactory",
                "com.stc.jmsjca.wmq.RAWMQObjectFactory",
            };
            
            for (int i = 0; i < classnames.length; i++) {
                try {
                    Class<?> c = Class.forName(classnames[i], true, this.getClass().getClassLoader());
                    RAJMSObjectFactory o = (RAJMSObjectFactory) c.newInstance();
                    if (o.isUrl(url)) {
                        mObjFactories.put(url, o);
                        return o;
                    }
                } catch (Exception e) {
                    sLog.warn(LOCALE.x("E700: Error while matching URL [{0}] with " 
                        + "classname [{1}]: {2}", url, classnames[i], e), e);
                }
            }

            throw Exc.rtexc(LOCALE.x("E702: The url [{0}] cannot be matched with a JMS provider", url));
        }
        
        // No ConnectionURL... try if this is a GenericJMSRA migration
        if (ra instanceof RAUnifiedResourceAdapter 
            && (spec == null || spec instanceof RAUnifiedActivationSpec) 
            && (fact == null || fact instanceof UnifiedMCFBase)) {
            
            RAUnifiedResourceAdapter p1 = (RAUnifiedResourceAdapter) ra;
            RAUnifiedActivationSpec p2 = (RAUnifiedActivationSpec) spec;
            UnifiedMCFBase p3 = (UnifiedMCFBase) fact;
            
            if ((p1 != null && p1.hasGenericJMSRAProperties()) 
                || (p2 != null && p2.hasGenericJMSRAProperties()) 
                || (p3 != null && p3.hasGenericJMSRAProperties())) {
                
                String mode = GJRTool.getLastNotNull(p1 != null ? p1.getProviderIntegrationMode() : null
                    , p2 != null ? p2.getProviderIntegrationMode() : null
                    , p3 != null ? p3.getProviderIntegrationMode() : null
                    , "javabean");
                if (mode.equalsIgnoreCase("jndi")) {
                    return new GJRObjectFactoryJNDI();
                } else {
                    return new GJRObjectFactoryJavaBean();
                }
            }
        }
        // No provider found
        throw Exc.rtexc(LOCALE.x("E704: The type of JMS provider could not be derived from the configuration: " 
            + "no ConnectionURL is specified, and no GenericJMSRA compatibility properties were specified.", url));
    }

    /**
     * Default constructor (required by spec)
     */
    public RAUnifiedResourceAdapter() {
    }
}
