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

package com.stc.jmsjca.test.wl;

import com.stc.jmsjca.test.core.JMSProvider;
import com.stc.jmsjca.test.core.QueueEndToEnd;

/**
 *
 * @author fkieviet
 * @version $Revision: 1.11 $
 */
public class QueueWLEar1 extends QueueEndToEnd {
    /**
     * @see com.stc.jmsjca.test.core.EndToEndBase#getJMSProvider()
     */
    @Override
    public JMSProvider getJMSProvider() {
        return new WLProvider();
    }
    
    /**
     * In WL, a temporaryQueue.delete() does not throw an exception if the destination
     * is already deleted.
     */
    public void skip_testRequestReplyN2() throws Throwable {
    }
    
    /**
     * In WL, a temporaryQueue.delete() does not throw an exception if the destination
     * is already deleted.
     */
    public void skip_testRequestReplyN3() throws Throwable {
    }

    /**
     * In WL, a temporaryQueue.delete() does not throw an exception if the destination
     * is already deleted.
     */
    public void skip_testA() throws Throwable {
    }
    
    /**
     * Because we're using a PseudoXASession, autocommit outside of a transaction is not
     * supported (the underlying session is a transacted session, and since the 
     * session is never committed, the message is never sent)
     */
    public void skip_testNoTransaction() {
    }
    
    /**
     * Because we're using a PseudoXASession, autocommit outside of a transaction is not
     * supported (the underlying session is a transacted session, and since the 
     * session is never committed, the message is never sent)
     */
    public void skip_testXASessionCommitAllocateOutsideOfTx() {
    }
}
