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

package com.stc.jmsjca.test.wave;

import com.spirit.wave.WaveProfile;
import com.spirit.wave.jms.WaveQueueConnectionFactory;
import com.spirit.wave.jms.WaveTopicConnectionFactory;
import com.stc.jmsjca.test.core.Passthrough;

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
 * @version $Revision: 1.1.1.2 $
 */
public class WavePassthrough extends Passthrough {

    public WavePassthrough(Properties server) {
        super(server);
    }
    
    private String getConnectionUrl() {
        return TestWaveJUStd.getConnectionUrl();
    }

    public boolean isDurableSubscriberPresent(String topic, String subname) throws Exception {
        return true;
    }

    public void removeDurableSubscriber(String clientID, String dest, String subname) throws JMSException {
        TopicConnectionFactory cf = createTopicConnectionFactory();
        TopicConnection conn = cf.createTopicConnection();
        conn.setClientID(clientID);
        TopicSession sess = conn.createTopicSession(false, Session.CLIENT_ACKNOWLEDGE);
        // need to create a durable subscription before deleting it to workaround a bug in wave
        // see bugzilla: http://hercules/show_bug.cgi?id=103
        sess.createDurableSubscriber(sess.createTopic(dest),subname);
        sess.unsubscribe(subname);
        sess.close();
        conn.close(); 
    }

    /**
     * @see com.stc.jmsjca.test.core.Passthrough#createTopicConnectionFactory()
     */
    public TopicConnectionFactory createTopicConnectionFactory() throws JMSException {
        WaveProfile p = TestWaveJUStd.createWaveProfile(getConnectionUrl());
        return new WaveTopicConnectionFactory(p);
    }

    /**
     * @see com.stc.jmsjca.test.core.Passthrough#createQueueConnectionFactory()
     */
    public QueueConnectionFactory createQueueConnectionFactory() throws JMSException {
        WaveProfile p = TestWaveJUStd.createWaveProfile(getConnectionUrl());
        return new WaveQueueConnectionFactory(p);
    }
}
