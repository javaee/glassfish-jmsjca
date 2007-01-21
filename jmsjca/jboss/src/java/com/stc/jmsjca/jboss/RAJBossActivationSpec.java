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
 * $RCSfile: RAJBossActivationSpec.java,v $
 * $Revision: 1.1.1.1 $
 * $Date: 2007-01-21 07:52:11 $
 *
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.  
 */

package com.stc.jmsjca.jboss;

import com.stc.jmsjca.core.RAJMSActivationSpec;

/**
 * Encapsulates the configuration of a MessageEndpoint.
 *
 * @version $Revision: 1.1.1.1 $
 * @author misc
 */
public class RAJBossActivationSpec extends RAJMSActivationSpec {

    /**
     * isValidDestinationName
     * Can be overridden if there are special limitations on the destination
     * names in Wave.
     *
     * @param name String
     * @return boolean
     */
    public boolean isValidDestinationName(String name) {
        return !empty(name);
    }
}
