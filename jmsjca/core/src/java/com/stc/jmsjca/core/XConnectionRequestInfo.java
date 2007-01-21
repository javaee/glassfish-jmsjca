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
 * $RCSfile: XConnectionRequestInfo.java,v $
 * $Revision: 1.1.1.2 $
 * $Date: 2007-01-21 07:52:44 $
 *
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.  
 */

package com.stc.jmsjca.core;

import com.stc.jmsjca.util.Str;

/**
 * <p>Describes how to create a JMS connection</p>
 *
 * @author Frank Kieviet
 * @version $Revision: 1.1.1.2 $
 */
public class XConnectionRequestInfo implements javax.resource.spi.ConnectionRequestInfo {
    private Class mConnectionClass;
    private Class mSessionClass;
    private String mUsername;
    private String mPassword;
    private String mClientID;
    private boolean mTransacted;
    private int mAcknowledgeMode;
    private String mOverrideUrl;

    /**
     * Constructor
     *
     * @param connectionClass JConnection
     * @param sessionClass Class
     * @param username String
     * @param password String
     * @param overrideUrl URL of different connection factory
     * @param clientID String
     * @param clientID2
     * @param transacted boolean
     * @param acknowledgeMode int
     */
    public XConnectionRequestInfo(Class connectionClass, Class sessionClass,
        String username, String password, String overrideUrl, String clientID, 
        boolean transacted, int acknowledgeMode) {
        mConnectionClass = connectionClass;
        mSessionClass = sessionClass;
        mUsername = username;
        mPassword = password;
        mClientID = clientID;
        mTransacted = transacted;
        mAcknowledgeMode = acknowledgeMode;
        mOverrideUrl = overrideUrl;
    }

    /**
     * Returns the Java class of the connection object to be created
     *
     * @return Class
     */
    public Class getConnectionClass() {
        return mConnectionClass;
    }

    /**
     * For authentication on connection
     *
     * @return username (may be null)
     */
    public String getUsername() {
        return mUsername;
    }

    /**
     * For authentication of connection
     *
     * @return password (may be null)
     */
    public String getPassword() {
        return mPassword;
    }

    /**
     * Returns the session class requested
     *
     * @return Class
     */
    public Class getSessionClass() {
        return mSessionClass;
    }
    
    /**
     * ClientID
     * 
     * @return String
     */
    public String getClientID() {
        return mClientID;
    }

    /**
     * Checks to see that two requests are similar in that the physical connections
     * are interchangeable.
     *
     * DOES ***NOT*** TAKE TRANSACTED/ACKMODE INTO ACCOUNT
     * CORRECTION: IT DOES (11-01-04)
     *
     * @param rhs request to compare with
     * @return true if similar
     */
    public boolean isCompatible(XConnectionRequestInfo rhs) {
        if (getConnectionClass() != rhs.getConnectionClass()) {
            return false;
        }

        // 11-01-04: assume when non-transacted is specified, connector should not
        // participate in transactions.
        if (getTransacted() != rhs.getTransacted()) {
            return false;
        }

        if (getSessionClass() != rhs.getSessionClass()) {
            return false;
        }

// Username and password are tested separately
//        if (!Str.isEqual(getUsername(), rhs.getUsername())) {
//            return false;
//        }
//
//        if (!Str.isEqual(getPassword(), rhs.getPassword())) {
//            return false;
//        }

        if (!Str.isEqual(getOverrideUrl(), rhs.getOverrideUrl())) {
            return false;
        }

        if (!Str.isEqual(mClientID, rhs.mClientID)) {
            return false;
        }

        // Apparently same
        return true;
    }

    /**
     * Checks whether this instance is equal to another. Since connectionRequestInfo is
     * defined specific to a resource adapter, the resource adapter is required to
     * implement this method. The conditions for equality are specific to the resource
     * adapter.
     *
     * DOES TAKE TRANSACTED/ACKMODE INTO ACCOUNT
     *
     * @param other the object to compare to
     * @return true if this object is compatible with rhs
     */
    public boolean equals(java.lang.Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof XConnectionRequestInfo)) {
            return false;
        }

        XConnectionRequestInfo that = (XConnectionRequestInfo) other;
        return isCompatible(that) && mTransacted == that.mTransacted
            && mAcknowledgeMode == that.mAcknowledgeMode;
    }

    /**
     * Returns the hashCode of the ConnectionRequestInfo.
     *
     * DOES TAKE TRANSACTED/ACKMODE INTO ACCOUNT
     *
     * @return hashcode
     */
    public int hashCode() {
        int result = 17;
        result = Str.hash(result, mSessionClass.getName());
        result = Str.hash(result, mConnectionClass.getName());
        result = Str.hash(result, mClientID);
        result = Str.hash(result, mUsername);
        result = Str.hash(result, mPassword);
        result = Str.hash(result, mOverrideUrl);
        result = Str.hash(result, mTransacted);
        result = 7 * result + mAcknowledgeMode;
        return result;
    }

    /**
     * DOMAIN_UNKNOWN
     */
    public static final int DOMAIN_UNKNOWN = 0;

    /**
     * DOMAIN_QUEUE_NONXA
     */
    public static final int DOMAIN_QUEUE_NONXA = 1;

    /**
     * DOMAIN_TOPIC_NONXA
     */
    public static final int DOMAIN_TOPIC_NONXA = 2;

    /**
     * DOMAIN_QUEUE_XA
     */
    public static final int DOMAIN_QUEUE_XA = 3;

    /**
     * DOMAIN_TOPIC_XA
     */
    public static final int DOMAIN_TOPIC_XA = 4;

    /**
     * DOMAIN_UNIFIED_XA
     */
    public static final int DOMAIN_UNIFIED_XA = 5;

    /**
     * DOMAIN_UNIFIED_NONXA
     */
    public static final int DOMAIN_UNIFIED_NONXA = 6;

    /**
     * NDOMAINS
     */
    public static final int NDOMAINS = 7;

    /**
     * DOMAINSTRS
     */
    public static final String[] DOMAINSTRS = {
        "Unknown",
        "p-to-p (non XA)",
        "pub/sub (non XA)",
        "p-to-p (XA)",
        "pub/sub (XA)",
        "unified (XA)",
        "unified (non XA)"
    };

    /**
     * Returns the domain identifier based on the xa and topic dimensions
     *
     * @param isXA boolean
     * @param isTopic boolean
     * @return int one of DOMAIN_XXX
     */
    public static int guessDomain(boolean isXA, boolean isTopic) {
        if (isXA) {
            if (isTopic) {
                return DOMAIN_TOPIC_XA;
            } else {
                return DOMAIN_QUEUE_XA;
            }
        } else {
            if (isTopic) {
                return DOMAIN_TOPIC_NONXA;
            } else {
                return DOMAIN_QUEUE_NONXA;
            }
        }
    }

    /**
     * Returns the domain (p2p, pub/sub)
     *
     * @return one of DOMAIN_xxx
     * @param isXA boolean
     */
    public int getDomain(boolean isXA) {
        int i = DOMAIN_UNKNOWN;
        if (mSessionClass == javax.jms.QueueSession.class) {
            i = isXA ? DOMAIN_QUEUE_XA : DOMAIN_QUEUE_NONXA;
        } else if (mSessionClass == javax.jms.TopicSession.class) {
            i = isXA ? DOMAIN_TOPIC_XA : DOMAIN_TOPIC_NONXA;
        } else if (mSessionClass == javax.jms.Session.class) {
            i = isXA ? DOMAIN_UNIFIED_XA : DOMAIN_UNIFIED_NONXA;
        }

        if (i == DOMAIN_UNKNOWN) {
            throw new RuntimeException("Unknown domain " + mSessionClass);
        }

        return i;
    }

    /**
     * getTransacted
     *
     * @return boolean
     */
    public boolean getTransacted() {
        return mTransacted;
    }

    /**
     * getAcknowledgeMode
     *
     * @return int
     */
    public int getAcknowledgeMode() {
        return mAcknowledgeMode;
    }

//    /**
//     * Indicates the subtype
//     *
//     * @return one or TR_xxx
//     */
//    public int getTransactionMode() {
//        return mTransactionMode;
//    }

    /**
     * @return Returns the override URL (may be null)
     */
    public String getOverrideUrl() {
        return mOverrideUrl;
    }
    
    
    /**
     * @param overrideUrl new override URL
     */
    public void setOverrideUrl(String overrideUrl) {
        mOverrideUrl = overrideUrl;
    }
}
