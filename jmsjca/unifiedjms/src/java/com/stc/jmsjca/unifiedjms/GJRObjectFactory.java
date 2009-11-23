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
import com.stc.jmsjca.util.ConnectionUrl;

import javax.jms.JMSException;

import java.io.Serializable;
import java.util.Properties;

/**
 * Common baseclass for GJR object factories
 * 
 * @author fkieviet
 */
public abstract class GJRObjectFactory extends RAJMSObjectFactory implements Serializable {
    /**
     * @see com.stc.jmsjca.core.RAJMSObjectFactory#getProperties(java.util.Properties, 
     * com.stc.jmsjca.core.RAJMSResourceAdapter, 
     * com.stc.jmsjca.core.RAJMSActivationSpec, 
     * com.stc.jmsjca.core.XManagedConnectionFactory, java.lang.String)
     */
    @Override
    public ConnectionUrl getProperties(Properties p, RAJMSResourceAdapter ra,
        RAJMSActivationSpec spec, XManagedConnectionFactory fact, String overrideUrl)
        throws JMSException {

        // GenericJMSRA Compatibility
        if (fact != null 
            && fact instanceof UnifiedMCFBase 
            && ((UnifiedMCFBase) fact).hasGenericJMSRAProperties()) {
            GJRTool.getGenericJMSRAProperties((RAUnifiedResourceAdapter) ra, p);
            GJRTool.getGenericJMSRAProperties((UnifiedMCFBase) fact, p);
        }
        if (spec != null 
            && spec instanceof RAUnifiedActivationSpec 
            && ((RAUnifiedActivationSpec) spec).hasGenericJMSRAProperties()) {
            GJRTool.getGenericJMSRAProperties((RAUnifiedResourceAdapter) ra, p);
            GJRTool.getGenericJMSRAProperties((RAUnifiedActivationSpec) spec, p);
        }

        return super.getProperties(p, ra, spec, fact, overrideUrl);
    }

    /**
     * @see com.stc.jmsjca.core.RAJMSObjectFactory#getJMSServerType()
     */
    @Override
    public String getJMSServerType() {
        return "GENERIC";
    }

    @Override
    public RAJMSActivationSpec createActivationSpec() {
        return new RAUnifiedActivationSpec();
    }

    @Override
    public boolean isUrl(String url) {
        return false;
    }
}
