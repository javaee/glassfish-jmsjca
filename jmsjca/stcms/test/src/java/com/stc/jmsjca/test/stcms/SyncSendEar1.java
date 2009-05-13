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

package com.stc.jmsjca.test.stcms;

import com.stc.jmsjca.container.EmbeddedDescriptor;
import com.stc.jmsjca.stcms.RASTCMSResourceAdapter;
import com.stc.jmsjca.stcms.RASTCMSResourceAdapterSync;
import com.stc.jmsjca.test.core.BaseTestCase;
import com.stc.jmsjca.test.core.JMSProvider;

/**
 * Excercises the same tests using Sync mode
 *
 * @author fkieviet
 * @version $Revision: 1.9 $
 */
public class SyncSendEar1 extends SendEar1 {

    /**
     * @see com.stc.jmsjca.test.core.EndToEndBase#getJMSProvider()
     */
    @Override
    public JMSProvider getJMSProvider() {
        return new StcmsProvider() {
            @Override
            public EmbeddedDescriptor changeDD(EmbeddedDescriptor dd, BaseTestCase.JMSTestEnv test) throws Exception {
            super.changeDD(dd, test);
            dd.findElementByText(RAXML, RASTCMSResourceAdapter.class.getName()).setText(
                RASTCMSResourceAdapterSync.class.getName());
            return dd;
            }
        };
    }
}
