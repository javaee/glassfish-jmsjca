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

package com.stc.jmsjca.localization;


/**
 * A wrapper around a string so that methods that should receive localized strings can
 * force the caller to make a localized string.
 *
 * @author fkieviet
 * @version $Revision: 1.3 $
 */
public class LocalizedString {
    private String mString;
    
    /**
     * Constructor
     * 
     * @param s string to wrap
     */
    public LocalizedString(String s) {
        mString = s;
    }
    
    /**
     * Returns the wrapped string
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return mString;
    }
    
    /**
     * @param s string to wrap
     * @return wrapped string
     */
    public static LocalizedString valueOf(String s) {
        return s == null ? null : new LocalizedString(s);
    }
}
