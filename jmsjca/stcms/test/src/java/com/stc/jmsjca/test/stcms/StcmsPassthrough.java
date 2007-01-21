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
 * $RCSfile: StcmsPassthrough.java,v $
 * $Revision: 1.1.1.1 $
 * $Date: 2007-01-21 07:52:12 $
 *
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.  
 */

package com.stc.jmsjca.test.stcms;

import com.stc.jms.client.STCQueueConnectionFactory;
import com.stc.jms.client.STCTopicConnectionFactory;
import com.stc.jms.queueviewer.Server;
import com.stc.jms.queueviewer.SubscriberInfo;
import com.stc.jmsjca.test.core.Passthrough;

import javax.jms.JMSException;
import javax.jms.QueueConnectionFactory;
import javax.jms.TopicConnectionFactory;

import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

/**
 *
 * @author fkieviet
 * @version $Revision: 1.1.1.1 $
 */
public class StcmsPassthrough extends Passthrough {
    private Properties mServerProperties;
    private Server mServer;

    public StcmsPassthrough(Properties server) {
        super(server);
        mServerProperties = server;
    }
    
    public static int getPort(Properties serverProperties) {
        int port = Integer.parseInt(serverProperties.getProperty(
            "stcms.instance.port", null));
        return port;
    }
    
    public static String getHost(Properties serverProperties) {
        return serverProperties.getProperty("host");        
    }

    public Server getServer() throws Exception {
        if (mServer == null) {
            mServer = new Server();
 
            int port = getPort(mServerProperties);

            mServer.connect(getHost(mServerProperties), port,
                    mServerProperties.getProperty("admin.user"),
                    mServerProperties.getProperty("admin.password"));
        }
        return mServer;
    }

    public SubscriberInfo findDurableSubscriber(String topic, String subname) throws Exception {
        Vector subs = new Vector();
        getServer().getSubscribersOfTopic(subs, topic);
        for (Enumeration iter = subs.elements(); iter.hasMoreElements();) {
            SubscriberInfo sub = (SubscriberInfo) iter.nextElement();
            if (sub.getSubscriberName().equals(subname)) {
                return sub;
            }
        }
        return null;
    }

    public void removeDurableSubscriber(String clientID, String dest, String subname) throws Exception {
        SubscriberInfo sub = findDurableSubscriber(dest, subname);
        if (sub != null) {
            getServer().deleteSubscriber(dest, subname, sub.getClientId());
        }
    }

    /**
     * @see com.stc.jmsjca.test.core.Passthrough#createTopicConnectionFactory()
     */
    public TopicConnectionFactory createTopicConnectionFactory() throws JMSException {
        int port = Integer.parseInt(mServerProperties.getProperty(
                "stcms.instance.port", null));
        return new STCTopicConnectionFactory(mServerProperties.
                getProperty("host"), port);
    }

    /**
     * @see com.stc.jmsjca.test.core.Passthrough#createQueueConnectionFactory()
     */
    public QueueConnectionFactory createQueueConnectionFactory() throws JMSException {
        int port = Integer.parseInt(mServerProperties.getProperty(
                "stcms.instance.port", null));
        return new STCQueueConnectionFactory(mServerProperties.
                getProperty("host"), port);
    }

    public boolean isDurableSubscriberPresent(String topic, String subname) throws Exception {
        return findDurableSubscriber(topic, subname) != null;
    }
}
