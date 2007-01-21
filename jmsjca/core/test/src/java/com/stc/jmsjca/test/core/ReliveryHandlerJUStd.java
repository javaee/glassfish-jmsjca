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
 * $RCSfile: ReliveryHandlerJUStd.java,v $
 * $Revision: 1.3 $
 * $Date: 2007-01-21 17:51:59 $
 *
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.  
 */

package com.stc.jmsjca.test.core;

import com.stc.jmsjca.core.DeliveryStats;
import com.stc.jmsjca.core.RAJMSActivationSpec;
import com.stc.jmsjca.core.RedeliveryHandler;
import com.stc.jmsjca.core.RedeliveryHandler.Action;
import com.stc.jmsjca.core.RedeliveryHandler.Move;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;

import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;

public class ReliveryHandlerJUStd extends TestCase {
    
    public ReliveryHandlerJUStd() {
        
    }
    
    public ReliveryHandlerJUStd(String name) {
        super(name);
    }
    
    public void valid(String actions) throws Exception {
        boolean valid = RedeliveryHandler.checkValid(actions);
        assertTrue(valid);
        
        Action[] a = RedeliveryHandler.parse(actions, "void", Queue.class.getName());
        assertTrue(a != null);
        assertTrue(a.length > 0);
        assertTrue(a[0].getAt() == 1);
    }
    
    public void invalid(String actions) {
        boolean valid = RedeliveryHandler.checkValid(actions);
        assertTrue(!valid);
    }

    public void test001() throws Throwable {
        invalid("1;");
        invalid(":1;");
        invalid("x;");
        invalid("1:1052:20;");
        invalid("1::10;20:49;");
        invalid("1:10;52:");
        invalid("1:10;:20");
        valid("1:10;2:20");
        
        valid("1:10;");
        valid("");
        valid("1:10;2:20;");
        valid("1:10;2:20;");
        valid("1:0");
    }
    
    public void test002() throws Throwable {
        valid("1:10;2:20;30:move(queue:qname)");
        valid("1:10;2:20;30:move(topic:tname)");
        valid("1:10;2:20;30:move(same:dname)");
        valid("1:10;2:20;30:delete");
        invalid("1:10;2:20;30:move(queue)");
        invalid("1:10;2:20;30:move(queue:)");
        invalid("1:10;2:20;30:move(same:)");
    }
    
    public void testx() throws Throwable {
        Pattern p = Pattern.compile("\\(((queue)|(topic))\\)");
        Matcher m = p.matcher("(queue)");
        assertTrue(m.matches());
        
        Action[] actions =   
            RedeliveryHandler.parse("1:10; 2: 20 ; 3:500; 5: 500; 600: move(queue:dlq($))", "myQueue", Queue.class.getName());
        assertTrue(actions.length == 5);
        assertTrue(actions[0].getAt() == 1);
        assertTrue(actions[1].getAt() == 2);
        assertTrue(actions[2].getAt() == 3);
        assertTrue(actions[3].getAt() == 5);
        assertTrue(actions[4].getAt() == 600);
        
        assertTrue(actions[4] instanceof Move);
        Move move = (Move) actions[4];
        assertTrue(move.getDestinationName().equals("dlq(myQueue)"));
        assertTrue(move.isQueue());
        assertTrue(!move.isTopic());
    }

    private class Spec extends RAJMSActivationSpec {
        public boolean isValidDestinationName(String name) {
            return false;
        }
    };
    
    private class Msg implements Message {
        private String mMsgid;
        
        // Functional methods
        public Msg(String msgid) {
            mMsgid = msgid;
        }
        public String getJMSMessageID() throws JMSException {
            return mMsgid;
        }
        public boolean getJMSRedelivered() throws JMSException {
            return true;
        }
        
        // Rest of methods are not used
        public void setJMSMessageID(String arg0) throws JMSException {
        }
        public long getJMSTimestamp() throws JMSException {
            return 0;
        }
        public void setJMSTimestamp(long arg0) throws JMSException {
        }
        public byte[] getJMSCorrelationIDAsBytes() throws JMSException {
            return null;
        }
        public void setJMSCorrelationIDAsBytes(byte[] arg0) throws JMSException {
        }
        public void setJMSCorrelationID(String arg0) throws JMSException {
        }
        public String getJMSCorrelationID() throws JMSException {
            return null;
        }
        public Destination getJMSReplyTo() throws JMSException {
            return null;
        }
        public void setJMSReplyTo(Destination arg0) throws JMSException {
        }
        public Destination getJMSDestination() throws JMSException {
            return null;
        }
        public void setJMSDestination(Destination arg0) throws JMSException {
        }
        public int getJMSDeliveryMode() throws JMSException {
            return 0;
        }
        public void setJMSDeliveryMode(int arg0) throws JMSException {
        }
        public void setJMSRedelivered(boolean arg0) throws JMSException {
        }
        public String getJMSType() throws JMSException {
            return null;
        }
        public void setJMSType(String arg0) throws JMSException {
        }
        public long getJMSExpiration() throws JMSException {
            return 0;
        }
        public void setJMSExpiration(long arg0) throws JMSException {
        }
        public int getJMSPriority() throws JMSException {
            return 0;
        }
        public void setJMSPriority(int arg0) throws JMSException {
        }
        public void clearProperties() throws JMSException {
        }
        public boolean propertyExists(String arg0) throws JMSException {
            return false;
        }
        public boolean getBooleanProperty(String arg0) throws JMSException {
            return false;
        }
        public byte getByteProperty(String arg0) throws JMSException {
            return 0;
        }
        public short getShortProperty(String arg0) throws JMSException {
            return 0;
        }
        public int getIntProperty(String arg0) throws JMSException {
            return 0;
        }
        public long getLongProperty(String arg0) throws JMSException {
            return 0;
        }
        public float getFloatProperty(String arg0) throws JMSException {
            return 0;
        }
        public double getDoubleProperty(String arg0) throws JMSException {
            return 0;
        }
        public String getStringProperty(String arg0) throws JMSException {
            return null;
        }
        public Object getObjectProperty(String arg0) throws JMSException {
            return null;
        }
        public Enumeration getPropertyNames() throws JMSException {
            return null;
        }
        public void setBooleanProperty(String arg0, boolean arg1) throws JMSException {
        }
        public void setByteProperty(String arg0, byte arg1) throws JMSException {
        }
        public void setShortProperty(String arg0, short arg1) throws JMSException {
        }
        public void setIntProperty(String arg0, int arg1) throws JMSException {
        }
        public void setLongProperty(String arg0, long arg1) throws JMSException {
        }
        public void setFloatProperty(String arg0, float arg1) throws JMSException {
        }
        public void setDoubleProperty(String arg0, double arg1) throws JMSException {
        }
        public void setStringProperty(String arg0, String arg1) throws JMSException {
        }
        public void setObjectProperty(String arg0, Object arg1) throws JMSException {
        }
        public void acknowledge() throws JMSException {
        }
        public void clearBody() throws JMSException {
        }
    }
    
    public void test000() throws Throwable {
        RAJMSActivationSpec act = new Spec();
        act.setRedeliveryHandling("5:10; 6:15; 10:30; 15:move(queue:a)");
        act.setDestinationType(Queue.class.getName());
        // 1*10 + 4*15 + 5*30 = 215
        final long[] delay = new long[1];
        RedeliveryHandler c = new RedeliveryHandler(act, new DeliveryStats(), 5) {
            protected void delayMessageDelivery(Message m, Encounter e, long howLong) {
                delay[0] += howLong;
            }

            protected void deleteMessage(Message m, Encounter e) {
                // TODO Auto-generated method stub
                
            }

            protected void move(Message m, Encounter e, boolean isTopic, String destinationName, Object cookie) throws Exception {
                // TODO Auto-generated method stub
                
            }
            
        };

        // Check survival of 1
        Msg m = new Msg("1x");
        for (int i = 0; i < 50; i++) {
            c.shouldDeliver(null, m);
            c.shouldDeliver(null, new Msg("void " + i));
        }
        System.out.println(delay[0]);
        assertTrue(delay[0] == 220);
        
        // Evict msg
        m = new Msg("2x");
        delay[0] = 0;
        for (int i = 0; i < 50; i++) {
            c.shouldDeliver(null, m);
            c.shouldDeliver(null, new Msg("void " + i));
        }
        System.out.println(delay[0]);
        assertTrue(delay[0] == 220);
        
        // Check msg was evicted
        m = new Msg("1x");
        delay[0] = 0;
        for (int i = 0; i < 50; i++) {
            c.shouldDeliver(null, m);
            c.shouldDeliver(null, new Msg("void " + i));
        }
        System.out.println(delay[0]);
        assertTrue(delay[0] == 220);
        
        
    }
    
    
}
