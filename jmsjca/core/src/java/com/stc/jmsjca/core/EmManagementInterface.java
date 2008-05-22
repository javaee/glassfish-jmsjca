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
 * This interface was designed by the EM team; each MBean exposed to EM needs to 
 * implement this interface.
 * 
 * @author EM team
 */
public interface EmManagementInterface {
    /**
     * Status
     */
    public static final String DISCONNECTED = "Down";

    /**
     * Status
     */
    public static final String CONNECTED = "Up";

    /**
     * Status
     */
    public static final String CONNECTING = "Connecting";

    /**
     * Status
     */
    public static final String DISCONNECTING = "Disconnecting";

    /**
     * start method
     *    start the component- the semantics of this operation is left to implementation
     * @throws Exception on failure
     */
    public void start() throws Exception;

    /**
     * restart method
     *    restart the component- the semantics of this operation is left to implementation
     * @throws Exception on failure
     */
    public void restart() throws Exception;

    /**
     * stop method
     *   stop the component - the semantics of this operation is left to implementation 
     * @throws Exception on failure
     */
    public void stop() throws Exception;

    /**
     * get status method
     * 
     * @return the status of the component e.g. Up/Down/Unknown
     * @throws Exception on failure
     */
    public String getStatus() throws Exception;

    /**
     * get properties method
     * 
     * @return a list of properties: name-value pairs
     */
    public Properties getProperties();

    /**
     * isStartable method
     * this method will be used to determine whether a "start" button would be presented to the user
     * 
     * @return true if the component can be started (remotely)
     */
    public Boolean isStartable();

    /**
     * isRestartable method
     * this method will be used to determine whether a "restart" button would be presented to the user
     * 
     * @return true if the componennt can be restarted
     */
    public Boolean isRestartable();

    /**
     * isStoppable method
     * this method will be used to determine whether a "stop" button would be presented to the user
     * 
     * @return true if the component can be stopped
     */
    public Boolean isStoppable();
}
