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

package com.stc.jmsjca.util;

import java.util.Properties;

/**
 * A URL parser base class
 *
 * @author fkieviet
 * @version $Revision: 1.6 $
 */
public abstract class ConnectionUrl {
    /**
     * Extracts the key value pairs from the query string
     *
     * @param toAddTo Properties key-value pairs will be added to this properties set
     */
    public abstract void getQueryProperties(Properties toAddTo);

    /**
     * Returns the query string in the form of key-value pairs
     *
     * @return Properties
     */
    public Properties getQueryProperties() {
        Properties ret = new Properties();
        getQueryProperties(ret);
        return ret;
    }
    
    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public abstract String toString();
}
