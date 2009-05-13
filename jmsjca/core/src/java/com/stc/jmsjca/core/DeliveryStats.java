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
 * Runtime statistics information on message delivery (used by an activation and the MBean
 * tied to that activation)
 *
 * @author fkieviet
 * @version $Revision: 1.6 $
 */
public class DeliveryStats {
    private int mNMessages;
    private long mt0;
    private long mt1;
    private long mt2;
    private long mt3;
    private long mt4;
    private int mn0 = 1;
    private int mn1 = 1000;
    private int mn2 = 10000;
    private int mn3 = 20000;
    private int mn4 = 100000;
    private int mNEndpoints;
    private int mNActiveEndpoints;
    private int mNHighestActiveEndpoints;
    private int mRedeliveredMsgs;
    private int mRedeliveries;

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
    }

    /**
     * messageDelivered
     */
    public synchronized void messageDelivered() {
        mNMessages++;
        if (mNMessages == mn0) {
            mt0 = System.currentTimeMillis();
        } else if (mNMessages == mn1) {
            mt1 = System.currentTimeMillis();
        } else if (mNMessages == mn2) {
            mt2 = System.currentTimeMillis();
        } else if (mNMessages == mn3) {
            mt3 = System.currentTimeMillis();
        } else if (mNMessages == mn4) {
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

    private String rate(int n, long dt) {
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
            + "; t(" + mn0 + ")=" + mt0
            + "; t(" + mn1 + ")=" + mt1
            + "; t(" + mn2 + ")=" + mt2
            + "; t(" + mn3 + ")=" + mt3
            + "; t(" + mn4 + ")=" + mt4
            + "; t(now)=" + tnow
            + "; rate(" + mn0 + "," + mn1 + ")=" + rate(mn1 - mn0, mt1 - mt0)
            + "; rate(" + mn1 + "," + mn2 + ")=" + rate(mn2 - mn1, mt2 - mt1)
            + "; rate(" + mn2 + "," + mn3 + ")=" + rate(mn3 - mn2, mt3 - mt2)
            + "; rate(" + mn3 + "," + mn4 + ")=" + rate(mn4 - mn3, mt4 - mt3)
            + "; rate(" + mn0 + "," + mNMessages + ")=" + rate(mNMessages - mn1, tnow - mt0)
            + "; nEndpoints=" + mNEndpoints
            + "; nRedelivies=" + mRedeliveries
            + "; nRedeliveredMsgs=" + mRedeliveredMsgs;
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
    public int getNActiveEndpoints() {
        return mNActiveEndpoints;
    }

    /**
     * getNHighestEndpoints
     *
     * @return int
     */
    public int getNHighestEndpoints() {
        return mNHighestActiveEndpoints;
    }

    /**
     * Called when a msg is being redelivered 
     */
    public void msgRedelivered() {
        mRedeliveries++;
    }

    /**
     * Called when a new redelivery is detected, e.g. a msg that was not seen before
     */
    public void msgRedeliveredFirstTime() {
        mRedeliveredMsgs++;
    }
}
