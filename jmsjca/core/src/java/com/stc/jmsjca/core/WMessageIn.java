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

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;

import java.util.Enumeration;

/**
 * A wrapper around a javax.jms.Message; this wrapper is given out to the 
 * MDB (this msg wrapper is only used for inbound). These wrappers are necessary for
 * batching and hold-until-ack functionality. As such it overrides the 
 * setBooleanProperty() (for setting the rollbackOnly flag) and the acknowledge() 
 * method.
 * 
 * @author Frank Kieviet
 * @version $Revision: 1.7 $
 */
public class WMessageIn implements Message, Unwrappable {
    private Message mDelegate;
    
    // Batch and HUA
    private boolean mBatchAndHua;
    private AckHandler mAckHandler;
    private int mIBatch;
    private int mBatchSize;
    private boolean mIsRollbackOnly;
    private boolean mIsAckCalled;
    
    // Stateful redelivery
    private RedeliveryStateHandler mRedeliveryStateHandler;

    private Activation mActivation;

    private static final Localizer LOCALE = Localizer.get();
    
    /**
     * Used to interface with the RedeliveryHandler
     * 
     * @author fkieviet
     */
    public interface RedeliveryStateHandler {
        /**
         * Allows a user property to be set from the MDB
         * 
         * @param key key
         * @param value value
         * @return true if the key was acceptable
         * @throws JMSException propagated from getJMSMessageID()
         */
        boolean setProperty(String key, String value) throws JMSException;
        
        /**
         * Allows a user property to be retrieved from the MDB
         * 
         * @param key key
         * @return value, or null if no such value
         * @throws JMSException propagated from getJMSMessageID()
         */
        String getProperty(String key) throws JMSException;
        
        /**
         * @return the number of times the message was seen before
         */
        int getRedeliveryCount();
    }
    
    /**
     * When used as a boolean property in setBooleanProperty() this sets the transaction
     * to rollback only (earliest release had a typo; this declaration is still there
     * to support applications depending on this string)
     */
    public static final String LEGACY_ISROLLBACKONLY = "JMSJCA.isRollbackOnly";
    
    /**
     * When used as a boolean property in setBooleanProperty() this sets the transaction
     * to rollback only
     */
    public static final String SETROLLBACKONLY = "JMSJCA.setRollbackOnly";
    
    /**
     * Name of an Object property (type Integer) that indicates the index of the message
     * in a batch. -1 indicates no batch.
     */
    public static final String IBATCH = "JMSJCA.batchIndex";
    
    /**
     * Name of an Object property (type Integer) that indicates the size of the batch
     */
    public static final String BATCHSIZE = "JMSJCA.batchSize";
    
    /**
     * Constructor
     * 
     * @param delegate real msg
     */
    public WMessageIn(Message delegate) {
        mDelegate = delegate;
    }

    /**
     * @see com.stc.jmsjca.core.Unwrappable#getWrappedObject()
     */
    public Object getWrappedObject() {
        return mDelegate;
    }
    
    /**
     * @return the wrapped object 
     */
    public Message getDelegate() {
        return mDelegate;
    }
    
    /**
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
        if (!mBatchAndHua) {
            mDelegate.acknowledge();
        } else {
            if (mAckHandler != null) {
                if (mIsAckCalled) {
                    // ignore duplicate calls
                } else {
                    mAckHandler.ack(mIsRollbackOnly, this);
                    mIsAckCalled = true;
                }
            } else {
                mDelegate.acknowledge();
            }
        }
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
    public Object getObjectProperty(String name) throws JMSException {
        // Legacy key?
        String original = name;
        if (name.startsWith(Options.MessageProperties.OLDFULLPREFIX)) {
            name = name.substring(Options.MessageProperties.OLDPREFIX.length());
        }

        if (mBatchAndHua && IBATCH.equalsIgnoreCase(name)) {
            return new Integer(mIBatch);
        } else if (mBatchAndHua && BATCHSIZE.equalsIgnoreCase(name)) {
            return new Integer(mBatchSize);
        } else if (mRedeliveryStateHandler != null 
            && Options.MessageProperties.REDELIVERYCOUNT.equalsIgnoreCase(name)) {
            return new Integer(mRedeliveryStateHandler.getRedeliveryCount());
        } else if (Options.MessageProperties.MBEANSERVER.equals(name)) {
            return mActivation.getRA().getMBeanServer();
        } else {
            return mDelegate.getObjectProperty(original);
        }
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
    public String getStringProperty(String name) throws JMSException {
        String ret = null;
        // Is using new name?
        if (name.startsWith(Options.MessageProperties.MSG_PROP_PREFIX)) {
            if (mRedeliveryStateHandler != null) {
                ret = mRedeliveryStateHandler.getProperty(name);
            }
            if (ret == null && Options.MessageProperties.MBEANNAME.equals(name)) {
                ret = mActivation.getActivationSpec().getMBeanName();
            }
            if (ret == null && Options.MessageProperties.MBEANNAME_OLD.equals(name)) {
                ret = mActivation.getActivationSpec().getMBeanName();
            }
        }

        // Is using old name?
        if (ret == null && name.startsWith(Options.MessageProperties.OLDFULLPREFIX)) {
            String newname = name.substring(Options.MessageProperties.OLDPREFIX.length());

            if (mRedeliveryStateHandler != null) {
                ret = mRedeliveryStateHandler.getProperty(newname);
            }
            if (ret == null && Options.MessageProperties.MBEANNAME.equals(newname)) {
                ret = mActivation.getActivationSpec().getMBeanName();
            }
            if (ret == null && Options.MessageProperties.MBEANNAME_OLD.equals(newname)) {
                ret = mActivation.getActivationSpec().getMBeanName();
            }
        }

        // Not found... delegate to original message
        if (ret == null) {
            ret = mDelegate.getStringProperty(name);
        }
        
        return ret;
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
    public void setBooleanProperty(String name, boolean value) throws JMSException {
        if (mBatchAndHua && (LEGACY_ISROLLBACKONLY.equalsIgnoreCase(name) || SETROLLBACKONLY.equalsIgnoreCase(name))) {
            if (!value) {
                throw Exc.jmsExc(LOCALE.x("E155: Illegal value: value must be true"));
            }
            if (mIsAckCalled) {
                throw Exc.jmsExc(LOCALE.x("E156: Cannot set isRollbackOnly after acknowledge() has been called"));
            }
            mIsRollbackOnly = true;
        } else {
            mDelegate.setBooleanProperty(name, value);
        }
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
        if (mRedeliveryStateHandler != null && mRedeliveryStateHandler.setProperty(arg0, arg1)) {
            // Set by redelivery handler
        } else {
            mDelegate.setStringProperty(arg0, arg1);
        }
    }

    /**
     * Relays the batchsize to the application through BATCHSIZE
     * 
     * @param batchSize int
     * @param ackHandler callback to call when ack() or recover() is called
     * @param iBatch index of this message in a batch; -1 for non-batched
     * @return this
     */
    public WMessageIn setBatchSize(int batchSize, AckHandler ackHandler, int iBatch) {
        mBatchAndHua = true;
        mBatchSize = batchSize;
        mAckHandler = ackHandler;
        mIBatch = iBatch;
        return this;
    }

    /**
     * Associates the redelivery state with this message wrapper
     * 
     * @param redeliveryState callback handler
     */
    public void setRedeliveryState(RedeliveryStateHandler redeliveryState) {
        mRedeliveryStateHandler = redeliveryState;
    }
    
    /**
     * Sets the activation associated with this message
     * 
     * @param a activation
     */
    public void setActivation(Activation a) {
        mActivation = a;
    }
}
