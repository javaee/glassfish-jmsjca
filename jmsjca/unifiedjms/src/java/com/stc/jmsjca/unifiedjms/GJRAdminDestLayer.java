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

/**
 * Adds GJR AdminDestination specific properties to the layered hierarchy
 * 
 * @author fkieviet
 */
public abstract class GJRAdminDestLayer extends GJRAdminDestCommonLayer {
    private boolean isUsed;
    /**
     * @see com.stc.jmsjca.unifiedjms.GJRCommonCfg#hasGenericJMSRAProperties()
     */
    @Override
    public boolean hasGenericJMSRAProperties() {
        return isUsed || super.hasGenericJMSRAProperties();
    }

    private String jndiName;
    private String destinationProperties;

    /**
     * @param jndiName
     */
    public void setDestinationJndiName(String jndiName) {
        isUsed = true;
        this.jndiName = jndiName;
    }

    /**
     * @return
     */
    public String getDestinationJndiName() {
        return this.jndiName;
    }

    /**
     * @return Returns the destinationProperties.
     */
    public String getDestinationProperties() {
        return destinationProperties;
    }

    /**
     * Setting destination property is mandatory, unless we find a
     * way out.
     * @param destinationProperties The destinationProperties to set.
     */
    public void setDestinationProperties(String destinationProperties) {
        isUsed = true;
        if (!Str.empty(destinationProperties)) {
            this.destinationProperties = destinationProperties;
        }
    }
}
