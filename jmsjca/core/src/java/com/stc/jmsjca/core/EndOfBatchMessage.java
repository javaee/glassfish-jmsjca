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

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;

import java.util.Enumeration;

/**
 * Signals the end of a batch of messages
 *
 * @author Frank Kieviet
 * @version $Revision: 1.9 $
 */
public class EndOfBatchMessage implements Message {
    private static final Localizer LOCALE = Localizer.get();

    /**
     * Property used to signal an end of batch
     */
    public static final String KEY_ENDOFBATCH = "JMSJCA.EndOfBatch";
    
    /**
     * @see javax.jms.Message#setJMSReplyTo(javax.jms.Destination)
     */
    public void setJMSReplyTo(Destination dest) throws JMSException {
    }

    /**
     * @see javax.jms.Message#acknowledge()
     */
    public void acknowledge() throws JMSException {
        throw Exc.jmsExc(LOCALE.x("E123: Invalid method call"));
    }

    /**
     * @see javax.jms.Message#clearBody()
     */
    public void clearBody() throws JMSException {
    }

    /**
     * @see javax.jms.Message#clearProperties()
     */
    public void clearProperties() throws JMSException {
    }

    /**
     * @see javax.jms.Message#getBooleanProperty(java.lang.String)
     */
    public boolean getBooleanProperty(String name) throws JMSException {
        throw Exc.jmsExc(LOCALE.x("E124: Invalid property name [{0}]", name));
    }

    /**
     * @see javax.jms.Message#getByteProperty(java.lang.String)
     */
    public byte getByteProperty(String name) throws JMSException {
        throw Exc.jmsExc(LOCALE.x("E124: Invalid property name [{0}]", name));
    }

    /**
     * @see javax.jms.Message#getDoubleProperty(java.lang.String)
     */
    public double getDoubleProperty(String name) throws JMSException {
        throw Exc.jmsExc(LOCALE.x("E124: Invalid property name [{0}]", name));
    }

    /**
     * @see javax.jms.Message#getFloatProperty(java.lang.String)
     */
    public float getFloatProperty(String name) throws JMSException {
        throw Exc.jmsExc(LOCALE.x("E124: Invalid property name [{0}]", name));
    }

    /**
     * @see javax.jms.Message#getIntProperty(java.lang.String)
     */
    public int getIntProperty(String name) throws JMSException {
        throw Exc.jmsExc(LOCALE.x("E124: Invalid property name [{0}]", name));
    }

    /**
     * @see javax.jms.Message#getJMSCorrelationID()
     */
    public String getJMSCorrelationID() throws JMSException {
        return null;
    }

    /**
     * @see javax.jms.Message#getJMSCorrelationIDAsBytes()
     */
    public byte[] getJMSCorrelationIDAsBytes() throws JMSException {
        return new byte[0];
    }

    /**
     * @see javax.jms.Message#getJMSDeliveryMode()
     */
    public int getJMSDeliveryMode() throws JMSException {
        return DeliveryMode.NON_PERSISTENT;
    }

    /**
     * @see javax.jms.Message#getJMSDestination()
     */
    public Destination getJMSDestination() throws JMSException {
        return null;
    }

    /**
     * @see javax.jms.Message#getJMSExpiration()
     */
    public long getJMSExpiration() throws JMSException {
        return -1;
    }

    /**
     * @see javax.jms.Message#getJMSMessageID()
     */
    public String getJMSMessageID() throws JMSException {
        return null;
    }

    /**
     * @see javax.jms.Message#getJMSPriority()
     */
    public int getJMSPriority() throws JMSException {
        return 4;
    }

    /**
     * @see javax.jms.Message#getJMSRedelivered()
     */
    public boolean getJMSRedelivered() throws JMSException {
        return false;
    }

    /**
     * @see javax.jms.Message#getJMSReplyTo()
     */
    public Destination getJMSReplyTo() throws JMSException {
        return null;
    }

    /**
     * @see javax.jms.Message#getJMSTimestamp()
     */
    public long getJMSTimestamp() throws JMSException {
        return -1;
    }

    /**
     * @see javax.jms.Message#getJMSType()
     */
    public String getJMSType() throws JMSException {
        return null;
    }

    /**
     * @see javax.jms.Message#getLongProperty(java.lang.String)
     */
    public long getLongProperty(String name) throws JMSException {
        throw Exc.jmsExc(LOCALE.x("E124: Invalid property name [{0}]", name));
    }

    /**
     * @see javax.jms.Message#getObjectProperty(java.lang.String)
     */
    public Object getObjectProperty(String name) throws JMSException {
        if (KEY_ENDOFBATCH.equals(name)) {
            return Boolean.TRUE;
        }
        throw Exc.jmsExc(LOCALE.x("E124: Invalid property name [{0}]", name));
    }

    /**
     * @see javax.jms.Message#getPropertyNames()
     */
    public Enumeration<?> getPropertyNames() throws JMSException {
        throw Exc.jmsExc(LOCALE.x("E125: Method not implemented"));
    }

    /**
     * @see javax.jms.Message#getShortProperty(java.lang.String)
     */
    public short getShortProperty(String name) throws JMSException {
        throw Exc.jmsExc(LOCALE.x("E124: Invalid property name [{0}]", name));
    }

    /**
     * @see javax.jms.Message#getStringProperty(java.lang.String)
     */
    public String getStringProperty(String name) throws JMSException {
        throw Exc.jmsExc(LOCALE.x("E124: Invalid property name [{0}]", name));
    }

    /**
     * @see javax.jms.Message#propertyExists(java.lang.String)
     */
    public boolean propertyExists(String arg0) throws JMSException {
        return false;
    }

    /**
     * @see javax.jms.Message#setBooleanProperty(java.lang.String, boolean)
     */
    public void setBooleanProperty(String name, boolean value) throws JMSException {
        throw Exc.jmsExc(LOCALE.x("E126: Message is read-only"));
    }

    /**
     * @see javax.jms.Message#setByteProperty(java.lang.String, byte)
     */
    public void setByteProperty(String arg0, byte arg1) throws JMSException {
        throw Exc.jmsExc(LOCALE.x("E126: Message is read-only"));
    }

    /**
     * @see javax.jms.Message#setDoubleProperty(java.lang.String, double)
     */
    public void setDoubleProperty(String arg0, double arg1) throws JMSException {
        throw Exc.jmsExc(LOCALE.x("E126: Message is read-only"));
    }

    /**
     * @see javax.jms.Message#setFloatProperty(java.lang.String, float)
     */
    public void setFloatProperty(String arg0, float arg1) throws JMSException {
        throw Exc.jmsExc(LOCALE.x("E126: Message is read-only"));
    }

    /**
     * @see javax.jms.Message#setIntProperty(java.lang.String, int)
     */
    public void setIntProperty(String arg0, int arg1) throws JMSException {
        throw Exc.jmsExc(LOCALE.x("E126: Message is read-only"));
    }

    /**
     * @see javax.jms.Message#setJMSCorrelationID(java.lang.String)
     */
    public void setJMSCorrelationID(String arg0) throws JMSException {
        throw Exc.jmsExc(LOCALE.x("E126: Message is read-only"));
    }

    /**
     * @see javax.jms.Message#setJMSCorrelationIDAsBytes(byte[])
     */
    public void setJMSCorrelationIDAsBytes(byte[] arg0) throws JMSException {
        throw Exc.jmsExc(LOCALE.x("E126: Message is read-only"));
    }

    /**
     * @see javax.jms.Message#setJMSDeliveryMode(int)
     */
    public void setJMSDeliveryMode(int arg0) throws JMSException {
        throw Exc.jmsExc(LOCALE.x("E126: Message is read-only"));
    }

    /**
     * @see javax.jms.Message#setJMSDestination(javax.jms.Destination)
     */
    public void setJMSDestination(Destination arg0) throws JMSException {
        throw Exc.jmsExc(LOCALE.x("E126: Message is read-only"));
    }

    /**
     * @see javax.jms.Message#setJMSExpiration(long)
     */
    public void setJMSExpiration(long arg0) throws JMSException {
        throw Exc.jmsExc(LOCALE.x("E126: Message is read-only"));
    }

    /**
     * @see javax.jms.Message#setJMSMessageID(java.lang.String)
     */
    public void setJMSMessageID(String arg0) throws JMSException {
        throw Exc.jmsExc(LOCALE.x("E126: Message is read-only"));
    }

    /**
     * @see javax.jms.Message#setJMSPriority(int)
     */
    public void setJMSPriority(int arg0) throws JMSException {
        throw Exc.jmsExc(LOCALE.x("E126: Message is read-only"));
    }

    /**
     * @see javax.jms.Message#setJMSRedelivered(boolean)
     */
    public void setJMSRedelivered(boolean arg0) throws JMSException {
        throw Exc.jmsExc(LOCALE.x("E126: Message is read-only"));
    }

    /**
     * @see javax.jms.Message#setJMSTimestamp(long)
     */
    public void setJMSTimestamp(long arg0) throws JMSException {
        throw Exc.jmsExc(LOCALE.x("E126: Message is read-only"));
    }

    /**
     * @see javax.jms.Message#setJMSType(java.lang.String)
     */
    public void setJMSType(String arg0) throws JMSException {
        throw Exc.jmsExc(LOCALE.x("E126: Message is read-only"));
    }

    /**
     * @see javax.jms.Message#setLongProperty(java.lang.String, long)
     */
    public void setLongProperty(String arg0, long arg1) throws JMSException {
        throw Exc.jmsExc(LOCALE.x("E126: Message is read-only"));
    }

    /**
     * @see javax.jms.Message#setObjectProperty(java.lang.String, java.lang.Object)
     */
    public void setObjectProperty(String arg0, Object arg1) throws JMSException {
        throw Exc.jmsExc(LOCALE.x("E126: Message is read-only"));
    }

    /**
     * @see javax.jms.Message#setShortProperty(java.lang.String, short)
     */
    public void setShortProperty(String arg0, short arg1) throws JMSException {
        throw Exc.jmsExc(LOCALE.x("E126: Message is read-only"));
    }

    /**
     * @see javax.jms.Message#setStringProperty(java.lang.String, java.lang.String)
     */
    public void setStringProperty(String arg0, String arg1) throws JMSException {
        throw Exc.jmsExc(LOCALE.x("E126: Message is read-only"));
    }
}
