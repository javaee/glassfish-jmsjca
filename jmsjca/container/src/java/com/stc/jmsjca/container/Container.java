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

package com.stc.jmsjca.container;

import java.util.Properties;

/**
 * A representation of an application server that can be used for
 * 1) deploying/undeploying/redeploying applications
 * 2) access MBeans remotely
 * 
 * @author fkieviet
 */
public abstract class Container {

    /**
     * Deploys the specified module; undeploys an existing module if already
     * there.
     * 
     * @param absolutePath full path to archive, e.g. "c:\data\send.ear"
     * @throws Exception fault
     */
    public abstract void redeployModule(String absolutePath) throws Exception;

    /**
     * Undeploys the specified module
     * 
     * @param moduleName e.g. "send"
     * @throws Exception fault
     */
    public abstract void undeploy(String moduleName) throws Exception;

    /**
     * Deploys a module to the container
     * 
     * @param absolutePath path
     * @throws Exception on failure
     */
    public abstract void deployModule(String absolutePath) throws Exception;
    
    /**
     * Set Container Properties
     * @param p
     * @throws Exception
     */
    public abstract void setProperties(Properties p) throws Exception;

    /**
     * Closes a container
     * 
     * @param c container, may be null
     */
    public static void safeClose(Container c) {
        if (c != null) {
            try {
                c.close();
            } catch (Exception ignore) {
                // ignore
            }
        }
    }

    /**
     * Closes a container
     * 
     * @throws Exception fault
     */
    public abstract void close() throws Exception;

    /**
     * Extracts the module name out of a path to a path to an EAR file,
     * WAR file, etc. E.g. "c:\data\send.ear" returns "send". Also
     * "send" returns "send"
     * 
     * @param path path
     * @return module name only
     */
    public static String getModuleName(String path) {
        // Chop off extension
        int i = path.lastIndexOf('.');
        String moduleName = i > 0 ? path.substring(0, i) : path;
        
        // Chop off directory
        moduleName = moduleName.substring(path.lastIndexOf('\\') + 1);
        moduleName = moduleName.substring(path.lastIndexOf('/') + 1);
        
        return moduleName;
    }

    /**
     * Finds out if the specified WAR, EAR, etc has already been deployed.
     * 
     * @param absolutePath or module name, e.g. "c:\data\send.ear" or "send"
     * @return true if deployed
     * @throws Exception fault
     */
    public abstract boolean isDeployed(String absolutePath) throws Exception;

    /**
     * Returns an object with the specified interface; method invocations
     * on this object will result in calls to the specified MBean on a
     * method with the exact same signature as in the interface.
     * 
     * It is recommended that the methods are declared with a "throws 
     * Exception" clause.
     * 
     * Special case: methods starting with ATTRPREFIX (xget) are assumed to
     * be attributes; rather than calling the corresponding method in the 
     * MBean, a getAttribute() is invoked with the name equal to the part
     * following ATTRPREFIX. Example: xgetFile() will return an attribute
     * with the name "File"; xgetfile() will return an attribute with the 
     * name "file". 
     * 
     * @param objectName MBean to call methods on or get attributes from
     * @param itf interface
     * @return an instance with the specified interface
     * @throws Exception fault
     */
    public abstract Object getMBeanProxy(String objectName, Class<?> itf) throws Exception;

    /**
     * Get an attribute from the specified MBean
     * 
     * @param objName specifies the MBean
     * @param name of the attribute
     * @return attribute value
     * @throws Exception on failure
     */
    public abstract Object getAttribute(String objName, String name) throws Exception;
}