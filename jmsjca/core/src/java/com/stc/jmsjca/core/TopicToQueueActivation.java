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

package com.stc.jmsjca.core;

import com.stc.jmsjca.localization.Localizer;
import com.stc.jmsjca.util.Exc;
import com.stc.jmsjca.util.Logger;
import com.stc.jmsjca.util.Str;
import com.stc.jmsjca.util.UrlParser;
import com.stc.jmsjca.util.XAssert;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.resource.ResourceException;
import javax.resource.spi.UnavailableException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

import java.lang.reflect.Method;
import java.rmi.MarshalledObject;
import java.util.Properties;

/**
 * Activation for distributed durable subscribers
 *
 * @author fkieviet
 * @version $Revision: 1.3 $
 */
public class TopicToQueueActivation extends ActivationBase {
    private static Logger sLog = Logger.getLogger(TopicToQueueActivation.class);
    private ActivationBase mTopicToQueue;
    private ActivationBase mQueue;
    private String mQueuename;

    private static final Localizer LOCALE = Localizer.get();
    
    /**
     * Activation constructor
     *
     * @param ra RAJMSResourceAdapter
     * @param epf MessageEndpointFactory
     * @param spec RAJMSActivationSpec
     * @throws Exception on invalid config
     */
    public TopicToQueueActivation(RAJMSResourceAdapter ra, MessageEndpointFactory epf,
        RAJMSActivationSpec spec) throws Exception {
        super(ra, epf, spec);
        
        UrlParser subscriberinfo = new UrlParser(spec.getSubscriptionName());
        Properties props = subscriberinfo.getQueryProperties();
        
        // Create a spec for topic-to-queue
        String subscribername = props.getProperty(Options.Subname.SUBSCRIBERNAME);
        String defaultQueuename = "LOADBALQ_" + spec.getDestination() + "_" + subscribername;
        mQueuename = props.getProperty(Options.Subname.QUEUENAME, defaultQueuename);
        RAJMSActivationSpec topicSpec = copy(spec);
        topicSpec.setSubscriptionName(subscribername);
        topicSpec.setConcurrencyMode(
            RAJMSActivationSpec.DELIVERYCONCURRENCY_STRS[RAJMSActivationSpec.DELIVERYCONCURRENCY_SYNC]);
        topicSpec.setEndpointPoolMaxSize("1");
        if (!Str.empty(topicSpec.getMBeanName())) {
            String mbeanname = props.getProperty(Options.Subname.MBEANNAME, 
                topicSpec.getMBeanName() + "-LOADBALQ");
            topicSpec.setMBeanName(mbeanname);
        }
        topicSpec.setBatchSize(props.getProperty(Options.Subname.BATCHSIZE, "10"));
        topicSpec.setHoldUntilAck("0");
        Properties options = new Properties();
        Str.deserializeProperties(topicSpec.getOptions(), options);
        options.setProperty(Options.In.OPTION_CONCURRENCYMODE, 
            RAJMSActivationSpec.DELIVERYCONCURRENCY_STRS[RAJMSActivationSpec.DELIVERYCONCURRENCY_SYNC]);
        topicSpec.setOptions(Str.serializeProperties(options));
        
        // Create spec for queue
        RAJMSActivationSpec queueSpec = copy(spec);
        queueSpec.setDestinationType(RAJMSActivationSpec.QUEUE);
        queueSpec.setSubscriptionName(null);
        queueSpec.setDestination(mQueuename);
        queueSpec.setSubscriptionDurability(RAJMSActivationSpec.NONDURABLE);
        queueSpec.setClientId(null);
        
        // Create Topic activation
        MessageEndpointFactory topicEPF = new MessageEndpointFactory() {
            public MessageEndpoint createEndpoint(XAResource arg0) throws UnavailableException {
                return null;
            }
            public boolean isDeliveryTransacted(Method arg0) throws NoSuchMethodException {
                // Ensure that XA is *not* used
                return false;
            }
        };
        
        // Create Queue activation
        mQueue = getObjectFactory().createActivation(ra, epf, queueSpec);

        mTopicToQueue = new Activation(ra, topicEPF, topicSpec) {
            private String queueName = mQueue.getName();

            public Delivery createDelivery() {
                TopicToQueueDelivery ret = new TopicToQueueDelivery(this, getStats(), mQueuename);
                return ret;
            }
            public String getName() {
              return super.getName() + " >> [" + queueName + "]";
            }
            protected void logDeliveryInitiationException(int attemptPlusOne, int dt, Exception e) {
                if (e instanceof Exc.ConsumerCreationException) {
                    if (attemptPlusOne == 1) {
                    sLog.info(LOCALE.x("E093: [{0}]: message delivery could not be initiated due to " +
                        "a failure to create the subscriber. Assuming that this deployment is on a node in " + 
                        "a cluster, there is likely another cluster node already receiving messages from " +
                        "this subscriber and delivering them to the load balancing queue where this " +
                        "deployment will receive them. The subscriber creation attempt will be retried " +
                        "periodically to detect when the active subscriber disconnects. Unsuccessful attempts " +
                        "to subscribe will not be logged. The subscriber could not created because of the " +
                        "following error: {3}", 
                        getName(), Integer.toString(attemptPlusOne), Integer.toString(dt), e.getCause()), e.getCause());
                    } else {
                        // Ignore
                    }
                } else {
                    super.logDeliveryInitiationException(attemptPlusOne, dt, e);
                }
            }
        };
    }
    
    private RAJMSActivationSpec copy(RAJMSActivationSpec tocopy) throws Exception {
        MarshalledObject copier = new MarshalledObject(tocopy);
        return (RAJMSActivationSpec) copier.get();
    }
    
    /**
     * Starts message delivery
     * @see com.stc.jmsjca.core.ActivationBase#activate()
     * 
     * @throws Exception on failure
     */
    public void activate() throws Exception {
        try {
            mTopicToQueue.activate();
            mQueue.activate();
        } catch (Exception e) {
            deactivate();
            throw e;
        }
    }
    
    /**
     * @see com.stc.jmsjca.core.ActivationBase#deactivate()
     * Halts message delivery
     * Should NOT throw an exception
     */
    public void deactivate() {
        mTopicToQueue.deactivate();
        mQueue.deactivate();
    }
    
    /**
     * Copies messages from a topic to a queue so that the messages can be processed
     * concurrently, even over multiple machines. This delivery should be setup as non-XA,
     * it should read from the topic, and the queue name should be specified TODO 
     *  
     * @author fkieviet
     */
    public class TopicToQueueDelivery extends SyncDelivery {
        private String mQueueName;

        /**
         * Constructor 
         * 
         * @param a Activation
         * @param stats DeliveryStats
         * @param queuename name of queue to send to 
         */
        public TopicToQueueDelivery(Activation a, DeliveryStats stats, String queuename) {
            super(a, stats);
            XAssert.xassert(a.isTopic());
            XAssert.xassert(!a.isXA());
            mQueueName = queuename;
        }
        
        /**
         * @see com.stc.jmsjca.core.Delivery#createMessageEndpoint(javax.transaction.xa.XAResource, javax.jms.Session)
         */
        protected MessageEndpoint createMessageEndpoint(XAResource xa, Session s) throws Exception {
            return new Copier(s);
        }
        
        /**
         * Have to create unified session so that a queue and topic can be created on the 
         * same session object
         * 
         * @see com.stc.jmsjca.core.SyncDelivery#getSessionClass()
         */
        protected Class getSessionClass() {
            return Session.class;        
        }

        /**
         * Have to create a unified connection so that a unified session can be created so
         * that a queue and topic can be created on the same object
         * 
         * @see com.stc.jmsjca.core.SyncDelivery#getDomain()
         */
        protected int getDomain() {
            return XConnectionRequestInfo.DOMAIN_UNIFIED_NONXA;        
        }

        private class Copier implements MessageEndpoint, MessageListener {
            private Session mSession;
            private MessageProducer mProducer;
            
            /**
             * Constructor
             * 
             * @param s session to use
             * @throws Exception propagated
             */
            public Copier(Session s) throws Exception {
                mSession = s;
                RAJMSObjectFactory o = mActivation.getObjectFactory();
                javax.jms.Destination dest = o.createDestination(
                    mSession,
                    false,
                    false,
                    mActivation.getActivationSpec(),
                    null,
                    mActivation.getRA(),
                    mQueueName);
                mProducer = o.createMessageProducer(mSession,
                    mActivation.isXA(),
                    false,
                    dest,
                    mActivation.getRA());
            }

            /**
             * @see javax.resource.spi.endpoint.MessageEndpoint#afterDelivery()
             */
            public void afterDelivery() throws ResourceException {
            }

            /**
             * @see javax.resource.spi.endpoint.MessageEndpoint#beforeDelivery(java.lang.reflect.Method)
             */
            public void beforeDelivery(Method arg0) throws NoSuchMethodException, ResourceException {
            }

            /**
             * @see javax.resource.spi.endpoint.MessageEndpoint#release()
             */
            public void release() {
                if (mProducer != null) {
                    try {
                        mProducer.close();
                        mProducer = null;
                    } catch (JMSException e) {
                        sLog.warn(LOCALE.x("E094 = This {0} could not be closed properly: {1}", 
                            mProducer.getClass().getName(), e), e);
                    }
                }
            }

            /**
             * @see javax.jms.MessageListener#onMessage(javax.jms.Message)
             * 
             * Sends the message to the queue
             */
            public void onMessage(Message m) {
                try {
                    if (m.getObjectProperty(EndOfBatchMessage.KEY_ENDOFBATCH) == null) {
                        mActivation.getObjectFactory().send(false, mProducer, m, 
                            m.getJMSPriority(), m.getJMSDeliveryMode());
                    }
                } catch (JMSException e) {
                    throw new RuntimeException("Redirecting message failed: " + e, e);
                }
            }
        }
    }
}
