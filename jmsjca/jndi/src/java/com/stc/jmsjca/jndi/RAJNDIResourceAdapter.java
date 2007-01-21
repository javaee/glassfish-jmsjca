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
 * $RCSfile: RAJNDIResourceAdapter.java,v $
 * $Revision: 1.1.1.2 $
 * $Date: 2007-01-21 07:52:11 $
 *
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.  
 */

package com.stc.jmsjca.jndi;

import com.stc.jmsjca.core.RAJMSObjectFactory;
import com.stc.jmsjca.util.Str;

/**
 * <p>From the spec:
 * This represents a resource adapter instance and contains operations for lifecycle
 * management and message endpoint setup. A concrete implementation of this interface
 * is required to be a JavaBean</p>
 *
 * @author Frank Kieviet
 * @version $Revision: 1.1.1.2 $
 */
public class RAJNDIResourceAdapter extends com.stc.jmsjca.core.RAJMSResourceAdapter {
//    private static Logger sLog = Logger.getLogger(RAJNDIResourceAdapter.class);
    private String mQueueCFJndiName;
    private String mTopicCFJndiName;
    private String mUnifiedCFJndiName;
    private String mInitialContextFactory;
    private String mProviderUrl;
    
    /**
     * Name of CF when specifying it in properties (this is the property name)
     */
    public static final String QUEUECF = "JMSJCA.QueueCF";
    
    /**
     * Name of CF when specifying it in properties (this is the property name)
     */
    public static final String TOPICCF = "JMSJCA.TopicCF";

    /**
     * Name of CF when specifying it in properties (this is the property name)
     */
    public static final String UNIFIEDCF = "JMSJCA.UnifiedCF";

    /**
     * Default constructor (required by spec)
     */
    public RAJNDIResourceAdapter() {
    }


    // Configuration properties

    /**
     * The factory specified by the JNDI name will be used to create connections.
     *
     * @param name JNDI name of delegate connection factory
     */
    public void setQueueConnectionFactoryJndiName(String name) {
        mQueueCFJndiName = name;
    }

    /**
     * The factory specified by the JNDI name will be used to create connections.
     *
     * @param name JNDI name of delegate connection factory
     */
    public void setTopicConnectionFactoryJndiName(String name) {
        mTopicCFJndiName = name;
    }

    /**
     * The factory specified by the JNDI name will be used to create connections.
     *
     * @param name JNDI name of delegate connection factory
     */
    public void setUnifiedConnectionFactoryJndiName(String name) {
        mUnifiedCFJndiName = name;
    }

    /**
     * Sets the factory for the delegate connection factories
     *
     * @param initialContextFactory factory
     */
    public void setInitialContextFactory(String initialContextFactory) {
        mInitialContextFactory = initialContextFactory;
    }

    /**
     * Sets the factory for the delegate connection factories
     *
     * @param providerUrl factory
     */
    public void setProviderUrl(String providerUrl) {
        mProviderUrl = providerUrl;
    }

    /**
     * @return Returns the initialContextFactory.
     */
    public final String getInitialContextFactory() {
        return this.mInitialContextFactory;
    }
    /**
     * @return Returns the jmsProviderFactoryQueueXAJndiName.
     */
    public final String getQueueConnectionFactoryJndiName() {
        return this.mQueueCFJndiName;
    }
    /**
     * @return Returns the jmsProviderFactoryTopicXAJndiName.
     */
    public final String getTopicConnectionFactoryJndiName() {
        return this.mTopicCFJndiName;
    }
    /**
     * @return Returns the jmsProviderFactoryXAJndiName.
     */
    public final String getUnifiedConnectionFactoryJndiName() {
        return this.mUnifiedCFJndiName;
    }
    /**
     * @return Returns the providerUrl.
     */
    public final String getProviderUrl() {
        return this.mProviderUrl;
    }

    /**
     * equals
     *
     * @param other Object
     * @return boolean
     */
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof RAJNDIResourceAdapter)) {
            return false;
        }
        RAJNDIResourceAdapter that = (RAJNDIResourceAdapter) other;

        return Str.isEqual(this.mQueueCFJndiName, that.mQueueCFJndiName)
            && Str.isEqual(this.mTopicCFJndiName, that.mTopicCFJndiName)
            && Str.isEqual(this.mUnifiedCFJndiName, that.mUnifiedCFJndiName)
            && Str.isEqual(this.mProviderUrl, that.mProviderUrl);
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        int ret = super.hashCode();
        ret = Str.hash(ret, mQueueCFJndiName);
        ret = Str.hash(ret, mTopicCFJndiName);
        ret = Str.hash(ret, mUnifiedCFJndiName);
        ret = Str.hash(ret, mProviderUrl);
        return ret;
    }

    /**
     * @see com.stc.jmsjca.core.RAJMSResourceAdapter#createObjectFactory(java.lang.String)
     */
    public RAJMSObjectFactory createObjectFactory(String urlstr) {
        return new RAJNDIObjectFactory();
    }
}
