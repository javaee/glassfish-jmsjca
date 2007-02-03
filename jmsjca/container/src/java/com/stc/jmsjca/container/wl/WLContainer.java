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

package com.stc.jmsjca.container.wl;

import com.stc.jmsjca.container.Container;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.naming.Context;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

/**
 * A container for Weblogic 9
 * 
 * @author sbyn
 */
public class WLContainer extends Container {
    private static final String PASSWORD_PROP = "admin.password";
    private static final String USERNAME_PROP = "admin.user";
    private static final String HOST_PROP = "admin.host";
    private static final String PORT_PROP = "admin.port";
    private static final String HTTPPORT_PROP = "http.port";
    private static final String SERVER_NAME = "server.name";
    
    private static final String DeployerClass = "weblogic.deploy.api.tools.deployer.Deployer";
    private MBeanServerConnection mConnection;
    private JMXConnector mConnector;

    
    public interface MBeanConfig {
        String NAME = "com.bea:Name=RuntimeService,Type=weblogic.management.mbeanservers.runtime.RuntimeServiceMBean";
    }

    /**
     * configuration
     */
    protected Properties mProperties;
    
    public WLContainer() {
    }
    
    public static class IOTools {
        /**
         * @param p properties
         * @param name key
         * @return value
         */
        public static String getPropertyNotNull(Properties p, String name) {
            String ret = p.getProperty(name, null);
            if (ret == null) {
                throw new RuntimeException("No value specified for [" + name + "]");
            }
            return ret;
        }
    }
    
    public void initConnection() throws Exception {    
        //home = Helper.getAdminMBeanHome(getUsername(), getPassword(), getAdminUrl());
        //mBeanServer = home.getMBeanServer();
        
        String protocol = "t3";
        Integer portInteger = Integer.valueOf(getPort());
        int port = portInteger.intValue();
        String jndiroot = "/jndi/";      
        String mserver = "weblogic.management.mbeanservers.runtime";
        JMXServiceURL serviceURL = new JMXServiceURL(protocol, getHost(), port, jndiroot + mserver);
        Hashtable h = new Hashtable();
        h.put(Context.SECURITY_PRINCIPAL, getUsername());
        h.put(Context.SECURITY_CREDENTIALS, getPassword());
        h.put(JMXConnectorFactory.PROTOCOL_PROVIDER_PACKAGES, "weblogic.management.remote");
        
        mConnector = JMXConnectorFactory.connect(serviceURL, h);
        mConnection = mConnector.getMBeanServerConnection();       
    }

    private List cmdLine() {
        ArrayList ret = new ArrayList();
        ret.add("-adminurl");
        ret.add(getAdminUrl());
        ret.add("-username");
        ret.add(getUsername());
        ret.add("-password");
        ret.add(getPassword());
        ret.add("-noexit");
        
        return ret;
    }
    
    /**
     * @see com.stc.tafw.core.AppServ#setProperties(java.util.Properties)
     */
    public void setProperties(String moniker, Properties p) throws Exception {
//        if (!JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_5)) {
//            throw new RuntimeException("Running on " + JavaEnvUtils.getJavaVersion()
//                + "; should be at least " + JavaEnvUtils.JAVA_1_5);
//        }

        mProperties = p;

        initConnection();
    }
    
    /**
     * @see com.stc.tafw.core.AppServ#redeployModule(java.lang.String)
     */
    public void redeployModule(String absolutePath) throws Exception {
        System.out.println("Redeploying " + absolutePath);
        
        if (isDeployed(absolutePath)) {
            undeploy(getModuleName(absolutePath));
        }
        
        deployModule(absolutePath);
    }

    /**
     * @see com.tafw.appserv.AppServ#undeploy(java.lang.String)
     */
    public void undeploy(String moduleName) throws Exception {
        //install();

        long t0 = System.currentTimeMillis();
        System.out.println("Undeploying " + moduleName);

        try {        
            List args = cmdLine();
            args.add("-undeploy");
            args.add("-name");
            args.add(getModuleName(moduleName));
            args.add("-targets");
            args.add(IOTools.getPropertyNotNull(mProperties, SERVER_NAME));
            execCmd(args);
        } catch (Exception e) {
            throw new Exception("Failed to undeploy " + moduleName + " : " + e, e);
        }

        System.out.println("Undeployment complete: " + (System.currentTimeMillis() - t0) + " ms");
    }

    /**
     * @see com.tafw.appserv.AppServ#deployModule(java.lang.String)
     */
    public void deployModule(String absolutePath) throws Exception {
        //Need to call install() earlier.
        //install();

        long t0 = System.currentTimeMillis();
        System.out.println("Deploying " + absolutePath);
        
        String modulename = getModuleName(absolutePath);
        
        try {
            List args = cmdLine();
            args.add("-deploy");
            args.add("-source");
            args.add(absolutePath);
            args.add("-name");
            args.add(modulename);
            execCmd(args);
        } catch (Exception e) {
            throw new Exception("Failed to deploy " + modulename + " : " + e, e);
        }
        
        System.out.println("Deployment complete: " + (System.currentTimeMillis() - t0) + " ms");
    }

    private void execCmd(List as) throws Exception {
//        ClassLoader currentcl = Thread.currentThread().getContextClassLoader();
        try {
//            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            String[] args = (String[]) as.toArray(new String[as.size()]);
            Object wlsDelegate = getDelegate(args);
            Method m = wlsDelegate.getClass().getMethod("run", new Class[0]);
            m.invoke(wlsDelegate, new Object[0]);
        } finally {
//            Thread.currentThread().setContextClassLoader(currentcl);
        }
    }
    
    private Object getDelegate(String[] as) throws Exception {
        Class class1 = Class.forName(DeployerClass);
        Constructor constructor = class1.getDeclaredConstructor(new Class[] {as.getClass()});
        return constructor.newInstance(new Object[] {as});
    }
    
    /**
     * @see com.tafw.appserv.AppServ#close()
     */
    public void closeimpl() {
        try {
            mConnector.close();         
            mConnection = null;
            mConnector = null;      
        } catch (Exception e) {
            System.out.println("Failed to close JMXConnector mConnector: " + e);
        }
    }

    /**
     * @see com.tafw.appserv.AppServ#isDeployed(java.lang.String)
     */
    public boolean isDeployed(String absolutePath) throws Exception {
//        String modulename = getModuleName(absolutePath);
//        String state = mState.getProperty(DEPLOYED_PROP, modulename, null);
//        return state != null; 

        String moduleName = getModuleName(absolutePath);
        //System.out.println("##### in isDeployed(), module: " + moduleName + "#####");     

        // Is deployed?
        String[] modulesRT = getRuntimeApplication();        
        //System.out.println("\nComparing with this running list - ");
        //for (int i=0; i<modulesRT.length; i++)
        //  System.out.println(modulesRT[i]);
        
        if (Arrays.asList(modulesRT).contains(moduleName)) {
            //System.out.println("##### in isDeployed(), module: '" + moduleName + "' has deployed! Return True!!! #####");     
            return true;
        }
        //System.out.println("##### in isDeployed(), module: '" + moduleName + "' has NOT deployed! Return False!!! #####");        
        return false;        
    }

    public String[] getRuntimeApplication() throws Exception {
        String[] ret = null;
        try {
            ObjectName drs = new ObjectName(MBeanConfig.NAME); 
            ObjectName serverRT = (ObjectName) getAttribute(drs, "ServerRuntime");
            ObjectName[] appRT = (ObjectName[]) getAttribute(serverRT, "ApplicationRuntimes"); 
            ret = new String[appRT.length];
            for (int i=0; i<appRT.length; i++) {
                ret[i] = getAppName(appRT[i].getCanonicalName());
            }
        } catch (Exception e) {
            throw new Exception("Failed to get Application Runtime MBean" + e, e);
        }
        return ret;
    }
    
    public String getAppName(String fullName) {
        int begin = fullName.indexOf("=");
        int end = fullName.indexOf(",");
        return fullName.substring(begin+1, end);        
    }

    
    public static final String ATTRPREFIX = "xget";

    /**
     * @see com.tafw.appserv.AppServ#getMBeanProxy(java.lang.String, java.lang.Class)
     */
    public Object getMBeanProxy(final String objectName, Class itf) throws Exception {
        InvocationHandler h = new InvocationHandler() {
            private String[] createSignatureList(Method m) {
                Class[] args = m.getParameterTypes();
                String[] ret = new String[args.length];
                for (int i = 0; i < args.length; i++) {
                    ret[i] = args[i].getName();
                }
                return ret;
            }

            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                ObjectName o = new ObjectName(objectName);
                if (method.getName().startsWith(ATTRPREFIX)) {
                    //return mConnection.getAttribute(o, method.getName().substring(ATTRPREFIX.length()));
                    return getAttribute(o, method.getName().substring(ATTRPREFIX.length()));
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
     * @see com.tafw.appserv.AppServ#getAttribute(java.lang.String, java.lang.String)
     */
    public Object getAttribute(String objName, String name) throws Exception {
        try {
            ObjectName objectName = new ObjectName(objName);
            return mConnection.getAttribute(objectName, name);
        } catch (Exception e) {
            throw new Exception("getAttribute(" + objName + ", " + name + ") failed: " + e, e);
        }
    }
    
    public Object getAttribute(ObjectName objName, String name) throws Exception {
        try {
            return mConnection.getAttribute(objName, name);
        } catch (Exception e) {
            throw new Exception("getAttribute(ObjectName, String) for " + objName + ", " + name + " failed: " + e, e);
        }
    }
    

    /**
     * @return configuration parameter
     */
    public String getPassword() {
        return IOTools.getPropertyNotNull(mProperties, PASSWORD_PROP);
    }

    /**
     * @return configuration parameter
     */
    public String getUsername() {
        return IOTools.getPropertyNotNull(mProperties, USERNAME_PROP);
    }

    /**
     * @return configuration parameter
     */
    public String getHost() {
        return IOTools.getPropertyNotNull(mProperties, HOST_PROP);
    }

    /**
     * @return configuration parameter
     */
    public String getPort() {
        return IOTools.getPropertyNotNull(mProperties, PORT_PROP);
    }

    /**
     * @return configuration parameter
     */
    public String getHttpPort() {
        return IOTools.getPropertyNotNull(mProperties, HTTPPORT_PROP);
    }

    /**
     * @return configuration parameter
     */
    public String getAdminUrl() {
        return "t3://" + getHost() + ":" + getPort();
    }

    /**
     * @see com.stc.tafw.core.AppServ#getDefaultEDName()
     */
    public String getDefaultEDName() {
        return "WebLogic90Svr1";
    }
    
    /**
     * @see com.stc.tafw.core.EnvironmentConfigSource#getSars()
     */
    public String getSars() {
        return "eGate.sar,weblogic90.sar";
    }
    
    /**
     * see com.stc.tafw.core.EnvironmentConfigSource#getLDAPFlag()
     * 
     * @return gets a configuration parameter from the properties file
     */
    public boolean getLDAPFlag() {
        return false;
    }

    public void setProperties(Properties p) throws Exception {
        setProperties(null, p);
    }

    public void close() throws Exception {
        closeimpl();
    }
}
