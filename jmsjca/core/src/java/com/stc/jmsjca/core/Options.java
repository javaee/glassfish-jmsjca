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
 * @version $Revision: 1.4 $
 */
public interface Options {
    /**
     * Property name that indicates the name of the TransactionManager locator class
     */
    String TXMGRLOCATOR = "JMSJCA.LocatorClass";

    /**
     * Property name to specify that XA should not be supported
     */
    String NOXA = "JMSJCA.NoXA";

    
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
        
//        /**
//         * Number of ms after which an idle connection becomes invalid
//         */
//        String STALETIMEOUT = "JMSJCA.idletimeout";
        
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
    }

}
