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

import com.stc.jmsjca.localization.LocalizedString;
import com.stc.jmsjca.localization.Localizer;
import com.stc.jmsjca.util.ConnectionUrl;
import com.stc.jmsjca.util.Exc;
import com.stc.jmsjca.util.Logger;
import com.stc.jmsjca.util.Str;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.InvalidPropertyException;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * The resource adapter; exposed through DD
 *
 * @author fkieviet
 * @version $Revision: 1.12 $
 */
public abstract class RAJMSResourceAdapter implements ResourceAdapter, java.io.Serializable {
    private static Logger sLog = Logger.getLogger(RAJMSResourceAdapter.class);    
    // cr 6675833 - improved project deployment time on AIX box when ldap message server 
    // url lookup is used in the project environment
    private static final long RETRY_INTERVAL_MS = 1000; //1000 * 3; 
    private static final int RETRY_COUNT = 0; //10 * 6; 
    private String mConnectionURL;
    private String mUserName;
    private String mPassword;
    private String mMBeanServerDomain;
    private String mMBeanObjectName;
    private String mOptionsStr;
    private String mTransformerMBeanName;
    // Prefix to use when a clientID needs to be automatically generated
    private String mClientIDPrefix;
    private String mConfigurationOverride;
    private String mConfigurationTemplate;
    private String mProjectInfo;
    
    // For closing connections associated with getXAResources()
    private transient List mRecoveryConnections;

    /**
     * BC
     */
    protected transient BootstrapContext mBootCtx;

    /**
     * All current activations
     */
    protected transient List mActivations = new ArrayList();

    private transient RAMBean mAdapterMBean;
    private transient ObjectName mServerMgtMBeanName;

    // For diagnostics purposes: counts the MCFs created
    private transient int mCtMCFCreated;
    
    // For diagnostics purposes: keeps track of all MCFs created
    private transient Map mMCFCreated = Collections.synchronizedMap(new WeakHashMap());
    
    private transient MBeanServer mMBeanServer;
    
    private transient Map mStopListeners;

    private static final Localizer LOCALE = Localizer.get();
    //flag for ldap MBean initialization
    private boolean ldapReady;

    /**
     * Constructor
     */
    protected RAJMSResourceAdapter() {
    }

    /**
     * Called by the AppServer to initialize the Resource Adapter.
     * 
     * @param ctx BootstrapContext
     * @throws ResourceAdapterInternalException failure
     */
    public void start(BootstrapContext ctx) throws ResourceAdapterInternalException {
        this.mBootCtx = ctx;
        checkRecoveryConnections();

        if (sLog.isDebugEnabled()) {
            sLog.debug("Starting RA");
        }
        
        overrideRAConfigFromJNDI();
        
        try {
            // Adjust Connection properties from the jndi resource in case of
            // url=lookup://
            boolean adjusted = adjustActivationSpec(); 
            if (adjusted) {
                if (sLog.isDebugEnabled()) {
                    sLog.debug("Activation spec configuration afer adjustment is: ["
                        + dumpConfiguration() + "]");
                }
            }
            // Create MBean
            if (!Str.empty(getMBeanObjectName())) {
                RAJMSObjectFactory fact = createObjectFactory(getConnectionURL());

                MBeanServer mbeanServer = getMBeanServer();
                ObjectName mbeanName = new ObjectName(getMBeanObjectName());
                mAdapterMBean = fact.createRAMBean(this);
                mbeanServer.registerMBean(mAdapterMBean, mbeanName);
                if (sLog.isDebugEnabled()) {
                    sLog.debug("Registered MBean [" + mAdapterMBean + "] in server ["
                        + mbeanServer.getDefaultDomain() + "] using name [" + mbeanName
                        + "]");
                }
                
                Object serverMgtMBean = fact.getServerMgtMBean(this, null);
                if (sLog.isDebugEnabled()) {
                    sLog.debug("Server mgt mbean=" + serverMgtMBean);
                }
                if (serverMgtMBean == null) {
                    // MBean feature not supported
                } else if (serverMgtMBean instanceof String) {
                    // Refers to an existing mbean name
                    mAdapterMBean.setJmsServerMBean((String) serverMgtMBean);
                } else {
                    // Refers to a newly created mbean; need to register
                    String name = getMBeanObjectName() + ",servermgt=true";
                    if (sLog.isDebugEnabled()) {
                        sLog.debug("Registering server mgt mbean with name " + name);
                    }
                    ObjectName mgtmbeanName = new ObjectName(name);
                    mbeanServer.registerMBean(serverMgtMBean, mgtmbeanName);
                    mServerMgtMBeanName = mgtmbeanName;
                    mAdapterMBean.setJmsServerMBean(name);
                    if (sLog.isDebugEnabled()) {
                        sLog.debug("Registered server mgt mbean with name " + name);
                    }
                }
            }
        } catch (Exception e) {
            killMBean();
            sLog.warn(LOCALE.x("E043: MBean with name [{0}] could not be "
                + "created. The RA will continue to start. The error was: {1}.",
                getMBeanObjectName(), e), e);
        }
    }

    private void killMBean() {
        // Get rid of server mbean if a new one was registered just for this activation
        if (mServerMgtMBeanName != null) {
            try {
                getMBeanServer().unregisterMBean(mServerMgtMBeanName);
            } catch (Exception e) {
                sLog.warn(LOCALE.x("E044: Exception on unregistering server mbean: {0}", e), e);
            }
            mServerMgtMBeanName = null;
        }
        
        // Get rid of normal RA mbean
        if (mAdapterMBean != null) {
            mAdapterMBean.destroy();
            mAdapterMBean = null;
        }
    }

    /**
     * Called by the Application Server to indicate shutdown is imminent. The
     * Application Server should have undeployed all the message endpoints prior
     * to this call, but the RA will iterate through them and ensure that all
     * the message endpoint are no longer consuming messages.
     */
    public void stop() {
        if (sLog.isDebugEnabled()) {
            sLog.debug("Stopping RA");
        }
        
        try {
            checkRecoveryConnections();
            if (mActivations != null) {
                synchronized (mActivations) {
                    for (Iterator i = mActivations.iterator(); i.hasNext();/*-*/) {
                        ActivationBase a = (ActivationBase) i.next();
                        if (sLog.isDebugEnabled()) {
                            sLog.debug("Stopping " + a);
                        }

                        a.deactivate();
                        i.remove();
                    }
                }
            }
        } finally {
            killMBean();
        }
        
        // Notify stop-listeners
        if (mStopListeners != null) {
            RAStopListener[] listeners = (RAStopListener[]) mStopListeners.keySet().toArray(
                new RAStopListener[mStopListeners.keySet().size()]);
            for (int i = 0; i < listeners.length; i++) {
                try {
                    listeners[i].stop();
                } catch (Exception e) {
                    sLog.warn(LOCALE.x("E045: Failed to process stop-listener {0}: {1}", 
                        listeners[i], e), e);
                }
            }
        }

        if (sLog.isDebugEnabled()) {
            sLog.debug("RA is now stopped");
        }
    }

    /**
     * Called by the application server when a message-driven bean
     * (MessageEndpoint) is deployed.
     * 
     * @param endpointFactory MessageEndpointFactory
     * @param spec ActivationSpec
     * @throws ResourceException failure
     */
    public void endpointActivation(MessageEndpointFactory endpointFactory,
        ActivationSpec spec) throws ResourceException {
        checkRecoveryConnections();
        if (mActivations == null) {
            mActivations = new ArrayList();
        }
        
        // Preamble
        if (sLog.isDebugEnabled()) {
            sLog.debug("Activating endpoint with endpointFactory [" + endpointFactory
                + "] and spec [" + spec + "]");
        }

        if (!(spec instanceof RAJMSActivationSpec)) {
            LocalizedString msg = LOCALE.x("E003: EndpointActivation can only be done " +
                    "with an ActivationSpec of class {0}; provided class is [{1}].", 
                    RAJMSActivationSpec.class.getName(), spec.getClass().getName());
            sLog.fatal(msg);
            throw new NotSupportedException(msg.toString());
        }

        if (sLog.isDebugEnabled()) {
            sLog.debug("Activation spec configuration is: ["
                + ((RAJMSActivationSpec) spec).dumpConfiguration() + "]");
        }
        
        // Adjust spec in case of url=lookup://
        boolean adjusted = adjustActivationSpec((RAJMSActivationSpec) spec);
        if (adjusted) {
            if (sLog.isDebugEnabled()) {
                sLog.debug("Activation spec configuration afer adjustment is: ["
                    + ((RAJMSActivationSpec) spec).dumpConfiguration() + "]");
            }
        }

        // Validate spec
        try {
            spec.validate();
        } catch (InvalidPropertyException ex1) {
            if (sLog.isDebugEnabled()) {
                sLog.debug("Validation of Activation Spec failed: " + ex1, ex1);
            }

            throw new NotSupportedException(LOCALE.x("E140: ActivationSpec validation error: {0}", ex1).toString(),
                ex1);
        }

        // Create new activation and register
        ActivationBase a;
        try {
            String url = ((RAJMSActivationSpec) spec).getConnectionURL();
            if (url == null || url.length() == 0) {
                url = getConnectionURL();
            }

            RAJMSObjectFactory fact = createObjectFactory(url);
            a = fact.createActivation(this, endpointFactory,
                (RAJMSActivationSpec) spec);
            a.activate();
            synchronized (mActivations) {
                mActivations.add(a);
            }
        } catch (Throwable ex) {
            LocalizedString msg = LOCALE.x("E010: Could not create Activation " +
                    "for {0} and spec {1}: {2}.", endpointFactory, spec, ex);
            sLog.error(msg, ex);
            throw new NotSupportedException(msg.toString(), ex);
        }

        if (sLog.isDebugEnabled()) {
            sLog.debug("Activation " + a + " succeeded");
        }
    }

    /**
     * Changes the values in an activation spec with values from an MCF and/or its RA.
     * This will only happen if the ConnectionURL of the spec denotes a JNDI name of an
     * MCF (lookup://).
     * 
     * @param spec activation spec to change
     * @return true if the spec was changed
     * @throws ResourceException propagated
     */
    private boolean adjustActivationSpec(RAJMSActivationSpec spec) throws ResourceException {
        boolean changed = false;
        String url = spec.getConnectionURL();
        if (Str.empty(url)) {
            url = getConnectionURL();
        }
        
        if (!Str.empty(url) && url.startsWith(Options.LOCAL_JNDI_LOOKUP)) {
            Context ctx = null;
            String name = url.substring(Options.LOCAL_JNDI_LOOKUP.length()); 
            if (sLog.isDebugEnabled()) {
                sLog.debug("Lookup in JNDI: " + name);
            }
            try {
                // Lookup object
                ctx = new InitialContext();
                Object o = ctx.lookup(name);
                if (sLog.isDebugEnabled()) {
                    sLog.debug("Found in JNDI using [" + name + "]: " + o);
                }
                
                String _url = null;
                String _userid = null;
                String _pw = null;
                Properties options = new Properties();
                {
                    if (!isInstanceOf(o.getClass(), JConnectionFactory.class.getName())) {
                        throw Exc.exc(LOCALE.x("E191: Incompatible object specified: the object must of type ''{0}'' "
                            + "but the object bound to ''{1}'' is of type ''{2}''", JConnectionFactory.class.getName()
                            , url, o.getClass().getName()));
                    }
                    // Merge properties from RA
                    Object ra = invokeMethod(o, "getRA");
                    Object mcf = invokeMethod(o, "getMCF");
                    String temp;
                    if (ra != this) {
                        temp = (String) invokeMethod(ra, "getConnectionURL");
                        if (!Str.empty((temp))) {
                            _url = temp;
                        }
                        temp = (String) invokeMethod(ra, "getUserName");
                        if (!Str.empty(temp)) {
                            _userid = temp;
                        }
                        temp = (String) invokeMethod(ra, "getClearTextPassword");
                        if (!Str.empty(temp)) {
                            _pw = temp;
                        }
                        temp = (String) invokeMethod(ra, "getOptions");
                        Str.deserializeProperties(Str.parseProperties(Options.SEP, temp), options);
                    }
                    
                    // Merge properties from MCF
                    temp = (String) invokeMethod(mcf, "getConnectionURL");
                    if (!Str.empty(temp)) {
                        _url = temp;
                    }
                    temp = (String) invokeMethod(mcf, "getUserName");
                    if (!Str.empty(temp)) {
                        _userid = temp;
                    }
                    temp = (String) invokeMethod(mcf, "getClearTextPassword");
                    if (!Str.empty(temp)) {
                        _pw = temp;
                    }
                    temp = (String) invokeMethod(mcf, "getOptions");
                    Str.deserializeProperties(Str.parseProperties(Options.SEP, temp), options);
                }
                
                
                // Merge properties into spec. URL: always
                if (_url == null) {
                    throw Exc.jmsExc(LOCALE.x("E192: A URL is not specified in the connection " 
                        + "pool or its resource adapter instance"));
                }
                spec.setConnectionURL(_url);

                // username/pw if not set
                if (Str.empty(spec.getUserName())) {
                    spec.setUserName(_userid);
                }
                if (Str.empty(spec.getClearTextPassword())) {
                    spec.setPassword(_pw);
                }
                
                // Options: inherit from mcf but allow overrides in spec; 
                // No need to bother with mark because the options will not be serialized to a single line
                Str.deserializeProperties(spec.getOptions(), options);
                spec.setOptions(Str.serializeProperties(options));

                changed = true;
            } catch (Exception e) {
                throw Exc.rsrcExc(LOCALE.x("E096: Failed to lookup [{0}] in [{1}]: {2}", name, url, e), e);
            } finally {
                if (ctx != null) {
                    try {
                        ctx.close();
                    } catch (Exception ignore) {
                        // ignore
                    }
                }
            }
        }
        
        return changed;
    }
    
    /**
     * Changes the connection properties with values from an MCF and/or its RA.
     * This will only happen if the ConnectionURL of the spec denotes a JNDI name of an
     * MCF (lookup://).
     * @return true if the spec was changed
     * @throws ResourceException propagated
     */
    private boolean adjustActivationSpec() throws ResourceException {
        boolean changed = false;
        String url = getConnectionURL();
        
        if (!Str.empty(url) && url.startsWith(Options.LOCAL_JNDI_LOOKUP)) {
            Context ctx = null;
            String name = url.substring(Options.LOCAL_JNDI_LOOKUP.length()); 
            if (sLog.isDebugEnabled()) {
                sLog.debug("Lookup in JNDI: " + name);
            }
            try {
                // Lookup object
                ctx = new InitialContext();
                Object o = ctx.lookup(name);
                if (sLog.isDebugEnabled()) {
                    sLog.debug("Found in JNDI using [" + name + "]: " + o);
                }
                
                String _url = null;
                String _userid = null;
                String _pw = null;
                Properties options = new Properties();
                {
                    if (!isInstanceOf(o.getClass(), JConnectionFactory.class.getName())) {
                        throw Exc.exc(LOCALE.x("E191: Incompatible object specified: the object must of type ''{0}'' "
                            + "but the object bound to ''{1}'' is of type ''{2}''", JConnectionFactory.class.getName()
                            , url, o.getClass().getName()));
                    }
                    // Merge properties from RA
                    Object ra = invokeMethod(o, "getRA");
                    Object mcf = invokeMethod(o, "getMCF");
                    String temp;
                    if (ra != this) {
                        temp = (String) invokeMethod(ra, "getConnectionURL");
                        if (!Str.empty((temp))) {
                            _url = temp;
                        }
                        temp = (String) invokeMethod(ra, "getUserName");
                        if (!Str.empty(temp)) {
                            _userid = temp;
                        }
                        temp = (String) invokeMethod(ra, "getClearTextPassword");
                        if (!Str.empty(temp)) {
                            _pw = temp;
                        }
                        temp = (String) invokeMethod(ra, "getOptions");
                        Str.deserializeProperties(Str.parseProperties(Options.SEP, temp), options);
                    }
                    
                    // Merge properties from MCF
                    temp = (String) invokeMethod(mcf, "getConnectionURL");
                    if (!Str.empty(temp)) {
                        _url = temp;
                    }
                    temp = (String) invokeMethod(mcf, "getUserName");
                    if (!Str.empty(temp)) {
                        _userid = temp;
                    }
                    temp = (String) invokeMethod(mcf, "getClearTextPassword");
                    if (!Str.empty(temp)) {
                        _pw = temp;
                    }
                    temp = (String) invokeMethod(mcf, "getOptions");
                    Str.deserializeProperties(Str.parseProperties(Options.SEP, temp), options);
                }
                
                
                // Merge properties into spec. URL: always
                if (_url == null) {
                    throw Exc.jmsExc(LOCALE.x("E192: A URL is not specified in the connection " 
                        + "pool or its resource adapter instance"));
                }
                setConnectionURL(_url);

                // username/pw if not set
                 setUserName(_userid);
                 setPassword(_pw);
                
                // Options: inherit from mcf but allow overrides in spec; 
                // No need to bother with mark because the options will not be serialized to a single line
                Str.deserializeProperties(getOptions(), options);
                setOptions(Str.serializeProperties(options));

                changed = true;
            } catch (Exception e) {
                throw Exc.rsrcExc(LOCALE.x("E096: Failed to lookup [{0}] in [{1}]: {2}", name, url, e), e);
            } finally {
                if (ctx != null) {
                    try {
                        ctx.close();
                    } catch (Exception ignore) {
                        // ignore
                    }
                }
            }
        }
        
        return changed;
    }
    
    private Object invokeMethod(Object o, String method) throws SecurityException, NoSuchMethodException
    , IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        Method m = o.getClass().getMethod(method, new Class[0]);
        return m.invoke(o, new Object[0]);
    }
    
    private boolean isInstanceOf(Class c, String actualClassName) {
        if (c == null) {
            return false;
        }

        if (c.getName().equals(actualClassName)) {
            return true;
        }
        boolean result = isInstanceOf(c.getSuperclass(), actualClassName);
        if (result) {
            return true;
        }

        Class[] interfaces = c.getInterfaces();
        if (interfaces == null) { 
            return false;
        }

        for (int i = 0; i < interfaces.length; ++i) {
            result = isInstanceOf(interfaces[i], actualClassName);
            if (result) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Checks for a java.util.Properties object in JNDI and if found, overrides the 
     * configuration of the RA with the one in the Properties object.
     * 
     * @return true if the RA got changed
     */
    public boolean overrideRAConfigFromJNDI() {
        boolean changed = false;
        if (!Str.empty(getConfigurationJNDIName())) {
            Context ctx = null;
            if (sLog.isDebugEnabled()) {
                sLog.debug("Lookup in JNDI: " + getConfigurationJNDIName());
            }
            try {
                // Lookup object
                ctx = new InitialContext();
                Object o = ctx.lookup(getConfigurationJNDIName());
                if (sLog.isDebugEnabled()) {
                    sLog.debug("Found in JNDI using [" + getConfigurationJNDIName() + "]: " + o);
                }
                
                // Cast
                if (!(o instanceof Properties)) {
                    throw Exc.exc(LOCALE.x("E191: Incompatible object specified: the object must of type ''{0}'' "
                        + "but the object bound to ''{1}'' is of type ''{2}''", Properties.class.getName()
                        , getConfigurationJNDIName(), o.getClass().getName()));
                }

                // Change values
                Properties p = (Properties) o;
                setConnectionURL(p.getProperty("MSURL", p.getProperty("ConnectionURL", getConnectionURL())));
                setUserName(p.getProperty("MSUsername", p.getProperty("UserName", getUserName())));
                setPassword(p.getProperty("MSPassword", p.getProperty("Password", getPassword())));
                setOptions(p.getProperty("MSOptions", p.getProperty("Options", getOptions())));
                
                changed = true;
            } catch (NamingException e) {
                if (sLog.isDebugEnabled()) {
                    sLog.debug("Could not find " + getConfigurationJNDIName() + ": " + e, e);
                }
            } catch (Exception e) {
                sLog.warn(LOCALE.x("E193: The configuration overrides bound to JNDI name " 
                    + "''{0}'' could not be applied; the default values will be used instead. " 
                    + "The error was: {1}", getConfigurationJNDIName(), e), e);
            } finally {
                if (ctx != null) {
                    try {
                        ctx.close();
                    } catch (Exception ignore) {
                        // ignore
                    }
                }
            }
        }
        
        return changed;
    }

    /**
     * Called by Application server when the MessageEndpoint (message-driven
     * bean) is undeployed.
     * 
     * @param endpointFactory MessageEndpointFactory
     * @param spec ActivationSpec
     */
    public void endpointDeactivation(MessageEndpointFactory endpointFactory,
        ActivationSpec spec) {
        if (sLog.isDebugEnabled()) {
            sLog.debug("Deactivating endpoint for " + endpointFactory + " and spec "
                + spec);
        }

        ActivationBase a = findActivation(endpointFactory, spec, true);

        // Assert activation is known
        if (a == null) {
            LocalizedString msg = LOCALE.x("E004: EndpointDeactivation was called on an activation"
                + " of which no record could be found. Endpointfactory=[{0}]; spec=[{1}].", 
                endpointFactory, spec);
            sLog.fatal(msg);
            throw new RuntimeException(msg.toString());
        }

        if (sLog.isDebugEnabled()) {
            sLog.debug("Deactivating activation " + a);
        }

        // Deactivate
        a.deactivate();

        if (sLog.isDebugEnabled()) {
            sLog.debug("Activation " + a + " deactivated");
        }
    }

    /**
     * Finds an activation; returns null if not found
     * 
     * @param endpointFactory MessageEndpointFactory
     * @param spec ActivationSpec
     * @param remove boolean
     * @return Activation
     */
    public ActivationBase findActivation(MessageEndpointFactory endpointFactory,
        ActivationSpec spec, boolean remove) {
        if (mActivations == null) {
            mActivations = new ArrayList();
        }
        
        // Assert type is correct
        if (!(spec instanceof RAJMSActivationSpec)) {
            LocalizedString msg = LOCALE.x("E005: EndpointDeactivation can only be " +
                    "done with a spec of the class [{0}]; provided class is [{1}].", 
                RAJMSActivationSpec.class.getName(), spec.getClass().getName());
            sLog.fatal(msg);
            throw new RuntimeException(msg.toString());
        }

        // Find the activation
        ActivationBase a = null;
        synchronized (mActivations) {
            for (Iterator iter = mActivations.iterator(); iter.hasNext();/*-*/) {
                ActivationBase candidate = (ActivationBase) iter.next();
                if (candidate.is(endpointFactory, (RAJMSActivationSpec) spec)) {
                    a = candidate;
                    if (remove) {
                        iter.remove();
                    }
                    break;
                }
            }
        }
        return a;
    }

    /**
     * This method is called by the Application server on the restart of the
     * Application server when there are potential pending transactions. For
     * example, it may be called after a server crash. The Application server
     * requests the XA Resources that correspond to the Activation Specs for the
     * endpoints that it is restarting. It may use those XA Resources to
     * determine transaction status and attempt to commit or rollback.
     * 
     * @param specs ActivationSpec[]
     * @throws ResourceException failure
     * @return XAResource[]
     */
    public javax.transaction.xa.XAResource[] getXAResources(ActivationSpec[] specs)
        throws ResourceException {

        // Reduce the set of specs to ones that point to different servers
        ArrayList uniqueSpecs = new ArrayList();
        Set signatures = new HashSet();
        for (int i = 0; i < specs.length; i++) {
            RAJMSActivationSpec spec = (RAJMSActivationSpec) specs[i];
            String urlstr = spec.getConnectionURL();
            if (urlstr == null || urlstr.length() == 0) {
                urlstr = getConnectionURL();
            }
            RAJMSObjectFactory fact = createObjectFactory(urlstr);

            if (fact.canDo(RAJMSObjectFactory.CANDO_XA) == RAJMSObjectFactory.CAP_NO) {
                continue;
            }
            
            Properties p = new Properties();
            try {
                ConnectionUrl url = fact.getProperties(p, this, spec, null, null);
                String sig = url.toString() + Str.serializeProperties(p);
                if (signatures.add(sig)) {
                    uniqueSpecs.add(spec);
                }
            } catch (JMSException e) {
                sLog.warn(LOCALE.x("E046: Could not get determine a signature for " 
                    + "activationspec [{0}]. This resource will NOT be recovered. "
                    + "The error was: {1}.", spec.dumpConfiguration(), e), e);
            }
        }

        // Get an XAConnection for each spec
        ArrayList connections = new ArrayList();
        ArrayList ret = new ArrayList();
        for (int i = 0; i < uniqueSpecs.size(); i++) {
            RAJMSActivationSpec s = (RAJMSActivationSpec) uniqueSpecs.get(i);
            try {
                String  urlstr = s.getConnectionURL();
                RAJMSObjectFactory fact = createObjectFactory(urlstr);
                ConnectionFactory qcf = fact.createConnectionFactory(
                    XConnectionRequestInfo.DOMAIN_QUEUE_XA, this, s, null, null);
                String username = s.getUserName() == null ? getUserName() 
                    : s.getUserName();
                String password = s.getClearTextPassword() == null ? getClearTextPassword() 
                    : s.getClearTextPassword();
                Connection con = fact.createConnection(qcf,
                    XConnectionRequestInfo.DOMAIN_QUEUE_XA, s, this, username, password);
                connections.add(con);
                Session sess = fact.createSession(con, true, 
                    QueueSession.class, this, s, false, 0);
                XAResource xa = fact.getXAResource(true, sess);
                ret.add(xa);
            } catch (JMSException e) {
                sLog.warn(LOCALE.x("E047: Could not get XAResource for activationspec [{0}]; "
                    + "this resource will NOT be recovered. The error was: {1}.", 
                    s.dumpConfiguration(), e), e);
            }
        }

        // Store connections to close so that they can be stored explicitly
        if (mRecoveryConnections == null) {
            mRecoveryConnections = new ArrayList();
        }
        mRecoveryConnections.addAll(connections);

        return (XAResource[]) ret.toArray(new XAResource[ret.size()]);
    }

    /**
     * Should be called on start/end/createActivation so that connections that
     * were allocated during recovery can be closed
     */
    private void checkRecoveryConnections() {
        if (mRecoveryConnections != null) {
            for (Iterator iter = mRecoveryConnections.iterator(); iter.hasNext();) {
                Connection c = (Connection) iter.next();
                try {
                    c.close();
                } catch (Exception e) {
                    sLog.warn(LOCALE.x("E048: Failure upon closing recovery connection: {0}", e), e);
                }
            }
            mRecoveryConnections = null;
        }
    }

    /**
     * toString
     * 
     * @return String
     */
    public String toString() {
        return "[" + this.getClass().getName() + ":with "
            + (mActivations != null ? mActivations.size() : 0) + " activations]";
    }

    /**
     * Returns the bootstrap context
     *
     * @return BootstrapContext
     */
    public BootstrapContext getBootstrapCtx() {
        return mBootCtx;
    }

    /**
     * getConnectionURL
     *
     * @return String
     */
    public final String getConnectionURL() {
        return mConnectionURL;
    }

    /**
     * setConnectionURL
     *
     * @param ConnectionURL
     *            String
     */
    public void setConnectionURL(String ConnectionURL) {
        this.mConnectionURL = ConnectionURL;
    }

    /**
     * getUserName
     *
     * @return String
     */
    public final String getUserName() {
        return mUserName;
    }

    /**
     * setUserName
     * 
     * @param UserName String
     */
    public final void setUserName(String UserName) {
        this.mUserName = UserName;
    }

    /**
     * getPassword
     * 
     * @return String
     */
    public final String getClearTextPassword() {
        return Str.pwdecode(mPassword);
    }

    /**
     * getPassword
     * 
     * @return String
     */
    public final String getPassword() {
        return mPassword;
    }

    /**
     * setPassword
     * 
     * @param Password String
     */
    public final void setPassword(String Password) {
        this.mPassword = Password;
    }
    
    /**
     * @param override jndi name of Properties set that overrides the configuration
     */
    public final void setConfigurationJNDIName(String override) {
        this.mConfigurationOverride = override;
    }

    /**
     * Getter for configurationOverride
     *
     * @return String jdni name
     */
    public final String getConfigurationJNDIName() {
        return mConfigurationOverride;
    }
    
    /**
     * Sets the configuration template; the template is not used by the RA itself but
     * only by external configuration tools. It's populated by CAPS. Having this method
     * here avoids warnings in the log about missing setter methods.
     * 
     * @param template String
     */
    public final void setConfigurationTemplate(String template) {
        this.mConfigurationTemplate = template;
    }
    
    /**
     * @return template
     */
    public final String getConfigurationTemplate() {
        return mConfigurationTemplate;
    }

    /**
     * equals
     * 
     * @param other Object
     * @return boolean
     */
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof RAJMSResourceAdapter)) {
            return false;
        }
        RAJMSResourceAdapter that = (RAJMSResourceAdapter) other;

        return Str.isEqual(this.mConnectionURL, that.mConnectionURL)
            && Str.isEqual(this.mUserName, that.mUserName)
            && Str.isEqual(this.mPassword, that.mPassword)
            && Str.isEqual(this.mOptionsStr, that.mOptionsStr)
            && Str.isEqual(this.mMBeanServerDomain, that.mMBeanServerDomain);
    }
    
    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        int ret = 37;
        ret = Str.hash(ret, mConnectionURL);
        ret = Str.hash(ret, mUserName);
        ret = Str.hash(ret, mPassword);
        ret = Str.hash(ret, mOptionsStr);
        ret = Str.hash(ret, mMBeanServerDomain);
        return ret;
    }

    /**
     * dumpConfiguration
     * 
     * @return String
     */
    public String dumpConfiguration() {
        return "connectionURL=" + mConnectionURL + "; mbeanServerDomain="
            + mMBeanServerDomain + "; username=" + mUserName + "; password="
            + Str.password(mPassword);
    }
    
    /**
     * Called by the MCF when setRA() is called on it; used for diagnostics purposes
     * so that the RA can maintain a list of all created MCFs.
     * 
     * @param fact factory created
     */
    public void notifyMCFCreated(XManagedConnectionFactory fact) {
        mCtMCFCreated++;
        mMCFCreated.put(fact, new Date());
    }
    
    /**
     * For diagnostics purposes only: dumps all info about the managed connection
     * factories that this RA was associated with
     * 
     * @return String representation of all managed connection factories
     */
    public String dumpMCFInfo() {
        StringBuffer ret = new StringBuffer();
        ret.append("Managed connection factories created: " + mCtMCFCreated + ";\n");
        ret.append("Current managed connection factories:\n");
        
        Object[] factories;
        synchronized (mMCFCreated) {
            factories = mMCFCreated.keySet().toArray();
        }
        
        for (int i = 0; i < factories.length; i++) {
            if (factories[i] != null) {
                XManagedConnectionFactory mcf = (XManagedConnectionFactory) factories[i];
                ret.append("[" + mcf.dumpMCFInfo() + "]\n");
            }
        }
        
        return ret.toString();
    }

    /**
     * getMBeanServerDomain
     * 
     * @return String
     */
    public String getMBeanServerDomain() {
        return mMBeanServerDomain;
    }

    /**
     * setMBeanServerDomain
     * 
     * @param domain String
     */
    public void setMBeanServerDomain(String domain) {
        mMBeanServerDomain = domain;
    }

    /**
     * Set the prefix to be used when a clientID needs to be automatically generated
     * 
     * @param prefix String
     */
    public void setClientIDPrefix(String prefix) {
        mClientIDPrefix = prefix;
    }
    
    /**
     * Return the prefix to be used when a clientID needs to be automatically generated
     * 
     * @return prefix
     */
    public String getClientIDPrefix() {
        return mClientIDPrefix;
    }

    /**
     * getOptions
     *
     * @return String
     */
    public String getOptions() {
        return mOptionsStr;
    }
    
    /**
     * setOptions
     * 
     * @param options String
     */
    public void setOptions(String options) {
        mOptionsStr = options;
    }

    /**
     * Getter
     * 
     * @return mbean object name (optional
     */
    public String getMBeanObjectName() {
        return mMBeanObjectName;
    }

    /**
     * Sets the mbean object name; if set, the RA will have an MBean that 
     * can be used for monitoring and managing.
     * 
     * @param beanObjectName String
     */
    public void setMBeanObjectName(String beanObjectName) {
        mMBeanObjectName = beanObjectName;
    }
    
    /**
     * Gets the name of the MBean used for LDAP look ups
     * 
     * @return mbean object name
     */
    public String getTransformerMBeanName() {
        return mTransformerMBeanName;
    }

    /**
     * Sets the name of the MBean used for LDAP look ups
     * 
     * @param beanObjectName String
     */
    public void setTransformerMBeanName(String beanObjectName) {
        mTransformerMBeanName = beanObjectName;
    }
    
    private void safeClose(Context ctx) {
        if (ctx != null) {
            try {
                ctx.close();
            } catch (NamingException ignore) {
                // ignore
            }
        }
    }
    
    /**
     * Obtains an MBeanServer using the domain name specified in the RA, or the default
     * (first one) if none specified
     *
     * @throws JMSException if no server could be obtained
     * @return MBeanServer
     */
    public MBeanServer getMBeanServer() throws JMSException {
        if (mMBeanServer != null) {
            return mMBeanServer;
        }
        
        MBeanServer mbeanServer = null;
        try {
            // Prefer Weblogic MBeanServer bound in JNDI
            InitialContext ctx = null;
            try {
                ctx = new InitialContext();
                mbeanServer = (MBeanServer) ctx.lookup("java:comp/jmx/runtime");
            } catch (Exception ignore) {
                // ignore
            } finally {
                safeClose(ctx);
            }
            
            // Try other MBeanServer
            if (mbeanServer == null) {
                // Find last mbean server in list, or first that matches the specified 
                // domain
                Iterator i = MBeanServerFactory.findMBeanServer(null).iterator();
                do {
                    if (!i.hasNext()) {
                        break;
                    }
                    mbeanServer = (MBeanServer) i.next();
                } while (!mbeanServer.getDefaultDomain().equals(getMBeanServerDomain()));
            }
            
            // Still none found; create one
            if (mbeanServer == null) {
                mbeanServer = MBeanServerFactory.createMBeanServer(
                    getMBeanServerDomain());
            }
        } catch (Exception e) {
            throw Exc.jmsExc(LOCALE.x("E141: No MBeanServer found; specified domain = [{0}]"
                , getMBeanServerDomain()));
        }
        if (sLog.isDebugEnabled()) {
            sLog.debug("Found MBeanServer [" + mbeanServer.getDefaultDomain()
                + "]; specified domain = [" + getMBeanServerDomain() + "]");
        }
        
        mMBeanServer = mbeanServer;
        
        return mMBeanServer;
    }
    
    /**
     * look up in LDAP data specified as being bound therein, if required
     * 
     * @param ldapURL possibly LDAP URL where we can find the value of
     * some parameter set in the environment 
     * @return if ldapURL is not an LDAP URL returns ldapURL unchanged, 
     * otherwise the value bound at the given ldap URL. 
     */
    public String lookUpLDAP(String ldapURL) {
        if (ldapURL == null || (!ldapURL.startsWith("ldap://") && !ldapURL.startsWith("ldaps://"))) {
            return ldapURL;
        }        

        try {
            if (getTransformerMBeanName() != null && getTransformerMBeanName().length() != 0) {
                ObjectName mbeanName = new ObjectName(getTransformerMBeanName());
                MBeanServer mbeanServer = getMBeanServer(); 
                
                // check if MBean is available due to the indefinite ordering of 
                // components deployment, need a better solution 
                Set objectNames = mbeanServer.queryNames(mbeanName, null);
                int reTryCount = 0;
                while (objectNames.isEmpty()) {
                    if (reTryCount++ >= RETRY_COUNT) {
                        break;
                    }
                    try {
                        Thread.sleep(RETRY_INTERVAL_MS);
                    } catch (Exception ex) {
                        // ignore
                    }
                    objectNames = mbeanServer.queryNames(mbeanName, null);
                }
                
                String[] signatures = {"java.lang.String"};
                String[] parameters = {ldapURL};    
                
                if (sLog.isDebugEnabled()) {
                    sLog.debug("Transforming " + ldapURL + " using MBean [" + getTransformerMBeanName() + "]");
                }
                Object result = mbeanServer.invoke(mbeanName, "attemptTransform", parameters, signatures);
                ldapReady = true;
                return (String) result;
                
            }
        } catch (Exception ex) {
            if (ldapReady) {
                sLog.warn(LOCALE.x("E049: Could not look up string [{0}]"
                    + " in ldap using MBean [{1}]: {2}.", ldapURL, mTransformerMBeanName,
                    ex), ex);
            } else {
                if (sLog.isDebugEnabled()) {
                    sLog.debug(LOCALE.x("E049: Could not look up string [{0}]"
                        + " in ldap using MBean [{1}]: {2}.", ldapURL,
                        mTransformerMBeanName, ex), ex);
                }
            }
        }
        return ldapURL;
    }

    /**
     * Registers a listener that is invoked when stop() is called
     * 
     * @param listener listener to call
     */
    public synchronized void addStopListener(RAStopListener listener) {
        if (mStopListeners == null) {
            mStopListeners = Collections.synchronizedMap(new WeakHashMap());
        }
        mStopListeners.put(listener, null);
    }

    /**
     * @param urlstr url to derive the type of JMS server for so that the right 
     *   object factory can be created; for static resource adapters, the connection
     *   url may be null
     * @return object factory, new or cached
     */
    public abstract RAJMSObjectFactory createObjectFactory(String urlstr);

    /**
     * Getter for projectInfo; for CAPS only, not used by JMSJCA
     *
     * @return String
     */
    public String getProjectInfo() {
        return mProjectInfo;
    }

    /**
     * Setter for projectInfo, for CAPS only -- not used for JMSJCA
     *
     * @param projectInfo StringThe projectInfo to set.
     */
    public void setProjectInfo(String projectInfo) {
        mProjectInfo = projectInfo;
    }
}
