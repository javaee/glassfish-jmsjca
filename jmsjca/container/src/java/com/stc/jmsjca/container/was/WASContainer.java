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

package com.stc.jmsjca.container.was;

import java.util.Properties;
import java.io.File;

import javax.enterprise.deploy.shared.ModuleType;
import javax.enterprise.deploy.spi.DeploymentManager;
import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.spi.exceptions.DeploymentManagerCreationException;
import javax.enterprise.deploy.spi.status.DeploymentStatus;
import javax.enterprise.deploy.spi.status.ProgressEvent;
import javax.enterprise.deploy.spi.status.ProgressListener;
import javax.enterprise.deploy.spi.status.ProgressObject;

import com.ibm.ws.management.application.j2ee.deploy.spi.factories.DeploymentFactoryImpl;
import com.stc.jmsjca.container.Container;

/**
 * A container for IBM WebSphere AS 6.0
 * 
 * @author cye
 */
public class WASContainer extends Container {
    public static final String PROP_HOST = "jmsjca.was.host";
    public static final String PROP_PORT = "jmsjca.was.adminport";
    public static final String PROP_USERID = "jmsjca.was.userid";
    public static final String PROP_PASSWORD = "jmsjca.was.password";
    public static final String PROP_INSTALL_ROOT = "jmsjca.was.install.dir";

    /**
     * Identifies this type of container 
     */
    public static final String ID = "was";
    /**
     * Access point to the container development functionality 
     */
    private DeploymentManager mDeploymentManager;
    /**
     * An association between a server or group of servers and a location 
     * to deploy applications
     */
    private Target[] mTargetList;

    /**
     * Constructor
     */
    public WASContainer() {
    }
    
    /* (non-Javadoc)
     * @see com.stc.jmsjca.container.Container#setProperties(java.util.Properties)
     */
    public void setProperties(Properties p) throws Exception {
        String host = p.getProperty(PROP_HOST);
        String port = p.getProperty(PROP_PORT);
        String user = p.getProperty(PROP_USERID);
        String password = p.getProperty(PROP_PASSWORD);
        String root = p.getProperty(PROP_INSTALL_ROOT);
        System.setProperty("was.install.root", root);
		
        String uri = "deployer:WebSphere:" + host + ":" + port;
        mDeploymentManager = getDeploymentManager(uri, user, password);
        mTargetList = mDeploymentManager.getTargets();
        
    }
    
    /**
     *	Gets Websphere-implementation DeploymentManager
     *  @param uri
     *  @param userId
     *  @param password
     *  @return DeploymentManager	 
     */
    private DeploymentManager getDeploymentManager(String uri, String userId, String password) 
        throws DeploymentManagerCreationException, Exception {        
    	if (mDeploymentManager == null) {
    		mDeploymentManager = new DeploymentFactoryImpl().getDeploymentManager(uri, userId, password);
    	}
        return mDeploymentManager;
    }
    
   /**
    * @see com.stc.jmsjca.container.Container#redeployModule(java.lang.String)
    */
   public void redeployModule(String absolutePath) throws Exception {
       deployModule(absolutePath);
   }

   /**
    * @see com.stc.jmsjca.container.Container#undeploy(java.lang.String)
    */
   public void undeploy(String moduleName) throws Exception {
	   
       stopModule(moduleName);       
       TargetModuleID moduleId = null;
       
       TargetModuleID[] earList = mDeploymentManager.getAvailableModules(ModuleType.EAR, mTargetList);
       boolean fail = true;
       for (int i = 0; i < earList.length; i++) {
           if (earList[i].getModuleID().indexOf(moduleName) > 0) {
               moduleId = earList[i];
               fail = false;
               break;
           }
       }
       if (fail) {
           throw new Exception ("updeploy failed, cannot list availabe module");
       }
       
       //undeploy module
       synchronized (this) {
           ProgressObject pObject = mDeploymentManager.undeploy(new TargetModuleID[]{moduleId});
           pObject.addProgressListener(new DeploymentListener(this));
       
           waitTask();
       }	   
   }

   /**
    * @see com.stc.jmsjca.container.Container#deployModule(java.lang.String)
    */
   public void deployModule(String absolutePath) throws Exception {
	   
       String moduleName = getModuleName(absolutePath);
       
       TargetModuleID moduleId = null;       
       TargetModuleID[] earList = mDeploymentManager.getAvailableModules(ModuleType.EAR, mTargetList);
       
       boolean exist = false;
       for (int i = 0; i < earList.length; i++) {
           if (earList[i].getModuleID().indexOf(moduleName) > 0) {
               moduleId = earList[i];
               exist = true;
               break;
           }
       }
       if (exist) {
           undeploy(moduleName);
       }
       
       //deploy module
       File earFile = new File(absolutePath);
       
       synchronized (this) {
           ProgressObject pObject = mDeploymentManager.distribute(mTargetList, earFile, null);
           pObject.addProgressListener(new DeploymentListener(this));
           waitTask();           
       }       
       startModule(moduleName);       
   }

   /**
    * @see com.stc.jmsjca.container.Container#close()
    */
   public void close() throws Exception {
       mDeploymentManager.release();
   }

   /**
    * @see com.stc.jmsjca.container.Container#isDeployed(java.lang.String)
    */
   public boolean isDeployed(String absolutePath) throws Exception {
       String moduleName = getModuleName(absolutePath);
       
       // list available module
       TargetModuleID[] earList = mDeploymentManager.getAvailableModules(ModuleType.EAR, mTargetList);
       boolean exists = false;
       for (int i = 0; i < earList.length; i++) {
           if (earList[i].getModuleID().indexOf(moduleName) > 0) {
               exists = true;
               break;
           }
       }
       return exists;
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

   private void startModule(String moduleName) throws Exception {
       TargetModuleID moduleId = null;
       
       TargetModuleID[] earList = mDeploymentManager.getAvailableModules(ModuleType.EAR, mTargetList);
       boolean fail = true;
       for (int i = 0; i < earList.length; i++) {
           if (earList[i].getModuleID().indexOf(moduleName) > 0) {
               fail = false;
               moduleId = earList[i];
               break;
           }
       }
       if (fail) {
           throw new Exception ("start module " + moduleName + " failed, cannot list deployed module." );
       }
       
       //start module
       synchronized (this) {
           ProgressObject pObject = mDeploymentManager.start(new TargetModuleID[]{moduleId});
           pObject.addProgressListener(new DeploymentListener(this));           
           waitTask();           
       }
       
       // list running module
       earList = mDeploymentManager.getRunningModules(ModuleType.EAR, mTargetList);
       fail = true;
       for (int i = 0; i < earList.length; i++) {
           if (earList[i].getModuleID().indexOf(moduleName) > 0) {
               moduleId = earList[i];
               fail = false;
               break;
           }
       }
       if (fail) {
           throw new Exception ("start module " + moduleName + " failed, cannot list running module.");
       }	   
   }
   
   private void stopModule(String moduleName) throws Exception {
	
       TargetModuleID moduleId = null;       
       TargetModuleID[] earList = mDeploymentManager.getRunningModules(ModuleType.EAR, mTargetList);
       boolean running = false;
       for (int i = 0; i < earList.length; i++) {
           if (earList[i].getModuleID().indexOf(moduleName) > 0) {
               running = true;
               moduleId = earList[i];
               break;
           }
       }
       
       if (running) {
           synchronized (this) {
               ProgressObject pObject = mDeploymentManager.stop(new TargetModuleID[]{moduleId});
               pObject.addProgressListener(new DeploymentListener(this));
               waitTask();               
           }
       }
       
       // list running module
       earList = mDeploymentManager.getRunningModules(ModuleType.EAR, mTargetList);
       boolean fail = false;
       for (int i = 0; i < earList.length; i++) {
           if (earList[i].getModuleID().indexOf(moduleName) > 0) {
               moduleId = earList[i];
               fail = true;
               break;
           }
       }
       if (fail) {
           throw new Exception ("stop module " + moduleName + " failed, can still list running module " +
                   "after stop.");
       }
	   
   }
 
   private void waitTask() {
       try {
           this.wait(300000);
       } catch (InterruptedException ie) {
       }
   }

   class DeploymentListener implements ProgressListener {
       private Object obj;
       
       DeploymentListener(Object obj) {
           this.obj = obj;
       }       
       public void handleProgressEvent(ProgressEvent event) {
           DeploymentStatus status = event.getDeploymentStatus();
           TargetModuleID moduleId = event.getTargetModuleID();           
           System.out.println("moduleId = " + (moduleId == null ? null : moduleId.getModuleID()) 
               + ", command = " + status.getCommand()
               + ", action = " + status.getAction()       
               + ", state = " + status.getState()                      
               + ", status = " + status.getMessage());
           
           if (status.isCompleted() || status.isFailed()) {
               synchronized (obj) {
                   obj.notify();
               }
           }           
       }
   }
   
}
