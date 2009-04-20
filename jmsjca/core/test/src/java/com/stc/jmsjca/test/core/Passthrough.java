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

package com.stc.jmsjca.test.core;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.JMSSecurityException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * Tool class that makes it easier to test JMS servers. A main piece of functionality
 * is to send messages to one destination and read it back from another destination.
 * 
 * @author fkieviet
 * @version $Revision: 1.11 $
 */
public abstract class Passthrough {
    private Properties mServerProperties;
    private List mThings = new ArrayList();
    private int mNMsgsToSend = 1000;
    private int mCommitSize  = 1000;
    private int mDrainTimeout = 1000;
    private int mTimeout = 60000;
    private int mBatchId;
    private String mMethodname;
    private MessageGenerator mMessageGenerator = new MessageGenerator();
    private Class mMsgType = TextMessage.class;
    private boolean mStrictOrder;
    private String mQueue1Name = "Queue1";
    private String mQueue2Name = "Queue2";
    private String mQueue3Name = "Queue3";
    private String mQueue4Name = "Queue4";
    private String mTopic1Name = "Topic1";
    private String mTopic2Name = "Topic2";
    private String mTopic1DurableName1 = "T1SUB";
    private String mTopic1DurableName2 = "T1SUB2";
    private String mTopic1DurableName3 = "T1SUB3";
    private JMSProvider mProvider;
    
    /**
     * @return UserId
     */
    public String getUserid() {
        return getJMSProvider().getUserName(mServerProperties);
    }
    
    /**
     * @return password
     */
    public String getPassword() {
        return getJMSProvider().getPassword(mServerProperties);            
    }

    /**
     * @return associated provider
     */
    public JMSProvider getJMSProvider() {
        return mProvider;
    }
    
    /**
     * @return server configuration
     */
    public Properties getServerProperties() {
        return mServerProperties;
    }

    /**
     * Constructor
     * 
     * @param server configuration
     */
    public Passthrough(Properties server, JMSProvider provider) {
        mServerProperties = server;
        mProvider = provider;
        mCommitSize = Integer.parseInt(server.getProperty("jmsjca.test.commitsize",
            Integer.toString(mCommitSize)));
        mNMsgsToSend = Integer.parseInt(server.getProperty("jmsjca.test.mNMsgsToSend",
                Integer.toString(mNMsgsToSend)));
            
    }
    
    /**
     * Sets the number of messages that can be commited in one call
     * 
     * @param n number of messages to commit in one batch 
     */
    public void setCommitSize(int n) {
        mCommitSize = n;
    }
    
    /**
     * @param gen generator
     */
    public void setMessageGenerator(MessageGenerator gen) {
        mMessageGenerator = gen;
    }
    
    /**
     * @return current message generator
     */
    public MessageGenerator getMessageGenerator() {
        return mMessageGenerator;
    }
    
    /**
     * Specifies the type of message to send
     * 
     * @param type JMS type
     */
    public void setMsgType(Class type) {
        mMsgType = type;
    }

    /**
     * Sets the number of of messages to send in a passthrough
     * 
     * @param n number of messages
     */
    public void setNMsgsToSend(int n) {
        mNMsgsToSend = n;
    }
    
    /**
     * Specifies the method name to set on the messages to be sent
     * 
     * @param methodname String
     */
    public void setMethodname(String methodname) {
        mMethodname = methodname;
    }
    
    /**
     * @param strict enforces the same order of messages to be read as they were sent 
     */
    public void setStrictOrder(boolean strict) {
        mStrictOrder = strict;
    }

    /**
     * @param timeout specifies the maximum time in ms that the client should wait for
     * the expected number messages
     */
    public void setTimeout(int timeout) {
        mTimeout = timeout;
    }
    
    /**
     * @param name destination name
     */
    public void setTopic1Name(String name) {
        mTopic1Name = name;
    }
    
    /**
     * @return destination name
     */
    public String getTopic1Name() {
        return mTopic1Name;
    }
    
    /**
     * @param name destination name
     */
    public void setTopic2Name(String name) {
        mTopic2Name = name;
    }
    
    /**
     * @return destination name
     */
    public String getTopic2Name() {
        return mTopic2Name;
    }
    
    /**
     * @param name durable subscriber name
     */
    public void setDurableTopic1Name(String name) {
        mTopic1DurableName1 = name;
    }
    
    /**
     * @return durable subscriber name
     */
    public String getDurableTopic1Name1() {
        return mTopic1DurableName1;
    }
    
    /**
     * @return durable subscriber name
     */
    public String getDurableTopic1Name2() {
        return mTopic1DurableName2;
    }
    
    /**
     * @return durable subscriber name
     */
    public String getDurableTopic1Name3() {
        return mTopic1DurableName3;
    }
    
    /**
     * @param name destination name
     */
    public void setQueue1Name(String name) {
        mQueue1Name = name;
    }
    
    /**
     * @return destination name
     */
    public String getQueue1Name() {
        return mQueue1Name;
    }
    
    /**
     * @param name destination name
     */
    public void setQueue2Name(String name) {
        mQueue2Name = name;
    }
    
    /**
     * @return destination name
     */
    public String getQueue2Name() {
        return mQueue2Name;
    }
    
    /**
     * @param name destination name
     */
    public void setQueue3Name(String name) {
        mQueue3Name = name;
    }
    
    /**
     * @return destination name
     */
    public String getQueue3Name() {
        return mQueue3Name;
    }
    
    /**
     * @param name destination name
     */
    public void setQueue4Name(String name) {
        mQueue4Name = name;
    }
    
    /**
     * @return destination name
     */
    public String getQueue4Name() {
        return mQueue4Name;
    }
    
    /**
     * @return timeout see setTimeout()
     */
    public int getTimeout() {
        return mTimeout;
    }
    
    /**
     * @return drain timeout
     */
    public int getDrainTimeout() {
        return mDrainTimeout;
    }

    /**
     * @return commit sie
     */
    public int getCommitSize() {
        return mCommitSize;
    }

    /**
     * @param batchid an index that is common to all messages to be sent through the 
     * system
     */
    public void setBatchId(int batchid) {
        mBatchId = batchid;
    }
    
    /**
     * Sets the time that the system should wait for a message when the destination is
     * to be cleaned
     * 
     * @param timeout in ms
     */
    public void setDrainTimeout(int timeout) {
        mDrainTimeout = timeout;
    }
    
    /**
     * @return factory
     * @throws JMSException fault
     */
    public abstract TopicConnectionFactory createTopicConnectionFactory() throws JMSException;

    /**
     * @return factory
     * @throws JMSException fault
     */
    public abstract QueueConnectionFactory createQueueConnectionFactory() throws JMSException;
    
    /**
     * Tool function: uses the JDK 1.4 cause field to propagate the causing
     * exception. Cause will be ignored in JDK 1.3.
     *
     * @param toSetOn exception to change
     * @param cause exception to set as cause (may be null)
     * @return exception passed in
     */
    public static Exception setCause(Exception toSetOn, Throwable cause) {
        if (cause != null) {
            try {
                Class c = toSetOn.getClass();
                Method m = c.getMethod("initCause", new Class[] {Throwable.class});
                m.invoke(toSetOn, new Object[] {cause});
            } catch (Exception ex) {
                // ignore
            }
        }
        return toSetOn;
    }

    /**
     * Creates a new exception with cause and linked exception filled in
     *
     * @param msg error text
     * @param cause the exception that caused this exception
     * @return new exception
     */
    public static JMSException jmsExc(String msg, Exception cause) {
        JMSException ret;
        if (cause instanceof JMSSecurityException || cause instanceof SecurityException) {
            ret = new JMSSecurityException(msg);
        } else {
            ret = new JMSException(msg);
        }
        setCause(ret, cause);
        return ret;
    }
    
    /**
     * Creates a queue
     * 
     * @param s session
     * @param name destination name
     * @return destination
     * @throws JMSException fault
     */
    public Queue createQueue(Session s, String name) throws JMSException {
        return s.createQueue(name);
    }

    /**
     * Creates a topic
     * 
     * @param s session
     * @param name destination name
     * @return destination
     * @throws JMSException fault
     */
    public Topic createTopic(Session s, String name) throws JMSException {
        return s.createTopic(name);
    }

    /**
     * Generates and checks messages to be sent / read back
     * 
     * @author fkieviet
     */
    public static class MessageGenerator {
        
        /**
         * Called for each message to be sent; sets the payload of the msg
         * 
         * @param m message
         * @param i msg index
         * @param iBatch batch index
         * @param type one of the message types
         * @throws JMSException fault
         */
        public void setMsgPayload(Message m, int i, int iBatch, Class type) throws JMSException {
            if (type == BytesMessage.class) {
                // nothing
            } else if (type == MapMessage.class) {
                // nothing
            } else if (type == ObjectMessage.class) {
                // nothing
            } else if (type == StreamMessage.class) {
                // nothing
            } else if (type == TextMessage.class) {
                TextMessage msg = (TextMessage) m;
                msg.setText(getStringPayload(i, iBatch));
            } else {
                // nothing (unknown type)
            }
        }
        
        /**
         * Generates a string payload
         * 
         * @param i msg index
         * @param iBatch msg batch index
         * @return payload
         */
        public String getStringPayload(int i, int iBatch) {
            return "Batch=" + iBatch + "; msgidx=" + i;
        }
        
        /**
         * Creates a msg
         * 
         * @param s session
         * @param type desired type (may be ignored)
         * @return msg
         * @throws JMSException fault
         */
        public Message createMessage(Session s, Class type) throws JMSException {
            Message msg;
            if (type == BytesMessage.class) {
                msg = s.createBytesMessage();
            } else if (type == MapMessage.class) {
                msg = s.createMapMessage();
            } else if (type == ObjectMessage.class) {
                msg = s.createObjectMessage();
            } else if (type == StreamMessage.class) {
                msg = s.createStreamMessage();
            } else if (type == TextMessage.class) {
                msg = s.createTextMessage();
            } else {
                msg = s.createMessage();
            }
            return msg;
        }

        /**
         * Checks the validity of a received message
         * 
         * @param m msg to check
         * @param i extracted msg index
         * @param iBatch extracted batch index
         * @return null if ok, error msg if faulty
         * @throws JMSException fault
         */
        public String checkMessage(Message m, int i, int iBatch) throws JMSException {
            return null;
        }
    }

    /**
     * A baseclass for a source or destination (both for queue and topic).
     * Note that this class provides some methods that formally should not
     * be in this class, but are anyways to avoid the dreaded multiple
     * inheritance problem.
     */
    public abstract class Endpoint {
        private String mName;

        /**
         * @param name name
         */
        public Endpoint(String name) {
            mName = name;
            mThings.add(this);
        }

        /**
         * @return name
         */
        public String getName() {
            return mName;
        }

        /**
         * Receives a message from this endpoint; both sources and
         * destinations can be received messages from. In the case of
         * sources that will be done to drain the source before a
         * test is started.
         *
         * @param timeout how long to wait
         * @return message
         * @throws JMSException fault
         */
        public abstract Message receive(int timeout) throws JMSException;

        /**
         * Returns the session
         *
         * @return Session
         */
        public abstract Session getSession();

        /**
         * Removes all messages from the endpoint
         *
         * @return how many messages were drained
         * @throws JMSException fault
         */
        public int drain() throws JMSException {
            int ret = 0;
            for (;;) {
                Message m = receive(mDrainTimeout);
                if (m == null) {
                    break;
                }
                ret++;
                if (ret % getCommitSize() == 0) {
                    getSession().commit();
                }
                if (ret % 100 == 0) {
                    System.out.println(ret + " messages were drained from " + mName);
                }
                if (ret < 100) {
                    System.out.print("Drained " + m.getClass());
                    if (m instanceof TextMessage) {
                        System.out.print(" Payload: " + ((TextMessage) m).getText());
                    }
                    System.out.println();
                }
            }
            if (ret != 0) {
                System.out.println("Total of " + ret + " messages were drained from " + mName);
            }
            getSession().commit();
            return ret;
        }

        /**
         * Ensures that the endpoint does not contain any messages
         *
         * @throws Exception fault
         */
        public void assertEmpty() throws Exception {
            int n = drain();
            if (n != 0) {
                throw new Exception(mName + " still has " + n + " messages in it");
            }
        }
        
        private void checkResults(long t0, List failures, int nReceived, int nExpected,
            int[] readbackCount, int[] readbackOrder) throws Exception {
            boolean failure = false;
            
            // Count
            int nFound0 = 0;
            int nFound1 = 0;
            int nFoundMore = 0;
            for (int j = 0; j < readbackCount.length; j++) {
                if (readbackCount[j] == 0) {
                    nFound0++;
                    failure = true;
                } else if (readbackCount[j] == 1) {
                    nFound1++;
                } else {
                    nFoundMore++;
                    failure = true;
                }
            }
            String countFailures = "bins with zero: " + nFound0 + "; bins with one "
                + nFound1 + "; bins with more: " + nFoundMore + ";";
            if (nFound0 < 10) {
                countFailures += " missing: (";
                for (int j = 0; j < readbackCount.length; j++) {
                    if (readbackCount[j] == 0) {
                        countFailures += j + " ";
                    }
                }
                countFailures += ")";
            }
            
            // Strict order
            String orderFailures = "";
            if (mStrictOrder) {
                int k = 0;
                for (int i = 0; i < readbackOrder.length; i++) {
                    if (readbackOrder[i] != i) {
                        failure = true;
                        if (++k < 10) {
                            orderFailures = "#" + i + "!=" + "#" + readbackOrder[i] + "; "; 
                        } else {
                            break;
                        }
                    }
                }
            }
            if (orderFailures.length() == 0) {
                orderFailures = "[no order failures]";
            } else {
                orderFailures = "[order failures: " + orderFailures + "]";
            }

            // Other failures (passed in)
            String otherFailures = " no other failures";
            if (!failures.isEmpty()) {
                failure = true;
                int k = 0;
                otherFailures = "[Other failures: ";
                for (Iterator iter = failures.iterator(); iter.hasNext();) {
                    String f = (String) iter.next();
                    if (k != 0) {
                        otherFailures += ", ";
                    }
                    if (k > 10) {
                        otherFailures += (failures.size() - k) + " more";
                        break;
                    }
                    otherFailures += f;
                }
                otherFailures += "]";
            }
            
            if (failure) {
                throw new Exception("Messages received after "
                    + (System.currentTimeMillis() - t0) + " ms: " + nReceived
                    + "; expected: " + nExpected + "; diag: [" + countFailures + "]; "
                    + otherFailures + "; " + orderFailures);
            }
        }

        /**
         * Reads messages from the destination
         * ONLY FOR DESTINATION; NOT FOR SOURCE
         *
         * @param n number of messages to read
         * @param iBatch index common to all messages
         * @throws Exception fault
         */
        public void readback(int n, int iBatch) throws Exception {
            int[] readbackCount = new int[n];
            int[] readbackOrder = new int[n];
            int nFailures = 0;
            List failures = new ArrayList();

            System.out.println("Waiting to receive " + n + " msgs from "
                + getName());
            long t0 = System.currentTimeMillis();
            int nReceived = 0;

            for (int i = 0; i < n; i++) {
                // Receive
                Message m = receive(mTimeout);
                if (m == null) {
                    // No msg received: evaluate results
                    getSession().commit();
                    
                    // Will throw
                    checkResults(t0, failures, nReceived, n, readbackCount, readbackOrder);
                    break;
                }

                nReceived++;
                
                int iBatchRB = m.getIntProperty("batch");
                int iRB = m.getIntProperty("idx");

                // Check this msg: batch
                if (iBatchRB != iBatch) {
                    System.out.println("Failed: invalid batch " + iRB + "; batch "
                        + iBatchRB);
                    nFailures++;
                }

                try {
                    String checkFail = mMessageGenerator.checkMessage(m, iRB, iBatchRB);
                    if (checkFail != null) {
                        System.out.println("Message check failure: " + checkFail);
                        nFailures++;
                    }
                } catch (JMSException e) {
                    System.out.println("Check failure: " + e);
                    nFailures++;
                }

                // Check count
                if (iRB >= readbackCount.length) {
                    String err = "#" + i + ": Out of range: index " + iRB;
                    System.out.println(err);
                    failures.add(err);
                    nFailures++;
                } else {
                    readbackCount[iRB]++;
                    readbackOrder[iRB] = i;
                
                    if (readbackCount[iRB] != 1) {
                        System.out.println("#" + i + ": Failed: dup of " + iRB + "; batch "
                            + iBatchRB + "; count now: " + readbackCount[iRB]);
                        nFailures++;
                    }
                }

                // Check type
                if (!mMsgType.isAssignableFrom(m.getClass())) {
                    String err = "#" + i + ": wrong type: " + m.getClass()
                    + " instead of" + mMsgType;
                    failures.add(err);
                    System.out.println(err);
                    nFailures++;
                }
                
                // Commit
                if (i % getCommitSize() == 0) {
                    getSession().commit();
                }
            }
            
            System.out.println(n + " msgs were received after "
                + (System.currentTimeMillis() - t0) + " ms");

            getSession().commit();

            // Check received so far
            checkResults(t0, failures, nReceived, n, readbackCount, readbackOrder);
            
            // Check no more messages
            Message toomany;
            int nTooMany = 0;
            while ((toomany = receive(mDrainTimeout)) != null) {
                nTooMany++;
                System.out.println("Received too many: " + toomany.getClass());
                if (toomany instanceof TextMessage) {
                    System.out.print(" Payload: " + ((TextMessage) toomany).getText());
                }
            }
            getSession().commit();
            if (nTooMany > 0) {
                throw new Exception("More messages than the " + n
                    + " that were expected: at least " + nTooMany
                    + " too many received at " + mDrainTimeout + " ms timeout");
            }
        }

        /**
         * @param m message
         * @throws JMSException fault
         */
        public void send(Message m) throws JMSException {
            throw new javax.jms.IllegalStateException("This + " + getClass().getName() + ": cannot send messages");
        }

        /**
         * Sends a batch of messages
         *
         * @param n number of messages
         * @param iBatch index common to all messages
         * @param start text payload in wrapper
         * @throws JMSException fault
         */
        public void sendBatch(int n, int iBatch, String start) throws JMSException {
            sendBatch(n, iBatch, start, mMsgType);
        }

        /**
         * Sends a batch of messages
         * 
         * @param n number of messages
         * @param iBatch index common to all messages
         * @param start text payload in wrapper
         * @param type type of messages to send
         * @throws JMSException fault
         */
        public void sendBatch(int n, int iBatch, String start, Class type) throws JMSException {
            sendBatch(n, iBatch, type, mMessageGenerator);
        }

        /**
         * Sends a batch of messages
         * 
         * @param n number of messages
         * @param iBatch index common to all messages
         * @param type type of messages to send
         * @param gen how to create and populate messages
         * @throws JMSException fault
         */
        private void sendBatch(int n, int iBatch, Class type, MessageGenerator gen) throws JMSException {
            System.out.println("Sending " + n + " msgs of type " + type + " to "
                + getName() + "; batch " + iBatch);
            
            for (int i = 0; i < n; i++) {
                Message msg = gen.createMessage(getSession(), type);
                
                msg.setIntProperty("batch", iBatch);
                msg.setIntProperty("idx", i);
                if (mMethodname != null) {
                    msg.setStringProperty("methodname", mMethodname);
                }
                gen.setMsgPayload(msg, i, iBatch, type);
                send(msg);
            }
            getSession().commit();
            
            System.out.println("Sent " + n + " msgs of type " + type + " to "
                    + getName() + "; batch " + iBatch);            
        }

        /**
         * Closes all resources associated with this endpoint
         */
        public abstract void close();

        /**
         * Will read at least n messages from the destination and then rollback
         * 
         * @param nAtLeast msgs to read
         * @throws Exception fault 
         */
        public void waitUntil(int nAtLeast) throws Exception {
            int nRead = 0;
            for (;;) {
                Message m = receive(mTimeout);
                if (m == null) {
                    break;
                }
                nRead++;
                if (nRead == nAtLeast) {
                    break;
                }
            }
            getSession().rollback();
            
            if (nRead != nAtLeast) {
                throw new Exception("Only " + nRead + " msgs received instead of " + nAtLeast);
            }
        }
    }


    ///////////////////////// Queue /////////////////////////////////////////////////////

    /**
     * Specializes an endpoint for a Queue (note: an alternative inheritance
     * structure would have been to derive Soure and Destination from
     * Endpoint)
     */
    public abstract class QueueEndpoint extends Endpoint {
        private QueueConnectionFactory mFact;
        private QueueConnection mCon;
        /**
         * Session
         */
        protected QueueSession mSession;
        /**
         * Consumer
         */
        protected QueueReceiver mConsumer;

        /**
         * @param name queue name
         */
        public QueueEndpoint(String name) {
            super(name);
        }

        /**
         * Creates a started session
         *
         * @throws Exception fault
         */
        public void connect() throws Exception {
            mFact = createQueueConnectionFactory();
            if (getUserid() == null) {
                mCon = mFact.createQueueConnection();
            } else {
                mCon = mFact.createQueueConnection(getUserid(), getPassword());
            }
            mSession = mCon.createQueueSession(true, 0);
            mCon.start();
        }
        
        /**
         * @return number of messages in a queue as measured through a queue browser
         * @throws JMSException fault
         */
        public int queueSize() throws JMSException {
            QueueBrowser qb = mSession.createBrowser(mSession.createQueue(getName()));
            int count = 0;
            for (Enumeration en = qb.getEnumeration(); en.hasMoreElements();) {
                en.nextElement();
                count++;           
            }
            qb.close();
            return count;
        }

        /**
         * @see com.stc.jmsjca.test.core.Passthrough.Endpoint#close()
         */
        public void close() {
            safeClose(mCon);
        }

        /**
         * @see com.stc.jmsjca.test.core.Passthrough.Endpoint#receive(int)
         */
        public Message receive(int timeout) throws JMSException {
            return mConsumer.receive(timeout);
        }

        /**
         * @see com.stc.jmsjca.test.core.Passthrough.Endpoint#getSession()
         */
        public Session getSession() {
            return mSession;
        }
    }

    /**
     * A source for a Queue
     */
    public class QueueSource extends QueueEndpoint {
        private QueueSender mProducer;
        private Queue mSource;

        /**
         * @param name destination name
         */
        public QueueSource(String name) {
            super(name);
        }

        /**
         * @see com.stc.jmsjca.test.core.Passthrough.QueueEndpoint#connect()
         */
        public void connect() throws Exception {
            super.connect();
            mSource = createQueue(mSession, getName());
            mProducer = mSession.createSender(mSource);
        }

        /**
         * @see com.stc.jmsjca.test.core.Passthrough.Endpoint#send(javax.jms.Message)
         */
        public void send(Message m) throws JMSException {
            mProducer.send(m);
        }

        /**
         * @see com.stc.jmsjca.test.core.Passthrough.Endpoint#drain()
         */
        public int drain() throws JMSException {
            mConsumer = mSession.createReceiver(mSource);
            int ret = super.drain();
            mConsumer.close();
            mConsumer = null;
            return ret;
        }
    }

    /**
     * A destination for queue
     */
    public class QueueDest extends QueueEndpoint {
        Queue mDest;

        /**
         * @param name queue name
         */
        public QueueDest(String name) {
            super(name);
        }

        /**
         * @see com.stc.jmsjca.test.core.Passthrough.QueueEndpoint#connect()
         */
        public void connect() throws Exception {
            super.connect();
            mDest = createQueue(mSession, getName());
            mConsumer = mSession.createReceiver(mDest);
        }
        public void close() {
            try {
              if (mConsumer != null) {
                  mConsumer.close();
              }
              if (mSession != null) {
                  mSession.close();
              }              
            } catch (JMSException ignore) {                
            }
            super.close();
        }
        
    }


    ///////////////////////// Topic /////////////////////////////////////////////////////

    /**
     * Specializes an endpoint for a Topic (note: an alternative inheritance
     * structure would have been to derive Soure and Destination from
     * Endpoint)
     */
    public abstract class TopicEndpoint extends Endpoint {
        private TopicConnectionFactory mFact;
        private TopicConnection mCon;
        /**
         * session
         */
        protected TopicSession mSession;
        /**
         * consumer
         */
        protected TopicSubscriber mConsumer;

        /**
         * @param name destination name
         */
        public TopicEndpoint(String name) {
            super(name);
        }

        /**
         * Creates a started session
         *
         * @throws Exception fault
         */
        public void connect() throws Exception {
            mFact = createTopicConnectionFactory();
            if (getUserid() == null) {
                mCon = mFact.createTopicConnection();
            } else {
                mCon = mFact.createTopicConnection(getUserid(), getPassword());
            }
            mSession = mCon.createTopicSession(true, 0);
            mCon.start();
        }

        /**
         * @see com.stc.jmsjca.test.core.Passthrough.Endpoint#close()
         */
        public void close() {
            safeClose(mCon);
        }

        /**
         * @see com.stc.jmsjca.test.core.Passthrough.Endpoint#receive(int)
         */
        public Message receive(int timeout) throws JMSException {
            return mConsumer.receive(timeout);
        }

        /**
         * @see com.stc.jmsjca.test.core.Passthrough.Endpoint#getSession()
         */
        public Session getSession() {
            return mSession;
        }
    }

    /**
     * A source for a Topic
     */
    public class TopicSource extends TopicEndpoint {
        private TopicPublisher mProducer;
        private Topic mSource;
        private String mDurableName;

        /**
         * @param name destination name
         * @param durableName subscriber name
         */
        public TopicSource(String name, String durableName) {
            super(name);
            mDurableName = durableName;
        }

        /**
         * @see com.stc.jmsjca.test.core.Passthrough.TopicEndpoint#connect()
         */
        public void connect() throws Exception {
            super.connect();
            mSource = createTopic(mSession, getName());
            mProducer = mSession.createPublisher(mSource);
        }

        /**
         * @see com.stc.jmsjca.test.core.Passthrough.Endpoint#send(javax.jms.Message)
         */
        public void send(Message m) throws JMSException {
            mProducer.publish(m);
        }

        /**
         * @see com.stc.jmsjca.test.core.Passthrough.Endpoint#drain()
         */
        public int drain() throws JMSException {
            if (mDurableName != null) {
                mConsumer = mSession.createDurableSubscriber(mSource, mDurableName);
            } else {
                mConsumer = mSession.createSubscriber(mSource);
            }
            int ret = super.drain();
            mConsumer.close();
            mConsumer = null;
            return ret;
        }
    }

    /**
     * A destination for topic
     */
    public class TopicDest extends TopicEndpoint {
        Topic mDest;

        /**
         * @param name topic name
         */
        public TopicDest(String name) {
            super(name);
        }

        /**
         * @see com.stc.jmsjca.test.core.Passthrough.TopicEndpoint#connect()
         */
        public void connect() throws Exception {
            super.connect();
            mDest = createTopic(mSession, getName());
            mConsumer = mSession.createSubscriber(mDest);
        }
    }


    ///////////////////////// Tools /////////////////////////////////////////////////////

    /**
     * @param c connection to close
     */
    public static void safeClose(Connection c) {
        if (c != null) {
            try {
                c.close();
            } catch (JMSException ignore) {
                // ignoe
            }
        }
    }
    /**
     * @param p test object to close
     */
    public static void safeClose(Passthrough p) {
        if (p != null) {
            try {
                p.close();
            } catch (Exception ignore) {
                // ignore
            }
        }
    }

    /**
     * Closes all resources associated with this endpoint test
     */
    public void close() {
        for (Iterator iter = mThings.iterator(); iter.hasNext();/*-*/) {
            Endpoint t = (Endpoint) iter.next();
            t.close();
            iter.remove();
        }
    }

    /**
     * Returns an endpoint by name already created in this passthrough
     *
     * @param name destination name
     * @return abstract object that represents a destination
     * @throws Exception fault
     */
    public Endpoint get(String name) throws Exception {
        for (Iterator iter = mThings.iterator(); iter.hasNext();/*-*/) {
            Endpoint q = (Endpoint) iter.next();
            if (q.getName().equals(name)) {
                return q;
            }
        }
        throw new Exception("Not found: " + name);
    }

    /**
     * Returns a topic endpoint by name already created in this passthrough
     *
     * @param name destination name
     * @return abstract object that represents a destination
     * @throws Exception fault
     */
    public TopicSource getTopicSource(String name) throws Exception {
        return (TopicSource) get(name);
    }


    ///////////////////////// Test templates ////////////////////////////////////////////
    
    /**
     * Wipes all messages from the three test queues
     * 
     * @throws Exception fault
     */
    public void clearQ1Q2Q3() throws Exception {
        close();
        String[] queuenames = new String[] {mQueue1Name, mQueue2Name, mQueue3Name};
        for (int i = 0; i < queuenames.length; i++) {
            QueueDest d1 = new QueueDest(queuenames[i]);
            d1.connect();
            d1.drain();
            close();
        }
    }

    /**
     * Wipes all messages from the three test queues
     * 
     * @throws Exception fault
     */
    public void clearQ1Q2Q3Q4() throws Exception {
        close();
        String[] queuenames = new String[] {mQueue1Name, mQueue2Name, mQueue3Name, mQueue4Name};
        for (int i = 0; i < queuenames.length; i++) {
            QueueDest d1 = new QueueDest(queuenames[i]);
            d1.connect();
            d1.drain();
            close();
        }
    }

    /**
     * Sends a batch of messages to Queue1 and waits for them to appear at Queue2
     * 
     * @throws Exception fault
     */
    public void passFromQ1ToQ2() throws Exception {
        close();

        QueueSource source = new QueueSource(mQueue1Name);
        QueueDest dest = new QueueDest(mQueue2Name);
        source.connect();
        dest.connect();

        dest.drain();
        source.sendBatch(mNMsgsToSend, mBatchId, "");
        dest.readback(mNMsgsToSend, mBatchId);
    }

    /**
     * Sends a batch of messages to Queue1 and waits for them to appear at Queue2; 
     * uses all types
     * 
     * @throws Exception fault
     */
    public void passFromQ1ToQ2Mix() throws Exception {
        close();

        QueueSource source = new QueueSource(mQueue1Name);
        QueueDest dest = new QueueDest(mQueue2Name);
        source.connect();
        dest.connect();

        dest.drain();
        
        Class[] types = new Class[] {
            TextMessage.class, BytesMessage.class,
            MapMessage.class, ObjectMessage.class, StreamMessage.class, Message.class };
        for (int i = 0; i < types.length; i++) {
            setMsgType(types[i]);
            source.sendBatch(mNMsgsToSend / types.length, mBatchId, "");
            dest.readback(mNMsgsToSend / types.length, mBatchId);
        }
    }

    /**
     * Sends a batch of messages to Queue1
     * 
     * @throws Exception fault
     */
    public void passToQ1() throws Exception {
        close();

        QueueSource source = new QueueSource(mQueue1Name);
        source.connect();

        source.sendBatch(mNMsgsToSend, mBatchId, "");
    }

    /**
     * Sends a batch of messages to Queue1 and waits for them to appear at Topic2
     * 
     * @throws Exception fault
     */
    public void passFromQ1ToT2() throws Exception {
        close();

        QueueSource source = new QueueSource(mQueue1Name);
        TopicDest dest = new TopicDest(mTopic2Name);
        source.connect();
        dest.connect();

        dest.drain();
        source.sendBatch(mNMsgsToSend, mBatchId, "");
        dest.readback(mNMsgsToSend, mBatchId);
    }

    /**
     * Removes all messages from a queue
     * 
     * @param queueName destination name
     * @throws Exception fault
     */
    public void drainQueue(String queueName) throws Exception {
        QueueDest dest = new QueueDest(queueName);
        dest.connect();
        dest.drain();
        dest.close();  
    }
    
    /**
     * Removes all messages from a topic
     * 
     * @param topicName destination name
     * @param durableName durable name
     * @throws Exception fault
     */
    public void drainTopic(String topicName, String durableName) throws Exception {
        close();
        TopicSource dest = new TopicSource(topicName, durableName);
        dest.connect();
        dest.drain();
        dest.close();         
    }
    
    /**
     * Sends a batch of messages from Topic1 to Topic2
     * 
     * @throws Exception fault
     */
    public void passFromT1ToT2() throws Exception {
        close();

        TopicSource source = new TopicSource(mTopic1Name, getDurableTopic1Name1());
        TopicDest dest = new TopicDest(mTopic2Name);
        source.connect();
        dest.connect();

        dest.drain();
        source.sendBatch(mNMsgsToSend, mBatchId, "");
        dest.readback(mNMsgsToSend, mBatchId);
    }

    /**
     * Sends a batch of messages from Topic1 to Queue2
     * 
     * @throws Exception fault
     */
    public void passFromT1ToQ2() throws Exception {
        close();

        TopicSource source = new TopicSource(mTopic1Name, null);
        QueueDest dest = new QueueDest(mQueue2Name);
        source.connect();
        dest.connect();

        dest.drain();
        source.sendBatch(mNMsgsToSend, mBatchId, "");
        dest.readback(mNMsgsToSend, mBatchId);
    }

    /**
     * Sends a batch of messages from Topic1 to Queue2
     * 
     * @throws Exception fault
     */
    public void passFromT1DurableToQ2() throws Exception {
        close();

        TopicSource source = new TopicSource(mTopic1Name, getDurableTopic1Name1());
        QueueDest dest = new QueueDest(mQueue2Name);
        source.connect();
        dest.connect();

        dest.drain();
        source.sendBatch(mNMsgsToSend, mBatchId, "");
        dest.readback(mNMsgsToSend, mBatchId);
    }
    
    /**
     * Sends a batch of messages to Topic1
     * 
     * @throws Exception fault
     */
    public void sendToT1() throws Exception {
        TopicSource source = new TopicSource(mTopic1Name, null);
        source.connect();
        source.sendBatch(mNMsgsToSend, mBatchId, "");
        source.close();
    }
    
    /**
     * Reads a batch of messages from Queue2
     * 
     * @throws Exception fault
     */
    public void readFromQ2() throws Exception {
        QueueDest dest = new QueueDest(mQueue2Name);
        dest.connect();
        dest.readback(mNMsgsToSend, mBatchId);
        dest.close();
    }
    
    /**
     * Subscribes to topic
     * 
     * @return topic
     * @throws Exception fault
     */
    public TopicDest subscribeToT2() throws Exception {
        TopicDest dest = new TopicDest(mTopic2Name);
        dest.connect();
        return dest;
    }
    
    /**
     * Reads a batch of messages from a topic
     * 
     * @param dest destination
     * @throws Exception fault
     */
    public void readFromTopic(TopicDest dest) throws Exception {
        dest.readback(mNMsgsToSend, mBatchId);
        dest.close();
    }
    
    /**
     * Checks that Queue2 is empty
     * 
     * @throws Exception fault
     */
    public void assertQ2Empty() throws Exception {
        QueueDest dest = new QueueDest(mQueue2Name);
        dest.connect();
        dest.assertEmpty();
        dest.close();
    }
    
    /**
     * Checks that Topic1 is empty
     * 
     * @throws Exception fault
     */
    public void assertT1Empty() throws Exception {
        TopicDest dest = new TopicDest(mTopic1Name);
        dest.connect();
        dest.assertEmpty();
        dest.close();
    }
    
    /**
     * Removes all messages from queue2
     * 
     * @throws Exception fault
     */
    public int drainQ2() throws Exception {
        int ret;
        QueueDest dest = new QueueDest(mQueue2Name);
        dest.connect();
        ret = dest.drain();
        dest.close();
        return ret;
    }

    /**
     * Sends a batch of messages to Topic1 and receives them at Topic2
     * 
     * @throws Exception fault
     */
    public void passFromT1DurableToT2() throws Exception {
        close();

        TopicSource source = new TopicSource(mTopic1Name, getDurableTopic1Name1());
        TopicDest dest = new TopicDest(mTopic2Name);
        source.connect();
        dest.connect();

        dest.drain();
        source.sendBatch(mNMsgsToSend, mBatchId, "");
        dest.readback(mNMsgsToSend, mBatchId);
    }

    /**
     * Sends a batch of messages to Queue1 and reads them back both at Queue2 and Queue3
     * 
     * @throws Exception fault
     */
    public void passFromQ1ToQ2andQ3() throws Exception {
        close();

        QueueSource source = new QueueSource(mQueue1Name);
        QueueDest dest1 = new QueueDest(mQueue2Name);
        QueueDest dest2 = new QueueDest("Queue3");
        source.connect();
        dest1.connect();
        dest2.connect();

        dest1.drain();
        dest2.drain();
        source.sendBatch(mNMsgsToSend, mBatchId, "");
        dest1.readback(mNMsgsToSend, mBatchId);
        dest2.readback(mNMsgsToSend, mBatchId);
    }
    
    /**
     * Removes all messages from Queue1 and Queue2
     * 
     * @throws Exception fault
     */
    public void prepareQ1ToQ2() throws Exception {
        QueueSource source = new QueueSource(mQueue1Name);
        QueueDest dest1 = new QueueDest(mQueue2Name);
        source.connect();
        dest1.connect();
        source.drain();
        dest1.drain();
    }

    /**
     * @param n number of messages to send through the system
     */
    public void setNMessagesToSend(int n) {
        mNMsgsToSend = n;
    }

    /**
     * @return number of messages to send through the system
     */
    public int getNMessagesToSend() {
        return mNMsgsToSend;
    }

    /**
     * @param clientID clientID
     * @param t12 destination
     * @param subscriptionName subscriptionname
     * @throws Exception fault
     */
    public abstract void removeDurableSubscriber(String clientID, String t12, String subscriptionName) throws Exception;
}
