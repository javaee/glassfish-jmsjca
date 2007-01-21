/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 *
 * You can obtain a copy of the license at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * HEADER in each file and include the License file at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * If applicable add the following below this CDDL HEADER,
 * with the fields enclosed by brackets "[]" replaced with
 * your own identifying information: Portions Copyright
 * [year] [name of copyright owner]
 */
/*
 * $RCSfile: JConnectionFactory.java,v $
 * $Revision: 1.1.1.2 $
 * $Date: 2007-01-21 07:52:44 $
 *
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.  
 */

package com.stc.jmsjca.core;

import com.stc.jmsjca.util.Logger;
import com.stc.jmsjca.util.Str;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.TopicConnectionFactory;
import javax.naming.BinaryRefAddr;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.resource.spi.ConnectionManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.rmi.server.UID;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * <p>This connection factory is used by the application to create connections.</p>
 *
 * <p>In a managed environment (i.e. within an application server), the connection
 * factory is created during startup and is bound to JNDI. At creation time, it is also
 * associated with a connection manager.</p>
 *
 * <p>In an un-managed environment, the factory will be using the default connection
 * manager.</p>
 *
 * <p>Typically, instances are created by calling createConnectionFactory on the
 * ManagedConnectionFactory</p>
 *
 * @author Frank Kieviet
 * @version $Revision: 1.1.1.2 $
 */
public class JConnectionFactory implements javax.jms.ConnectionFactory,
    java.io.Serializable, javax.resource.Referenceable, javax.naming.spi.ObjectFactory {

    private static Logger sLog = Logger.getLogger(JConnectionFactory.class);
    private ConnectionManager mConnectionManager;
    private XManagedConnectionFactory mManagedConnectionFactory;
    private javax.naming.Reference mReference;
    private int mUniqueID;

    // For getReference(), need to be able to lookup connection factories by a String ID
    private static Map sAllFactories = new WeakHashMap();  // key: JConnectionFactory, value: null
    private static int sIdGenerator;
    private static String sBaseId = (new UID()).toString();

    /**
     * constructor
     */
    public JConnectionFactory() {
    }
    
    private static final boolean isWebLogic(ConnectionManager cm) {
        if (cm != null) {
            return "weblogic.connector.outbound.ConnectionManagerImpl".equals(cm.getClass().getName());
        } else {
            return false;
        }
    }

    /**
     * Constructor
     *
     * @param managedConnectionFactory The JCA internal factory that will create the
     * physical connections
     * @param connectionManager Application Server provided connection manager; may be
     * null in an unmanaged environment
     */
    public JConnectionFactory(XManagedConnectionFactory managedConnectionFactory,
        ConnectionManager connectionManager) {
        this();
        mManagedConnectionFactory = managedConnectionFactory;
        mConnectionManager = connectionManager;
        if (mConnectionManager == null) {
            mConnectionManager = new XDefaultConnectionManager(managedConnectionFactory);
        }

        // The connection manager in Weblogic cannot be serialized, hence use an 
        // alternative solution to getting references
        if (isWebLogic(connectionManager)) {
            synchronized (sAllFactories) {
                mUniqueID = ++sIdGenerator; 
                sAllFactories.put(this, null);
            }
        }
    }

    /**
     * For testing
     *
     * @return ConnectionManager
     */
    public ConnectionManager getConnectionManager() {
        return mConnectionManager;
    }


    // JNDI

    /**
     * Associates this instance with a reference
     *
     * @param reference to reference with
     */
    public void setReference(final javax.naming.Reference reference) {
        this.mReference = reference;
    }

    /**
     * Returns the reference this instance is referencable with
     *
     * @return ref
     * @throws NamingException failure
     */
    public javax.naming.Reference getReference() throws NamingException {
        if (mReference == null) {
            return getInternalReference();
        }
        return mReference;
    }

    /**
     * Implement Serializable.writeObject
     *
     * @param out
     * @exception IOException
     */
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeObject(mConnectionManager);
        out.writeObject(mManagedConnectionFactory);
        out.flush();
    }

    /**
     * Implement Serializable.readObject
     *
     * @param in
     * @exception IOException
     * @exception ClassNotFoundException
     */
    private void readObject(ObjectInputStream in) throws IOException,
        ClassNotFoundException {
        mConnectionManager = (ConnectionManager) in.readObject();
        mManagedConnectionFactory = (XManagedConnectionFactory) in.readObject();
    }

    /**
     * RefAddr type for Reference use
     */
    static final String REFADDTYPE = "XConnectionFactory";

    /**
     * RefAddr type for Reference use without serialization
     */
    static final String REFADDTYPEUSINGID = "XConnectionFactory-using-id";
    
    private String getUniqueID() {
        return sBaseId + "/" + mUniqueID;
    }
    
    private JConnectionFactory getByID(String id) {
        synchronized (sAllFactories) {
            for (Iterator iter = sAllFactories.keySet().iterator(); iter.hasNext();) {
                JConnectionFactory cand = (JConnectionFactory) iter.next();
                if (id.equals(cand.getUniqueID())) {
                    return cand;
                }
            }
        }
        return null;
    }
    
    /**
     * JNDI
     *
     * @return ref
     * @throws javax.naming.NamingException failure
     */
    public javax.naming.Reference getInternalReference()
        throws javax.naming.NamingException {
        javax.naming.Reference ref = null;

        if (isWebLogic(mConnectionManager)) {
            // Weblogic: simply return a reference that CONTAINS this object
            ref = new Reference(getClass().getName(), new StringRefAddr(
                REFADDTYPEUSINGID, getUniqueID()));
        } else {
            // Non-Weblogic: simply return a reference that CONTAINS this object
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream x = new ObjectOutputStream(baos);
                writeObject(x);
                byte[] bytes = baos.toByteArray();
                
                ref = new Reference(getClass().getName(),
                    new BinaryRefAddr(REFADDTYPE, bytes),
                    getClass().getName(),
                    null);
            } catch (IOException ex) {
                javax.naming.NamingException tothrow =
                    new javax.naming.NamingException("Could not obtain an internal reference: " + ex);
                tothrow.initCause(ex);
                throw tothrow;
            }
        }

        return ref;
    }

    /**
     * getObjectInstance
     *
     * @param obj Object
     * @param name Name
     * @param ctx Context
     * @param env Hashtable
     * @throws Exception failure
     * @return Object
     */
    public Object getObjectInstance(Object obj, Name name, Context ctx,
        Hashtable env) throws Exception {

        if (obj instanceof Reference) {
            Reference ref = (Reference) obj;
            RefAddr refAddr = ref.get(REFADDTYPE);
            if (refAddr == null) {
                // Weblogic
                refAddr = ref.get(REFADDTYPEUSINGID);
                if (refAddr != null) {
                    String id = (String) refAddr.getContent();
                    JConnectionFactory f = getByID(id);
                    if (f != null) {
                        return f;
                    }
                }
            } else {
                byte[] bytes = (byte[]) refAddr.getContent();
                ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                ObjectInputStream x = new ObjectInputStream(bais);
                readObject(x);
                return this;
            }
        }

        return null;
    }

    
    // JMS interface

    /**
     * <p>Mandated by javax.jms.QueueConnectionFactory</p>
     *
     * <p>As per JCA spec, delegates the request to the ConnectionManager</p>
     *
     * @return new or pooled connection
     * @throws JMSException failure
     */
    public QueueConnection createQueueConnection() throws JMSException {
        return createQueueConnection(null, null);
    }

    /**
     * <p>Mandated by javax.jms.QueueConnectionFactory</p>
     *
     * <p>As per JCA spec, delegates the request to the ConnectionManager</p>
     *
     * @param username username used to authenticate with server
     * @param password password used to authenticate with server
     * @return new or pooled connection
     * @throws JMSException failure
     */
    public QueueConnection createQueueConnection(final String username,
        final String password) throws JMSException {
        return (QueueConnection) createConnection(javax.jms.QueueConnection.class,
            username, password);
    }

    /**
     * <p>Mandated by javax.jms.TopicConnectionFactory</p>
     *
     * <p>As per JCA spec, delegates the request to the ConnectionManager</p>
     *
     * @return new or pooled connection
     * @throws JMSException failure
     */
    public javax.jms.TopicConnection createTopicConnection() throws javax.jms.
        JMSException {
        return createTopicConnection(null, null);
    }

    /**
     * <p>Mandated by javax.jms.TopicConnectionFactory</p>
     *
     * <p>As per JCA spec, delegates the request to the ConnectionManager</p>
     *
     * @param username username used to authenticate with server
     * @param password password used to authenticate with server
     * @return new or pooled connection
     * @throws JMSException failure
     */
    public javax.jms.TopicConnection createTopicConnection(final String username,
        final String password) throws JMSException {

        if (sLog.isDebugEnabled()) {
            sLog.debug("Creating topic connection");
        }

        return (javax.jms.TopicConnection) createConnection(javax.jms.TopicConnection.class,
            username, password);
    }

    private Object createConnection(Class c, String username, String password) throws JMSException {
        if (!mManagedConnectionFactory.getOptionBypassRA()) {
            return new JConnection(mManagedConnectionFactory, mConnectionManager, c,
                username, password).getWrapper();
        } else {
            // Bypassing the RA completely: return the concrete factory provided by the
            // JMS provider directly
            
            // Computes the effective userid and password applying the presedence rules:
            // 1) createConnection(username, password)
            // 2) MCF
            // 3) RA
            if (Str.empty(username)) {
                if (!Str.empty(mManagedConnectionFactory.getUserName())) {
                    username = mManagedConnectionFactory.getUserName();
                    password = mManagedConnectionFactory.getPassword();
                } else if (!Str.empty(getRA().getUserName())) {
                    username = getRA().getUserName();
                    password = getRA().getPassword();
                }
            }
            
            // Extract URL, userid and password first if the username is set and contains
            // a special encoding
            String[] useridpwurl = mManagedConnectionFactory.getUserIdAndPasswordAndUrl(username, password);
            
            if (c == javax.jms.QueueConnection.class) {
                QueueConnectionFactory f = (QueueConnectionFactory)
                    mManagedConnectionFactory.getConnectionFactory(XConnectionRequestInfo.
                    DOMAIN_QUEUE_NONXA, useridpwurl[2]);
                return useridpwurl[0] == null ? f.createQueueConnection()
                    : f.createQueueConnection(useridpwurl[0], useridpwurl[1]);
            } else if (c == javax.jms.TopicConnection.class) {
                TopicConnectionFactory f = (TopicConnectionFactory)
                    mManagedConnectionFactory.getConnectionFactory(XConnectionRequestInfo.
                    DOMAIN_TOPIC_NONXA, useridpwurl[2]);
                return useridpwurl[0] == null ? f.createTopicConnection() 
                    : f.createTopicConnection(useridpwurl[0], useridpwurl[1]);
            } else if (c == javax.jms.Connection.class) {
                ConnectionFactory f = (ConnectionFactory)
                    mManagedConnectionFactory.getConnectionFactory(XConnectionRequestInfo.
                    DOMAIN_UNIFIED_NONXA, useridpwurl[2]);
                return useridpwurl[0] == null ? f.createConnection() 
                    : f.createConnection(useridpwurl[0], useridpwurl[1]);
            } else {
                throw new JMSException("Unknown domain " + c);
            }
        }
    }

    /**
     * createConnection
     *
     * @return Connection
     * @throws JMSException failure
     */
    public javax.jms.Connection createConnection() throws JMSException {
        return createConnection(null, null);
    }

    /**
     * createConnection
     *
     * @param username String
     * @param password String
     * @return Connection
     * @throws JMSException failure
     */
    public javax.jms.Connection createConnection(final String username,
        final String password) throws JMSException {
        if (sLog.isDebugEnabled()) {
            sLog.debug("Creating unified connection");
        }
        return (javax.jms.Connection) createConnection(javax.jms.Connection.class, username, password);
    }

    /**
     * Returns the RA that is associated with this CF
     *
     * @return RAJMSResourceAdapter
     */
    public RAJMSResourceAdapter getRA() {
        return mManagedConnectionFactory.getRAJMSResourceAdapter();
    }
    
    /**
     * For unit testing only: returns the MCF associated with this CF
     * 
     * @return mcf
     */
    public XManagedConnectionFactory testGetMCF() {
        return mManagedConnectionFactory;
    }
}
