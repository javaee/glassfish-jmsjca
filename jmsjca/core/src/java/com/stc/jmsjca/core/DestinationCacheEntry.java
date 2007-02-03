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

import javax.jms.Destination;

/**
 * A single named destination in the cache; the destination itself may be null; the 
 * caller needs to explicitly check that and set the destination if it is null.
 * 
 * The caller may synchronize on the object; none of the methods are synchronized.
 *
 * @author Frank Kieviet
 * @version $Revision: 1.1.1.3 $
 */
public class DestinationCacheEntry  {
    private Destination mDestination;
    
    /**
     * Getter for the destination
     * 
     * @return the destination (may be null)
     */
    public Destination get() {
        return mDestination;
    }
    
    /**
     * Sets the destination
     * 
     * @param dest Destination
     */
    public void set(Destination dest) {
        mDestination = dest;
    }
}
