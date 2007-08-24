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

import com.stc.jmsjca.localization.Localizer;
import com.stc.jmsjca.util.ConnectionUrl;
import com.stc.jmsjca.util.Logger;
import com.stc.jmsjca.util.Str;
import com.stc.jmsjca.util.Utility;

import javax.jms.JMSException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.security.PasswordCredential;
import javax.security.auth.Subject;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * <p>This connection factory is internal to JCA. It is used by the application server
 * to create managed connections and connection factories. While this class is invisible
 * to the application, the connection factories are used by the application.</p>
 *
 * This class acts like a Java bean with getters and setters; these are used to configure
 * the connection factory through the deployment descriptor.
 *
 * @author Frank Kieviet
 * @version $Revision: 1.8 $
 */
public abstract class XManagedConnectionFactory implements ManagedConnectionFactory,
    javax.resource.spi.ResourceAdapterAssociation,
    javax.resource.spi.ValidatingManagedConnectionFactory {
    private static Logger sLog = Logger.getLogger(XManagedConnectionFactory.class);
    private transient PrintWriter mLogWriter;
    private RAJMSResourceAdapter mRA;
    private Map mConnectionFactories; // key=overrideURL, value=Object[]=connection factories
    private Object[] mDefaultConnectionFactories;
    private boolean mProducerPoolingOn;
    private String mConnectionURL;
    private String mUserName;
    private String mPassword;
    private String mOptionsStr;
    private Properties mOptions;
    private String mClientId;
    private long mIdleTimeout = 30000;

    private transient RAJMSObjectFactory mObjFactory;
    
    // For special connectors such as WebLogic
    private transient DestinationCache mQueueCache;
    private transient DestinationCache mTopicCache;
    private transient TxMgr mTxMgr;

    // For lazy eval of options
    private transient boolean mOptionsAreSet;
    private transient String mRAUrl;
    private transient String mRAOptionsStr;

    // Options
    private transient boolean mNoXA;
    private transient boolean mIgnoreTx;
    private transient boolean mClientContainer;
    private transient boolean mBypassRA;
    private transient boolean mDoNotCacheConnectionFactories;
    private transient boolean mStrict;
    private transient String mTxMgrLocatorClass;
    
    // For diagnostics: counts how many MCs were created
    private transient int mCtMCCreated;
    // For diagnostics: counts how many MCs were destroyed
    private transient int mCtMCDestroyed;
    // For diagnostics purposes: keeps track of all MCs created
    private transient Map mMCCreated;
    // For unit testing
    private transient boolean mIsTestModeInvalidConnections;
    private transient TestAllocator mAllocator;
    
    // Caching password credentials; key=Subject; value=PasswordCredential 
    private transient IdentityHashMap mCredentialCache; 
    private static final int MAXCREDENTIALCACHE = 50;

    private static final Localizer LOCALE = Localizer.get();
    
    /**
     * Used for testing: allows derived XManagedConnections to be used so that special
     * failure behavior can be introduced
     */
    public interface TestAllocator {
        /**
         * @param factory factory
         * @param subject subject
         * @param descr descr
         * @return MC
         * @throws ResourceException on failure
         */
        XManagedConnection createConnection(XManagedConnectionFactory factory,
            Subject subject, XConnectionRequestInfo descr) throws ResourceException;
    }
    
    /**
     * Associates this MCF with the containing RA
     *
     * @param ra ResourceAdapter
     */
    public void setResourceAdapter(javax.resource.spi.ResourceAdapter ra) {
        mRA = (RAJMSResourceAdapter) ra;
        mRA.notifyMCFCreated(this);
    }

    /**
     * Returns the RA associated with this MCF
     *
     * @return ResourceAdapter
     */
    public javax.resource.spi.ResourceAdapter getResourceAdapter() {
        return mRA;
    }
    
    private Map getActiveManagedConnections() {
        if (mMCCreated == null) {
            mMCCreated = Collections.synchronizedMap(new WeakHashMap());        
        }
        return mMCCreated;
    }
    

    /**
     * Returns the RA associated with this MCF
     *
     * @return ResourceAdapter
     */
    public RAJMSResourceAdapter getRAJMSResourceAdapter() {
        return mRA;
    }

    /**
     * getObjFactory
     *
     * @return RAJMSObjectFactory
     */
    public RAJMSObjectFactory getObjFactory() {
        return mObjFactory;
    }

    /**
     * <p>From the spec:</p>
     * Creates a Connection Factory instance. The Connection Factory instance gets
     * initialized with a default ConnectionManager provided by the resource adapter.
     *
     * @return EIS-specific Connection Factory instance or javax.resource.cci.ConnectionFactory instance
     * @throws ResourceException Generic exception
     */
    public java.lang.Object createConnectionFactory() throws ResourceException {
        return createConnectionFactory(null);
    }

    /**
     * Gets a connection factory from the cache, or creates one if none is cached
     *
     * @param domain int
     * @param overrideUrl different URL
     * @throws JMSException on failure
     * @return Object
     */
    public synchronized Object getConnectionFactory(int domain, String overrideUrl) throws
        JMSException {
        if (domain >= XConnectionRequestInfo.NDOMAINS) {
            throw new JMSException("Logic fault: invalid domain " + domain);
        }
        
        if (getOptionDoNotCacheConnectionFactories()) {
            Object o = getObjFactory().createConnectionFactory(domain, 
                (RAJMSResourceAdapter) mRA, null, this, overrideUrl);
            return o;
        } else {
            Object[] factories;

            if (overrideUrl == null) {
                if (mDefaultConnectionFactories == null) {
                    mDefaultConnectionFactories = new Object[XConnectionRequestInfo.NDOMAINS];
                }
                factories = mDefaultConnectionFactories;
            } else {
                // Lazy init
                if (mConnectionFactories == null) {
                    mConnectionFactories = new HashMap();
                }

                // Lookup factories with lazy init
                factories = (Object[]) mConnectionFactories.get(overrideUrl);
                if (factories == null) {
                    factories = new Object[XConnectionRequestInfo.NDOMAINS];
                    mConnectionFactories.put(overrideUrl, factories);
                }
            }

            // Find proper factory type with lazy init
            if (factories[domain] == null) {
                RAJMSResourceAdapter ra = (RAJMSResourceAdapter) mRA;
                Object o = getObjFactory().createConnectionFactory(domain, ra, null, this, overrideUrl);
                factories[domain] = o;
            }

            return factories[domain];
        }
    }

    /**
     * Gets a connection factory from the cache, or creates one if none is cached
     *
     * @param descr int
     * @param isXA true if an XA CF should be used
     * @throws JMSException on failure
     * @return Object
     */
    public Object getConnectionFactory(boolean isXA, XConnectionRequestInfo descr) throws
        JMSException {
        return getConnectionFactory(descr.getDomain(isXA), descr.getOverrideUrl());
    }

    /**
     * <p>From the spec:</p>
     * <p>Creates a new physical connection to the underlying EIS resource manager,
     * ManagedConnectionFactory uses the security information (passed as Subject) and
     * additional ConnectionRequestInfo (which is specific to ResourceAdapter and
     * opaque to application server) to create this new connection. End spec.</p>
     *
     * @param subject Caller's security information
     * @param cxRequestInfo Additional resource adapter specific connection request information
     * @return ManagedConnection instance
     * @throws ResourceException generic exception
     */
    public ManagedConnection createManagedConnection(javax.security.auth.Subject subject,
        ConnectionRequestInfo cxRequestInfo) throws ResourceException {
        if (sLog.isDebugEnabled()) {
            sLog.debug("Creating managed connection for subject " + subject
                + " and request info " + cxRequestInfo);
        }

        XConnectionRequestInfo descr = (XConnectionRequestInfo) cxRequestInfo;
        XManagedConnection ret = mAllocator == null ? new XManagedConnection(this,
            subject, descr) : mAllocator.createConnection(this, subject, descr);
        getActiveManagedConnections().put(ret, new Date());
        mCtMCCreated++;
        return ret;
    }
    
    /**
     * Computes the effective userid and password applying the presedence rules:
     * 1) Subject
     * 2) createConnection()
     * 3) MCF
     * 4) RA
     * 
     * @param descr optional XConnectionRequestInfo
     * @param subject optional Subject
     * @return String[2] { userid, password }
     * @throws ResourceException propagated
     */
    String[] getEffectiveUseridAndPassword(XConnectionRequestInfo descr,
        javax.security.auth.Subject subject) throws ResourceException {

        javax.resource.spi.security.PasswordCredential pc = getPasswordCredential(subject);
        String userid = null;
        String password = null;
        if (pc != null) {
            userid = pc.getUserName();
            password = new String(pc.getPassword());
        } else if (descr != null && !Str.empty(descr.getUsername())) {
            userid = descr.getUsername();
            password = descr.getPassword();
        } else if (!Str.empty(getUserName())) {
            userid = getUserName();
            password = getPassword();
        } else if (!Str.empty(getRAJMSResourceAdapter().getUserName())) {
            userid = getRAJMSResourceAdapter().getUserName();
            password = getRAJMSResourceAdapter().getPassword();
        }
        
        return new String[] {userid, password};
    }

    /**
     * @param subject subject to explore (may be null)
     * @return password credential (may be null)
     * @throws ResourceException propagated
     */
    public javax.resource.spi.security.PasswordCredential getPasswordCredential(
        javax.security.auth.Subject subject) throws ResourceException {

        // subject may be null...
        if (subject == null) {
            return null;
        }
        
        // Cache the password credential: the extraction of the password credential out
        // of the subject is expensive. It appears getting the hashcode of a subject
        // is also expensive: therefore use an IdentityHashMap.
        synchronized (this) {
            if (mCredentialCache == null) {
                mCredentialCache = new IdentityHashMap();
            }
            if (mCredentialCache.containsKey(subject)) {
                return (PasswordCredential) mCredentialCache.get(subject);
            }
        }
        
        // Not found... lookup and add to cache
        PasswordCredential ret = Cred.extractPasswordCredential(this, subject);
        synchronized (this) {
            if (mCredentialCache.size() > MAXCREDENTIALCACHE) {
                mCredentialCache.clear();
            }
            mCredentialCache.put(subject, ret);
        }
        return ret;
    }
    
    /**
     * <p>From the spec:</p>
     * Returns a matched connection from the candidate set of connections.
     *
     * ManagedConnectionFactory uses the security info (as in Subject) and information
     * provided through ConnectionRequestInfo and additional Resource Adapter specific
     * criteria to do matching. Note that criteria used for matching is specific to a
     * resource adapter and is not prescribed by the Connector specification.
     *
     * This method returns a ManagedConnection instance that is the best match for
     * handling the connection allocation request.
     *
     * @param connectionSet candidate connection set
     * @param subject caller's security information
     * @param cxRequestInfo additional resource adapter specific connection request information
     * @return ManagedConnection if resource adapter finds an acceptable match otherwise null
     * @throws ResourceException generic exception
     */
    public ManagedConnection matchManagedConnections(java.util.Set connectionSet,
        javax.security.auth.Subject subject,
        ConnectionRequestInfo cxRequestInfo) throws ResourceException {

        XConnectionRequestInfo descr = (XConnectionRequestInfo) cxRequestInfo;
        
        extractOptions();

        for (Iterator it = connectionSet.iterator(); it.hasNext();/*-*/) {
            Object obj = it.next();
            if (obj instanceof XManagedConnection) {
                XManagedConnection mc = (XManagedConnection) obj;
                XManagedConnectionFactory mcf = mc.getManagedConnectionFactory();

                // Managed connection factory MUST be the same
                if (mcf.equals(this)) {
                    // Descriptors should be compatible
                    if (mc.getDescription().isCompatible(descr)) {
                        // Extract credentials
                        String[] uidpw = getEffectiveUseridAndPassword(descr, subject);
                        String userid = uidpw[0];
                        String password = uidpw[1];

                        // Credentials should be compatible
                        if (Str.isEqual(userid, mc.getUserid()) && Str.isEqual(password, mc.getPassword())) {
                            if (sLog.isDebugEnabled()) {
                                sLog.debug("matchManagedConnections: was offered "
                                    + connectionSet.size()
                                    + " connections for " + cxRequestInfo
                                    + "; found a match in " + mc);
                            }
                            
                            return mc;
                        }
                    }
                }
            }
        }

        if (sLog.isDebugEnabled()) {
            sLog.debug("matchManagedConnections: was offered " + connectionSet.size()
                + " connections for " + cxRequestInfo + "; found no match");
        }

        return null;
    }

    /**
     * This method returns a set of invalid ManagedConnection objects chosen from a
     * specified set of ManagedConnection objects.
     *
     * @param toCheck Set
     * @throws ResourceException on failure
     * @return Set
     */
    public Set getInvalidConnections(Set toCheck) throws ResourceException {
        Set ret = new HashSet();
        for (Iterator  iter = toCheck.iterator(); iter.hasNext();/*-*/) {
            ManagedConnection cmc = (ManagedConnection) iter.next();
            if (cmc instanceof XManagedConnection) {
                XManagedConnection mc = (XManagedConnection) cmc;
                if (mc.isInvalid()) {
                    ret.add(mc);
                }
            }

        }
        return ret;
    }

    /**
     * <p>From the spec:</p>
     * Set the log writer for this ManagedConnectionFactory instance.
     * The log writer is a character output stream to which all logging and tracing
     * messages for this ManagedConnectionfactory instance will be printed.
     *
     * ApplicationServer manages the association of output stream with the
     * ManagedConnectionFactory. When a ManagedConnectionFactory object is created the
     * log writer is initially null, in other words, logging is disabled. Once a log
     * writer is associated with a ManagedConnectionFactory, logging and tracing for
     * ManagedConnectionFactory instance is enabled.
     *
     * The ManagedConnection instances created by ManagedConnectionFactory "inherits"
     * the log writer, which can be overridden by ApplicationServer using
     * ManagedConnection.setLogWriter to set ManagedConnection specific logging and
     * tracing.
     *
     * @param out PrintWriter - an out stream for error logging and tracing
     * @throws ResourceException generic exception
     */
    public void setLogWriter(PrintWriter out) throws ResourceException {
        mLogWriter = out;
    }

    /**
     * <p>From the spec:</p> Get the log writer for this ManagedConnectionFactory
     * instance. The log writer is a character output stream to which all logging and
     * tracing messages for this ManagedConnectionFactory instance will be printed.
     * ApplicationServer manages the association of output stream with the
     * ManagedConnectionFactory. When a ManagedConnectionFactory object is created the log
     * writer is initially null, in other words, logging is disabled.
     *
     * @return log writer
     * @throws ResourceException never
     */
    public java.io.PrintWriter getLogWriter() throws ResourceException {
        return mLogWriter;
    }

    private synchronized void extractOptions() {
        // Check if cached options need to be refreshed
        if (mOptionsAreSet && mRAUrl == mRA.getConnectionURL() && mRAOptionsStr == mRA.getOptions()) {
            // Note: in above if-conditions, == is intentional
            return;
        }
        
        String urlstr = mConnectionURL == null ? mRA.getConnectionURL() : mConnectionURL;
        mObjFactory =  ((RAJMSResourceAdapter) mRA).createObjectFactory(urlstr);

        Properties p = new Properties();
        mRAUrl = mRA.lookUpLDAP(mRA.getConnectionURL());
        mRAOptionsStr = mRA.getOptions();

        // Lowest precedence: RA-options
        Str.deserializeProperties(Str.parseProperties(Options.SEP, mRAOptionsStr), p);  

        // Higher precedence: RA-url
        if (mRAUrl != null) {
            getObjFactory().createConnectionUrl(mRAUrl).getQueryProperties(p);
        }

        // Override 1: locally defined options
        Str.deserializeProperties(Str.parseProperties(Options.SEP, mOptionsStr), p);
        mOptions = p;

        // Higher precedence: locally defined url
        if (mConnectionURL != null) {
            getObjFactory().createConnectionUrl(mConnectionURL).getQueryProperties(p);
        }

        // Extract values; system properties have highest precedence
        mClientContainer = Utility.isTrue(p.getProperty(Options.Out.CLIENTCONTAINER), false);
        mClientContainer = Utility.getSystemProperty(Options.Out.CLIENTCONTAINER, mClientContainer);
        mIgnoreTx = Utility.isTrue(p.getProperty(Options.Out.IGNORETX), !mClientContainer);
        mIgnoreTx = Utility.getSystemProperty(Options.Out.IGNORETX, mIgnoreTx);
        mNoXA = Utility.isTrue(p.getProperty(Options.NOXA), mClientContainer);
        mNoXA = Utility.getSystemProperty(Options.NOXA, mNoXA);
        mBypassRA = Utility.isTrue(p.getProperty(Options.Out.BYPASSRA), false);
        mBypassRA = Utility.getSystemProperty(Options.Out.BYPASSRA, mBypassRA);
        mDoNotCacheConnectionFactories = Utility.isTrue(p.getProperty(Options.Out.DONOTCACHECONNECTIONFACTORIES), 
            !getObjFactory().shouldCacheConnectionFactories());
        mStrict = Utility.isTrue(p.getProperty(Options.Out.STRICT), false);
        mStrict = Utility.getSystemProperty(Options.Out.STRICT, mStrict);
        mTxMgrLocatorClass = p.getProperty(Options.TXMGRLOCATOR, TxMgr.class.getName());
        mTxMgrLocatorClass = Utility.getSystemProperty(Options.TXMGRLOCATOR, mTxMgrLocatorClass);

        mOptionsAreSet = true;
    }

    /**
     * <p>From the spec:</p>
     * Returns the hash code for the ManagedConnectionFactory
     *
     * @return hash code for the ManagedConnectionFactory
     */
    public int hashCode() {
        int h = 7;
        h = Str.hash(h, mUserName);
        h = Str.hash(h, mPassword);
        h = Str.hash(h, mConnectionURL);
        h = Str.hash(h, mProducerPoolingOn);
        h = Str.hash(h, mRA);
        h = Str.hash(h, mOptionsStr);
        return h;
    }
    
    /**
     * Extracts the username and password from the parameters passed to createConnection()
     * if any
     * 
     * @param suggestedUsername username
     * @param suggestedPassword password
     * @return non-null array; 0=userid, 1=password
     */
    public String[] getUserIdAndPasswordAndUrl(String suggestedUsername, String suggestedPassword) {
        String userid = null;
        String password = null;
        String url = null;
        
        // Examine specified parameters
        if (suggestedUsername != null) {
            // Is this a URL?
            if (!getObjFactory().isUrl(suggestedUsername)) {
                userid = suggestedUsername;
                password = suggestedPassword;
            } else {
                ConnectionUrl p = getObjFactory().createConnectionUrl(suggestedUsername);
                Properties props = p.getQueryProperties();
                userid = props.getProperty("username");
                password = props.getProperty("password");
                if (password == null) {
                    password = suggestedPassword;
                }
                url = p.toString();
            }
        }
        
        return new String[] {userid, password, url };
    }

    /**
     * <p>
     * From the spec:
     * </p>
     * Check if this ManagedConnectionFactory is equal to another
     * ManagedConnectionFactory.
     * 
     * @param rhs
     *            Object
     * @return true if two instances are equal
     */
    public boolean equals(java.lang.Object rhs) {
        if (rhs == this) {
            return true;
        }
        if (rhs == null) {
            return false;
        }
        if (!(rhs instanceof XManagedConnectionFactory)) {
            return false;
        }
        
        if (!rhs.getClass().equals(this.getClass())) {
            return false;
        }

        XManagedConnectionFactory x = (XManagedConnectionFactory) rhs;

        return Str.isEqual(mUserName, x.mUserName)
            && Str.isEqual(mPassword, x.mPassword)
            && Str.isEqual(mConnectionURL, x.mConnectionURL)
            && isProducerPoolingOn() == x.isProducerPoolingOn()
            && mRA.equals(x.mRA)
            && Str.isEqual(mOptionsStr, x.mOptionsStr);
    }

    /**
     * Turns on producer pooling (session pooling must be on to be effective)
     *
     * @param setOn true to turn on
     */
    public void setProducerPooling(String setOn) {
        mProducerPoolingOn = "true".equalsIgnoreCase(setOn);
        if (sLog.isDebugEnabled()) {
            sLog.debug("Setting mProducerPoolingOn to " + mProducerPoolingOn);
        }
    }

    /**
     * getProducerPooling
     *
     * @return String
     */
    public String getProducerPooling() {
        return Boolean.toString(mProducerPoolingOn);
    }

    /**
     * Returns true if producer pooling is on
     *
     * @return true if on
     */
    public boolean isProducerPoolingOn() {
        return mProducerPoolingOn;
    }

    /**
     * setConnectionURL
     *
     * @param url String
     */
    public void setConnectionURL(String url) {
        if (url != null && url.trim().length() > 0) {
            mConnectionURL = url;
        } else {
            mConnectionURL = null;
        }
    }

    /**
     * getConnectionURL
     *
     * @return String
     */
    public String getConnectionURL() {
        return mConnectionURL;
    }

    /**
     * setClientId
     *
     * @param clientId String
     */
    public void setClientId(String clientId) {
        mClientId = clientId;
    }

    /**
     * getClientId
     *
     * @return String
     */
    public String getClientId() {
        return mClientId;
    }
    
    /**
     * setUserName
     *
     * @param username String
     */
    public void setUserName(String username) {
        mUserName = username;
    }

    /**
     * getUserName
     *
     * @return String
     */
    public String getUserName() {
        return mUserName;
    }

    /**
     * setPassword
     *
     * @param password String
     */
    public void setPassword(String password) {
        mPassword = password;
    }

    /**
     * getPassword
     *
     * @return String
     */
    public String getPassword() {
        return mPassword;
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
        mOptionsAreSet = false;
    }

    /**
     * getOptionsAsProperties
     *
     * @return String
     */
    public Properties getOptionsAsProperties() {
        extractOptions();
        return mOptions;
    }

    /**
     * Returns true if XA has been disabled (for buggy application client)
     *
     * @return boolean
     */
    public boolean getOptionNoXA() {
        extractOptions();
        return mNoXA;
    }

    /**
     * Returns true if running in the client container
     *
     * @return boolean
     */
    public boolean getOptionClientContainer() {
        extractOptions();
        return mClientContainer;
    }

    /**
     * If the transacted-attribute to calls to createXSession(tranacted, ackmode) needs to
     * be ignored, this will return true. This should be the case when running within an
     * application server.
     *
     * @return boolean
     */
    public boolean getOptionIgnoreNonTx() {
        extractOptions();
        return mIgnoreTx;
    }

    /**
     * Returns true if the RA should be bypassed completely
     *
     * @return boolean
     */
    public boolean getOptionBypassRA() {
        extractOptions();
        return mBypassRA;
    }

    /**
     * Returns true if connection factories should not be cached
     *
     * @return boolean
     */
    public boolean getOptionDoNotCacheConnectionFactories() {
        extractOptions();
        return mDoNotCacheConnectionFactories;
    }

    /**
     * Returns true if settings closest to CTS should be used
     *
     * @return boolean
     */
    public boolean getOptionStrict() {
        extractOptions();
        return mStrict;
    }
    
    /**
     * Provides access to the Queue Cache
     * 
     * @return cache, never null
     */
    public synchronized DestinationCache getQueueCache() {
        if (mQueueCache == null) {
            mQueueCache = new DestinationCache();
        }
        return mQueueCache;
    }

    /**
     * Provides access to the Topic Cache
     * 
     * @return cache, never null
     */
    public synchronized DestinationCache getTopicCache() {
        if (mTopicCache == null) {
            mTopicCache = new DestinationCache();
        }
        return mTopicCache;
    }
    
    /**
     * For diagnostics purposes: called when a managed connection is destroyed
     * 
     * @param c connection being destroyed
     */
    public void notifyMCDestroyed(XManagedConnection c) {
        mCtMCDestroyed++;
        getActiveManagedConnections().remove(c);
    }

    /**
     * For diagnostics purposes: dumps all the runtime information on this MCF
     * 
     * @return human readable string
     */
    public String dumpMCFInfo() {
        StringBuffer ret = new StringBuffer();
        ret.append("Class: " + getClass() + ";\n");
        ret.append("Managed connections created: " + mCtMCCreated + ";\n");
        ret.append("Managed connections destroyed: " + mCtMCDestroyed + ";\n");
        ret.append("Current managed connections:\n");
        
        Object[] factories;
        synchronized (getActiveManagedConnections()) {
            factories = getActiveManagedConnections().keySet().toArray();
        }
        
        for (int i = 0; i < factories.length; i++) {
            if (factories[i] != null) {
                XManagedConnection mc = (XManagedConnection) factories[i];
                ret.append("[" + mc.dumpMCInfo() + "]\n");
            }
        }
        
        return ret.toString();
    }

    /**
     * Indicates if this mcf is in a special test mode (for unit testing) that makes
     * all the connections return invalid
     * 
     * @return boolean
     */
    public boolean isTestModeInvalidConnections() {
        return mIsTestModeInvalidConnections;
    }
    
    /**
     * Sets the mcf into a special test mode (for unit testing) that makes all the 
     * connections return invalid when asked isValid()
     * 
     * @param setInvalid true to set invalid
     */
    public void testSetModeInvalidConnections(boolean setInvalid) {
        mIsTestModeInvalidConnections = setInvalid;
    }
    
    /**
     * For testing only: intercepts the creation of a MC
     * 
     * @param allocator (can be null)
     */
    public void testSetAllocator(TestAllocator allocator) {
        mAllocator = allocator;
    }

    /**
     * Provides access to the J2EE transaction manager
     * 
     * @return tx manager, or null if not accessible (e.g. unknown container)
     */
    public synchronized TxMgr getTxMgr() {
        if (mTxMgr == null) {
            try {
                Class c = Class.forName(mTxMgrLocatorClass, false, this.getClass().getClassLoader()); 
                TxMgr txmgr = (TxMgr) c.newInstance();
                extractOptions();
                txmgr.init(mOptions);
                mTxMgr = txmgr;
            } catch (Exception e) {
                sLog.warn(LOCALE.x("E088: Transaction manager locator cannot be initialized: {0}", e), e);
            }
        }            
        return mTxMgr;
    }

    /**
     * @return idle timeout in ms
     */
    public long internalGetIdleTimeout() {
        return mIdleTimeout;
    }
    
    /**
     * Sets the maximum time that a connection can be idle after which it will be 
     * marked as invalid. Idle is defined as not being used to either send or 
     * receive.
     * 
     * @param idleTimeout idletime out in ms
     */
    public void internalSetIdleTimeout(long idleTimeout) {
        if (idleTimeout < 1000) {
            throw new RuntimeException("Invalid timeout [" + idleTimeout 
                + "] ms: should be > 1000 ms");
        }
        mIdleTimeout = idleTimeout;
    }
    /**
     * @return idle timeout in ms
     */

    /**
     * @return String representation of idletimeout
     */
    public String getIdleTimeout() {
        return Long.toString(internalGetIdleTimeout());
    }
    
    /**
     * Sets the maximum time that a connection can be idle after which it will be 
     * marked as invalid. Idle is defined as not being used to either send or 
     * receive.
     * 
     * @param idleTimeout idletime out in ms
     */
    public void setIdleTimeout(String idleTimeout) {
        internalSetIdleTimeout(Integer.parseInt(idleTimeout));
    }
}
