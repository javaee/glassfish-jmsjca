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

import com.stc.jmsjca.core.Options;
import com.stc.jmsjca.core.RAJMSActivationSpec;
import com.stc.jmsjca.util.Str;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Tools to deal with the GenericJMSRA configuration
 * 
 * @author fkieviet
 */
public final class GJRTool {
    static final Localizer L = Localizer.get();
    
    /**
     * Cannot be instantiated: just static tools
     */
    private GJRTool() {
    }

    /**
     * Returns the last non-null value from the specified array
     * 
     * @param <T>
     * @param ts
     * @return
     */
    public static <T> T getLastNotNull(T... ts) {
        T ret = ts[0];
        for (T t : ts) {
            if (t != null) {
                ret = t;
            }
        }
        
        return ret;
    }
    
    /**
     * Remove all null values from an array
     * 
     * @param <T>
     * @param ts
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] removeNull(T... ts) {
        List<T> ret = new ArrayList<T>();
        for (T t : ts) {
            if (t != null) {
                ret.add(t);
            }
        }
        return ret.toArray((T[]) Array.newInstance(ts.getClass().getComponentType(), ret.size()));
    }
    

    /**
     * default separator used by resource adapter
     */
    public static final String SEPARATOR = "=";

    /**
     * default delimiter used by resource adapter
     */
    public static final String DELIMITER = ",";

    /**
     * Converts a delimited string in GenericJMSRA format to a properties object. Code 
     * lifted from GenericJMSRA.
     * 
     * @param prop string to parse
     * @return Properties
     */
    public static Map<String, String> parseToProperties(String prop) {
        HashMap<String, String> ret = new HashMap<String, String>();

        if ((prop == null) || prop.trim().equals("")) {
            return ret;
        }
        
        GJRCustomTokenizer tokenList = new GJRCustomTokenizer(prop, DELIMITER);

        while (tokenList.hasMoreTokens()) {
            String propValuePair = tokenList.nextToken();

            int loc = propValuePair.indexOf(SEPARATOR);
            String propName = propValuePair.substring(0, loc);
            String propValue = propValuePair.substring(loc + SEPARATOR.length());
            ret.put(propName, propValue);
        }

        return ret;
    }

    
    /**
     * Adds configuration values to the specified properties set, translating values
     * from GenericJMSRA to JMSJCA.
     * 
     * @param p properties set to add to
     * @throws ResourceException 
     */
    public static void getGenericJMSRAProperties(GJRCommonCfg c, Properties p) {
        if (GJRCommonCfg.ISSAMERMOVERRIDE.equalsIgnoreCase(c.getRMPolicy())) {
            p.setProperty(Options.OVERRIDEISSAMERM, Boolean.toString(true));
        }
        
        // XA
        {
            // Default is no support for XA
            p.setProperty(Options.NOXA, p.getProperty(Options.NOXA, Boolean.toString(true)));
            if (c.getSupportsXA3() != null) {
                p.setProperty(Options.NOXA, Boolean.toString(!c.getSupportsXA()));
            }
        }
        
        // JNDI
        if (c.getJndiProperties() != null) {
            Map<String, String> jndi = GJRTool.parseToProperties(c.getJndiProperties());
            p.putAll(jndi);
        }
        
        // Delivery mode
        if (!Str.empty(c.getDeliveryType())) {
            if ("Synchronous".equals(c.getDeliveryType())) {
                p.setProperty(Options.In.OPTION_CONCURRENCYMODE, 
                    RAJMSActivationSpec.DELIVERYCONCURRENCY_STRS[RAJMSActivationSpec.DELIVERYCONCURRENCY_SYNC]);
            } else if ("Asynchronous".equals(c.getDeliveryType())) {
                if ("SERIAL".equals(c.getDeliveryConcurrencyMode())) {
                    p.setProperty(Options.In.OPTION_CONCURRENCYMODE, 
                        RAJMSActivationSpec.DELIVERYCONCURRENCY_STRS[RAJMSActivationSpec.DELIVERYCONCURRENCY_SYNC]);
                } else {
                    p.setProperty(Options.In.OPTION_CONCURRENCYMODE, 
                        RAJMSActivationSpec.DELIVERYCONCURRENCY_STRS[RAJMSActivationSpec.DELIVERYCONCURRENCY_CC]);
                }
            }
        }
    }

//    /**
//     * Creates a destination name based on the GJR admin destination configuration
//     * 
//     * @param adminDestination
//     * @return
//     */
//    public static String composeAdminName(GJRAdminDestLayer cfg) {
//        if (Str.empty(cfg.getDestinationJndiName())) {
//            if (!Str.empty(cfg.getDestinationProperties())) {
//                try {
//                    return "jmsjca://?" + Options.Dest.NAME + "=JavaBean&factprops=" 
//                    + URLEncoder.encode(cfg.getDestinationProperties(), "UTF-8");
//                } catch (UnsupportedEncodingException e) {
//                    throw Exc.rtexc(L.x("E224: Unexpected encoding exception in [{0}]: {1}", 
//                        cfg.getDestinationProperties(), e), e);
//                }
//            }
//        }
//        return null;
//    }
    
    
}
