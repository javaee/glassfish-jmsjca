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

/**
 * Collects all options that can be set in the VM or ra.xml in one place.
 * 
 * @author fkieviet
 * @version $Revision: 1.19 $
 */
public interface Options {
    /**
     * Property name that indicates the name of the TransactionManager locator class
     */
    String TXMGRLOCATOR = "JMSJCA.LocatorClass";

    /**
     * Property name to specify that XA should be emulated
     */
    String NOXA = "JMSJCA.NoXA";
    
    /**
     * Property name to specify that the indication by the application server about 
     * the MDB being transactional should be ignored, and that BMT should be assumed
     * instead.
     */
    String FORCE_BMT = "JMSJCA.ForceBMT";
    
    /**
     * Separator to be able to have a properties set in one line 
     */
    String SEP = "JMSJCA.sep=";

    /**
     * Prefix for destination names to indicate that the destination is an object that
     * needs to be looked up in JNDI. This bound object may be an administrative object
     * or may be a real destination. 
     */
    String LOCAL_JNDI_LOOKUP = "lookup://";
    
    /**
     * When set to true, this will wrap the XAResource and will override isSameRM() 
     * such that it will always return false. Valid values: true, false
     */
    String OVERRIDEISSAMERM = "JMSJCA.overrideissamerm";

    /**
     * Options specific to the inbound part of the RA
     */
    public interface In {
        /**
         * Specify this as an option to redirect message instead of foward in
         * redelivery handling
         */
        String OPTION_REDIRECT = "JMSJCA.redeliveryredirect";

        /**
         * The redelivery handling can be set in a property of in the activation
         * spec
         */
        String OPTION_REDELIVERYHANDLING = "JMSJCA.redeliveryhandling";

        /**
         * Override for concurrency mode (e.g. to switch the concurrency to sync for 
         * STCMS FIFO modes)
         */
        String OPTION_CONCURRENCYMODE = "JMSJCA.concurrencymode";

        /**
         * Wraps each message so that the listener can place redelivery data on the 
         * message.
         */
        String OPTION_REDELIVERYWRAP = "JMSJCA.messagewrapping";
        
        /**
         * Specifies an override selector
         */
        String OPTION_SELECTOR = "JMSJCA.selector";
        
        /**
         * The receive time out used in SyncWorkers receive, to consume messages.
         *  
         */
        String RECEIVE_TIMEOUT = "JMSJCA.receivetimeout";

        /**
         * Prints only one error message for a durable subscriber error
         */
        String OPTION_MINIMAL_RECONNECT_LOGGING = "JMSJCA.minimalreconnectlogging";

        /**
         * Prints only one error message for a durable subscriber error
         */
        String OPTION_MINIMAL_RECONNECT_LOGGING_DURSUB = "JMSJCA.minimalreconnectloggingds";
    }
    
    /**
     * Options specific to the outbound part of the RA
     */
    public interface Out {
        /**
         * CLIENTCONTAINER: property name that tells the RA to behave as if it were in a
         * client container
         */
        String CLIENTCONTAINER = "JMSJCA.ACC";

        /**
         * Property name that indicates to ignore the parameters passed in
         * createXSession(transacted, ackmode)
         */
        String IGNORETX = "JMSJCA.IgnoreTx";

        /**
         * Property name that indicates not to use RA at all (can be used in clients or ACC)
         * The connection factory that the application uses, will delegate straight to the
         * JMS provider connection factory, rather than using any of the connection pooling
         * mechanics of the server, and any of the RA mechanics provided by JMSJCA.
         */
        String BYPASSRA = "JMSJCA.BypassRA";

        /**
         * Property name that indicates to use all settings closest to the CTS settings
         */
        String STRICT = "JMSJCA.Strict";
        
        /**
         * Number of ms after which an idle connection becomes invalid
         */
        String STALETIMEOUT = "JMSJCA.idletimeout";
        
        /**
         * Do not cache connection factories (for jndi, wl)
         */
        String DONOTCACHECONNECTIONFACTORIES = "JMSJCA.nocfcache";

        /**
         * The maximum number of connections in the pool (only applies when the
         * connection manager provided by the RA is used)
         */
        String POOL_MAXSIZE = "JMSJCA.poolmaxsize";

        /**
         * The minimum number of connections in the pool (only applies when the
         * connection manager provided by the RA is used)
         */
        String POOL_MINSIZE = "JMSJCA.poolminsize";

        /**
         * When the application requests a connection and the number of in-use 
         * connections has reached the poolmaxsize, this is the maximum time the pool
         * will block before it will throw an exception back to the application
         */
        String POOL_TIMEOUT = "JMSJCA.pooltimeout";
        
        /**
         * Turns on producer pooling
         */
        String PRODUCER_POOLING = "JMSJCA.producerpooling";
    }

    /**
     * Options for subscriber name
     */
    public interface Subname {
        /**
         * names the subscriber name
         */
        String SUBSCRIBERNAME = "subscribername";

        /**
         * names the queue
         */
        String QUEUENAME = "queue";
        
        /**
         * indicates what kind of clustering is required
         */
        String DISTRIBUTION_TYPE = "distribution";
        
        /**
         * To recognize a specially formatted subscriber name
         */
        String PREFIX = "jmsjca://";
        
        /**
         * MBean name
         */
        String MBEANNAME = "mbeanname";
        
        /**
         * Batch size
         */
        String BATCHSIZE = "batchsize";
    }
    
    /**
     * Options for destination names
     */
    public interface Dest {
        /**
         * To recognize a specially formatted queue or topic name
         */
        String PREFIX = "jmsjca://";
        
        /**
         * Destination name, e.g. Queue1
         */
        String NAME = "name";

        /**
         * Internal use only
         */
        String ORIGINALNAME = "JMSJCA.originalname";
    }
    
    /**
     * Additional properties on a message for redelivery handling
     * 
     * @author fkieviet
     */
    public interface MessageProperties {

        /**
         * Old prefix that should be chopped off to get the new prefix
         */
        String OLDPREFIX = "JMS_Sun";

        /**
         * Prefix for JMSX properties
         */
        String MSG_PROP_PREFIX = "_JMSJCA_";

        /**
         * Old prefix
         */
        String OLDFULLPREFIX = "JMS_Sun_JMSJCA_";

        // copy: ok
        // read access in MDB: n/a
        // write access in MDB: n/a
        // test: ok
        /**
         * Property name for copying messages to DLQ
         */
        String ORIGINAL_MSGID_OLD = "JMS_Sun_JMSJCA_OriginalJMSMessageID";

        /**
         * Property name for copying messages to DLQ
         */
        String ORIGINAL_MSGID = "_JMSJCA_OriginalJMSMessageID";

        // copy: ok
        // read access in MDB: n/a
        // write access in MDB: n/a
        // test: ok
        /**
         * Property name for copying messages to DLQ
         */
        String ORIGINAL_CORRELATIONID_OLD = "JMS_Sun_JMSJCA_OriginalJMSCorrelationID";

        /**
         * Property name for copying messages to DLQ
         */
        String ORIGINAL_CORRELATIONID = "_JMSJCA_OriginalJMSCorrelationID";

        // copy: ok
        // read access in MDB: n/a
        // write access in MDB: n/a
        // test: NO
        /**
         * Property name for copying messages to DLQ
         */
        String ORIGINAL_CLIENTID_OLD = "JMS_Sun_JMSJCA_OriginalClientID";

        /**
         * Property name for copying messages to DLQ
         */
        String ORIGINAL_CLIENTID = "_JMSJCA_OriginalClientID";

        // copy: ok
        // read access in MDB: ok
        // write access in MDB: n/a
        // test: ok
        /**
         * For stateful redelivery: the user can set these properties if prefixed with 
         * these value
         */
        String USER_ROLLBACK_DATA_PREFIX_OLD = "JMS_Sun_JMSJCA_UserRollbackData";

        /**
         * For stateful redelivery: the user can set these properties if prefixed with 
         * these value
         */
        String USER_ROLLBACK_DATA_PREFIX = "_JMSJCA_UserRollbackData";

        // copy: ok
        // read access in MDB: ok
        // write access in MDB: n/a
        // test: ok
        /**
         * Property name for copying messages to DLQ
         */
        String LAST_EXCEPTIONCLASS_OLD = "JMS_Sun_JMSJCA_ExceptionClass";

        /**
         * Property name for copying messages to DLQ
         */
        String LAST_EXCEPTIONCLASS = "_JMSJCA_ExceptionClass";

        // copy: ok
        // read access in MDB: ok
        // write access in MDB: n/a
        // test: ok
        /**
         * Property name for copying messages to DLQ
         */
        String LAST_EXCEPTIONMESSAGE_OLD = "JMS_Sun_JMSJCA_ExceptionMessage";

        /**
         * Property name for copying messages to DLQ
         */
        String LAST_EXCEPTIONMESSAGE = "_JMSJCA_ExceptionMessage";

        // copy: ok
        // read access in MDB: ok
        // write access in MDB: n/a
        // test: ok
        /**
         * Property name for copying messages to DLQ
         */
        String LAST_EXCEPTIONTRACE_OLD = "JMS_Sun_JMSJCA_ExceptionStackTrace";

        /**
         * Property name for copying messages to DLQ
         */
        String LAST_EXCEPTIONTRACE = "_JMSJCA_ExceptionStackTrace";

        // copy: ok
        // read access in MDB: ok
        // write access in MDB: ok 
        // test: ok
        /**
         * Property name for msg wrapping
         */
        String REDELIVERY_HANDLING_OLD = "JMS_Sun_JMSJCA_RedeliveryHandling";

        /**
         * Property name for msg wrapping
         */
        String REDELIVERY_HANDLING = "_JMSJCA_RedeliveryHandling";

        // copy: ok
        // read access in MDB: ok
        // write access in MDB: ok 
        // test: ok
        /**
         * Property name for msg wrapping
         */
        String STOP_CONNECTOR_OLD = "JMS_Sun_JMSJCA_StopMessageDelivery";

        /**
         * Property name for msg wrapping
         */
        String STOP_CONNECTOR = "_JMSJCA_StopMessageDelivery";

        // copy: n/a
        // read access in MDB: ok
        // write access in MDB: n/a 
        // test: ok
        /**
         * Object property that returns the MBean server used to register the 
         * Activation MBean
         */
        String MBEANSERVER_OLD = "JMS_Sun_JMSJCA_MBeanServer";

        /**
         * Object property that returns the MBean server used to register the 
         * Activation MBean
         */
        String MBEANSERVER = "_JMSJCA_MBeanServer";

        // copy: n/a
        // read access in MDB: ok
        // write access in MDB: n/a 
        // test: ok
        /**
         * String property that returns the MBean Name from the activation spec
         */
        String MBEANNAME_OLD = "JMS_Sun_JMSJCA_MBeanName";

        /**
         * String property that returns the MBean Name from the activation spec
         */
        String MBEANNAME = "_JMSJCA_MBeanName";

        // copy: ok
        // read access in MDB: ok
        // write access in MDB: n/a
        // test: ok
        /**
         * Property name for copying messages to DLQ
         */
        String REDELIVERYCOUNT_OLD = "JMS_Sun_JMSJCA_RedeliveryCount";

        /**
         * Property name for copying messages to DLQ
         */
        String REDELIVERYCOUNT = "_JMSJCA_RedeliveryCount";

        // copy: ok
        // read access in MDB: n/a
        // write access in MDB: n/a
        // test: ok
        /**
         * Property name for copying messages to DLQ
         */
        String ORIGINALDESTINATIONNAME_OLD = "JMS_Sun_JMSJCA_OriginalDestinationName";

        /**
         * Property name for copying messages to DLQ
         */
        String ORIGINALDESTINATIONNAME = "_JMSJCA_OriginalDestinationName";

        // copy: ok
        // read access in MDB: n/a
        // write access in MDB: n/a
        // test: ok
        /**
         * Property name for copying messages to DLQ
         */
        String ORIGINALDESTINATIONTYPE_OLD = "JMS_Sun_JMSJCA_OriginalDestinationType";

        /**
         * Property name for copying messages to DLQ
         */
        String ORIGINALDESTINATIONTYPE = "_JMSJCA_OriginalDestinationType";

        // copy: ok
        // read access in MDB: n/a
        // write access in MDB: n/a
        // test: ok
        /**
         * Property name for copying messages to DLQ
         */
        String ORIGINALTIMESTAMP_OLD = "JMS_Sun_JMSJCA_OriginalTimestamp";

        /**
         * Property name for copying messages to DLQ
         */
        String ORIGINALTIMESTAMP = "_JMSJCA_OriginalTimestamp";

        // copy: ok
        // read access in MDB: n/a
        // write access in MDB: n/a
        // test: ok
        /**
         * Property name for copying messages to DLQ
         */
        String SUBSCRIBERNAME_OLD = "JMS_Sun_JMSJCA_SubscriberName";

        /**
         * Property name for copying messages to DLQ
         */
        String SUBSCRIBERNAME = "_JMSJCA_SubscriberName";

        // copy: ok
        // read access in MDB: n/a
        // write access in MDB: n/a
        // test: ok
        /**
         * Property name for copying messages to DLQ
         */
        String CONTEXTNAME_OLD = "JMS_Sun_JMSJCA_ContextName";

        /**
         * Property name for copying messages to DLQ
         */
        String CONTEXTNAME = "_JMSJCA_ContextName";
    }
    
    /**
     * Selector related constants
     * 
     * @author fkieviet
     */
    public interface Selector {
        /**
         * Name of subscribername that can be used as "${subscribername}" in a selector
         */
        String SUB_NAME = "subscribername";
        
        /**
         * Evaluates to the selector specified in the activation spec
         */
        String SELECTOR = "selector";
        
        /**
         * Evaluates to "and (${selector})" if selector is non-empty string, or 
         * evaluates to an empty string "" if selector is an empty string
         */
        String ANDSELECTOR = "andselector";
        
        /**
         * Evaluates to "(${selector}) and" if selector is non-empty string, or 
         * evaluates to an empty string "" if selector is an empty string
         */
        String SELECTORAND = "selectorand";
    }
    
    /**
     * Options to configure interceptors
     * 
     * @author fkieviet
     */
    public interface Interceptor {
        /**
         * Service locator file name (normally equates  
         */
        String DEFAULT_SERVICENAME = "jmsjca.interceptor";
        
        /**
         * Prefix for service name locator file
         */
        String SERVICEPREFIX = "META-INF/services/";
        
        /**
         * Option name of the interceptors service name
         */
        String SERVICENAME = "JMSJCA.interceptorsvcname";
        
        /**
         * Context data: the message
         */
        String KEY_MESSAGE = "JMSJCA.message";
        
        /**
         * A test packaged with the RAR
         */
        String TEST_SVC = "jmsjca-private.test.interceptor";
    }
}
