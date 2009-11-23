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

package com.stc.jmsjca.unifiedjms;

import com.stc.jmsjca.core.RAJMSActivationSpec;
import com.stc.jmsjca.core.RAJMSResourceAdapter;
import com.stc.jmsjca.core.XConnectionRequestInfo;
import com.stc.jmsjca.core.XManagedConnectionFactory;
import com.stc.jmsjca.util.Exc;
import com.stc.jmsjca.util.Str;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Session;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;

/**
 * For Java Bean provider. This is a way of instantiating and configuring connectino
 * factories through reflection.
 *
 * @author Frank Kieviet
 * @version $Revision$
 */
public class GJRObjectFactoryJavaBean extends GJRObjectFactory implements Serializable {
//    private static Logger sLog = Logger.getLogger(GJRObjectFactoryJavaBean.class);

    /**
     * When used as the destName, indicates that this is a JavaBean destination, i.e. the 
     * destination needs to be created using the destinationProperties
     */
    public static final String BEANMODE_DEST = "BEANMODE DESTINATION";

    /**
     * The key for the JMdestination properties in the JMSJCA options 
     */
    public static final String DESTINATION_PROPERTIES = "DESTINATION PROPERTIES";
        
    private static final Localizer LOCALE = Localizer.get();
    
    /**
     * Picks the last not-null value
     * 
     * @param <T>
     * @param t1
     * @param t2
     * @return
     */
    private static <T> T lastNotNull(T t1, T t2) {
        return t2 == null ? t1 : t2;
    }

    /**
     * Converts a string into a parameter type used in reflection
     * 
     * @param target target class to convert the string to
     * @param value string to convert
     * @return converted value
     */
    private static Object convertToParameterType(Class<?> target, String value) {
        try {
            if (target == String.class) {
                return value;
            }
            if (target == Object.class) {
                return value;
            }
            if (target == int.class || target == Integer.class) {
                return Integer.parseInt(value);
            }
            if (target == byte.class || target == Byte.class) {
                return Byte.parseByte(value);
            }
            if (target == short.class || target == Short.class) {
                return Short.parseShort(value);
            }
            if (target == long.class || target == Long.class) {
                return Long.parseLong(value);
            }
            if (target == float.class || target == Float.class) {
                return Float.parseFloat(value);
            }
            if (target == double.class || target == Double.class) {
                return Double.parseDouble(value);
            }
            if (target == boolean.class || target == Boolean.class) {
                return Boolean.parseBoolean(value);
            }
            throw Exc.rtexc(LOCALE.x("E713: Unknown target type {0}", target));
        } catch (Exception e) {
            throw Exc.rtexc(LOCALE.x("E714: The value {0} could not be converted to {1}: {2}", value, target, e), e);
        }
    }

    /**
     * Populates a bean with the specified values using a single method, e.g. setProperty(key, value)
     * 
     * @param setterMethodName e.g setProperty(key, value)
     * @param obj bean to modify
     * @param props key value pairs to set
     */
    private void invokeCommonSetterMethod(String setterMethodName, Object obj, Map<String, String> props) {
        Class<?>[] params = new Class[] {String.class, String.class };
        Method m;
        try {
            m = obj.getClass().getMethod(setterMethodName, params);
        } catch (Exception e) {
            throw Exc.rtexc(LOCALE.x("E715: The specified bean setter method {0} could not be accessed: {1}", 
                setterMethodName, e));
        }
        Class<?>[] parameterTypes = m.getParameterTypes();
        if (parameterTypes.length != 2) {
            throw Exc.rtexc(LOCALE.x("E716: The specified bean setter method {0} should take 2 arguments.", m));
        }
        
        for (Map.Entry<String, String> kv : props.entrySet()) {
            try {
                Object[] args = new Object[2];
                args[0] = convertToParameterType(parameterTypes[0], kv.getKey());
                args[1] = convertToParameterType(parameterTypes[1], kv.getValue());
                m.invoke(obj, args);
            } catch (Exception e) {
                throw Exc.rtexc(LOCALE.x("E712: Could not set property {0} (value={1}) using method {2}: {3}", 
                    kv.getKey(), kv.getValue(), m, e), e);
            }
        }
    }

    /**
     * Calls setter methods on the specified object. Behavior has to be consistent with
     * GenericJMSRA: e.g. non existing properties are ignored. 
     * 
     * @param obj
     * @param props
     */
    private void invokeSetterMethods(Object obj, Map<String, String> props) {
        Method[] methods = obj.getClass().getMethods();

        for (Method m : methods) {
            String name = m.getName();
            if (name.startsWith("set")) {
                try {
                    String propName = name.substring(3);
                    if (props.containsKey(propName)) {
                        String propValue = props.get(propName);
                        Class<?>[] parameterTypes = m.getParameterTypes();
                        if (parameterTypes.length == 1) {
                            Object[] values = new Object[1];
                            values[0] = convertToParameterType(parameterTypes[0], propValue);
                            m.invoke(obj, values);
                        }
                    }
                } catch (Exception e) {
                    throw Exc.rtexc(LOCALE.x("E717: Could not set property for {0}: {1}", name, e), e);
                }
            }
        }
    }
    
    /**
     * @see com.stc.jmsjca.core.RAJMSObjectFactory#createConnectionFactory(
     * int, com.stc.jmsjca.core.RAJMSResourceAdapter, 
     * com.stc.jmsjca.core.RAJMSActivationSpec, com
     * .stc.jmsjca.core.XManagedConnectionFactory, 
     * java.lang.String)
     */
    @Override
    public ConnectionFactory createConnectionFactory(int domain, RAJMSResourceAdapter raUncast,
        RAJMSActivationSpec spec, XManagedConnectionFactory fact, String overrideUrl) throws JMSException {
        
        RAUnifiedResourceAdapter ra = (RAUnifiedResourceAdapter) raUncast;
        
        ConnectionFactory ret = null;

        // Get the connection properties
        Properties p = new Properties();
        getProperties(p, ra, spec, fact, overrideUrl);
        
        // Reduce options set to ra + one alternative
        GJRCommonCfg alt = ra;
        if (spec != null) {
            alt = (RAUnifiedActivationSpec) spec;
        }
        if (fact != null) {
            alt = (UnifiedMCFBase) fact;
        }
        
        // Deduce classname
        String classname;
        switch (domain) {
        case XConnectionRequestInfo.DOMAIN_QUEUE_NONXA:
            classname = lastNotNull(ra.getQueueConnectionFactoryClassName(), alt.getQueueConnectionFactoryClassName());
            break;
        case XConnectionRequestInfo.DOMAIN_QUEUE_XA:
            classname = lastNotNull(ra.getXAQueueConnectionFactoryClassName(), alt.getXAQueueConnectionFactoryClassName());
            break;
        case XConnectionRequestInfo.DOMAIN_TOPIC_NONXA:
            classname = lastNotNull(ra.getTopicConnectionFactoryClassName(), alt.getTopicConnectionFactoryClassName());
            break;
        case XConnectionRequestInfo.DOMAIN_TOPIC_XA:
            classname = lastNotNull(ra.getXATopicConnectionFactoryClassName(), alt.getXATopicConnectionFactoryClassName());
            break;
        case XConnectionRequestInfo.DOMAIN_UNIFIED_NONXA:
            classname = lastNotNull(ra.getConnectionFactoryClassName(), alt.getConnectionFactoryClassName());
            break;
        case XConnectionRequestInfo.DOMAIN_UNIFIED_XA:
            classname = lastNotNull(ra.getXAConnectionFactoryClassName(), alt.getXAConnectionFactoryClassName());
            break;
        default:
            throw Exc.jmsExc(LOCALE.x("E710: Logic fault: invalid domain {0}", Integer.toString(domain)));
        }

        // Instantiate factory
        try {
            Class<?> c = Class.forName(classname);
            ret = (ConnectionFactory) c.newInstance();
        } catch (Exception e) {
            throw Exc.jmsExc(LOCALE.x("E711: Could not load or instantiate the specified " 
                + "connection factory class (classname={0}): {1}", classname, e), e);
        }
        
        // Populate factory
        String beanpropsstr = lastNotNull(ra.getConnectionFactoryProperties(), alt.getConnectionFactoryProperties());
        Map<String, String> beanprops = GJRTool.parseToProperties(beanpropsstr);
        String commonSetter = lastNotNull(ra.getCommonSetterMethodName(), alt.getCommonSetterMethodName()); 
        if (Str.empty(commonSetter)) {
            invokeSetterMethods(ret, beanprops);
        } else {
//beanprops.put("imqAddressList", "mq://localhost:7676/"); // BAD TODO            
            invokeCommonSetterMethod(commonSetter, ret, beanprops);
        }
        
        return ret;
    }
    
    /**
     * @see com.stc.jmsjca.core.RAJMSObjectFactory#adjustDeliveryMode(int, boolean)
     */
    @Override
    public int adjustDeliveryMode(int mode, boolean xa) {
        int newMode = mode;
        // Don't take chances with serial mode
        if (mode == RAJMSActivationSpec.DELIVERYCONCURRENCY_SERIAL) {
            newMode = RAJMSActivationSpec.DELIVERYCONCURRENCY_SYNC;
        }
        return newMode;        
    }
    
    /**
     * Picks the last value that is not null for the specified key in the specified
     * set of Property objects
     * 
     * @param key
     * @param ps
     * @return
     */
    private String lastNotNull(String key, Properties... ps) {
        String ret = null;
        for (Properties p : ps) {
            if (p != null) {
                String v = p.getProperty(key);
                if (v != null) {
                    ret = v;
                }
            }
        }
        return ret;
    }

    /**
     * createDestination()
     *
     * @param sess Session
     * @param isXA boolean
     * @param isTopic boolean
     * @param activationSpec RAJMSActivationSpec
     * @param fact MCF
     * @param raUncast RAJMSResourceAdapter
     * @param destName String
     * @param options Options
     * @param sessionClass domain
     * @return Destination
     * @throws JMSException failure
     */
    @Override
    public Destination createDestination(Session sess, boolean isXA, boolean isTopic,
        RAJMSActivationSpec activationSpec, XManagedConnectionFactory fact,  RAJMSResourceAdapter raUncast,
        String destName, Properties options, Class<?> sessionClass) throws JMSException {
        
        // Encode a destination name from the properties
        if (Str.empty(destName)) {
            destName = BEANMODE_DEST;
        }
        
        return super.createDestination(sess, isXA, isTopic, activationSpec, fact, raUncast, destName, options, sessionClass);
    }
    
//    private static class Merged {
//        GJRCommonCfg ra;
//        GJRCommonCfg alt;
//        
//        public Merged(RAJMSResourceAdapter raUncast, RAJMSActivationSpec specUncast, XManagedConnectionFactory factUncast) {
//            ra = (GJRCommonCfg) raUncast;
//            alt = ra;
//            if (specUncast != null) {
//                alt = (GJRCommonCfg) specUncast;
//            }
//            if (factUncast != null) {
//                alt = (GJRCommonCfg) factUncast;
//            }
//        }
//
//        /**
//         * Getter for ra
//         *
//         * @return GJRCommonCfg
//         */
//        public final GJRCommonCfg getRa() {
//            return ra;
//        }
//
//        /**
//         * Getter for alt
//         *
//         * @return GJRCommonCfg
//         */
//        public final GJRCommonCfg getAlt() {
//            return alt;
//        }
//
//    }
    
    /**
     * @see com.stc.jmsjca.core.RAJMSObjectFactory#instantiateDestination(javax.jms.Session, boolean, boolean, 
     * com.stc.jmsjca.core.RAJMSActivationSpec, com.stc.jmsjca.core.XManagedConnectionFactory, 
     * com.stc.jmsjca.core.RAJMSResourceAdapter, java.lang.String, java.lang.Class, java.util.Properties[])
     */
    @Override
    protected Destination instantiateDestination(Session sess, boolean isXA, boolean isTopic,
        RAJMSActivationSpec activationSpec, XManagedConnectionFactory fact,  RAJMSResourceAdapter raUncast,
        String destName, Class<?> sessionClass, Properties... options) throws JMSException {
        
        Destination ret = null;
        if (!BEANMODE_DEST.equals(destName)) {
            ret = super.instantiateDestination(sess, isXA, isTopic, activationSpec, fact, 
                raUncast, destName, sessionClass, options);
        } else {
            // Reduce options set to ra + one alternative
            GJRCommonCfg ra = (GJRCommonCfg) raUncast;
            GJRCommonCfg alt = ra;
            if (activationSpec != null) {
                alt = (RAUnifiedActivationSpec) activationSpec;
            }
            if (fact != null) {
                alt = (UnifiedMCFBase) fact;
            }

            String classname = isTopic ? lastNotNull(ra.getTopicClassName(), alt.getTopicClassName()) 
                : lastNotNull(ra.getQueueClassName(), alt.getQueueClassName());

            // Instantiate destination
            try {
                Class<?> c = Class.forName(classname);
                ret = (Destination) c.newInstance();
            } catch (Exception e) {
                throw Exc.jmsExc(LOCALE.x("E719: Could not load or instantiate the specified " 
                    + "destination class (classname={0}): {1}", classname, e), e);
            }

            // Populate factory
            String beanpropsstr = lastNotNull(DESTINATION_PROPERTIES, options);
            if (Str.empty(beanpropsstr)) { 
                beanpropsstr = ((RAUnifiedActivationSpec) activationSpec).getDestinationProperties();
            }
            Map<String, String> beanprops = GJRTool.parseToProperties(beanpropsstr);
            String commonSetter = lastNotNull(ra.getCommonSetterMethodName(), alt.getCommonSetterMethodName()); 
            if (Str.empty(commonSetter)) {
                invokeSetterMethods(ret, beanprops);
            } else {
                invokeCommonSetterMethod(commonSetter, ret, beanprops);
            }
        }

        return ret;
    }    
}