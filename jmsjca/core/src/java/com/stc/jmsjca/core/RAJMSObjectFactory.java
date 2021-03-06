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
import com.stc.jmsjca.util.Exc;
import com.stc.jmsjca.util.Logger;
import com.stc.jmsjca.util.Str;
import com.stc.jmsjca.util.UrlParser;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionConsumer;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageEOFException;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.ServerSessionPool;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.jms.XAConnection;
import javax.jms.XAConnectionFactory;
import javax.jms.XAQueueConnection;
import javax.jms.XAQueueConnectionFactory;
import javax.jms.XAQueueSession;
import javax.jms.XASession;
import javax.jms.XATopicConnection;
import javax.jms.XATopicConnectionFactory;
import javax.jms.XATopicSession;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * An object factory for JMS provider specific objects; also provides some provider
 * specific utilities.
 *
 * @author fkieviet
 * @version $Revision: 1.19 $
 */
public abstract class RAJMSObjectFactory {
    private static Logger sLog = Logger.getLogger(RAJMSObjectFactory.class);
    /**
     * Capability
     */
    public static final int CAP_YES = 1;
    /**
     * Capability
     */
    public static final int CAP_NO = 0;
    /**
     * Capability
     */
    public static final int CAP_UNKNOWN = -1;
    /**
     * Query parameter for capabilities
     */
    public static final int CANDO_XA = 0;

    private static final Localizer LOCALE = Localizer.get();

    /**
     * Returns if this RA has a particular capability
     *
     * @param what one of CANDO_XXX
     * @return one of CAP_XXX
     */
    public int canDo(int what) {
        switch (what) {
        case CANDO_XA : return CAP_YES;
        default:
            return CAP_UNKNOWN;
        }
    }
    
    /**
     * Allows the RA to veto a delivery mode and put in a different mode instead, 
     * e.g. NSJMS does not support async in XA mode and should use sync mode instead
     * 
     * @param mode one of RAJMSActivationSpec.DELIVERYCONCURRENCY_XXXX
     * @param xa true if this delivery will use XA
     * @return same or changed mode
     */
    public int adjustDeliveryMode(int mode, boolean xa) {
        return mode;
    }
    
    /**
     * Creates an activation
     *
     * @param ra RAJMSResourceAdapter
     * @param endpointFactory MessageEndpointFactory
     * @param spec RAJMSActivationSpec
     * @return Activation
     * @throws Exception on invalid config
     */
    public ActivationBase createActivation(RAJMSResourceAdapter ra,
        MessageEndpointFactory endpointFactory, RAJMSActivationSpec spec) throws Exception {
        
        ActivationBase ret = null;
        
        // Check for special distributed durable subscribers
        if (RAJMSActivationSpec.TOPIC.equals(spec.getDestinationType())) {
            String subname = spec.getSubscriptionName();
            if (subname != null && subname.startsWith(Options.Subname.PREFIX)) {
                UrlParser u = new UrlParser(subname);
                Properties p = u.getQueryProperties();
                if ("1".equals(p.getProperty(Options.Subname.DISTRIBUTION_TYPE, "0"))) {
                    ret = new TopicToQueueActivation(ra, endpointFactory, spec);
                }
            }
        }
        
        if (ret == null) {
            ret = new Activation(ra, endpointFactory, spec);            
        }
        
        return ret;
    }

    /**
     * createConnectionFactory
     *
     * @param domain boolean
     * @param ra RAJMSResourceAdapter
     * @param activationSpec RAJMSActivationSpec
     * @param fact XManagedConnectionFactory
     * @param overrideUrl override URL: don't use URL from RA, CF, or activation spec (may be null)
     * @return ConnectionFactory
     * @throws JMSException failure
     */
    public abstract ConnectionFactory createConnectionFactory(int domain,
        RAJMSResourceAdapter ra, RAJMSActivationSpec activationSpec,
        XManagedConnectionFactory fact, String overrideUrl) throws JMSException;

    /**
     * createConnection
     *
     * @param fact ConnectionFactory
     * @param domain boolean
     * @param activationSpec RAJMSActivationSpec
     * @param ra RAJMSResourceAdapter
     * @param username String
     * @param password String
     * @return Connection
     * @throws JMSException failure
     */
    public Connection createConnection(Object fact, int domain,
        RAJMSActivationSpec activationSpec, RAJMSResourceAdapter ra, String username,
        String password) throws JMSException {

        // need to ensure that the user and password aren't an indirect reference, i.e.
        // actually an LDAP URL where the real value is bound
        String uname = ra.lookUpLDAP(username);
        String pwd = ra.lookUpLDAP(password);
        
        switch (domain) {
        case XConnectionRequestInfo.DOMAIN_QUEUE_NONXA:
            return Str.empty(uname) 
            ? ((QueueConnectionFactory) fact).createQueueConnection() 
            : ((QueueConnectionFactory) fact).createQueueConnection(uname, pwd);
        case XConnectionRequestInfo.DOMAIN_QUEUE_XA:
            return Str.empty(uname) 
            ? ((XAQueueConnectionFactory) fact).createXAQueueConnection() 
            : ((XAQueueConnectionFactory) fact).createXAQueueConnection(uname, pwd);
        case XConnectionRequestInfo.DOMAIN_TOPIC_NONXA:
            return Str.empty(uname) 
            ? ((TopicConnectionFactory) fact).createTopicConnection() 
            : ((TopicConnectionFactory) fact).createTopicConnection(uname, pwd);
        case XConnectionRequestInfo.DOMAIN_TOPIC_XA:
            return Str.empty(uname) 
            ? ((XATopicConnectionFactory) fact).createXATopicConnection() 
            : ((XATopicConnectionFactory) fact).createXATopicConnection(uname, pwd);
        case XConnectionRequestInfo.DOMAIN_UNIFIED_NONXA:
            return Str.empty(uname) 
            ? ((ConnectionFactory) fact).createConnection()
            : ((ConnectionFactory) fact).createConnection(uname, pwd);
        case XConnectionRequestInfo.DOMAIN_UNIFIED_XA:
            return Str.empty(uname) 
            ? ((XAConnectionFactory) fact).createXAConnection() 
            : ((XAConnectionFactory) fact).createXAConnection(uname, pwd);
        default:
            throw Exc.jmsExc(LOCALE.x("E133: Unknown domain {0}", Integer.toString(domain)));
        }
    }
    
    /**
     * Creates a provider specific UrlParser
     * 
     * @param s connectionURL to parse
     * @return parser
     */
    public ConnectionUrl createConnectionUrl(String s) {
        return new UrlParser(s);
    }

    /**
     * Gets the connection type properties and connection URL
     *
     * @param p properties to fill in
     * @param ra resouce adapter
     * @param spec activation spec
     * @param fact factory
     * @param overrideUrl optional URL specified in createConnection(URL, password)
     * @return url parser
     * @throws JMSException on incorrect URL
     */
    public ConnectionUrl getProperties(Properties p, RAJMSResourceAdapter ra, RAJMSActivationSpec spec,
            XManagedConnectionFactory fact, String overrideUrl) throws JMSException {
        
        // Obtain URL from (1) override, (2) activationspec or fact, (3) RA
        String urlstr = overrideUrl;
        if (urlstr == null && spec != null && !Str.empty(spec.getConnectionURL())) {
            urlstr = spec.getConnectionURL();
        }
        if (urlstr == null && fact != null && !Str.empty(fact.getConnectionURL())) {
            urlstr = fact.getConnectionURL();
        }
        if (urlstr == null) {
            urlstr = ra.getConnectionURL();
        }

        // need to ensure that this URL isn't an indirect reference, i.e.
        // actually an LDAP URL where the real value is bound
        String realUrl = ra.lookUpLDAP(urlstr);
        
        ConnectionUrl url = createConnectionUrl(realUrl);
        validateAndAdjustURL(url);

        // Connection properties: from (1) options-field in RA, (2) options-field
        // in activation spec or fact, (3) from connection URL
        Str.deserializeProperties(Str.parseProperties(Options.SEP, ra.getOptions()), p);
        if (spec != null) {
            Str.deserializeProperties(Str.parseProperties(Options.SEP, spec.getOptions()), p);
        }
        if (fact != null) {
            Str.deserializeProperties(Str.parseProperties(Options.SEP, fact.getOptions()), p);
        }
        if (!Str.empty(realUrl)) {
            url.getQueryProperties(p);
        }
        
        return url;
    }

    /**
     * Checks the validity of the URL; adjusts connection parameters if necessary
     *
     * @param url UrlParser
     * @throws javax.jms.JMSException on format failure
     * @return boolean true if the url specified url object was changed by this validation
     */
    public boolean validateAndAdjustURL(ConnectionUrl url) throws JMSException {
        return false;
    }

    /**
     * createSession; if XA is specified, this will return a javax.jms.XASession 
     * XAQueueSession or XATopicSession; these may or may not be implementing resp.
     * Session, QueueSession and TopicSession. Call getNonXASession() to obtain the 
     * resp. Session, QueueSession and TopicSession.
     *
     * @param conn Connection
     * @param isXA boolean
     * @param sessionClass boolean
     * @param ra RAJMSResourceAdapter
     * @param transacted boolean
     * @param ackmode int
     * @param activationSpec RAJMSActivationSpec
     * @return Session Session
     * @throws JMSException failure
     */
    public Session createSession(Connection conn, boolean isXA, Class<?> sessionClass,
        RAJMSResourceAdapter ra, RAJMSActivationSpec activationSpec, boolean transacted,
        int ackmode) throws JMSException {
        if (isXA) {
            if (sessionClass == TopicSession.class) {
                return ((XATopicConnection) conn).createXATopicSession();
            } else if (sessionClass == QueueSession.class) {
                return ((XAQueueConnection) conn).createXAQueueSession();
            } else if (sessionClass == Session.class) {
                return ((XAConnection) conn).createXASession();
            }
        } else {
            if (sessionClass == TopicSession.class) {
                return ((TopicConnection) conn).createTopicSession(transacted, ackmode);
            } else if (sessionClass == QueueSession.class) {
                return ((QueueConnection) conn).createQueueSession(transacted, ackmode);
            } else if (sessionClass == Session.class) {
                return conn.createSession(transacted, ackmode);
            }
        }
        throw Exc.rtexc(LOCALE.x("E131: Unknown class: {0}", sessionClass));
    }

    /**
     * Looks up a destination in local JNDI if the destination name starts with
     * lookup://
     * 
     * @param destName destination name
     * @return null if no such prefix; a concrete destination, or an admin destination
     * @throws JMSException on lookup failure or null
     */
    public Destination adminDestinationLookup(String destName) throws JMSException {
        Destination ret = null;
        if (Str.empty(destName)) {
            throw Exc.jmsExc(LOCALE.x("E095: The destination should not be empty or null"));
        }
        
        if (destName.startsWith(Options.LOCAL_JNDI_LOOKUP)) {
            Context ctx = null;
            String name = destName.substring(Options.LOCAL_JNDI_LOOKUP.length()); 
            if (sLog.isDebugEnabled()) {
                sLog.debug("Lookup in JNDI: " + name);
            }
            try {
                ctx = new InitialContext();
                ret = (Destination) ctx.lookup(name);
                if (sLog.isDebugEnabled()) {
                    sLog.debug("Found in JNDI using [" + name + "]: " + ret);
                }
            } catch (Exception e) {
                throw Exc.jmsExc(LOCALE.x("E096: Failed to lookup [{0}] in [{1}]: {2}", name, destName, e), e);
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
        ret = checkGeneric(ret);
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
     * @param ra RAJMSResourceAdapter
     * @param destName String
     * @param options optional settings for destination creation (may be null)
     * @param sessionClass exact interface class of the session
     * @return Destination
     * @throws JMSException failure
     */
    public Destination createDestination(Session sess, boolean isXA, boolean isTopic,
        RAJMSActivationSpec activationSpec, XManagedConnectionFactory fact,  RAJMSResourceAdapter ra,
        String destName, Properties options, Class<?> sessionClass) throws JMSException {
        
        if (sLog.isDebugEnabled()) {
            sLog.debug("createDestination(" + destName + ")");
        }

        if (Str.empty(destName)) {
            throw Exc.jmsExc(LOCALE.x("E095: The destination should not be empty or null"));
        }

        // Check for lookup:// destination: this may return an admin destination 
        Destination ret = adminDestinationLookup(destName);
        
        // Check if this is a GenericJMSRA destination, if so this will return a JMSJCA admin destination
        ret = checkGeneric(ret);
        
        // Unwrap admin destination if necessary
        Properties options2 = null;
        if (ret != null && ret instanceof AdminDestination) {
            AdminDestination admindest = (AdminDestination) ret;
            destName = admindest.retrieveCheckedName();
            options2 = admindest.retrieveProperties();
            
            if (sLog.isDebugEnabled()) {
                sLog.debug(ret + " is an admin object: embedded name: " + destName);
            }
            ret = null;
        }
        
        // Needs to parse jmsjca:// format?
        Properties options3 = new Properties();
        if (ret == null && destName.startsWith(Options.Dest.PREFIX)) {
            UrlParser u = new UrlParser(destName);
            options3 = u.getQueryProperties();

            // Reset name from options
            if (Str.empty(options3.getProperty(Options.Dest.NAME))) {
                throw Exc.jmsExc(LOCALE.x("E207: The specified destination string [{0}] does not " 
                    + "specify a destination name. Destination names are specified using " 
                    + "the ''name'' key, e.g. ''jmsjca://?name=Queue1''.", 
                    options3.getProperty(Options.Dest.ORIGINALNAME)));
            }
            destName = options3.getProperty(Options.Dest.NAME);
        }
        
        // Create if necessary
        if (ret == null) {
            ret = instantiateDestination(sess, isXA, isTopic, activationSpec, fact, ra, destName
                , sessionClass, options, options2, options3);
        }
        
        return ret;
    }
    
    /**
     * Instantiates a destination. Called from createDestination() which does all the 
     * unwrapping. Default implementation: calls createQueue() or createTopic().
     * 
     * @param sess Session
     * @param isXA true if XA
     * @param isTopic true if topic
     * @param activationSpec spec
     * @param fact mcf
     * @param ra ra
     * @param destName name of the destination, does no longer contain jmsjca:// or lookup://
     * @param sessionClass session class
     * @param options Series of options in more specific order: original options, 
     *   options from admin destination, and options from jmsjca:// notation  
     * @return destination; should NOT return null
     * @throws JMSException propagated
     */
    protected Destination instantiateDestination(Session sess, boolean isXA, boolean isTopic,
        RAJMSActivationSpec activationSpec, XManagedConnectionFactory fact,  RAJMSResourceAdapter ra,
        String destName, Class<?> sessionClass, Properties... options) throws JMSException {

        if (sLog.isDebugEnabled()) {
            sLog.debug("Creating " + destName + " using createQueue()/createTopic()");
        }
        if (!isTopic) {
            return getNonXASession(sess, isXA, sessionClass).createQueue(destName);
        } else {
            return getNonXASession(sess, isXA, sessionClass).createTopic(destName);
        }
    }
    
    /**
     * Returns the last non-null value from the specified array of Properties
     * 
     * @param key
     * @param ps
     * @return
     */
    public static String getLastNotNull(String key, Properties... ps) {
        String ret = null;
        for (Properties p : ps) {
            if (p != null) {
                ret = p.getProperty(key, ret);
            }
        }
        
        return ret;
    }
    
    
    
    /**
     * Computes a message selector with substitutions
     * 
     * @param ra RA
     * @param spec activation spec
     * @return selector
     * @throws JMSException on parse errors
     */
    public String getMessageSelector(RAJMSResourceAdapter ra, RAJMSActivationSpec spec) throws JMSException {
        try {
            
            Properties q = new Properties();
            getProperties(q, ra, spec, null, null);
            
            String ret = q.getProperty(Options.In.OPTION_SELECTOR);
            if (Str.empty(ret)) {
                ret = spec.getMessageSelector();
            }
            
            String specsel = spec.getMessageSelector() == null ? "" : spec.getMessageSelector();
            
            if (!Str.empty(ret)) {
                // Determine subscriber name
                String subname = spec.getSubscriptionName();
                if (subname != null && subname.startsWith(Options.Subname.PREFIX)) {
                    UrlParser u = new UrlParser(subname);
                    Properties p = u.getQueryProperties();
                    subname = p.getProperty(Options.Subname.SUBSCRIBERNAME);
                }

                // Setup substitution parameters
                final Map<String, String> map = new HashMap<String, String>();
                map.put(Options.Selector.SUB_NAME, subname == null ? "" : subname);
                map.put(Options.Selector.SELECTOR, specsel);
                if (Str.empty(spec.getMessageSelector())) {
                    map.put(Options.Selector.ANDSELECTOR, "");
                    map.put(Options.Selector.SELECTORAND, "");
                } else {
                    map.put(Options.Selector.ANDSELECTOR, "and (" + specsel + ")");
                    map.put(Options.Selector.SELECTORAND, "(" + specsel + ") and");
                }

                // MsgSelector
                int[] nResolved = new int[1];
                int[] nUnresolved = new int[1];
                Str.Translator t = new Str.Translator() {
                    public String get(String key) {
                        String ret = map.get(key);
                        if (ret == null) {
                            ret = System.getProperty(key);
                        }
                        return ret;
                    }
                };
                ret = Str.substituteAntProperty(ret, t , nResolved, nUnresolved);

                if (sLog.isDebugEnabled()) {
                    sLog.debug("Selector [" + spec.getMessageSelector() + "] --> [" + ret + "]"
                        + " with " + nResolved[0] + " resolved, " + nUnresolved[0] + " unresolved.");
                }
            }

            return ret;
        } catch (Exception e) {
            throw Exc.jmsExc(LOCALE.x("E116: Could not compute message selector. Selector in "
                + "activation spec is [{0}], exception is: {1}", spec.getMessageSelector(), e), e);
        }
    }

    /**
     * Creates a message consumer for the inbound part of the RA
     *
     * @param sess Session
     * @param isXA boolean
     * @param isTopic boolean
     * @param dest Destination
     * @param spec RAJMSActivationSpec
     * @param ra RAJMSResourceAdapter
     * @throws JMSException failure
     * @return MessageConsumer
     */
    public MessageConsumer createMessageConsumer(Session sess, boolean isXA,
        boolean isTopic, Destination dest, RAJMSActivationSpec spec,
        RAJMSResourceAdapter ra) throws JMSException {
        if (isXA) {
            if (isTopic) {
                if (RAJMSActivationSpec.DURABLE.equals(spec.getSubscriptionDurability())) {
                    try {
                        return ((XATopicSession) sess).getTopicSession().
                        createDurableSubscriber((Topic) dest,
                            spec.getSubscriptionName(),
                            getMessageSelector(ra, spec), false);
                    } catch (JMSException e) {
                        throw new Exc.ConsumerCreationException(e);
                    }
                } else {
                    return ((XATopicSession) sess).getTopicSession().
                        createSubscriber((Topic) dest,
                            getMessageSelector(ra, spec), false);
                }
            } else {
                return ((XAQueueSession) sess).getQueueSession().createReceiver(
                    (Queue) dest, getMessageSelector(ra, spec));
            }
        } else {
            if (isTopic) {
                if (RAJMSActivationSpec.DURABLE.equals(spec.getSubscriptionDurability())) {
                    try {
                        return sess.createDurableSubscriber((Topic) dest,
                            spec.getSubscriptionName(),
                            getMessageSelector(ra, spec), false);
                    } catch (JMSException e) {
                        throw new Exc.ConsumerCreationException(e);
                    }
                } else {
                    return ((TopicSession) sess).
                    createSubscriber((Topic) dest,
                        getMessageSelector(ra, spec), false);
                }
            } else {
                return ((QueueSession) sess).createReceiver((Queue) dest,
                    getMessageSelector(ra, spec));
            }
        }
    }

    /**
     * Creates a message consumer for the inbound part of the RA
     *
     * @param sess Session
     * @param isXA boolean
     * @param isTopic boolean
     * @param dest Destination
     * @param ra RAJMSResourceAdapter
     * @throws JMSException failure
     * @return MessageConsumer
     */
    public MessageProducer createMessageProducer(Session sess, boolean isXA,
        boolean isTopic, Destination dest,
        RAJMSResourceAdapter ra) throws JMSException {
        if (isXA) {
            if (isTopic) {
                return ((XATopicSession) sess).getTopicSession().createPublisher((Topic) dest);
            } else {
                return ((XAQueueSession) sess).getQueueSession().createSender((Queue) dest);
            }
        } else {
            if (isTopic) {
                return ((TopicSession) sess).createPublisher((Topic) dest);
            } else {
                // Patch: domain of session is not properly propagated for TopicToQueue
                // Delivery hence the instanceof check
                if (sess instanceof QueueSession) {
                    return ((QueueSession) sess).createSender((Queue) dest);
                } else {
                    return sess.createProducer(dest);
                }
            }
        }
    }

    /**
     * createConnectionConsumer
     *
     * @param conn Session
     * @param isXA boolean
     * @param isTopic boolean
     * @param isDurable boolean
     * @param activationSpec RAJMSActivationSpec
     * @param ra RAJMSResourceAdapter
     * @param dest Destination
     * @param subscriptionName String
     * @param selector String
     * @param pool ServerSessionPool
     * @param batchsize batchsize propagated to connection consumer
     * @return ConnectionConsumer
     * @throws JMSException failure
     */
    public ConnectionConsumer createConnectionConsumer(Connection conn, boolean isXA,
        boolean isTopic, boolean isDurable, RAJMSActivationSpec activationSpec,
        RAJMSResourceAdapter ra, Destination dest, String subscriptionName,
        String selector, ServerSessionPool pool, int batchsize) throws JMSException {
        try {
            if (isXA) {
                if (isTopic) {
                    if (isDurable) {
                        return ((XAConnection) conn).createDurableConnectionConsumer((Topic)
                            dest, subscriptionName, selector, pool, batchsize);
                    } else {
                        return ((XAConnection) conn).createConnectionConsumer(dest, selector, pool, batchsize);
                    }
                } else {
                    return ((XAConnection) conn).createConnectionConsumer(dest, selector, pool, batchsize);
                }
            } else {
                if (isTopic) {
                    if (isDurable) {
                        return conn.createDurableConnectionConsumer((Topic)
                            dest, subscriptionName, selector, pool, batchsize);                    
                    } else {
                        return conn.createConnectionConsumer(dest, selector, pool, batchsize);                    
                    }
                } else {
                    return conn.createConnectionConsumer(dest, selector, pool, batchsize);
                }
            }
        } catch (JMSException ex) {
            throw new Exc.ConsumerCreationException(ex);
        }
    }
    
    /**
     * Gets the XAResource out of a Session; returns null if the specified XA flag is
     * false.
     *
     * @param isXA boolean
     * @param s Session
     * @return XAResource
     * @throws JMSException on failure
     */
    public XAResource getXAResource(boolean isXA, Session s) throws JMSException {
        return !isXA ? null : ((XASession) s).getXAResource();
    }

    /**
     * Creates a provider specific sessionConnection
     *
     * @param connectionFactory Object
     * @param objfact RAJMSObjectFactory
     * @param ra RAJMSResourceAdapter
     * @param mc XManagedConnection
     * @param descr XConnectionRequestInfo
     * @param isXa boolean
     * @param isTransacted boolean
     * @param acknowledgmentMode int
     * @param sessionClass Class
     * @throws JMSException failure
     * @return SessionConnection
     */
    public SessionConnection createSessionConnection(Object connectionFactory,
        RAJMSObjectFactory objfact, RAJMSResourceAdapter ra,
        XManagedConnection mc,
        XConnectionRequestInfo descr, boolean isXa, boolean isTransacted,
        int acknowledgmentMode, Class<?> sessionClass)
    throws JMSException {
        return new GenericSessionConnection(connectionFactory, objfact, ra,
            mc, descr, isXa, isTransacted, acknowledgmentMode, sessionClass);
    }

    /**
     * createActivationMBean
     *
     * @param activation Activation
     * @return Object
     */
    public ActivationMBean createActivationMBean(Activation activation) {
        return new ActivationMBean(activation);
    }

    /**
     * Returns true if the specified string may be a recognised URL
     *
     * @param url String
     * @return true if may be URL
     */
    public abstract boolean isUrl(String url);

    /**
     * Sets the ClientID
     * 
     * @param connection to set on
     * @param clientID ID to set
     * @throws JMSException on failure
     */
    public void setClientID(Connection connection, String clientID) throws JMSException {
        try {
            connection.setClientID(clientID);
        } catch (JMSException e) {
            throw new Exc.ConsumerCreationException(e);
        }
    }

    /**
     * Sets the clientID for durable subscribers if not already set, and if the 
     * clientID is specified in the spec.
     * 
     * @param connection connection
     * @param isTopic isTopic
     * @param spec activation spec
     * @param ra ra
     * @throws JMSException on failure
     */
    public void setClientID(Connection connection, boolean isTopic,
        RAJMSActivationSpec spec, RAJMSResourceAdapter ra) throws JMSException {
        if (isTopic && RAJMSActivationSpec.DURABLE.equals(spec.getSubscriptionDurability())) {
            if (spec.getClientId() != null && spec.getClientId().length() != 0) {
                String currentClientId = connection.getClientID();
                if (currentClientId == null || currentClientId.length() == 0) {
                    // Set it
                    setClientID(connection, spec.getClientId());
                } else {
                    if (spec.getClientId().equals(currentClientId)) {
                        // ok: already set
                    } else {
                        sLog.warn(LOCALE.x("E042: Ignoring ClientID: the ClientID is " 
                            + "already set to [{0}]; cannot set to [{1}] as required in "
                            + "activationspec [{2}]", currentClientId, spec.getClientId(), spec)); 
                    }
                }
            }
        }
    }

    /**
     * Sets the clientID from the activation spec. If none is specified, it will set
     * a synthetic generated one as CLIENDID-SUBNAME. If the connection already has a
     * different ClientID, a warning will be logged.    
     * 
     * @param connection Connection
     * @param isTopic true if TopicConnection
     * @param spec ActivationSpec
     * @param ra RA
     * @throws JMSException propagated
     */
    protected void setClientIDIfNotSpecified(Connection connection, boolean isTopic,
        RAJMSActivationSpec spec, RAJMSResourceAdapter ra) throws JMSException {
        if (isTopic
            && RAJMSActivationSpec.DURABLE.equals(spec.getSubscriptionDurability())) {
            // Ensure a clientID will be set
            String newClientId = spec.getClientId();
            boolean notSpecified = false;
            if (newClientId == null || newClientId.length() == 0) {
                newClientId = "CLIENTID-" + spec.getSubscriptionName();
                notSpecified = true;
            }

            String currentClientId = connection.getClientID();
            if (currentClientId == null || currentClientId.length() == 0) {
                // Set it
                setClientID(connection, newClientId);
            } else {
                if (newClientId.equals(currentClientId)) {
                    // ok: already set
                } else {
                    if (notSpecified) {
                        // Apparently the clientID was specified in the
                        // connection factory, and the user did not specify a
                        // clientid
                    } else {
                        sLog.warn(LOCALE.x("E195: ClientID is already set to [{0}]"
                                + "; cannot set to [{1}] as required in "
                                + "activationspec [{2}]", currentClientId, newClientId,
                                spec));
                    }
                }
            }
        }
    }

    /**
     * Gets the non-XA type session from an XA session, e.g. a QueueSession from an XAQueueSession.
     * For non-XA, simply will return the same object.
     * 
     * @param session the XA session
     * @param isXA indicates if XA
     * @param sessionClass the type (queue, topic, unified)
     * @return the non-XA sessioin
     * @throws JMSException propagated
     */
    public Session getNonXASession(Session session, boolean isXA, Class<?> sessionClass) throws JMSException {
        if (!isXA) {
            return session;
        } else {
            if (sessionClass == TopicSession.class) {
                return ((XATopicSession) session).getTopicSession();
            } else if (sessionClass == QueueSession.class) {
                return ((XAQueueSession) session).getQueueSession();
            } else if (sessionClass == Session.class) {
                return ((XASession) session).getSession();
            }
        }
        throw Exc.rtexc(LOCALE.x("E131: Unknown class: {0}", sessionClass));
    }

    /**
     * Creates an MBean for the RA; the MBean can be used to monitor the RA
     * as well as provide management capabilities for the JMS server.
     * 
     * @param adapter adapter
     * @return MBean
     */
    public RAMBean createRAMBean(RAJMSResourceAdapter adapter) {
        return new RAMBean(this, adapter); 
    }

    /**
     * Returns the type of JMS Server this RA services, e.g. STCMS, WAVE, STCMS453, ...
     * 
     * @return String with type
     */
    public abstract String getJMSServerType();

    /**
     * This returns an mbean that can be used to manage the JMS server that this RA is 
     * associated with. It can either return (1) null if this feature is not supported,
     * (2) a string that represents the ObjectName of an mbean that already exists, or
     * (3) an object that is a newly created mbean. In the latter case, the caller is
     * responsible for registering the object in an mbean server and cleaning it up.
     * 
     * @param ra the resource adapter
     * @param spec an optional activation spec (may be null)
     * @return null, a string, or an mbean object.
     * @throws JMSException on failure
     */
    public Object getServerMgtMBean(RAJMSResourceAdapter ra, RAJMSActivationSpec spec)
        throws JMSException {
        return null;
    }
    
    /**
     * Opportunity for provider to clean up connection before it is returned to the pool
     * For example, it may be necessary to unset clientID.
     *      
     * @param con XManagedConnection
     */
    public void cleanup(XManagedConnection con) {
    }
    
    /**
     * Provider specific check if the connection is invalid.
     * Called by XManagedConnection.cleanup().
     *
     * @param con XManagedConnection
     * @return boolean true if the coonnection is invalid
     */
    public boolean isInvalid(XManagedConnection con) {
        return false;
    }
    
    /**
     * Creates an unstarted delivery object
     * 
     * @param mode type of delivery, one of RAJMSActivationSpec.DELIVERYCONCURRENCY_XXX 
     * @param activation the activation associated with this delivery
     * @param stats running statistics
     * @return a new Delivery object
     * @throws Exception on failure
     */
    public Delivery createDelivery(int mode, Activation activation, DeliveryStats stats)
        throws Exception {
        Delivery ret = null;
        switch (mode) {
        case RAJMSActivationSpec.DELIVERYCONCURRENCY_CC:
            ret = new CCDelivery(activation, stats);
            break;
        case RAJMSActivationSpec.DELIVERYCONCURRENCY_SERIAL:
            ret = new SerialDelivery(activation, stats);
            break;
        case RAJMSActivationSpec.DELIVERYCONCURRENCY_SYNC:
            ret = new SyncDelivery(activation, stats);
            break;
        default:
            throw Exc.exc(LOCALE.x("E140 Invalid concurrency ''{0}''",
                Integer.toString(activation.getActivationSpec().getInternalDeliveryConcurrencyMode())));
        }
        return ret;
    }
    
    /**
     * Provides a filter so that providers can invoke some action just before or 
     * immediately after message delivery.   
     * 
     * @param target onMessageListener that needs to be invoked by any filter
     * @param isXA true if XA
     * @return non-null MessageListener
     */
    public MessageListener getMessagePreprocessor(MessageListener target, boolean isXA) {
        return target;
    }

    /**
     * Copies a JMS message from one type to the other
     * 
     * @param toCopy message to copy
     * @param s session to be used to create the new msg
     * @param isXA true if XA
     * @param isTopic true if topic
     * @param ra owning RA
     * @return new message
     * @throws JMSException propagated
     */
    public Message copyMessage(Message toCopy, Session s, boolean isXA,
        boolean isTopic, RAJMSResourceAdapter ra) throws JMSException {
        Message ret = null;
        
        // Check for multiple interfaces
        int nItf = 0;

        if (toCopy instanceof TextMessage) {
            nItf++;
        } else if (toCopy instanceof BytesMessage) {
            nItf++;
        } else if (toCopy instanceof MapMessage) {
            nItf++;
        } else if (toCopy instanceof ObjectMessage) {
            nItf++;
        } else if (toCopy instanceof StreamMessage) {
            nItf++;
        }
        if (nItf > 1) {
            throw Exc.jmsExc(LOCALE.x("E188: Cannot determine message type: the message [{0}]" 
                + "implements multiple interfaces ({1}).", toCopy, Integer.toString(nItf)));
        }
        
        // Create a new message and copy the payload
        if (toCopy instanceof TextMessage) {
            ret = s.createTextMessage(((TextMessage) toCopy).getText());
        } else if (toCopy instanceof BytesMessage) {
            BytesMessage in = (BytesMessage) toCopy;
            BytesMessage b = s.createBytesMessage();
            byte[] buf = new byte[4 * 1024];
            for (;;) {
                int n = in.readBytes(buf);
                if (n < 0) {
                    break;
                }
                b.writeBytes(buf, 0, n);
            }
            ret = b;
        } else if (toCopy instanceof MapMessage) {
            MapMessage in = (MapMessage) toCopy;
            MapMessage out = s.createMapMessage();
            for (Enumeration<?> en = in.getMapNames(); en.hasMoreElements();/*-*/) {
                String name = (String) en.nextElement();
                Object o = in.getObject(name);
                out.setObject(name, o);
            }
            ret = out;
        } else if (toCopy instanceof ObjectMessage) {
            ObjectMessage in = (ObjectMessage) toCopy;
            ret = s.createObjectMessage(in.getObject());
        } else if (toCopy instanceof StreamMessage) {
            StreamMessage in = (StreamMessage) toCopy;
            StreamMessage out = s.createStreamMessage();
            for (;;) {
                Object o;
                try {
                    o = in.readObject();
                } catch (MessageEOFException stop) {
                    break;
                }
                if (o == null) {
                    break;
                }
                out.writeObject(o);
            }
            ret = out;
        } else {
            ret = s.createMessage();
        }
        
        // Copy user properties
        for (Enumeration<?> en = toCopy.getPropertyNames(); en.hasMoreElements();/*-*/) {
            String name = (String) en.nextElement();
            Object o = toCopy.getObjectProperty(name);
            String originalName = name;
            if (name.startsWith("JMSX")) {
                name = Options.MessageProperties.MSG_PROP_PREFIX + name;
            }
            if (o instanceof Integer) {
                ret.setIntProperty(name, ((Integer) o).intValue());
            } else if (o instanceof Long) {
                ret.setLongProperty(name, ((Long) o).longValue());
            } else if (o instanceof String) {
                ret.setStringProperty(name, ((String) o).toString());
            } else if (o instanceof Boolean) {
                ret.setBooleanProperty(name, ((Boolean) o).booleanValue());
            } else if (o instanceof Byte) {
                ret.setByteProperty(name, ((Byte) o).byteValue());
            } else if (o instanceof Short) {
                ret.setShortProperty(name, ((Short) o).shortValue());
            } else if (o instanceof Float) {
                ret.setFloatProperty(name, ((Float) o).floatValue());
            } else if (o instanceof Double) {
                ret.setDoubleProperty(name, ((Double) o).doubleValue());
            } else {
                throw Exc.jmsExc(LOCALE.x("E189: Unknown property type for {0}: {1}"
                    , originalName, o.getClass().getName()));
            }
            
            // Copy other properties
            if (toCopy.getJMSCorrelationID() != null) {
                ret.setJMSCorrelationID(toCopy.getJMSCorrelationID());
            }
            if (toCopy.getJMSReplyTo() != null) {
                ret.setJMSReplyTo(toCopy.getJMSReplyTo());
            }
            if (toCopy.getJMSType() != null) {
                ret.setJMSType(toCopy.getJMSType());
            }
        }
        
        return ret;
    }

    /**
     * Sends a msg (for DLQ)
     * 
     * @param isTopic true if topic
     * @param producer object to use to send
     * @param m message to send
     * @param priority priority
     * @param deliveryMode deliveyMode
     * @throws JMSException propagated
     */
    public void send(boolean isTopic, MessageProducer producer, Message m, int priority,
        int deliveryMode) throws JMSException {
        if (isTopic) {
            TopicPublisher publisher = (TopicPublisher) producer;
            publisher.publish(m, deliveryMode, priority, 0);
        } else {
            // For unified domain which is not supported in 1.0.2, need to avoid cast
            // to QueueSender 
            if (producer instanceof QueueSender) {
                QueueSender sender = (QueueSender) producer;
                sender.send(m, deliveryMode, priority, 0);
            } else {
                producer.send(m, deliveryMode, priority, 0);
            }
        }
    }

    /**
     * @return true if connection factories is beneficial for performance and does
     * not interfere with connection failure recovery
     */
    public boolean shouldCacheConnectionFactories() {
        return true;
    }

    /**
     * @return true if producer pooling should be used
     */
    public boolean shouldUseProducerPooling() {
        return false;
    }

    /**
     * Converts optionally from a genericra destination to an admin destination
     * 
     * @param d destination to inspect
     * @return admin destination or same destination
     * @throws JMSException on conversion failure
     */
    public Destination checkGeneric(Destination d) throws JMSException {
        if (d == null) {
            return null;
        }
        
        Class<?> c = d.getClass();
        String classname = c.getName();
        boolean isGenQueue = classname.equals("com.sun.genericra.outbound.QueueProxy");
        boolean isGenTopic = !isGenQueue && classname.equals("com.sun.genericra.outbound.TopicProxy"); 
        if (isGenQueue || isGenTopic) { 
            try {
                Method m = c.getMethod("getDestinationJndiName", (Class<?>[]) null);
                String jndiname = (String) m.invoke(d, (Object[]) null);
                if (isGenQueue) {
                    AdminQueue ret = new AdminQueue();
                    ret.setName("jndi://" + jndiname);
                    d = ret;
                } else {
                    AdminTopic ret = new AdminTopic();
                    ret.setName("jndi://" + jndiname);
                    d = ret;
                }
            } catch (Exception e) {
                throw Exc.jmsExc(LOCALE.x("E117: Could not convert automatically "
                    + "from genericra destination: {0}", e), e);
            }
        }
        
        return d;
    }

    /**
     * @return true if message properties may begin with JMS. WMQ does not allow this.
     */
    public boolean isMsgPrefixOK() {
        return true;
    }

    /**
     * @return a new and empty activation spec
     */
    public abstract RAJMSActivationSpec createActivationSpec();

    /**
     * Allows for XAWrappers to be created for some JMS implementations
     * 
     * @throws JMSException propagated
     */
    public XAResource getXAResource(Activation activation, boolean isXA, Session sess) throws JMSException {
        return getXAResource(isXA, sess);
    }
}
