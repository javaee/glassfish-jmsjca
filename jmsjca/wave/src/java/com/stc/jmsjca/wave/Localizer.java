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

package com.stc.jmsjca.wave;

import com.stc.jmsjca.localization.LocalizationSupport;

import java.util.regex.Pattern;

/**
 * Provides localization services for java files in this jar. This class needs
 * to be a separate class instead of re-using the base class because the resource bundle
 * is located in this jar rather than the one of the base class. Note that the name
 * of this class needs to be Localizer to support automatic discovery of 
 * internationalizable messages in the java files in this jar.
 * 
 * @author Frank Kieviet
 * @version $Revision: 1.2 $
 */
public class Localizer extends LocalizationSupport {
    private static Localizer sLocalizer = new Localizer(LocalizationSupport.DEFAULTPATTERN);

    private Localizer(Pattern idpattern) {
        super(idpattern);
    }

    /**
     * @return singleton object
     */
    public static Localizer get() {
        return sLocalizer;
    }
}
