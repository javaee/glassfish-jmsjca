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

package com.stc.jmsjca.container.jboss;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import javax.naming.InitialContext;
import javax.naming.Context;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.management.ObjectName;
import com.stc.jmsjca.container.Container;
import org.jboss.jmx.adaptor.rmi.RMIAdaptor;

/**
 * JBoss 4.0.2 Deployer 
 * @author cye
 * @version $Revision: 1.2 $
 */
public class Deployer extends Container {

    public static final String ATTRPREFIX = "xget";
    
    Properties mProperties;
    RMIAdaptor mMBeanServerConnection;
    ObjectName mDeployerObjectName;

    /**
     * Constructor
     * @param p
     */
     public Deployer() {
     }
     
    /**
     * Constructor
     * @param p
     */
     public Deployer(Properties p) {
         try {
             mProperties = getContainerProperties(p);
             InitialContext ctx = new InitialContext(mProperties);
             //can be "jmx/invoker/HttpAdaptor", but jboss does not create it by default
             mMBeanServerConnection = (RMIAdaptor) ctx.lookup("jmx/invoker/RMIAdaptor");             
             mDeployerObjectName = new ObjectName("jboss.system:service=MainDeployer");  
             String  name = (String) mMBeanServerConnection.getAttribute(mDeployerObjectName, "Name");                  
             if ("MainDeployer".equals(name)) {
                 System.out.println("jboss.system:service=MainDeployer" + " was found");
             } else {
                 throw new Exception("No JBoss MainDeployer found");
             }
         } catch (Exception e) {
             e.printStackTrace();
         }
     }

     @Override
    public void setProperties(Properties p) throws Exception {
         try {
             mProperties = getContainerProperties(p);
             InitialContext ctx = new InitialContext(mProperties);
             //can be "jmx/invoker/HttpAdaptor", but jboss does not create it by default
             mMBeanServerConnection = (RMIAdaptor) ctx.lookup("jmx/invoker/RMIAdaptor");             
             mDeployerObjectName = new ObjectName("jboss.system:service=MainDeployer");  
             String  name = (String) mMBeanServerConnection.getAttribute(mDeployerObjectName, "Name");                  
             if ("MainDeployer".equals(name)) {
                 System.out.println("jboss.system:service=MainDeployer" + " was found");
             } else {
                 throw new Exception("No JBoss MainDeployer found");
             }
         } catch (Exception e) {
             e.printStackTrace();
         }     
     }
     
     public Properties getContainerProperties(Properties p) {
         Properties pp = new Properties();
         pp.setProperty(Context.INITIAL_CONTEXT_FACTORY, p.getProperty("jboss.initial.context.factory"));
         pp.setProperty(Context.PROVIDER_URL, p.getProperty("jboss.provider.url"));
         pp.setProperty(Context.URL_PKG_PREFIXES, p.getProperty("jboss.url.pkg.prefixes"));         
         return pp; 
     }
     
     /**
      * @see com.stc.jmsjca.container.Container#redeployModule(java.lang.String)
      */
     @Override
    public void redeployModule(String moduleName) throws Exception {
         Object[] params = new Object[]{moduleName};
         String[] signatures = new String[]{String.class.getName()};
         mMBeanServerConnection.invoke(mDeployerObjectName, "redeploy", params, signatures);
         System.out.println(moduleName + " redeployed");         
     }

     /**
      * @see com.stc.jmsjca.container.Container#undeploy(java.lang.String)
      */
     @Override
    public void undeploy(String moduleName) throws Exception {
         Object[] params = new Object[]{moduleName};
         String[] signatures = new String[]{String.class.getName()};
         mMBeanServerConnection.invoke(mDeployerObjectName, "undeploy", params, signatures);
         System.out.println(moduleName + " undeployed");         
     }

     /**
      * @see com.stc.jmsjca.container.Container#deployModule(java.lang.String)
      */
     @Override
    public void deployModule(String absolutePath) throws Exception {
         Object[] params = new Object[]{absolutePath};
         String[] signatures = new String[]{String.class.getName()};         
         mMBeanServerConnection.invoke(mDeployerObjectName, "deploy", params, signatures);
         System.out.println(absolutePath + " deployed");         
     }

     /**
      * @see com.stc.jmsjca.container.Container#close()
      */
     @Override
    public void close() throws Exception {
         mMBeanServerConnection = null;
     }

     /**
      * @see com.stc.jmsjca.container.Container#isDeployed(java.lang.String)
      */
     @Override
    public boolean isDeployed(String moduleName) throws Exception {
         Object[] params = new Object[]{moduleName};
         String[] signatures = new String[]{String.class.getName()};                  
         Boolean b = (Boolean) mMBeanServerConnection.invoke(mDeployerObjectName, "isDeployed", params, signatures);
         if (b.booleanValue()) {
             System.out.println(moduleName + " is deployed");         
         } else {
             System.out.println(moduleName + " is not deployed");                      
         }
         return b.booleanValue();
     }

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
                     return mMBeanServerConnection.getAttribute(o, method.getName().substring(ATTRPREFIX.length()));
                 } else {
                     String[] sig = createSignatureList(method);
                     args = args == null ? args = new Object[0] : args;
                     return mMBeanServerConnection.invoke(o, method.getName(), args, sig);
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
         return mMBeanServerConnection.getAttribute(mDeployerObjectName, name);                           
     }
     
    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
           Properties p = new Properties();
           InputStream in = null;
           in = new FileInputStream("F:/eGate510Dev/jmsjca/jboss/meta/JBossContainer.properties");
           p.load(in);
           in.close();
           Deployer test = new Deployer(p);
           String earFile = new String("D:/jboss/ratest-test.ear"); 
           String moduleName = new String("file:/D:/jboss/ratest-test.ear");
           test.isDeployed(moduleName);
           test.deployModule(earFile);
           test.isDeployed(moduleName);
           test.redeployModule(moduleName);
           test.isDeployed(moduleName);
           test.undeploy(moduleName);
           test.isDeployed(moduleName);           
        } catch(Exception e) {            
        }
    }

}
