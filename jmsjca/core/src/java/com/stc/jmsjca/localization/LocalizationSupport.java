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

package com.stc.jmsjca.localization;

import com.stc.jmsjca.util.Str;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tools for obtaining localized messages
 *
 * @author fkieviet
 * @version $Revision: 1.3 $
 */
public abstract class LocalizationSupport {
    private PropertyResourceBundle mBundle;
    private Pattern mIdPattern;
    private String mPrefix;
    
    private static final String NAME = "msgs";

    /**
     * Default pattern to parse a message with
     */
    public static Pattern DEFAULTPATTERN = Pattern.compile("([A-Z]\\d\\d\\d)(: )(.*)"); 
    
    /**
     * @param idpattern pattern to parse message
     * @param prefix module name
     */
    protected LocalizationSupport(Pattern idpattern, String prefix) {
        mIdPattern = idpattern;
        mPrefix = prefix;
        // Strip off the class to obtain the package name
        String packagename = this.getClass().getName();
        int lastdot = packagename.lastIndexOf(".");
        packagename = packagename.substring(0, lastdot);
        try {
            mBundle = (PropertyResourceBundle) ResourceBundle.getBundle(packagename + "." + NAME, 
                Locale.getDefault(), getClass().getClassLoader());
        } catch (Exception e) {
            throw new RuntimeException("Resource bundle could not be loaded: " + e, e);
        }
    }
    
    /**
     * Msg is a string of the form "E001: this is a message". The format of the id is 
     * specified in the constructor, and is the first group of the pattern. E.g. in a 
     * specified pattern of "([A-Z]\d\d\d)(: )( .*), the message id is [A-Z]\d\d\d; in 
     * the example that would be E001.
     * 
     * @param msg Message to be localized
     * @param args arguments
     * @return localized message
     */
    public LocalizedString x(String msg, Object[] args) {
        try {
            Matcher matcher = mIdPattern.matcher(msg);
            if (matcher.matches() && matcher.groupCount() > 1) {
                String msgid = matcher.group(1);
                try {
                    String localizedmsg = mBundle.getString(msgid);
                    return new LocalizedString(mPrefix + "-" + msgid + ": " 
                        + MessageFormat.format(localizedmsg, args));
                } catch (Exception e) {
                    return new LocalizedString(mPrefix + "-" + MessageFormat.format(msg, args));
                }
            } else {
                return new LocalizedString(mPrefix + ": " + MessageFormat.format(msg, args));
            }
        } catch (Exception e) {
            String suffix = args == null || args.length == 0 ? "" : " {" + Str.concat(args, ", ") + "}";
            return new LocalizedString(mPrefix + "<msg ID unknown due to " + e + "> " + msg + suffix);
        }
    }
    
    /**
     * Convenience method for x(String, Object[])
     *  
     * @param msg message to localize
     * @return Localized message
     */
    public LocalizedString x(String msg) {
        return x(msg, new Object[0]);
    }
    
    /**
     * Convenience method for x(String, Object[])
     *  
     * @param msg message to localize
     * @param arg0 argument
     * @return Localized message
     */
    public LocalizedString x(String msg, Object arg0) {
        return x(msg, new Object[] {arg0});
    }
    
    /**
     * Convenience method for x(String, Object[])
     *  
     * @param msg message to localize
     * @param arg0 argument
     * @param arg1 argument
     * @return Localized message
     */
    public LocalizedString x(String msg, Object arg0, Object arg1) {
        return x(msg, new Object[] {arg0, arg1});
    }
    
    /**
     * Convenience method for x(String, Object[])
     *  
     * @param msg message to localize
     * @param arg0 argument
     * @param arg1 argument
     * @param arg2 argument
     * @return Localized message
     */
    public LocalizedString x(String msg, Object arg0, Object arg1, Object arg2) {
        return x(msg, new Object[] {arg0, arg1, arg2});
    }
    
    /**
     * Convenience method for x(String, Object[])
     *  
     * @param msg message to localize
     * @param arg0 argument
     * @param arg1 argument
     * @param arg2 argument
     * @param arg3 argument
     * @return Localized message
     */
    public LocalizedString x(String msg, Object arg0, Object arg1, Object arg2, Object arg3) {
        return x(msg, new Object[] {arg0, arg1, arg2, arg3});
    }
    
    /**
     * Convenience method for x(String, Object[])
     *  
     * @param msg message to localize
     * @param arg0 argument
     * @param arg1 argument
     * @param arg2 argument
     * @param arg3 argument
     * @param arg4 argument
     * @return Localized message
     */
    public LocalizedString x(String msg, Object arg0, Object arg1, Object arg2, Object arg3, Object arg4) {
        return x(msg, new Object[] {arg0, arg1, arg2, arg3, arg4});
    }
    
    /**
     * Convenience method for x(String, Object[])
     *  
     * @param msg message to localize
     * @param arg0 argument
     * @param arg1 argument
     * @param arg2 argument
     * @param arg3 argument
     * @param arg4 argument
     * @param arg5 argument
     * @return Localized message
     */
    public LocalizedString x(String msg, Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
        return x(msg, new Object[] {arg0, arg1, arg2, arg3, arg4, arg5});
    }
}
