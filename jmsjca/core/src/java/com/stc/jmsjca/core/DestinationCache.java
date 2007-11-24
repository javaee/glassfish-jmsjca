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

package com.stc.jmsjca.core;

import java.util.HashMap;
import java.util.Map;

/**
 * A cache for DestinationEntry objects. A destination object may be used for connectors
 * that rely on JNDI lookups for obtaining destinations, and want to provide a 
 * createQueue() and createTopic() method that will hide that. The WebLogic connector
 * is an example.
 *
 * @author Frank Kieviet
 * @version $Revision: 1.5 $
 */
public class DestinationCache  {
    private Map mEntries = new HashMap();
    
    /**
     * Looks up a destination cache entry by name; the returned cache entry is NEVER
     * null, although that entry may contain a null destination. The caller can
     * synchronize on the returned entry.
     * 
     * @param name destination name
     * @return never null
     */
    public synchronized DestinationCacheEntry get(String name) {
        DestinationCacheEntry ret = (DestinationCacheEntry) mEntries.get(name);
        if (ret == null) {
            ret = new DestinationCacheEntry();
            mEntries.put(name, ret);
        }
        return ret;
    }
}
