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
 * $RCSfile: RASTCMSObjectFactorySync.java,v $
 * $Revision: 1.1.1.1 $
 * $Date: 2007-01-21 07:52:12 $
 *
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.  
 */

package com.stc.jmsjca.stcms;

import com.stc.jmsjca.core.RAJMSActivationSpec;
import com.stc.jmsjca.util.Logger;

/**
 * Encapsulates the configuration of a MessageEndpoint.
 * 
 * @author Frank Kieviet
 * @version $Revision: 1.1.1.1 $
 */
public class RASTCMSObjectFactorySync extends RASTCMSObjectFactory implements
    java.io.Serializable {
    private static Logger sLog = Logger.getLogger(RASTCMSObjectFactorySync.class);

    /**
     * @see com.stc.jmsjca.core.RAJMSObjectFactory#adjustDeliveryMode(int, boolean)
     */
    public int adjustDeliveryMode(int mode, boolean xa) {
        int newMode = mode;
        if (mode != RAJMSActivationSpec.DELIVERYCONCURRENCY_SYNC) {
            newMode = RAJMSActivationSpec.DELIVERYCONCURRENCY_SYNC;
            sLog.warn("Current delivery mode ["
                + RAJMSActivationSpec.DELIVERYCONCURRENCY_STRS[mode]
                + "] not supported; switching to ["
                + RAJMSActivationSpec.DELIVERYCONCURRENCY_STRS[newMode]
                + "]");
        }
        return newMode;
    }
}
