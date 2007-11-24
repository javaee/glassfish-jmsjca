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

import com.stc.jmsjca.util.MBeanBase;

/**
 * Base class for RAMBean and ActivationMBean
 *
 * @author fkieviet
 * @version $Revision: 1.5 $
 */
public class CommonMBean extends MBeanBase {
    //private static Logger sLog = Logger.getLogger(ActivationMBean.class);
    private String mJmsServerMBeanName;
    private RAJMSObjectFactory mObjectFactory;

    /**
     * Constructor
     * 
     * @param objfactory object factory
     * @param descr descr
     */
    public CommonMBean(RAJMSObjectFactory objfactory, String descr) {
        super(descr);
        mObjectFactory = objfactory;
    }
    
    /**
     * Associates this MBean with a server mgt mbean
     * 
     * @param mbeanName String
     */
    public void setJmsServerMBean(String mbeanName) {
        mJmsServerMBeanName = mbeanName;
    }

    /**
     * Returns the ObjectName of the JMS server Mgt Mbean that can be used to 
     * obtain listings of queues, etc.
     * 
     * @return ObjectName String, null if feature not supported 
     */
    public String getJMSServerMBean() {
        return mJmsServerMBeanName;
    }
    
    /**
     * Exposes the attribute as an MBean attribute
     * 
     * @return Attribute description
     */
    public String[] mbmgetJMSServerMBean() {
        return new String[] {"Objectname of the MBean that provides access to "
            + "management capabilities of the JMS server" };
    }
    
    /**
     * Returns the type of JMS server
     * 
     * @return String type 
     */
    public String getJMSServerType() {
        return mObjectFactory.getJMSServerType();
    }
    
    /**
     * Exposes the attribute as an MBean attribute
     * 
     * @return Attribute description
     */
    public String[] mbmgetJMSServerType() {
        return new String[] {"Type of JMS server serviced by this RA" };
    }
    
    /**
     * For MBean testing only
     * 
     * @param args args
     */
    public static void main(String[] args) {
        try {
            new CommonMBean(null, "").mbeanTest();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
