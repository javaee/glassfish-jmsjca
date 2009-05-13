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
import com.stc.jmsjca.test.core.QueueEndToEnd;

/**
 *
 * @author fkieviet
 * @version $Revision: 1.8 $
 */
public class QueueSunOneEar1 extends QueueEndToEnd {
    /**
     * JMQ does not adhere to this:
     * tempdest = s.createTempQueue();
     * tempdest.delete();
     * tempdest.delete(); <-- does not throw
     */
    public void disabled_testA() {
    }

    /**
     * JMQ's Receive buffer makes this test invalid
     * @throws Throwable
     */
    public void skip_testRequestReply0() throws Throwable {
    }

    /**
     * JMQ does not adhere to this:
     * tempdest = s.createTempQueue();
     * tempdest.delete();
     * tempdest.delete(); <-- does not throw
     */
    public void skip_testRequestReplyN2() throws Throwable {
    }
    
    /**
     * JMQ does not adhere to this:
     * tempdest = s.createTempQueue();
     * tempdest.delete();
     * tempdest.delete(); <-- does not throw
     */
    public void skip_testRequestReplyN3() throws Throwable {
    }

    /**
     * @see com.stc.jmsjca.test.core.EndToEndBase#getJMSProvider()
     */
    @Override
    public JMSProvider getJMSProvider() {
        return new SunOneProvider();
    }

    // Following tests need looking into:    
//        public void skip_testNoTransaction() throws Throwable {
//        }
//        public void skip_testNoXATransacted() throws Throwable {
//        }
//        public void skip_testNoXANonTransacted() throws Throwable {
//        }
}
