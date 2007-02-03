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
 * A listener that is invoked when RA.stop() is called. Currently used for the connection
 * manager so that it can destroy connections and mark the connection manager as stopped.
 *
 * @author Frank Kieviet
 * @version $Revision: 1.1.1.3 $
 */
public interface RAStopListener {
    /**
     * Called by the RA when RA.stop() is called
     */
    void stop();
}
