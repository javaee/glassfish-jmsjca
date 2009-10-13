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

package com.stc.jmsjca.container.glassfishv3;

import com.stc.jmsjca.container.Container;
import com.stc.jmsjca.util.Semaphore;

import org.glassfish.deployapi.SunDeploymentFactory;

import javax.enterprise.deploy.shared.ModuleType;
import javax.enterprise.deploy.spi.DeploymentManager;
import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.spi.exceptions.DeploymentManagerCreationException;
import javax.enterprise.deploy.spi.status.DeploymentStatus;
import javax.enterprise.deploy.spi.status.ProgressEvent;
import javax.enterprise.deploy.spi.status.ProgressListener;
import javax.enterprise.deploy.spi.status.ProgressObject;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author Frank Kieviet
 * @author Edward Chou
 */
public class GlassFishv3Container extends Container {
    public static final String PROP_HOST = "jmsjca.glassfishv3.host";
    public static final String PROP_PORT = "jmsjca.glassfishv3.adminport";
    public static final String PROP_JMXPORT = "jmsjca.glassfishv3.jmxport";
    public static final String PROP_USERID = "jmsjca.glassfishv3.userid";
    public static final String PROP_PASSWORD = "jmsjca.glassfishv3.password";

    public static final String ID = "glassfishv3";
    
    private DeploymentManager dm;
    private Target[] targetList;
    private MBeanServerConnection mConnection;
    private JMXConnector mConnector;
    
    /**
     * @see com.stc.jmsjca.container.Container#setProperties(java.util.Properties)
     */
    @Override
    public void setProperties(Properties p) throws Exception {
        String host = p.getProperty(PROP_HOST);
        String port = p.getProperty(PROP_PORT);
        String jmxport = p.getProperty(PROP_JMXPORT);
        String user = p.getProperty(PROP_USERID);
        String password = p.getProperty(PROP_PASSWORD);
        
        // Setup deployment manager and targetlist
        String uri = "deployer:Sun:S1AS::" + host + ":" + port;
        dm = getDeploymentManager(uri, user, password);
        targetList = dm.getTargets();
        
        // Setup JMX Connection
        try {
            final JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + host + ":" + jmxport + "/jmxrmi");
            final Map<String, Object> env = new HashMap<String, Object>();
            String[] creds = {user, password};
            env.put(JMXConnector.CREDENTIALS, creds);
            mConnector = JMXConnectorFactory.connect(url, env);
            mConnection = mConnector.getMBeanServerConnection();
        } catch (Exception e) {
            throw new Exception("Error when connecting to MBeanServer: " + e.getMessage(), e);
        }        
    }
    
    private DeploymentManager getDeploymentManager(String anURI, String aUserName, String aPassword) 
        throws DeploymentManagerCreationException, Exception {
        
        DeploymentManager dm = null;
        SunDeploymentFactory dmFactory = new SunDeploymentFactory();
        dm = dmFactory.getDeploymentManager(anURI, aUserName, aPassword);
        return dm;
    }

    /**
     * @see com.stc.jmsjca.container.Container#redeployModule(java.lang.String)
     */
    @Override
    public void redeployModule(String absolutePath) throws Exception {
        deployModule(absolutePath);
    }

    /**
     * @see com.stc.jmsjca.container.Container#undeploy(java.lang.String)
     */
    @Override
    public void undeploy(String moduleName) throws Exception {
        stopModule(moduleName);
        
        // list running module
        TargetModuleID moduleId = null;
        
        TargetModuleID[] earList = dm.getAvailableModules(ModuleType.EAR, targetList);
        boolean fail = true;
        for (int i = 0; i < earList.length; i++) {
            if (earList[i].getModuleID().equals(moduleName)) {
                moduleId = earList[i];
                fail = false;
                break;
            }
        }
        if (fail) {
            throw new Exception ("Test failed, cannot list EAR files");
        }
        
        // Undeploy module
        ProgressObject pObject = dm.undeploy(new TargetModuleID[]{moduleId});
        Semaphore waitUntilDone = new Semaphore(0);
        pObject.addProgressListener(new DeploymentListener(waitUntilDone));
        waitTask(waitUntilDone);
    }

    /**
     * @see com.stc.jmsjca.container.Container#deployModule(java.lang.String)
     */
    @Override
    public void deployModule(String absolutePath) throws Exception {
        String moduleName = getModuleName(absolutePath);
        
        // list running module
        TargetModuleID moduleId = null;
        TargetModuleID[] earList = dm.getAvailableModules(ModuleType.EAR, targetList);
        boolean exist = false;
        for (int i = 0; i < earList.length; i++) {
            if (earList[i].getModuleID().equals(moduleName)) {
                moduleId = earList[i];
                exist = true;
                break;
            }
        }
        if (exist) {
            undeploy(moduleName);
        }
        
        // Get rid of warning
        moduleId = moduleId  == null ? moduleId : moduleId;
        
        // Deploy module
        File earFile = new File(absolutePath);
        ProgressObject pObject = dm.distribute(targetList, earFile, null);
        Semaphore waitUntilDone = new Semaphore(0);
        pObject.addProgressListener(new DeploymentListener(waitUntilDone));
        waitTask(waitUntilDone);

        startModule(getModuleName(absolutePath));
    }
    
    private void startModule(String moduleName) throws Exception {
        // list available module
        TargetModuleID moduleId = null;
        
        // Wait until module shows up as a deployed module
        boolean fail = true;
        for (int iTry = 0; iTry < 30; iTry++) {
            TargetModuleID[] earList = dm.getAvailableModules(ModuleType.EAR, targetList);
            for (int i = 0; i < earList.length; i++) {
                if (earList[i].getModuleID().equals(moduleName)) {
                    fail = false;
                    moduleId = earList[i];
                    break;
                }
            }
            if (fail) {
                Thread.sleep(1000);
            } else {
                break;
            }
        }

        if (fail) {
            throw new Exception ("Deployed module did not show up in the list of deployed modules");
        }
        
        // start module
        ProgressObject pObject = dm.start(new TargetModuleID[]{moduleId});
        Semaphore waitUntilDone = new Semaphore(0);
        pObject.addProgressListener(new DeploymentListener(waitUntilDone));
        waitTask(waitUntilDone);
        
        // list running module
        for (int iTry = 0; iTry < 30; iTry++) {
            TargetModuleID[] earList = dm.getRunningModules(ModuleType.EAR, targetList);
            fail = true;
            for (int i = 0; i < earList.length; i++) {
                if (earList[i].getModuleID().equals(moduleName)) {
                    moduleId = earList[i];
                    fail = false;
                    break;
                }
            }
            if (fail) {
                Thread.sleep(1000);
            } else {
                break;
            }
        }
        if (fail) {
            throw new Exception ("Deployed and started module did not show up in the list of running modules");
        }

    }
    
    private void stopModule(String moduleName) throws Exception {
        // list available module
        TargetModuleID moduleId = null;
        
        TargetModuleID[] earList = dm.getRunningModules(ModuleType.EAR, targetList);
        boolean running = false;
        for (int i = 0; i < earList.length; i++) {
            if (earList[i].getModuleID().equals(moduleName)) {
                running = true;
                moduleId = earList[i];
                break;
            }
        }
        
        if (running) {
            //stop module
            ProgressObject pObject = dm.stop(new TargetModuleID[]{moduleId});
            Semaphore waitUntilDone = new Semaphore(0);
            pObject.addProgressListener(new DeploymentListener(waitUntilDone));
            waitTask(waitUntilDone);
        }
        
        // list running module
        earList = dm.getRunningModules(ModuleType.EAR, targetList);
        boolean fail = false;
        for (int i = 0; i < earList.length; i++) {
            if (earList[i].getModuleID().equals(moduleName)) {
                moduleId = earList[i];
                fail = true;
                break;
            }
        }
        if (fail) {
            throw new Exception ("Test failed, can still list running Ear " +
                    "file after stop");
        }
        
    }

    /**
     * @see com.stc.jmsjca.container.Container#close()
     */
    @Override
    public void close() throws Exception {
        dm.release();
        mConnector.close();
        mConnection = null;
        mConnector = null;
    }

    /**
     * @see com.stc.jmsjca.container.Container#isDeployed(java.lang.String)
     */
    @Override
    public boolean isDeployed(String absolutePath) throws Exception {
        String moduleName = getModuleName(absolutePath);
        
        // list available module
        TargetModuleID[] earList = dm.getAvailableModules(ModuleType.EAR, targetList);
        boolean exists = false;
        for (int i = 0; i < earList.length; i++) {
            if (earList[i].getModuleID().equals(moduleName)) {
                exists = true;
                break;
            }
        }

        return exists;
    }
    
    public static final String ATTRPREFIX = "xget";

    /**
     * @see com.stc.jmsjca.container.Container#getMBeanProxy(java.lang.String, java.lang.Class)
     */
    @Override
    public Object getMBeanProxy(final String objectName, Class<?> itf) throws Exception {
        InvocationHandler h = new InvocationHandler() {
            private String[] createSignatureList(Method m) {
                Class<?>[] args = m.getParameterTypes();
                String[] ret = new String[args.length];
                for (int i = 0; i < args.length; i++) {
                    ret[i] = args[i].getName();
                }
                return ret;
            }

            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                ObjectName o = new ObjectName(objectName);
                if (method.getName().startsWith(ATTRPREFIX)) {
                    return mConnection.getAttribute(o, method.getName().substring(ATTRPREFIX.length()));
                } else {
                    String[] sig = createSignatureList(method);
                    args = args == null ? args = new Object[0] : args;
                    return mConnection.invoke(o, method.getName(), args, sig);
                }
            }
        };
        return Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] {itf}, h);
    }

    /**
     * @see com.stc.jmsjca.container.Container#getAttribute(java.lang.String, java.lang.String)
     */
    @Override
    public Object getAttribute(String objName, String name) throws Exception {
        try {
            ObjectName objectName = new ObjectName(objName);
            return mConnection.getAttribute(objectName, name);
        } catch (Exception e) {
            throw new Exception("getAttribute(" + objName + ", " + name + ") failed: " + e, e);
        }
    }
    
    private void waitTask(Semaphore s) throws Exception {
        if (!s.attempt(300000)) {
            throw new Exception("Timeout waiting for completion from server");
        }
    }

    /**
     * Gets notified upon progress during the deployment process. Will release a semaphore
     * so that a blocked caller can use this to synchronize itself.
     * 
     * @author fkieviet
     */
    class DeploymentListener implements ProgressListener {
        private Semaphore semaphore;
        
        DeploymentListener(Semaphore obj) {
            this.semaphore = obj;
        }
        
        public void handleProgressEvent(ProgressEvent event) {
            DeploymentStatus status = event.getDeploymentStatus();
            if (status.isCompleted() || status.isFailed()) {
                semaphore.release();
            }
        }
    }
}
