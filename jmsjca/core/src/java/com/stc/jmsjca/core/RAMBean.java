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

import java.util.Properties;

/**
 * An MBean that is associated with a jmsjca connector
 * 
 * @author fkieviet
 */
public class RAMBean extends CommonMBean implements EmManagementInterface {
    private RAJMSResourceAdapter mAdapter;

    /**
     * Constructor
     * 
     * @param objfactory object factory
     * @param adapter RAJMS
     */
    public RAMBean(RAJMSObjectFactory objfactory, RAJMSResourceAdapter adapter) {
        this(objfactory, adapter, "Provides statistics and management capabilities for "
            + "JMSJCA adapters.");
    }

    /**
     * Constructor for overriding classes
     * 
     * @param objfact object factory
     * @param adapter RAJMS
     * @param description describes the mbean
     */
    protected RAMBean(RAJMSObjectFactory objfact, RAJMSResourceAdapter adapter, String description) {
        super(objfact, description);
        mAdapter = adapter;
    }
    
    /**
     * @return String
     */
    public String getRAInfo() {
        return mAdapter.dumpConfiguration();
    }
    
    /**
     * Exposes the attribute as an MBean attribute
     * 
     * @return Attribute description
     */
    public String mbaRAInfo() {
        return "Configuration information of the RA";
    }

    /**
     * Dumps diagnostics runtime state of this RA's MCFs
     * 
     * @return String
     */
    public String getConnectionFactoriesInfo() {
        return mAdapter.dumpMCFInfo();
    }
    
    /**
     * Exposes the attribute as an MBean attribute
     * 
     * @return Attribute description
     */
    public String[] mbmgetConnectionFactoriesInfo() {
        return new String[] { 
            "Dumps runtime state of Connection factories associated with this RA" 
        };
    }

    /**
     * @see com.stc.jmsjca.core.EmManagementInterface#start()
     */
    public String[] mbmstart() {
        return new String[] {"Required management method for the EmManagementInterface; " 
            + "this method is INVALID for this mbean." };
    }

    /**
     * @see com.stc.jmsjca.core.EmManagementInterface#start()
     */
    public void start() throws Exception {
        throw new Exception("Method is invalid for this MBean");
    }

    /**
     * @see com.stc.jmsjca.core.EmManagementInterface#restart()
     */
    public String[] mbmrestart() {
        return new String[] {"Required management method for the EmManagementInterface; " 
            + "this method is INVALID for this mbean." };
    }

    /**
     * @see com.stc.jmsjca.core.EmManagementInterface#restart()
     */
    public void restart() throws Exception {
        throw new Exception("Method is invalid for this MBean");
    }

    /**
     * @see com.stc.jmsjca.core.EmManagementInterface#stop()
     */
    public String[] mbmstop() {
        return new String[] {"Required management method for the EmManagementInterface; " 
            + "this method is INVALID for this mbean." };
    }

    /**
     * @see com.stc.jmsjca.core.EmManagementInterface#stop()
     */
    public void stop() throws Exception {
        throw new Exception("Method is invalid for this MBean");
    }

    /**
     * @see com.stc.jmsjca.core.EmManagementInterface#getStatus()
     */
    public String[] mbmgetStatus() {
        return new String[] {"Required management method for the EmManagementInterface; " 
            + "this method is INVALID for this mbean." };
    }

    /**
     * @see com.stc.jmsjca.core.EmManagementInterface#getStatus()
     */
    public String getStatus() throws Exception {
        throw new Exception("Method is invalid for this MBean");
    }

    /**
     * @see com.stc.jmsjca.core.EmManagementInterface#getProperties()
     */
    public String[] mbmgetProperties() {
        return new String[] {"Required management method for the EmManagementInterface; " 
            + "returns an empty properties set" };
    }

    /**
     * @see com.stc.jmsjca.core.EmManagementInterface#getProperties()
     */
    public Properties getProperties() {
        return new Properties();
    }

    /**
     * @see com.stc.jmsjca.core.EmManagementInterface#isStartable()
     */
    public String[] mbmisStartable() {
        return new String[] {"Required management method for the EmManagementInterface; " 
            + "will return false for this MBean" };
    }

    /**
     * @see com.stc.jmsjca.core.EmManagementInterface#isStartable()
     */
    public Boolean isStartable() {
        return Boolean.FALSE;
    }

    /**
     * @see com.stc.jmsjca.core.EmManagementInterface#isRestartable()
     */
    public String[] mbmisRestartable() {
        return new String[] {"Required management method for the EmManagementInterface; " 
            + "returns false for this MBean." };
    }

    /**
     * @see com.stc.jmsjca.core.EmManagementInterface#isRestartable()
     */
    public Boolean isRestartable() {
        return Boolean.FALSE;
    }

    /**
     * @see com.stc.jmsjca.core.EmManagementInterface#isStoppable()
     */
    public String[] mbmisStoppable() {
        return new String[] {"Required management method for the EmManagementInterface; " 
            + "returns false for this mbean" };
    }

    /**
     * @see com.stc.jmsjca.core.EmManagementInterface#isStoppable()
     */
    public Boolean isStoppable() {
        return Boolean.FALSE;
    }
}
