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
 * $RCSfile: Bug6440581.java,v $
 * $Revision: 1.1.1.1 $
 * $Date: 2007-01-19 22:28:53 $
 *
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.  
 */

package com.stc.jmsjca.test.sunone;

import com.stc.jmsjca.util.Semaphore;

import javax.jms.*;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author fkieviet 
 * @version 1.0
 */
public class Bug6440581 {
    /**
     * An implementation of XID; this is used for local transactions that are really based
     * on an XA session.
     *
     * Copied from STCMS
     *
     * @author JMS Team
     * @version $Revision: 1.1.1.1 $
     */
    public static final class XXid implements Xid, Serializable {
        static byte[] ipAddress;
        static long counter = 0;
        static ServerSocket sock;
        static {
            try {
                // use local host address + bound ip port as unique
                // host/process identifier
                byte[] hostAddress = InetAddress.getLocalHost().getAddress();
                ipAddress = new byte[hostAddress.length + 2];
                System.arraycopy(hostAddress, 0, ipAddress, 0, hostAddress.length);
                sock = new ServerSocket(0);
                int port = sock.getLocalPort();
                ipAddress[ipAddress.length - 2] = (byte) ((port >> 8) & 0xF);
                ipAddress[ipAddress.length - 1] = (byte) (port & 0xF);
            } catch (Exception exc) {
                exc.printStackTrace();
            }
        }

        private int formatId = 987654;

        private byte[] branchQualifier;
        private byte[] globalTransactionId;


        static synchronized long incrCounter() {
            return ++counter;
        }

        /**
         * Constructor
         */
        public XXid() {
            branchQualifier = new byte[8 + ipAddress.length];
            int i;
            for (i = 0; i < ipAddress.length; i++) {
                branchQualifier[i] = ipAddress[i];
            }
            // add system time and counter to produce global unique id
            long count = incrCounter();
            long uid = (System.currentTimeMillis() << 12) + (count & 0xFFF);
            for (int j = 0; j < 8; j++, i++) {
                branchQualifier[i] = (byte) uid;
                uid >>= 8;
            }
            globalTransactionId = branchQualifier;
        }


        /**
         * Getter for GlobalTransactionId attribute of the XidImpl object
         *
         * @return xid
         */
        public byte[] getGlobalTransactionId() {
            return globalTransactionId;
        }

        /**
         * Setter for GlobalTransactionId attribute of the XidImpl object
         *
         * @param value global transaction id
         */
        public void setGlobalTransactionId(byte[] value) {
            globalTransactionId = value;
        }

        /**
         * Getter for BranchQualifier attribute of the XidImpl object
         *
         * @return binary xid
         */
        public byte[] getBranchQualifier() {
            return branchQualifier;
        }

        /**
         * Setter for BranchQualifier attribute of the XidImpl object
         *
         * @param value branch qualifier
         */
        public void setBranchQualifier(byte[] value) {
            branchQualifier = value;
        }

        /**
         * Getter for FormatId attribute of the XidImpl object
         *
         * @return format id
         */
        public int getFormatId() {
            return formatId;
        }

        /**
         * Setter for FormatId attribute of the XidImpl object
         *
         * @param value format id
         */
        public void setFormatId(int value) {
            formatId = value;
        }

        /**
         * See Object
         *
         * @return hash code
         */
        public int hashCode() {
            int result = 0;
            for (int i = 0; i < branchQualifier.length; i++) {
                result += (result << 3) + branchQualifier[i];
            }
            for (int i = 0; i < globalTransactionId.length; i++) {
                result += (result << 3) + globalTransactionId[i];
            }
            return result;
        }

        /**
         * See Object
         *
         * @param that other object to compare to
         * @return true if objects are equal
         */
        public boolean equals(Object that) {
            if (this == that) {
                return true;
            }
            if (!(that instanceof XXid)) {
                return false;
            }
            XXid thatXid = (XXid) that;
            for (int i = 0; i < branchQualifier.length; i++) {
                if (branchQualifier[i] != thatXid.branchQualifier[i]) {
                    return false;
                }
            }
            for (int i = 0; i < globalTransactionId.length; i++) {
                if (globalTransactionId[i] != thatXid.globalTransactionId[i]) {
                    return false;
                }
            }
            return true;
        }

        static final char hexChar(int c) {
            final String hex = "0123456789ABCDEF";
            return hex.charAt(c & 0xF);
        }

        /**
         * See Object
         *
         * @return pretty print string of this object
         */
        public String toString() {
            StringBuffer result = new StringBuffer();
            result.append("xid:");

            result.append(this.formatId);
            // format id
            result.append(":");

            for (int i = 0; i < globalTransactionId.length; i++) {
                byte b = globalTransactionId[i];
                result.append(hexChar(b >> 4));
                result.append(hexChar(b));
            }
            result.append(":");
            for (int i = 0; i < branchQualifier.length; i++) {
                byte b = branchQualifier[i];
                result.append(hexChar(b >> 4));
                result.append(hexChar(b));
            }
            return result.toString();
        }
    }

    /**
     * Behaves as a counting semaphore: allows one thread to wait for the counter
     * to reach a particular value
     * 
     * @author fkieviet
     */
    private static class ConditionVar {
        private int mCount;
        private int mMaxSeen;
        private int mTriggerAt;
       
        /**
         * Increments the counter
         */
        public synchronized void inc() {
            mCount++;
            if (mCount > mMaxSeen) {
                mMaxSeen = mCount;
            }
            if (mTriggerAt == mCount) {
                this.notifyAll();
            }
        }
        
        /**
         * Increments the counter
         */
        public synchronized void dec() {
            mCount--;
            if (mTriggerAt == mCount) {
                this.notifyAll();
            }
        }
        
        /**
         * @return current value of counter
         */
        public synchronized int current() {
            return mCount;
        }
        
        /**
         * @return maximum value seen
         */
        public synchronized int getMax() {
            return mMaxSeen;
        }
        
        /**
         * Blocks until the specified counter value has been reached
         * 
         * @param value block until value reached
         * @param timeout max wait time
         * @throws Exception on failure
         */
        public synchronized void waitForDown(int value, long timeout) throws Exception {
            mTriggerAt = value;
            for (;;) {
                this.wait(timeout);
                if (mTriggerAt <= mCount) {
                    break;
                } else {
                    throw new Exception("Timeout");
                }
            }
        }

        /**
         * Blocks until the specified counter value has been reached
         * 
         * @param value block until value reached
         * @param timeout max wait time
         * @throws Exception on failure
         */
        public synchronized void waitForUp(int value, long timeout) throws Exception {
            mTriggerAt = value;
            for (;;) {
                if (mCount >= mTriggerAt) {
                    break;
                }
                this.wait(timeout);
                if (mCount >= mTriggerAt) {
                    break;
                } else {
                    throw new Exception("Timeout");
                }
            }
        }
    }

    private static final String USERID = "admin";
    private static final String PASSWORD = "admin";

    /**
     * Implements the stop procedure for XA and connection consumers as proposed by
     * George: close the connection consumer, wait until all server sessions have been
     * returned, and close the connection.
     * 
     * This test tests message loss when going through the shut down procedure while
     * processing messages. Messages are all rolled back five times, and then a wait
     * is invoked.
     * 
     * @param testStop
     * @throws Throwable
     */
    public int doTestXACCStopCloseRolback() throws Throwable {
        final int POOLSIZE = 32;

        // Connection factory
        com.sun.messaging.XAQueueConnectionFactory fact = new com.sun.messaging.XAQueueConnectionFactory();
        fact.setProperty("imqAddressList", "mq://localhost:7676");
        
        // Msgs to process
        final int NMESSAGES = 100;
        
        // Assure that Queue1 is empty
        {
            final QueueConnection conn = fact.createQueueConnection(USERID, PASSWORD);
            conn.start();
            QueueSession session = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            QueueReceiver r = session.createReceiver(session.createQueue("Queue1"));
            int nDrained = 0;
            for (;;) {
                Message m = r.receive(250);
                if (m == null) {
                    break;
                }
                nDrained++;
            }
            System.out.println(nDrained + " msgs drained");
            conn.close();
        }
        
        // Populate Queue1
        Queue dest;
        {
            final QueueConnection conn = fact.createQueueConnection(USERID, PASSWORD);
            QueueSession session = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            dest = session.createQueue("Queue1");
            QueueSender producer = session.createSender(dest);
            for (int i = 0; i < NMESSAGES; i++) {
                TextMessage msg1 = session.createTextMessage("Msg 1 for Q1");
                producer.send(msg1);
            }
            conn.close();
        }
        
        // Keeps track of how many msgs were received
        final ConditionVar inUse = new ConditionVar();
        final ConditionVar isDead = new ConditionVar();
        final ConditionVar waiting = new ConditionVar();
        final ConditionVar endWaiter = new ConditionVar();
        final ConditionVar rolledback = new ConditionVar();

        // Define server session pool: this is a fixed sized pool with precreated 
        // sessions and blocking behavior
        final XAQueueConnection mdbconn = fact.createXAQueueConnection(USERID, PASSWORD);
        ServerSessionPool pool = new ServerSessionPool() {
            Semaphore mInPool = new Semaphore(POOLSIZE);
            ArrayList mPool = new ArrayList(POOLSIZE);
            boolean mInitialized;
            
            /**
             * Populates the pool
             */
            synchronized void init() {
                if (!mInitialized) {
                    for (int i = 0; i < POOLSIZE; i++) {
                        mPool.add(newServerSession());
                    }
                    mInitialized = true;
                }
            }
            
            /**
             * @return a new server session
             */
            ServerSession newServerSession() {
                return new ServerSession() {
                    XAQueueSession xs;
                    XXid xid;
                    ServerSession xthis = this;
                    Map msgids = new HashMap();
                    
                    /**
                     * Creates a new JMS session
                     * 
                     * @see javax.jms.ServerSession#getSession()
                     */
                    public Session getSession() throws JMSException {
                        try {
                            if (xs == null) {
                                xs = mdbconn.createXAQueueSession();
                                xs.getQueueSession().setMessageListener(new MessageListener() {
                                    public void onMessage(Message msg) {
                                        try {
                                            xs.getXAResource().end(xid, XAResource.TMSUCCESS);
                                        } catch (XAException e1) {
                                            e1.printStackTrace();
                                        }
                                        
                                        String msgid = null;
                                        try {
                                            msgid = msg.getJMSMessageID();
                                        } catch (JMSException e1) {
                                            e1.printStackTrace();
                                        }
                                        
                                        // Find out if a msg needs to be delayed: a msg
                                        // needs to be delayed if it has been rolled
                                        // back 5 times
                                        int[] cnt;
                                        synchronized (msgids) {
                                            cnt = (int[]) msgids.get(msgid); 
                                        }
                                        if (cnt == null) {
                                            cnt = new int[1];
                                            synchronized (msgids) {
                                                msgids.put(msgid, cnt);
                                            }
                                        }
                                        cnt[0]++;
                                        if (cnt[0] == 5) {
                                            waiting.inc();
                                            try {
                                                endWaiter.waitForUp(1, 30000);
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        }

                                        try {
                                            xs.getXAResource().start(xid, XAResource.TMJOIN);
                                        } catch (XAException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });
                            }
                            return xs;
                        } catch (Exception e) {
                            System.out.println("Unexpected in getSession(): " + e);
                            throw new JMSException("Error: " + e);
                        } 
                    }

                    /**
                     * Called by the JMS client runtime to indicate the container should
                     * process the messages. By lack of a container, this creates a new
                     * thread that does the processing.
                     * 
                     * @see javax.jms.ServerSession#start()
                     */
                    public void start() throws JMSException {
                        new Thread() {
                            /**
                             * Processes the msgs; handles XA
                             * 
                             * @see java.lang.Runnable#run()
                             */
                            public void run() {
                                try {
                                    xid = new XXid();
                                    xs.getXAResource().start(xid, XAResource.TMNOFLAGS);
                                    xs.run();
                                    xs.getXAResource().end(xid, XAResource.TMSUCCESS);
                                    xs.getXAResource().rollback(xid);
                                    rolledback.inc();
                                    returnToPool(xthis);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }.start();
                    }
                };
            }
            
            /**
             * @see javax.jms.ServerSessionPool#getServerSession()
             */
            public ServerSession getServerSession() throws JMSException {
                synchronized (this) {
                    if (isDead.current() == 1) {
                        throw new JMSException("Consumer has already shut down");
                    }
                    inUse.inc();
                }

                init();
                try {
                    if (!mInPool.attempt(60000)) {
                        inUse.dec();
                        throw new Exception("Timeout");
                    }
                } catch (Exception e) {
                    throw new JMSException("getServerSession failure: " + e);
                }
                synchronized (this) {
                    return (ServerSession) mPool.remove(mPool.size() - 1);
                }
            }
            
            /**
             * Returns a server session to the pool
             * 
             * @param s ServerSession
             */
            void returnToPool(ServerSession s) {
                synchronized(this) {
                    mPool.add(s);
                }
                mInPool.release();
                inUse.dec();
            }
        };

        // ---- TEST STARTS HERE ----
        ConnectionConsumer ccx = mdbconn.createConnectionConsumer(dest, null, pool, 1);
        mdbconn.start();
        
        // Wait until message processing is fully going
        if (false) {
            // The test passes if all the threads are stuck waiting
            waiting.waitForUp(POOLSIZE, 30000);
        } else {
            // The test fails if close() is called while processing
            rolledback.waitForUp(5, 30000);
        }
        System.out.println("All threads are waiting: " + waiting.current());
        
        ccx.close();
        isDead.inc();
        System.out.println("Triggering end");
        endWaiter.inc();
        System.out.println("Waiting for inuse=0");
        inUse.waitForDown(0, 3000);
        System.out.println("Closing connection");
        mdbconn.close();
        System.out.println("Connection closed");
        
        // Check indoubt
        {
            final XAQueueConnection conn = fact.createXAQueueConnection(USERID, PASSWORD);
            conn.start();
            XAQueueSession session = conn.createXAQueueSession();
            Xid[] xids = session.getXAResource().recover(XAResource.TMSTARTRSCAN);
            System.out.println("Recover: " + xids.length);
            conn.close();
        }

        // Drain remaining messages
        int nDrained = 0;
        {
            final QueueConnection conn = fact.createQueueConnection(USERID, PASSWORD);
            conn.start();
            QueueSession session = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            QueueReceiver r = session.createReceiver(session.createQueue("Queue1"));
            for (;;) {
                Message m = r.receive(250);
                if (m == null) {
                    break;
                }
                nDrained++;
            }
            System.out.println(nDrained + " msgs drained");
            conn.close();
        }
        
        System.out.println("Rolledback: " + rolledback.current());
        
        assertTrue("Msg found: " + nDrained, nDrained == NMESSAGES);
        
        return inUse.getMax();
    }
    
    private void assertTrue(String string, boolean b) {
        if (!b) {
            throw new RuntimeException(string);
        }
        
    }

    public void testXACCStopCloseRolback() throws Throwable {
        doTestXACCStopCloseRolback();
    }
    
    public static void main(String[] args) {
        try {
            new Bug6440581().testXACCStopCloseRolback();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
