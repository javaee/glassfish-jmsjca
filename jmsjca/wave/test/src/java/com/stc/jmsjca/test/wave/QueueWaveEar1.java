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

package com.stc.jmsjca.test.wave;

import com.stc.jmsjca.test.core.JMSProvider;
import com.stc.jmsjca.test.core.QueueEndToEnd;

/**
 *
 * @author fkieviet, cye
 * @version $Revision: 1.10 $
 */
public class QueueWaveEar1 extends QueueEndToEnd {
    /**
     * @see com.stc.jmsjca.test.core.QueueEndToEnd#getMaxConcurrency()
     */
    protected int getMaxConcurrency() {
        // set to the value of maxQueuePresend 
        return 10;
    }
    
    /**
     * Grid does not implement batch size in CC
     */
    public void skip_testBatchXACC() throws Throwable {        
    }
    
    /**
     * Grid does not implement batch size in CC
     */
    public void skip_testBatchXARBCC() throws Throwable {
    }
    
    /**
     * Grid does not implement batch size in CC
     */
    public void skip_testBatchUTCC() throws Throwable {        
    }
    
    /**
     * Grid does not implement batch size in CC
     */
    public void skip_testBatchXAHUACC() throws Throwable {
    }
    
    /**
     * Grid does not implement batch size in CC
     */
    public void skip_testBatchXAHUARBCC() throws Throwable {
    }

    /**
     * @see com.stc.jmsjca.test.core.EndToEndBase#getJMSProvider()
     */
    public JMSProvider getJMSProvider() {
        return new WaveProvider();
    }
}
