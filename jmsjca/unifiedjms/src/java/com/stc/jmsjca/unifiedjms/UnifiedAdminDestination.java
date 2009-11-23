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

import com.stc.jmsjca.util.Str;

import javax.jms.JMSException;

import java.util.Properties;

/**
 * Common baseclass for Unified Admin destinations; this caps off the compatibility layers
 * 
 * @author fkieviet
 */
public abstract class UnifiedAdminDestination extends GJRAdminDestLayer {
    /**
     * @return non-null name
     * @throws JMSException on configuration failure
     */
    @Override
    public String retrieveCheckedName() throws JMSException {
        String ret = getName();
        
        // Put in marker for JavaBean 
        if (Str.empty(ret) && !Str.empty(getDestinationProperties())) {
            ret = GJRObjectFactoryJavaBean.BEANMODE_DEST;
            
        }
        
        if (Str.empty(ret)) {
            ret = super.retrieveCheckedName();
        }

        return ret;
    }
    
    /**
     * @return the Options field in the form of a properties set
     * @throws JMSException 
     */
    @Override
    public Properties retrieveProperties() throws JMSException {
        if (GJRObjectFactoryJavaBean.BEANMODE_DEST.equals(retrieveCheckedName())) {
            Properties p = new Properties();
            p.setProperty(GJRObjectFactoryJavaBean.DESTINATION_PROPERTIES, getDestinationProperties());
            return p;
        } else {
            return super.retrieveProperties();
        }
    }
}
