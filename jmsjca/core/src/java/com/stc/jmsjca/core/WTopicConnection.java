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
 * $RCSfile: WTopicConnection.java,v $
 * $Revision: 1.3 $
 * $Date: 2007-01-21 17:51:48 $
 *
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.  
 */

package com.stc.jmsjca.core;

import javax.jms.ConnectionConsumer;
import javax.jms.JMSException;
import javax.jms.ServerSessionPool;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicSession;

/**
 * See WConnection
 *
 * @author Frank Kieviet
 * @version $Revision: 1.3 $
 */
public class WTopicConnection extends WConnection implements TopicConnection {

    /**
     * WTopicConnection
     *
     * @param mgr JConnection
     */
    public WTopicConnection(JConnection mgr) {
        super(mgr);
    }

    /**
     * createTopicSession
     *
     * @param boolean0 boolean
     * @param int1 int
     * @return TopicSession
     * @throws JMSException on failure
     */
    public TopicSession createTopicSession(boolean boolean0, int int1) throws JMSException {
        if (mMgr == null) {
            invokeOnClosed();
        }
        return mMgr.createTopicSession(boolean0, int1);
    }

    /**
     * createConnectionConsumer
     *
     * @param topic Topic
     * @param string String
     * @param serverSessionPool ServerSessionPool
     * @param int3 int
     * @return ConnectionConsumer
     * @throws JMSException on failure
     */
    public ConnectionConsumer createConnectionConsumer(Topic topic, String string,
        ServerSessionPool serverSessionPool, int int3) throws JMSException {
        if (mMgr == null) {
            invokeOnClosed();
        }
        return mMgr.createConnectionConsumer(topic, string, serverSessionPool, int3);
    }
}
