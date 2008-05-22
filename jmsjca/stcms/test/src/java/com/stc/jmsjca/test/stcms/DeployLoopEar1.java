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

import com.stc.jmsjca.container.Container;
import com.stc.jmsjca.container.EmbeddedDescriptor;
import com.stc.jmsjca.test.core.EndToEndBase;
import com.stc.jmsjca.test.core.Passthrough;

import java.util.Properties;

/**
 * Tests STCMS reconnects on inbound connections
 *
 * @author fkieviet
 * @version $Revision: 1.6 $
 */
public class DeployLoopEar1 extends EndToEndBase {
    
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
    public void testDummy() {}
    /**
     * Queue to queue XA on in, XA on out CC-mode
     * 
     * @throws Throwable
     */
    public void xtestContainerManaged() throws Throwable {
        EmbeddedDescriptor dd = getDD();
        dd.findElementByText(EJBDD, "XContextName").setText("j-testQQXAXA");
        dd.update();

        // Deploy
        Container c = createContainer();
        Passthrough p = createPassthrough(mServerProperties);

        try {
            for (;;) {
                p.setNMessagesToSend(1);
                c.redeployModule(mTestEar.getAbsolutePath());
                p.passFromQ1ToQ2();
                Passthrough.safeClose(p);
                p = createPassthrough(mServerProperties);
            }
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
        }
    }
}
