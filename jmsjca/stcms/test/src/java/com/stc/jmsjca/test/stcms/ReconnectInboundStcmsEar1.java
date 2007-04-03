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

package com.stc.jmsjca.test.stcms;

import com.stc.jmsjca.container.EmbeddedDescriptor;
import com.stc.jmsjca.test.core.Passthrough;
import com.stc.jmsjca.test.core.ReconnectionTestsInbound;

import java.util.Properties;

/**
 * Tests STCMS reconnects on inbound connections
 *
 * @author fkieviet
 * @version $Revision: 1.1.1.2 $
 */
public class ReconnectInboundStcmsEar1 extends ReconnectionTestsInbound {
    
    /**
     * @see com.stc.jmsjca.test.core.EndToEndBase#getDD()
     */
    public EmbeddedDescriptor getDD() throws Exception {
        EmbeddedDescriptor dd = super.getDD();
        dd = SendEar1.getDDstcms(dd, this);
        return dd;
    }
    
    /**
     * @see com.stc.jmsjca.test.core.EndToEndBase#createPassthrough(java.util.Properties)
     */
    public Passthrough createPassthrough(Properties serverProperties) {
        return new StcmsPassthrough(serverProperties);
    }
    
    /**
     * @see com.stc.jmsjca.test.core.ReconnectionTestsOutbound#getConnectionUrl()
     */
    public String getConnectionUrl() {
        return createConnectionUrl(mServerProperties.getProperty("host"), 
            Integer.parseInt(mServerProperties.getProperty("stcms.instance.port")));
    }
    
    /**
     * @see com.stc.jmsjca.test.core.ReconnectionTestsOutbound#createConnectionUrl(java.lang.String, int)
     */
    public String createConnectionUrl(String server, int port) {
        return "stcms://" + server + ":" + port;
    }
}