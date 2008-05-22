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

package com.stc.jmsjca.test.jboss;

import com.stc.jmsjca.jboss.RAJBossObjectFactory;
import com.stc.jmsjca.localization.LocalizedString;
import com.stc.jmsjca.test.core.Passthrough;
import com.stc.jmsjca.util.Exc;

import javax.jms.JMSException;
import javax.jms.QueueConnectionFactory;
import javax.jms.Session;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;

import java.util.Properties;

/**
 *
 * @author fkieviet
 * @version $Revision: 1.6 $
 */
public class JBossPassthrough extends Passthrough {

    public JBossPassthrough(Properties server) {
        super(server);
    }

    /**
     * @see com.stc.jmsjca.test.core.Passthrough#createTopicConnectionFactory()
     */
    public TopicConnectionFactory createTopicConnectionFactory() throws JMSException {
        try {
            return (TopicConnectionFactory) TestJBossJUStd.getInitialContext().lookup(
                RAJBossObjectFactory.DEFAULT_XACF);
        } catch (Exception e) {
            throw Exc.jmsExc(LocalizedString.valueOf("Cannot create cf: " + e), e);
        }
    }

    /**
     * @see com.stc.jmsjca.test.core.Passthrough#createQueueConnectionFactory()
     */
    public QueueConnectionFactory createQueueConnectionFactory() throws JMSException {
        return (QueueConnectionFactory) createTopicConnectionFactory();
    }

    /**
     * @see com.stc.jmsjca.test.core.Passthrough#removeDurableSubscriber(java.lang.String, java.lang.String, java.lang.String)
     */
    public void removeDurableSubscriber(String clientID, String destname,
        String subscriptionName) throws Exception {
        TopicConnectionFactory cf = createTopicConnectionFactory();
        TopicConnection conn = cf.createTopicConnection();
        conn.setClientID(clientID);
        TopicSession sess = conn.createTopicSession(false, Session.CLIENT_ACKNOWLEDGE);
        try {
            sess.unsubscribe(subscriptionName);
        } catch (JMSException ignore) {
            // acceptable: subscriber may not be there yet
        }
        sess.close();
        conn.close(); 
    }
}
