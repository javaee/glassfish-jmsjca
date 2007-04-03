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
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

import java.util.Enumeration;
import java.util.Properties;

/**
 * An object factory for JMS provider specific objects; also provides some provider
 * specific utilities.
 *
 * @author fkieviet
 * @version $Revision: 1.5 $
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
     */
    public Activation createActivation(RAJMSResourceAdapter ra,
        MessageEndpointFactory endpointFactory, RAJMSActivationSpec spec) {
        return new Activation(ra, endpointFactory, spec);
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
            throw new JMSException("Logic fault: invalid domain " + domain);
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
        Str.deserializeProperties(ra.getOptions(), p);
        if (spec != null) {
            Str.deserializeProperties(spec.getOptions(), p);
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
    public Session createSession(Connection conn, boolean isXA, Class sessionClass,
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
                return ((Connection) conn).createSession(transacted, ackmode);
            }
        }
        throw new RuntimeException("Unknown class " + sessionClass);
    }

    /**
     * createDestination()
     * This is called by the Delivery classes for inbound message delivery
     *
     * @param sess Session
     * @param isXA boolean
     * @param isTopic boolean
     * @param activationSpec RAJMSActivationSpec
     * @param fact MCF
     * @param ra RAJMSResourceAdapter
     * @param destName String
     * @return Destination
     * @throws JMSException failure
     */
    public Destination createDestination(Session sess, boolean isXA, boolean isTopic,
        RAJMSActivationSpec activationSpec, XManagedConnectionFactory fact,  RAJMSResourceAdapter ra,
        String destName) throws JMSException {
        if (isXA) {
            if (isTopic) {
                return ((XATopicSession) sess).getTopicSession().createTopic(destName);
            } else {
                return ((XAQueueSession) sess).getQueueSession().createQueue(destName);
            }
        } else {
            if (isTopic) {
                return ((TopicSession) sess).createTopic(destName);
            } else {
                return ((QueueSession) sess).createQueue(destName);
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
                    return ((XATopicSession) sess).getTopicSession().
                        createDurableSubscriber((Topic) dest,
                        spec.getSubscriptionName(),
                        spec.getMessageSelector(), false);
                } else {
                    return ((XATopicSession) sess).getTopicSession().
                        createSubscriber((Topic) dest,
                        spec.getMessageSelector(), false);
                }
            } else {
                return ((XAQueueSession) sess).getQueueSession().createReceiver(
                    (Queue) dest, spec.getMessageSelector());
            }
        } else {
            if (isTopic) {
                if (RAJMSActivationSpec.DURABLE.equals(spec.getSubscriptionDurability())) {
                    return ((TopicSession) sess).
                        createDurableSubscriber((Topic) dest,
                        spec.getSubscriptionName(),
                        spec.getMessageSelector(), false);
                } else {
                    return ((TopicSession) sess).
                    createSubscriber((Topic) dest,
                    spec.getMessageSelector(), false);
                }
            } else {
                return ((QueueSession) sess).createReceiver((Queue) dest,
                    spec.getMessageSelector());
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
                return ((QueueSession) sess).createSender((Queue) dest);
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
     * @return ConnectionConsumer
     * @throws JMSException failure
     */
    public ConnectionConsumer createConnectionConsumer(Connection conn, boolean isXA,
        boolean isTopic, boolean isDurable, RAJMSActivationSpec activationSpec,
        RAJMSResourceAdapter ra, Destination dest, String subscriptionName,
        String selector, ServerSessionPool pool) throws JMSException {
        if (isXA) {
            if (isTopic) {
                if (isDurable) {
                    return ((XATopicConnection) conn).createDurableConnectionConsumer((Topic)
                            dest, subscriptionName, selector, pool, 1);
                } else {
                    return ((XATopicConnection) conn).createConnectionConsumer((Topic)
                            dest, selector, pool, 1);
                }
            } else {
                return ((XAQueueConnection) conn).createConnectionConsumer((Queue) dest, selector, pool, 1);
            }
        } else {
            if (isTopic) {
                if (isDurable) {
                    return ((TopicConnection) conn).createDurableConnectionConsumer((Topic)
                            dest, subscriptionName, selector, pool, 1);                    
                } else {
                    return ((TopicConnection) conn).createConnectionConsumer((Topic)
                            dest, selector, pool, 1);                    
                }
            } else {
                return ((QueueConnection) conn).createConnectionConsumer((Queue) dest, selector, pool, 1);
            }
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
        int acknowledgmentMode, Class sessionClass)
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
        connection.setClientID(clientID);
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
                        sLog.warn(LOCALE.x("E042: ClientID is already set to [{0}]; "  
                            + "cannot set to [{1}] as required in "
                            + "activationspec [{2}]", currentClientId, spec.getClientId(), spec)); 
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
    public Session getNonXASession(Session session, boolean isXA, Class sessionClass) throws JMSException {
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
        throw new RuntimeException("Unknown class " + sessionClass);
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
            throw new Exception("Invalid concurrency: "
                + activation.getActivationSpec().getDeliveryConcurrencyMode());
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
            throw new JMSException("Cannot determine message type: the message " 
                + "implements multiple interfaces.");
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
            for (Enumeration en = in.getMapNames(); en.hasMoreElements();/*-*/) {
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
                Object o = in.readObject();
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
        for (Enumeration en = toCopy.getPropertyNames(); en.hasMoreElements();/*-*/) {
            String name = (String) en.nextElement();
            Object o = toCopy.getObjectProperty(name);
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
                throw new JMSException("Unknown property type for " + name + ": "
                    + o.getClass().getName());
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
            QueueSender sender = (QueueSender) producer;
            sender.send(m, deliveryMode, priority, 0);
        }
    }

    /**
     * @return true if the enlistment of a tranaction in CC can be done in the onMessage
     *   method; if false, the enlistment will happen in the run() method of the 
     *   serversession
     */
    public boolean canCCEnlistInOnMessage() {
        return true;
    }

}
