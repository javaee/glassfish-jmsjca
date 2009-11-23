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


/**
 * Encapsulates the configuration of a MessageEndpoint.
 *
 * @version $Revision: 1.7 $
 * @author misc
 */
public class RAUnifiedActivationSpec extends GJRActivationSpecLayer {

    /**
     * isValidDestinationName
     * Can be overridden if there are special limitations on the destination
     * names in Wave.
     *
     * @param name String
     * @return boolean
     */
    @Override
    public boolean isValidDestinationName(String name) {
        if (!empty(name)) {
            return true;
        } else {
            if (hasGenericJMSRAProperties() || ((RAUnifiedResourceAdapter) getResourceAdapter()).hasGenericJMSRAProperties()) {
                return true;
            }
            return false;
        }
    }
}
