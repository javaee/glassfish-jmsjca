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
 * $RCSfile: SyncSpecialFeaturesEar1.java,v $
 * $Revision: 1.1.1.1 $
 * $Date: 2007-01-21 07:52:12 $
 *
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.  
 */

package com.stc.jmsjca.test.stcms;

import com.stc.jmsjca.stcms.RASTCMSResourceAdapter;
import com.stc.jmsjca.stcms.RASTCMSResourceAdapterSync;
import com.stc.jmsjca.container.EmbeddedDescriptor;

/**
 * Tests the sync mode
 *
 * @author fkieviet
 * @version $Revision: 1.1.1.1 $
 */
public class SyncSpecialFeaturesEar1 extends SpecialFeaturesEar1 {
    /**
     * Enforces the use of sync instead of any other delivery mode
     * 
     * @see com.stc.jmsjca.test.core.EndToEndBase#getDD()
     */
    public EmbeddedDescriptor getDD() throws Exception {
        EmbeddedDescriptor dd = super.getDD();
        dd.findElementByText(RAXML, RASTCMSResourceAdapter.class.getName()).setText(
            RASTCMSResourceAdapterSync.class.getName());
        return dd;
    }
}
