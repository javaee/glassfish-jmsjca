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

package com.stc.jmsjca.util;

import com.stc.jmsjca.localization.Localizer;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.RuntimeOperationsException;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * This is a baseclass or proxy for dynamic MBeans. Say that class A needs to be
 * exposed as an MBean: instantiate this class and pass it a reference to the instance
 * of class A and an object representing the meta information of class A, here 
 * called M. Note that M and A can be the same class; M and A can even be a subclass 
 * of this class.
 * 
 * To expose a method in A, say <code>public int update(int x)</code>: add a method 
 * to M with the following signature: <code>public String[] mbmUpdate(int x)</code>. 
 * The returned String[] (called <code>d</code>) will be used as a description in the 
 * MBean description of the method itself as well as the arguments to the method. 
 * The zero-th parameter <code>d[0]</code> is the method descriptor and should have
 * this format: <code>name: description</code>. Entry <code>d[i]</code>
 * is the descriptor for the ith argument; a description of the return parameter does not
 * need to be specified.
 * If <code>d</code> is null or empty, it is assumed that the method is disabled and 
 * will not be listed, nor will it be allowed to execute.
 * 
 * To expose an attribute in A, say <code>attr</code>, A should have 
 * <code>public X getA()</code> and <code>public void setA(X x)</code>, and M should 
 * have <code>public String mbaA()</code>. The returned string will be used as the 
 * description of the attribute. If the returned String is null, this implies that the 
 * attribute can not be read from or written to and will not show up in the attribute
 * liset. If A does not have the getter method, the attribute will be marked as 
 * write-only; if A does not have the setter method, the attribute will be marked as 
 * read-only.    
 *
 * @author fkieviet
 * @version $Revision: 1.5 $
 */
public abstract class MBeanHelper implements MBeanRegistration, DynamicMBean {
    private static Logger sLog = Logger.getLogger(MBeanHelper.class);
    private static String MBM = "mbm";
    private static String MBA = "mba";
    private ArrayList mServers = new ArrayList();
    private ArrayList mNames = new ArrayList();

    private static final Localizer LOCALE = Localizer.get();
    
    /**
     * Removes this MBean from the MBeanServers it is registered with
     */
    public void destroy() {
        for (int i = 0; i < mServers.size(); i++) {
            MBeanServer s = (MBeanServer) mServers.get(i);
            try {
                s.unregisterMBean((ObjectName) mNames.get(i));
            } catch (Exception ex) {
                sLog.warn(LOCALE.x("E089: Exception unregistering activation MBean [{0}]" 
                    + " on server [{1}]", mNames.get(i), mServers.get(i)));
            }
        }
        mNames.clear();
        mServers.clear();
    }

    /**
     * preRegister
     *
     * @param mBeanServer MBeanServer
     * @param objectName ObjectName
     * @return ObjectName
     */
    public ObjectName preRegister(MBeanServer mBeanServer, ObjectName objectName) {
        mServers.add(mBeanServer);
        mNames.add(objectName);
        return objectName;
    }

    /**
     * postRegister
     *
     * @param boolean0 Boolean
     */
    public void postRegister(Boolean boolean0) {
    }

    /**
     * preDeregister
     */
    public void preDeregister() {
    }

    /**
     * postDeregister
     */
    public void postDeregister() {
    }

    /**
     * setAttribute
     *
     * @param attribute Attribute
     */
    public void setAttribute(Attribute attribute) {
        //TODO
    }

    /**
     * getAttributes
     *
     * @param attributeNames String[]
     * @return AttributeList
     */
    public AttributeList getAttributes(String[] attributeNames) {
        AttributeList ret = new AttributeList();

        for (int i = 0; i < attributeNames.length; i++) {
            try {
                ret.add(new Attribute(attributeNames[i], getAttribute(attributeNames[i])));
            } catch (Exception ex) {
                sLog.warn(LOCALE.x("E090: Failed to get attribute list: {0}", ex), ex);
            }
        }
        return ret;
    }

    /**
     * setAttributes
     *
     * @param attributeList AttributeList
     * @return AttributeList
     */
    public AttributeList setAttributes(AttributeList attributeList) {
        //TODO
        return null;
    }

    /**
     * getMBeanInfo
     *
     * @return MBeanInfo
     */
    public MBeanInfo getMBeanInfo() {
        return getMBeanInfo(false);
    }

    /**
     * getMBeanInfo
     *
     * @param throwFailures boolean
     * @return MBeanInfo
     */
    public MBeanInfo getMBeanInfo(boolean throwFailures) {
        return new MBeanInfo(this.getClass().getName(), getMBeanDescription(),
            getAttributeInfo(throwFailures), new MBeanConstructorInfo[0],
            getOperationsInfo(throwFailures), new MBeanNotificationInfo[0]);
    }

    /**
     * Gathers the attribute info
     * 
     * @param throwFailures boolean
     * @return non null array
     */
    protected MBeanAttributeInfo[] getAttributeInfo(boolean throwFailures) {
        ArrayList attrinfos = new ArrayList();

        Object target = getDelegate();
        Method[] targetmethods = target.getClass().getMethods();
        Object meta = getMetaObject();
        Method[] methods = meta.getClass().getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method m = methods[i];
            if (m.getName().startsWith(MBA) && m.getName().length() > MBA.length()) {
                try {
                    String attrname = m.getName().substring(MBA.length());

                    // Try to get description: invoke the mba method.
                    Class[] types = m.getParameterTypes();
                    Object[] args = new Object[types.length];
                    for (int j = 0; j < types.length; j++) {
                        args[i] = null;
                    }
                    String descr = (String) m.invoke(meta, args);

                    // Null indicates the attribute is disabled
                    if (descr == null) {
                        continue;
                    }

                    // Verify getters and setters
                    boolean getter = false;
                    boolean setter = false;
                    Class attrType = null;
                    for (int j = 0; j < targetmethods.length; j++) {
                        Method cand = targetmethods[j];
                        if (cand.getName().equals("get" + attrname)) {
                            attrType = cand.getReturnType();
                            getter = true;
                        }

                        if (cand.getName().equals("set" + attrname)
                            && cand.getParameterTypes().length == 1) {
                            setter = true;
                            if (attrType == null) {
                                attrType = cand.getParameterTypes()[0];
                            }
                        }

                        if (getter && setter) {
                            break;
                        }
                    }

                    if (!getter && !setter) {
                        // logic fault
                        // TODO: handle
                        break;
                    }

                    // Found a valid attribute; "publish" it
                    MBeanAttributeInfo info = new MBeanAttributeInfo(attrname, 
                        attrType.getName(), descr, getter, setter, false);
                    attrinfos.add(info);
                } catch (Exception e) {
                    if (throwFailures) {
                        throw Exc.rtexc(LOCALE.x("E169: getAttributeInfo() failed: {0}", e), e);
                    } else {
                        sLog.warn(LOCALE.x("E091: Failure while building attribute descriptions for "
                            + "{0}: {1}", this.getClass().getName(), e));
                    }
                }
            }
        }
        MBeanAttributeInfo[] attrs = (MBeanAttributeInfo[]) 
            attrinfos.toArray(new MBeanAttributeInfo[attrinfos.size()]);
        return attrs;
    }

    /**
     * Gets the meta information on operations
     * 
     * @param throwFailures boolean
     * @return non null array
     */
    protected MBeanOperationInfo[] getOperationsInfo(boolean throwFailures) {
        ArrayList opinfos = new ArrayList();
        Object target = getDelegate();
        Method[] targetmethods = target.getClass().getMethods();
        Object meta = getMetaObject();
        Method[] methods = meta.getClass().getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method m = methods[i];
            if (m.getName().startsWith(MBM) && m.getName().length() > MBM.length()) {
                try {
                    String name = m.getName().substring(MBM.length());
                    Method targetMethod = null;

                    // Find corresponding method in target
                    for (int j = 0; j < targetmethods.length; j++) {
                        Method cand = targetmethods[j];
                        if (name.equals(cand.getName())) {
                            // Check arguments
                            boolean matches = true;
                            if (m.getParameterTypes().length == cand.getParameterTypes().length) {
                                for (int k = 0; k < m.getParameterTypes().length; k++) {
                                    if (!m.getParameterTypes()[k].equals(
                                        cand.getParameterTypes()[k])) {
                                        matches = false;
                                        break;
                                    }
                                }
                            }
                            if (matches) {
                                targetMethod = cand;
                                break;
                            }
                        }
                    }

                    if (targetMethod == null) {
                        throw Exc.exc(LOCALE.x("E171: Target method not found for {0}", m));
                    }

                    // Invoke method to get description
                    Class[] types = m.getParameterTypes();
                    Object[] args = new Object[types.length];
                    for (int j = 0; j < types.length; j++) {
                        args[j] = getReasonableDummyValue(types[j]);
                    }
                    String[] descr = (String[]) m.invoke(meta, args);
                    if (descr.length - 1 != types.length) {
                        throw Exc.exc(LOCALE.x("E172: Mismatching number of arguments for {0}", m));
                    }

                    MBeanParameterInfo[] paraminfos = new MBeanParameterInfo[types.length];
                    for (int j = 0; j < paraminfos.length; j++) {
                        String s = descr[j + 1];
                        int index = s.indexOf(':');
                        String parmname = "";
                        String parmdescr = s;
                        if (index > 0) {
                            parmdescr = s.substring(index + 1);
                            parmname = s.substring(0, index);
                        }
                        paraminfos[j] = new MBeanParameterInfo(parmname, 
                            targetMethod.getParameterTypes()[j].getName(), parmdescr);
                    }

                    opinfos.add(new MBeanOperationInfo(name, descr[0], paraminfos,
                        targetMethod.getReturnType().getName(),
                        MBeanOperationInfo.UNKNOWN));
                } catch (Exception e) {
                    if (throwFailures) {
                        throw Exc.rtexc(LOCALE.x(
                            "E173: Failure in getOperationsInfo() for method [{0}]: {1}", m, e),
                            e);
                    } else {
                        sLog.warn(LOCALE.x("E092: Failure while building method descriptions for "
                            + "{0}: {1}", this.getClass().getName(), e));
                    }
                }
            }
        }
        return (MBeanOperationInfo[]) opinfos.toArray(
            new MBeanOperationInfo[opinfos.size()]);
    }

    private Object getReasonableDummyValue(Class c) {
        if (c.equals(int.class)) {
            return new Integer(0);
        } else if (c.equals(long.class)) {
            return new Long(0);
        } else if (c.equals(boolean.class)) {
            return new Boolean(false);
        } else if (c.equals(double.class)) {
            return new Double(0);
        } else if (c.equals(float.class)) {
            return new Float(0);
        } else if (c.equals(short.class)) {
            return new Short((short) 0);
        } else if (c.equals(byte.class)) {
            return new Byte((byte) 0);
        } else if (c.equals(char.class)) {
            return new Character((char) 0);
        } else {
            return null;
        }
    }

    /**
     * Concatenates string components
     * 
     * @param strs components
     * @param delim delimeter, e.g. ", "
     * @return concatenated string
     */
    public static String concat(Object[] strs, String delim) {
        StringBuffer ret = new StringBuffer();
        if (strs != null) {
            for (int i = 0; i < strs.length; i++) {
                if (i != 0) {
                    ret.append(delim);
                }
                ret.append(strs[i]);
            }
        }
        return ret.toString();
    }

    /**
     * Finds a method on the specified class. Copied from 
     * RTS\server\jmx\src\jmxri\javax\management\modelmbean\RequiredModelMBean.java
     * and modified.
     * 
     * @param targetClass class to find method on
     * @param opMethodName methodname
     * @param sig signature
     * @return method null if not found
     * @throws ReflectionException
     */
    private static Method resolveMethod(Class targetClass, String opMethodName,
        String[] sig) throws ReflectionException {
        
        if (sLog.isDebugEnabled()) {
            sLog.debug("Resolving method " + targetClass + "." + opMethodName + "("
                + concat(sig, ", ") + ")");
        }
        
        // Find parameter classes
        final Class[] argClasses;
        if (sig == null) {
            argClasses = null;
        } else {
            final ClassLoader targetClassLoader = targetClass.getClassLoader();
            argClasses = new Class[sig.length];
            for (int i = 0; i < sig.length; i++) {
                argClasses[i] = (Class) PRIMITIVECLASSMAP.get(sig[i]);
                if (argClasses[i] == null) {
                    try {
                        argClasses[i] =
                            Class.forName(sig[i], false, targetClassLoader);
                    } catch (ClassNotFoundException e) {
                        if (sLog.isDebugEnabled()) {
                            sLog.debug("Class not found: " + sig[i]);
                        }
                        throw new ReflectionException(e, LOCALE.x(
                            "Parameter class not found: [{0}]", sig[i]).toString());
                    }
                }
            }
        }
        
        // Find method
        try {
            return targetClass.getMethod(opMethodName, argClasses);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
    
    private static final Class[] PRIMITIVECLASSES = {
        int.class, long.class, boolean.class, double.class,
        float.class, short.class, byte.class, char.class,
    };
    
    private static final Map/*<String,Class>*/ PRIMITIVECLASSMAP = new HashMap();

    static {
        for (int i = 0; i < PRIMITIVECLASSES.length; i++) {
            final Class c = PRIMITIVECLASSES[i];
            PRIMITIVECLASSMAP.put(c.getName(), c);
        }
    }
    
    /**
     * Copied from
     * RTS\server\jmx\src\jmxri\javax\management\modelmbean\RequiredModelMBean.java
     * and modified
     * 
     * @see javax.management.DynamicMBean#invoke(java.lang.String, java.lang.Object[], java.lang.String[])
     */
    public Object invoke(String opName, Object[] opArgs, String[] sig)
    throws MBeanException, ReflectionException {
        String tracename = opName + "(" + concat(opArgs, ", ") + ")"; 
        
        if (opName == null) {
            final RuntimeException x = Exc.illarg(LOCALE.x("E175: Method name must not be null"));
            throw new RuntimeOperationsException(x, LOCALE.x(
                "E176: An exception occured while trying to " +
            "invoke MBean method [{0}]: {1}", tracename, x).toString());
        }
        
        // Determine classname and method
        String opMethodName;
        
        // Parse for class name and method
        int opSplitter = opName.lastIndexOf(".");
        if (opSplitter > 0) {
            opMethodName = opName.substring(opSplitter + 1);
        } else {
            opMethodName = opName;
        }
        
        // Ignore anything after a left paren.  We keep this for
        // compatibility but it isn't specified.
        opSplitter = opMethodName.indexOf("(");
        if (opSplitter > 0) {
            opMethodName = opMethodName.substring(0, opSplitter);
        }
        
        if (sLog.isDebugEnabled()) {
            sLog.debug("invoke: searching operation " + opName + " as " + opMethodName
                + " [" + tracename + "]");
        }
        
        // Find invmethod
        Method invMethod = resolveMethod(getDelegate().getClass(), opMethodName, sig);
        if (invMethod == null) {
            throw new MBeanException(Exc.rtexc(LOCALE.x("E174: Method {0} not found on {1}"
                , tracename, getDelegate().getClass())));
        }
        
        // Determine if invocation is allowed
        Method metaMethod = resolveMethod(getMetaObject().getClass(), MBM + opMethodName, sig);
        if (metaMethod == null) {
            throw new MBeanException(Exc.rtexc(LOCALE.x("E177: Method {0} not found on {1} (invokation check"
                , tracename, getMetaObject().getClass())));
        }
        Object invAllowed;
        try {
            invAllowed = metaMethod.invoke(getMetaObject(), opArgs);
        } catch (Exception e) {
            throw new MBeanException(Exc.rtexc(LOCALE.x("E178: Access check invocation " +
                    "error on method {0} on {1}", tracename, getMetaObject().getClass()), e));
        }
        if (invAllowed == null) {
            throw new MBeanException(Exc.rtexc(LOCALE.x("E179: Method {0} may not be invoked on {1}"
                , tracename, getMetaObject().getClass())));
        }
        
        // Invoke
        try {
            return invMethod.invoke(getDelegate(), opArgs);
        } catch (Exception e) {
            throw new MBeanException(Exc.rtexc(LOCALE.x("E180: Invocation error on method {0} on {1}"
                , tracename, getDelegate().getClass()), e));
        }
    }

    /**
     * getAttribute
     *
     * @param name String
     * @return Object
     * @throws AttributeNotFoundException when name is invalid
     * @throws MBeanException never
     * @throws ReflectionException never
     */
    public Object getAttribute(String name) throws AttributeNotFoundException,
        MBeanException, ReflectionException {
        try {
            Object target = getDelegate();
            Method[] targetmethods = target.getClass().getMethods();

            // Find method
            Method m = null;
            for (int i = 0; i < targetmethods.length; i++) {
                Method cand = targetmethods[i];
                if (cand.getName().equals("get" + name)
                    && cand.getParameterTypes().length == 0) {
                    m = cand;
                }
            }

            // Invoke
            if (m != null) {
                return m.invoke(target, new Object[0]);
            }
        } catch (Exception ex) {
            throw new MBeanException(ex, LOCALE.x("E181: Retrieval of attribute [{0}] failed: {1}"
                , name, ex).toString());
        }
        throw new AttributeNotFoundException(LOCALE.x("E182: Invalid attribute [{0}]", name).toString());
    }

    /**
     * To be overridden
     * 
     * @return description of this mbean
     */
    protected abstract String getMBeanDescription();

    /**
     * Provides access to the meta object; see class comments.
     * 
     * @return object (could be "this")
     */
    protected abstract Object getMetaObject();

    /**
     * Provides access to the object that has the methods that should
     * be invoked when someone invokes methods on the mbean.
     * 
     * @return object (could be "this")
     */
    protected abstract Object getDelegate();

    /**
     * Throws an exception if there is an error in the meta information. This method is
     * used to test the MBean in isolation to see if the meta information matches up.
     */
    public void mbeanTest() {
        MBeanInfo info = getMBeanInfo(true);
        System.out.println(info.getDescription());
        System.out.println("Attributes:");
        MBeanAttributeInfo[] attrs = info.getAttributes();
        for (int i = 0; i < attrs.length; i++) {
            System.out.println("attr " + i + ": " + attrs[i].getType() + " "
                + attrs[i].getName() + ": " + attrs[i].getDescription());
        }
        System.out.println("Operations:");
        for (int i = 0; i < info.getOperations().length; i++) {
            MBeanOperationInfo o = info.getOperations()[i];
            System.out.println("Operation " + i + ": " + o.getReturnType() + " "
                + o.getName() + ": " + o.getDescription());
            for (int j = 0; j < o.getSignature().length; j++) {
                MBeanParameterInfo s = o.getSignature()[j];
                System.out.println("arg " + j + ": " + s.getType() + " " + s.getName()
                    + ": " + s.getDescription());
            }
        }
        System.out.println("End information");
    }
}
