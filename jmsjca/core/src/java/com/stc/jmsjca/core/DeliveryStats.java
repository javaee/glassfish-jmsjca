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

import java.util.HashMap;
import java.util.Map;

/**
 * Runtime statistics information on message delivery (used by an activation and the MBean
 * tied to that activation)
 *
 * @author fkieviet
 * @version $Revision: 1.8 $
 */
public class DeliveryStats {
    private int mNMessages;
    private long mt0;
    private long mt1;
    private long mt2;
    private long mt3;
    private long mt4;
    private static final int SAMPLEPOINT_0 = 1;
    private static final int SAMPLEPOINT_1 = 1000;
    private static final int SAMPLEPOINT_2 = 10000;
    private static final int SAMPLEPOINT_3 = 20000;
    private static final int SAMPLEPOINT_4 = 100000;
    private int mNEndpoints;
    private int mNActiveEndpoints;
    private int mNHighestActiveEndpoints;
    private int mRedeliveredMsgs;
    private int mRedeliveries;
    private int mBypassCommits;
    private int mDeliveryCommits;
    private int mBypassCommitsSinceLastDeliveryCommit;
    private int mDeliveryCommitsSinceLastBypassCommit;

    /**
     * reset
     */
    public synchronized void reset() {
        mNMessages = 0; 
        mt0 = 0;
        mt1 = 0;
        mt2 = 0;
        mt3 = 0;
        mt4 = 0;
        mNHighestActiveEndpoints = 0;
        mBypassCommits = 0;
        mDeliveryCommits = 0;
        mBypassCommitsSinceLastDeliveryCommit = 0;
        mDeliveryCommitsSinceLastBypassCommit = 0;
    }
    
    /**
     * Partial reset: called when the delivery starts so that the stats can be used
     * to monitor deadletter activity 
     */
    public synchronized void resetDeliveryStats() {
        mBypassCommits = 0;
        mDeliveryCommits = 0;
        mBypassCommitsSinceLastDeliveryCommit = 0;
        mDeliveryCommitsSinceLastBypassCommit = 0;
    }

    /**
     * messageDelivered (irrespective transaction outcome)
     */
    public synchronized void messageDelivered() {
        mNMessages++;
        if (mNMessages == SAMPLEPOINT_0) {
            mt0 = System.currentTimeMillis();
        } else if (mNMessages == SAMPLEPOINT_1) {
            mt1 = System.currentTimeMillis();
        } else if (mNMessages == SAMPLEPOINT_2) {
            mt2 = System.currentTimeMillis();
        } else if (mNMessages == SAMPLEPOINT_3) {
            mt3 = System.currentTimeMillis();
        } else if (mNMessages == SAMPLEPOINT_4) {
            mt4 = System.currentTimeMillis();
        }
        mNActiveEndpoints--;
    }

    /**
     * Called before a message is delivered to an endpoint
     */
    public synchronized void aboutToDeliverMessage() {
        mNActiveEndpoints++;
        if (mNActiveEndpoints > mNHighestActiveEndpoints) {
            mNHighestActiveEndpoints = mNActiveEndpoints;
        }
    }

    /**
     * addMessageEndpoint
     */
    public synchronized void addMessageEndpoint() {
        mNEndpoints++;
    }

    /**
     * removeMessageEndpoint
     */
    public synchronized void removeMessageEndpoint() {
        mNEndpoints--;
    }

    /**
     * getNMessages
     *
     * @return int
     */
    public synchronized int getNMessages() {
        return mNMessages;
    }

    private static String rate(int n, long dt) {
        if (n <= 0 || dt <= 0) {
            return "n/a";
        }

        return "" + 1000 * (float) n / dt;
    }

    /**
     * Dumps out all stats in human readable form
     *
     * @return String
     */
    @Override
    public synchronized String toString() {
        long tnow = System.currentTimeMillis();
        return "nMessages=" + mNMessages
            + "; t(" + SAMPLEPOINT_0 + ")=" + mt0
            + "; t(" + SAMPLEPOINT_1 + ")=" + mt1
            + "; t(" + SAMPLEPOINT_2 + ")=" + mt2
            + "; t(" + SAMPLEPOINT_3 + ")=" + mt3
            + "; t(" + SAMPLEPOINT_4 + ")=" + mt4
            + "; t(now)=" + tnow
            + "; rate(" + SAMPLEPOINT_0 + "," + SAMPLEPOINT_1 + ")=" + rate(SAMPLEPOINT_1 - SAMPLEPOINT_0, mt1 - mt0)
            + "; rate(" + SAMPLEPOINT_1 + "," + SAMPLEPOINT_2 + ")=" + rate(SAMPLEPOINT_2 - SAMPLEPOINT_1, mt2 - mt1)
            + "; rate(" + SAMPLEPOINT_2 + "," + SAMPLEPOINT_3 + ")=" + rate(SAMPLEPOINT_3 - SAMPLEPOINT_2, mt3 - mt2)
            + "; rate(" + SAMPLEPOINT_3 + "," + SAMPLEPOINT_4 + ")=" + rate(SAMPLEPOINT_4 - SAMPLEPOINT_3, mt4 - mt3)
            + "; rate(" + SAMPLEPOINT_0 + "," + mNMessages + ")=" + rate(mNMessages - SAMPLEPOINT_1, tnow - mt0)
            + "; nEndpoints=" + mNEndpoints
            + "; nRedelivies=" + mRedeliveries
            + "; nRedeliveredMsgs=" + mRedeliveredMsgs
            + "; msgsDeliveryCommits=" + mDeliveryCommits
            + "; msgsDeliveryCommitsSinceLastBypassCommit=" + mDeliveryCommitsSinceLastBypassCommit
            + "; msgsBypassCommits=" + mBypassCommits
            + "; msgsBypassCommitsSinceLastDeliveryCommit=" + mBypassCommitsSinceLastDeliveryCommit;
        
    }

    /**
     * getNTotalMDBs
     *
     * @return int
     */
    public synchronized int getNTotalEndpoints() {
        return mNEndpoints;
    }

    /**
     * getNActiveMDBs
     *
     * @return int
     */
    public synchronized int getNActiveEndpoints() {
        return mNActiveEndpoints;
    }

    /**
     * getNHighestEndpoints
     *
     * @return int
     */
    public synchronized int getNHighestEndpoints() {
        return mNHighestActiveEndpoints;
    }

    /**
     * Called when a msg is being redelivered (irrespective transaction outcome)
     */
    public synchronized void msgRedelivered() {
        mRedeliveries++;
    }

    /**
     * Called when a new redelivery is detected, e.g. a msg that was not seen before
     */
    public synchronized void msgRedeliveredFirstTime() {
        mRedeliveredMsgs++;
    }

    /**
     * Called when a msg bypass (moved to DLQ or deleted) was successful
     */
    public synchronized void msgDeliveryBypassCommit() {
        mBypassCommits++;
        mBypassCommitsSinceLastDeliveryCommit++;
        mDeliveryCommitsSinceLastBypassCommit = 0;
    }

    /**
     * Called when a msg was successfully delivered and committed
     */
    public synchronized void msgDeliveryCommit() {
        mDeliveryCommits++;
        mBypassCommitsSinceLastDeliveryCommit = 0;
        mDeliveryCommitsSinceLastBypassCommit++;
    }
    
    /**
     * @return all stats in the form of a map
     */
    public synchronized Map<String, Long> getDump() {
        Map<String, Long> ret = new HashMap<String, Long>();
        ret.put(Options.Stats.NMESSAGES, Long.valueOf(mNMessages));
        ret.put(Options.Stats.TIME_AT_SAMPLEPOINT_0, mt0);
        ret.put(Options.Stats.TIME_AT_SAMPLEPOINT_1, mt1);
        ret.put(Options.Stats.TIME_AT_SAMPLEPOINT_2, mt2);
        ret.put(Options.Stats.TIME_AT_SAMPLEPOINT_3, mt3);
        ret.put(Options.Stats.TIME_AT_SAMPLEPOINT_4, mt4);
        ret.put(Options.Stats.MSGS_AT_SAMPLEPOINT_0, Long.valueOf(SAMPLEPOINT_0));
        ret.put(Options.Stats.MSGS_AT_SAMPLEPOINT_1, Long.valueOf(SAMPLEPOINT_1));
        ret.put(Options.Stats.MSGS_AT_SAMPLEPOINT_2, Long.valueOf(SAMPLEPOINT_2));
        ret.put(Options.Stats.MSGS_AT_SAMPLEPOINT_3, Long.valueOf(SAMPLEPOINT_3));
        ret.put(Options.Stats.MSGS_AT_SAMPLEPOINT_4, Long.valueOf(SAMPLEPOINT_4));
        ret.put(Options.Stats.ENDPOINTS, Long.valueOf(mNEndpoints));
        ret.put(Options.Stats.ACTIVE_ENDPOINTS, Long.valueOf(mNActiveEndpoints));
        ret.put(Options.Stats.HIGEST_ENDPOINTS, Long.valueOf(mNHighestActiveEndpoints));
        ret.put(Options.Stats.REDELIVERED_MSGS, Long.valueOf(mRedeliveredMsgs));
        ret.put(Options.Stats.REDELIVERIES, Long.valueOf(mRedeliveries));
        ret.put(Options.Stats.BYPASS_COMMITS, Long.valueOf(mBypassCommits));
        ret.put(Options.Stats.DELIVERY_COMMITS, Long.valueOf(mDeliveryCommits));
        ret.put(Options.Stats.BYPASS_COMMITS_SINCE_LAST_DELIVERY_COMMIT, Long.valueOf(mBypassCommitsSinceLastDeliveryCommit));
        ret.put(Options.Stats.DELIVERY_COMMITS_SINCE_LAST_BYPASS_COMMIT, Long.valueOf(mDeliveryCommitsSinceLastBypassCommit));
        return ret;
    }
}
