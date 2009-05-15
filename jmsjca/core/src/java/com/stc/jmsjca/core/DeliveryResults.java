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
 * Encapsulates the result of a call to deliver
 * 
 * @author fkieviet
 */
public class DeliveryResults {
    private boolean mShouldDiscardEndpoint;
    private boolean mBeforeDeliveryFailed;
    private boolean mAfterDeliveryFailed;
    private boolean mOnMessageFailed;
    private Exception mException;
    private boolean mOnMessageWasCalled;
    private int mNOnMessageWasCalled;
    private boolean mOnMessageWasBypassed;
    private boolean mOnMessageSucceeded;
    private boolean mIsRollbackOnly;
    
    /**
     * Clears the state
     */
    public void reset() {
        mShouldDiscardEndpoint = false;
        mBeforeDeliveryFailed = false;
        mAfterDeliveryFailed = false;
        mIsRollbackOnly = false;
        mOnMessageFailed = false;
        mException = null;
        mOnMessageSucceeded = false;
        mOnMessageWasCalled = false;
        mNOnMessageWasCalled = 0;
        mOnMessageWasBypassed = false;
    }
    
    /**
     * Clears all state except state concerning transactions
     */
    public void resetDeliveryState() {
        mOnMessageFailed = false;
        mException = null;
        mOnMessageSucceeded = false;
        mOnMessageWasCalled = false;
        mOnMessageWasBypassed = false;
    }
    
    /**
     * Getter for onMessageSucceeded
     *
     * @return boolean
     */
    public boolean getOnMessageSucceeded() {
        return mOnMessageSucceeded;
    }
    /**
     * Setter for onMessageSucceeded
     *
     * @param onMessageSucceeded booleanThe onMessageSucceeded to set.
     */
    public void setOnMessageSucceeded(boolean onMessageSucceeded) {
        mOnMessageSucceeded = onMessageSucceeded;
    }
    /**
     * Getter for afterDeliveryFailed
     *
     * @return boolean
     */
    public boolean getAfterDeliveryFailed() {
        return mAfterDeliveryFailed;
    }
    /**
     * Setter for afterDeliveryFailed
     *
     * @param afterDeliveryFailed booleanThe afterDeliveryFailed to set.
     */
    public void setAfterDeliveryFailed(boolean afterDeliveryFailed) {
        mAfterDeliveryFailed = afterDeliveryFailed;
    }
    /**
     * Getter for beforeDeliveryFailed
     *
     * @return boolean
     */
    public boolean getBeforeDeliveryFailed() {
        return mBeforeDeliveryFailed;
    }
    /**
     * Setter for beforeDeliveryFailed
     *
     * @param beforeDeliveryFailed booleanThe beforeDeliveryFailed to set.
     */
    public void setBeforeDeliveryFailed(boolean beforeDeliveryFailed) {
        mBeforeDeliveryFailed = beforeDeliveryFailed;
    }
    /**
     * Getter for exception
     *
     * @return Exception
     */
    public Exception getException() {
        return mException;
    }
    /**
     * Setter for exception
     *
     * @param exception ExceptionThe exception to set.
     */
    public void setException(Exception exception) {
        mException = exception;
    }
    /**
     * Getter for onMessageFailed
     *
     * @return boolean
     */
    public boolean getOnMessageFailed() {
        return mOnMessageFailed;
    }
    /**
     * Setter for onMessageFailed
     *
     * @param onMessageFailed booleanThe onMessageFailed to set.
     */
    public void setOnMessageFailed(boolean onMessageFailed) {
        mOnMessageFailed = onMessageFailed;
    }
    /**
     * Getter for shouldDiscardEndpoint
     *
     * @return boolean
     */
    public boolean getShouldDiscardEndpoint() {
        return mShouldDiscardEndpoint;
    }
    /**
     * Setter for shouldDiscardEndpoint
     *
     * @param shouldDiscardEndpoint booleanThe shouldDiscardEndpoint to set.
     */
    public void setShouldDiscardEndpoint(boolean shouldDiscardEndpoint) {
        mShouldDiscardEndpoint = shouldDiscardEndpoint;
    }

    /**
     * Getter for onMessageWasBypassed
     *
     * @return boolean
     */
    public boolean getOnMessageWasBypassed() {
        return mOnMessageWasBypassed;
    }

    /**
     * Setter for onMessageWasBypassed
     *
     * @param onMessageWasBypassed booleanThe onMessageWasBypassed to set.
     */
    public void setOnMessageWasBypassed(boolean onMessageWasBypassed) {
        mOnMessageWasBypassed = onMessageWasBypassed;
    }

    /**
     * Getter for onMessageWasCalled
     *
     * @return boolean
     */
    public boolean getOnMessageWasCalled() {
        return mOnMessageWasCalled;
    }

    /**
     * Setter for onMessageWasCalled
     *
     * @param onMessageWasCalled booleanThe onMessageWasCalled to set.
     */
    public void setOnMessageWasCalled(boolean onMessageWasCalled) {
        mOnMessageWasCalled = onMessageWasCalled;
        if (onMessageWasCalled) {
            mNOnMessageWasCalled++;
        }
    }

    /**
     * Getter for isRollbackOnly
     *
     * @return boolean
     */
    public boolean getIsRollbackOnly() {
        return mIsRollbackOnly;
    }

    /**
     * Setter for isRollbackOnly
     *
     * @param isRollbackOnly booleanThe isRollbackOnly to set.
     */
    public void setRollbackOnly(boolean isRollbackOnly) {
        mIsRollbackOnly = isRollbackOnly;
    }

    /**
     * Getter for nOnMessageWasCalled
     *
     * @return int
     */
    public int getNOnMessageWasCalled() {
        return mNOnMessageWasCalled;
    }
}

