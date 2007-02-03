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

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;

import java.util.Enumeration;

/**
 * A wrapper around a javax.jms.Message; this wrapper is given out to the 
 * application code. It is necessary to wrap the message so that the getJMSReplyTo()
 * method can be overridden. This is necessary in this scenario:
 * - the application creates a temporary destination; it actually gets a wrapped 
 *   temporary destination so that the adapter can override the .delete() method.
 * - the application passes the wrapped temporary destination to the .setJMSReplyTo() 
 *   method. The JMS provider however expects a destination of a class that it knows
 *   about, and not a wrapped object.
 * Since this the setJMSReplyTo() method is the only method that needs to be overridden
 * and since this method is most likely only called on a message that was created,
 * only messages that are created through Session.createXXXMessage() are wrapped.
 * 
 * @author Frank Kieviet
 * @version $Revision: 1.1.1.3 $
 */
public class WMessageOut implements Message, Unwrappable {
    private Message mDelegate;
    
    /**
     * Constructor
     * 
     * @param delegate real msg
     */
    public WMessageOut(Message delegate) {
        mDelegate = delegate;
    }

    /**
     * @see com.stc.jmsjca.core.Unwrappable#getWrappedObject()
     */
    public Object getWrappedObject() {
        return mDelegate;
    }
    
    /**
     * SPECIAL BEHAVIOR
     * 
     * @see javax.jms.Message#setJMSReplyTo(javax.jms.Destination)
     */
    public void setJMSReplyTo(Destination dest) throws JMSException {
        if (dest instanceof Unwrappable) {
            dest = (Destination) ((Unwrappable) dest).getWrappedObject();
        }
        mDelegate.setJMSReplyTo(dest);
    }

    /**
     * @see javax.jms.Message#acknowledge()
     */
    public void acknowledge() throws JMSException {
        mDelegate.acknowledge();
    }

    /**
     * @see javax.jms.Message#clearBody()
     */
    public void clearBody() throws JMSException {
        mDelegate.clearBody();
    }

    /**
     * @see javax.jms.Message#clearProperties()
     */
    public void clearProperties() throws JMSException {
        mDelegate.clearProperties();
    }

    /**
     * @see javax.jms.Message#getBooleanProperty(java.lang.String)
     */
    public boolean getBooleanProperty(String arg0) throws JMSException {
        return mDelegate.getBooleanProperty(arg0);
    }

    /**
     * @see javax.jms.Message#getByteProperty(java.lang.String)
     */
    public byte getByteProperty(String arg0) throws JMSException {
        return mDelegate.getByteProperty(arg0);
    }

    /**
     * @see javax.jms.Message#getDoubleProperty(java.lang.String)
     */
    public double getDoubleProperty(String arg0) throws JMSException {
        return mDelegate.getDoubleProperty(arg0);
    }

    /**
     * @see javax.jms.Message#getFloatProperty(java.lang.String)
     */
    public float getFloatProperty(String arg0) throws JMSException {
        return mDelegate.getFloatProperty(arg0);
    }

    /**
     * @see javax.jms.Message#getIntProperty(java.lang.String)
     */
    public int getIntProperty(String arg0) throws JMSException {
        return mDelegate.getIntProperty(arg0);
    }

    /**
     * @see javax.jms.Message#getJMSCorrelationID()
     */
    public String getJMSCorrelationID() throws JMSException {
        return mDelegate.getJMSCorrelationID();
    }

    /**
     * @see javax.jms.Message#getJMSCorrelationIDAsBytes()
     */
    public byte[] getJMSCorrelationIDAsBytes() throws JMSException {
        return mDelegate.getJMSCorrelationIDAsBytes();
    }

    /**
     * @see javax.jms.Message#getJMSDeliveryMode()
     */
    public int getJMSDeliveryMode() throws JMSException {
        return mDelegate.getJMSDeliveryMode();
    }

    /**
     * @see javax.jms.Message#getJMSDestination()
     */
    public Destination getJMSDestination() throws JMSException {
        return mDelegate.getJMSDestination();
    }

    /**
     * @see javax.jms.Message#getJMSExpiration()
     */
    public long getJMSExpiration() throws JMSException {
        return mDelegate.getJMSExpiration();
    }

    /**
     * @see javax.jms.Message#getJMSMessageID()
     */
    public String getJMSMessageID() throws JMSException {
        return mDelegate.getJMSMessageID();
    }

    /**
     * @see javax.jms.Message#getJMSPriority()
     */
    public int getJMSPriority() throws JMSException {
        return mDelegate.getJMSPriority();
    }

    /**
     * @see javax.jms.Message#getJMSRedelivered()
     */
    public boolean getJMSRedelivered() throws JMSException {
        return mDelegate.getJMSRedelivered();
    }

    /**
     * @see javax.jms.Message#getJMSReplyTo()
     */
    public Destination getJMSReplyTo() throws JMSException {
        return mDelegate.getJMSReplyTo();
    }

    /**
     * @see javax.jms.Message#getJMSTimestamp()
     */
    public long getJMSTimestamp() throws JMSException {
        return mDelegate.getJMSTimestamp();
    }

    /**
     * @see javax.jms.Message#getJMSType()
     */
    public String getJMSType() throws JMSException {
        return mDelegate.getJMSType();
    }

    /**
     * @see javax.jms.Message#getLongProperty(java.lang.String)
     */
    public long getLongProperty(String arg0) throws JMSException {
        return mDelegate.getLongProperty(arg0);
    }

    /**
     * @see javax.jms.Message#getObjectProperty(java.lang.String)
     */
    public Object getObjectProperty(String arg0) throws JMSException {
        return mDelegate.getObjectProperty(arg0);
    }

    /**
     * @see javax.jms.Message#getPropertyNames()
     */
    public Enumeration getPropertyNames() throws JMSException {
        return mDelegate.getPropertyNames();
    }

    /**
     * @see javax.jms.Message#getShortProperty(java.lang.String)
     */
    public short getShortProperty(String arg0) throws JMSException {
        return mDelegate.getShortProperty(arg0);
    }

    /**
     * @see javax.jms.Message#getStringProperty(java.lang.String)
     */
    public String getStringProperty(String arg0) throws JMSException {
        return mDelegate.getStringProperty(arg0);
    }

    /**
     * @see javax.jms.Message#propertyExists(java.lang.String)
     */
    public boolean propertyExists(String arg0) throws JMSException {
        return mDelegate.propertyExists(arg0);
    }

    /**
     * @see javax.jms.Message#setBooleanProperty(java.lang.String, boolean)
     */
    public void setBooleanProperty(String arg0, boolean arg1) throws JMSException {
        mDelegate.setBooleanProperty(arg0, arg1);
    }

    /**
     * @see javax.jms.Message#setByteProperty(java.lang.String, byte)
     */
    public void setByteProperty(String arg0, byte arg1) throws JMSException {
        mDelegate.setByteProperty(arg0, arg1);
    }

    /**
     * @see javax.jms.Message#setDoubleProperty(java.lang.String, double)
     */
    public void setDoubleProperty(String arg0, double arg1) throws JMSException {
        mDelegate.setDoubleProperty(arg0, arg1);
    }

    /**
     * @see javax.jms.Message#setFloatProperty(java.lang.String, float)
     */
    public void setFloatProperty(String arg0, float arg1) throws JMSException {
        mDelegate.setFloatProperty(arg0, arg1);
    }

    /**
     * @see javax.jms.Message#setIntProperty(java.lang.String, int)
     */
    public void setIntProperty(String arg0, int arg1) throws JMSException {
        mDelegate.setIntProperty(arg0, arg1);
    }

    /**
     * @see javax.jms.Message#setJMSCorrelationID(java.lang.String)
     */
    public void setJMSCorrelationID(String arg0) throws JMSException {
        mDelegate.setJMSCorrelationID(arg0);
    }

    /**
     * @see javax.jms.Message#setJMSCorrelationIDAsBytes(byte[])
     */
    public void setJMSCorrelationIDAsBytes(byte[] arg0) throws JMSException {
        mDelegate.setJMSCorrelationIDAsBytes(arg0);
    }

    /**
     * @see javax.jms.Message#setJMSDeliveryMode(int)
     */
    public void setJMSDeliveryMode(int arg0) throws JMSException {
        mDelegate.setJMSDeliveryMode(arg0);
    }

    /**
     * @see javax.jms.Message#setJMSDestination(javax.jms.Destination)
     */
    public void setJMSDestination(Destination arg0) throws JMSException {
        mDelegate.setJMSDestination(arg0);
    }

    /**
     * @see javax.jms.Message#setJMSExpiration(long)
     */
    public void setJMSExpiration(long arg0) throws JMSException {
        mDelegate.setJMSExpiration(arg0);
    }

    /**
     * @see javax.jms.Message#setJMSMessageID(java.lang.String)
     */
    public void setJMSMessageID(String arg0) throws JMSException {
        mDelegate.setJMSMessageID(arg0);
    }

    /**
     * @see javax.jms.Message#setJMSPriority(int)
     */
    public void setJMSPriority(int arg0) throws JMSException {
        mDelegate.setJMSPriority(arg0);
    }

    /**
     * @see javax.jms.Message#setJMSRedelivered(boolean)
     */
    public void setJMSRedelivered(boolean arg0) throws JMSException {
        mDelegate.setJMSRedelivered(arg0);
    }

    /**
     * @see javax.jms.Message#setJMSTimestamp(long)
     */
    public void setJMSTimestamp(long arg0) throws JMSException {
        mDelegate.setJMSTimestamp(arg0);
    }

    /**
     * @see javax.jms.Message#setJMSType(java.lang.String)
     */
    public void setJMSType(String arg0) throws JMSException {
        mDelegate.setJMSType(arg0);
    }

    /**
     * @see javax.jms.Message#setLongProperty(java.lang.String, long)
     */
    public void setLongProperty(String arg0, long arg1) throws JMSException {
        mDelegate.setLongProperty(arg0, arg1);
    }

    /**
     * @see javax.jms.Message#setObjectProperty(java.lang.String, java.lang.Object)
     */
    public void setObjectProperty(String arg0, Object arg1) throws JMSException {
        mDelegate.setObjectProperty(arg0, arg1);
    }

    /**
     * @see javax.jms.Message#setShortProperty(java.lang.String, short)
     */
    public void setShortProperty(String arg0, short arg1) throws JMSException {
        mDelegate.setShortProperty(arg0, arg1);
    }

    /**
     * @see javax.jms.Message#setStringProperty(java.lang.String, java.lang.String)
     */
    public void setStringProperty(String arg0, String arg1) throws JMSException {
        mDelegate.setStringProperty(arg0, arg1);
    }
}
