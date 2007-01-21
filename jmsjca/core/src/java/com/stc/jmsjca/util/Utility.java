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
 * $RCSfile: Utility.java,v $
 * $Revision: 1.3 $
 * $Date: 2007-01-21 17:51:53 $
 *
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.  
 */

package com.stc.jmsjca.util;

import java.util.Properties;

/**
 *
 * Handy class full of static functions.
 * @author cye
 */
public final class Utility {

    /**
     * 
     * @param s String
     * @param defaultValue boolean
     * @return true if String value "true" 
     */
    public static boolean isTrue(String s, boolean defaultValue) {
        if (s == null || s.length() == 0) {
            return defaultValue;
        }
        return s.equalsIgnoreCase("true");
    }

    /**
     * 
     * @param name String
     * @param defaultValue boolean
     * @return propertty value
     */
    public static boolean getSystemProperty(String name, boolean defaultValue) {
        try {
            return isTrue(System.getProperty(name), defaultValue);
        } catch (Exception ignore) {
            // ignore
        }
        return defaultValue;
    }     

    /**
     * 
     * @param name String
     * @param defaultValue boolean
     * @return propertty value
     */
    public static String getSystemProperty(String name, String defaultValue) {
        try {
            return System.getProperty(name, defaultValue);
        } catch (Exception ignore) {
            // ignore
        }
        return defaultValue;
    }
    
    /**
     * Gets an int from a properties set
     * 
     * @param p properties
     * @param name key
     * @param defaultValue if not found
     * @return int value
     */
    public static int getIntProperty(Properties p, String name, int defaultValue) {
        int ret = defaultValue;
        String s = p.getProperty(name);
        if (s != null) {
            ret = Integer.parseInt(s);
        }
        return ret;
    }
}