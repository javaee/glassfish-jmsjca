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
 * $RCSfile: LastAgentResource.java,v $
 * $Revision: 1.1.1.2 $
 * $Date: 2007-01-21 07:52:44 $
 *
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.  
 */

package com.stc.jts.jtsxa;

import javax.transaction.xa.XAResource;

/**
 * This indicates that this XAResource is aware of the Last Agent Commit optimization in
 * the SeeBeyond Integration Server and can indicate a preference to be chosen as Last
 * Agent.
 * 
 * @author vtsyganok
 */

public interface LastAgentResource extends XAResource {

    /**
     * The resource MUST always be last agent. It's an error to have two such resources
     * enlisted in the same transaction.
     */
    public static final int NON_XA = 0;

    /**
     * Indicates that this XAResource merely emulates functionality of prepare() and
     * recover(); commit()/rollback() are unlikely to throw exceptions. Multiple of these
     * resources can be used in a transaction relatively safely if running without a
     * persistent transaction log. Lower value indicates higher possiblity of error
     * occurency in commit().
     */
    public static final int PSEUDO_XA_MIN = 1;

    /**
     * Indicates that this XAResource merely emulates functionality of prepare() and
     * recover(); commit()/rollback() are unlikely to throw exceptions -- more unlikely
     * than PSEUDO_XA_MIN. Multiple of these resources can be used in a transaction
     * relatively safely if running without a persistent transaction log. Lower value
     * indicates higher possiblity of error occurency in commit().
     */
    public static final int PSEUDO_XA_MAX = 0x40000000;

    /**
     * Indicates that this resource fully implementats the two-phase commit protocol. The
     * purpose of these values is to indicate how expensive two-phase protocol is
     * relatively to single-phase commit. Lower values mean higher expenses and will be
     * most likely selected as last agent.
     */
    public static final int TRUE_XA_MIN = PSEUDO_XA_MAX + 1;

    /**
     * Indicates that this resource fully implementats the two-phase commit protocol. The
     * purpose of these values is to indicate how expensive two-phase protocol is
     * relatively to single-phase commit. Lower value means higher expenses and will be
     * most likely selected as last agent.
     */
    public static final int TRUE_XA_MAX = Integer.MAX_VALUE;

    /**
     * CORBA resources are considered to be true-XA, but they are preferred to be selected
     * as last agent over other resources.
     */
    public static final int CORBA_XA = 0x50000000;

    /**
     * Default value for XA resources which doesn't implement this interface.
     */
    public static final int NORMAL_XA = 0x60000000;
    
    /**
     * Resource should never be a last agent. It either improperly process
     * single-phase commit in some scenarios or have no performance benefit.  
     */
    public static final int NEVER = -1; 

    /**
     * This method is used to select which XAResource will be selected as the last agent
     * if the Last Agent Commit Optimization feature is used. A lower value translates
     * into a higher likelihood that this XAResource will be selected.
     * 
     * @return value for priority
     */

    public int lastAgentPreferenceLevel();

}
