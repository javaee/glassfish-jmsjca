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

package com.stc.jmsjca.test.sunone;

import com.stc.jmsjca.test.core.JMSProvider;
import com.stc.jmsjca.test.core.ReconnectionTestsInbound;

/**
 * Tests STCMS reconnects on inbound connections
 *
 * @author fkieviet
 * @version $Revision: 1.3 $
 */
public class ReconnectInboundSunOneEar1 extends ReconnectionTestsInbound {
    
    /**
     * @see com.stc.jmsjca.test.core.EndToEndBase#getJMSProvider()
     */
    @Override
    public JMSProvider getJMSProvider() {
        return new SunOneProvider();
    }
    
    /**
     * Whether to use a TCP proxy to monitor reconnections.
     * Subclasses should override this when the use of a proxy would not be possible
     * 
     * @return   
     */
    @Override
    protected boolean useProxy(){
        
        /**
         * Can't use a proxy in direct mode
         */
        if (((SunOneProvider) getJMSProvider()).isDirect()){
            return false;
        }
        
        return true;
    }
}
