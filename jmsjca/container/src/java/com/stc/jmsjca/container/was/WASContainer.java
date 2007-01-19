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
 * $RCSfile: WASContainer.java,v $
 * $Revision: 1.1.1.1 $
 * $Date: 2007-01-19 22:28:53 $
 *
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.  
 */

package com.stc.jmsjca.container.was;

import java.util.Properties;

import com.stc.jmsjca.container.Container;

/**
 * A container for IBM WebSphere AS 6.0
 * 
 * @author cye
 */
public class WASContainer extends Container {
    private  Container mDelegate;

    /**
     * Identifies this type of container 
     */
    public static final String ID = "wmq";

    /**
     * Constructor
     * @param impl
     */
    public WASContainer(Container impl) {
        mDelegate = impl;        
    }
    
    /* (non-Javadoc)
     * @see com.stc.jmsjca.container.Container#setProperties(java.util.Properties)
     */
    public void setProperties(Properties p) throws Exception {
        // TODO:
    }
    
   /**
    * @see com.stc.jmsjca.container.Container#redeployModule(java.lang.String)
    */
   public void redeployModule(String absolutePath) throws Exception {
       //TODO
       // Get rid of warning
       mDelegate = mDelegate == mDelegate ? mDelegate : mDelegate;
   }

   /**
    * @see com.stc.jmsjca.container.Container#undeploy(java.lang.String)
    */
   public void undeploy(String moduleName) throws Exception {
       //TODO
   }

   /**
    * @see com.stc.jmsjca.container.Container#deployModule(java.lang.String)
    */
   public void deployModule(String absolutePath) throws Exception {
       //TODO
   }

   /**
    * @see com.stc.jmsjca.container.Container#close()
    */
   public void close() throws Exception {
       //TODO
   }

   /**
    * @see com.stc.jmsjca.container.Container#isDeployed(java.lang.String)
    */
   public boolean isDeployed(String absolutePath) throws Exception {
       //TODO
       return false;
   }

   /**
    * @see com.stc.jmsjca.container.Container#getMBeanProxy(java.lang.String, java.lang.Class)
    */
   public Object getMBeanProxy(String objectName, Class itf) throws Exception {
       //TODO
       return null;
   }

   /**
    * @see com.stc.jmsjca.container.Container#getAttribute(java.lang.String, java.lang.String)
    */
   public Object getAttribute(String objName, String name) throws Exception {
       //TODO
       return null;
   }
}
