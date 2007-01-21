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
 * $RCSfile: RTSContainer.java,v $
 * $Revision: 1.1.1.2 $
 * $Date: 2007-01-21 07:52:11 $
 *
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.  
 */

package com.stc.jmsjca.container.rts;

import com.stc.jmsjca.container.Container;

import java.util.Properties;

/**
 * Container implementation for RTS
 * 
 * @author fkieviet
 */
public class RTSContainer extends Container {
    private com.stc.rts.deploy.Container mDelegate;
    
    /**
     * Default Constructor
     * 
     */
    public RTSContainer() {
    }
    
    /* (non-Javadoc)
     * @see com.stc.jmsjca.container.Container#setProperties(java.util.Properties)
     */
    public void setProperties(Properties p) throws Exception {
        mDelegate = new com.stc.rts.deploy.Container(p);
    }
    
    /**
     * @see com.stc.jmsjca.container.Container#redeployModule(java.lang.String)
     */
    public void redeployModule(String absolutePath) throws Exception {
        long t0 = System.currentTimeMillis();
        System.out.println("Redeploying " + absolutePath);
        mDelegate.redeployModule(absolutePath);
        System.out.println("Redeployment complete: " + (System.currentTimeMillis() - t0) + " ms");
    }

    /**
     * @see com.jmsjca.container.Container#undeploy(java.lang.String)
     */
    public void undeploy(String moduleName) throws Exception {
        long t0 = System.currentTimeMillis();
        System.out.println("Undeploying " + moduleName);
        mDelegate.undeploy(moduleName);
        System.out.println("Undeployment complete: " + (System.currentTimeMillis() - t0) + " ms");
    }

    /**
     * @see com.jmsjca.container.Container#deployModule(java.lang.String)
     */
    public void deployModule(String absolutePath) throws Exception {
        long t0 = System.currentTimeMillis();
        System.out.println("Deploying " + absolutePath);
        mDelegate.deployModule(absolutePath);
        System.out.println("Deployment complete: " + (System.currentTimeMillis() - t0) + " ms");
    }

    /**
     * @see com.jmsjca.container.Container#close()
     */
    public void close() throws Exception {
        if (mDelegate != null) {
            mDelegate.close();
            mDelegate = null;
        }
    }

    /**
     * @see com.jmsjca.container.Container#isDeployed(java.lang.String)
     */
    public boolean isDeployed(String absolutePath) throws Exception {
        return mDelegate.isDeployed(absolutePath);
    }

    /**
     * @see com.jmsjca.container.Container#getMBeanProxy(java.lang.String, java.lang.Class)
     */
    public Object getMBeanProxy(String objectName, Class itf) throws Exception {
        return mDelegate.getMBeanProxy(objectName, itf);
    }

    /**
     * @see com.jmsjca.container.Container#getAttribute(java.lang.String, java.lang.String)
     */
    public Object getAttribute(String objName, String name) throws Exception {
        return mDelegate.getAttribute(objName, name);
    }

}