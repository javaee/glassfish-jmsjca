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
package com.stc.jmsjca.util;

import com.stc.jmsjca.localization.LocalizedString;

import java.util.ResourceBundle;

/**
 * A logger that exposes the same "interface" as the Log4J logger but in fact logs to a
 * java.util.logging.Logger delegate. It can be used to make migration from the log4j
 * package to the java.util.logging package easier.
 *
 * @author Frank Kieviet
 * @version $Revision: 1.7 $
 */
public final class Logger {
    private final java.util.logging.Logger mDelegate;

    private Logger(java.util.logging.Logger delegate) {
        mDelegate = delegate;
    }

    /**
     * See {@link org.apache.log4j.Logger#getLogger}
     *
     * @param name name of the logger
     * @return Logger instance
     */
    public static Logger getLogger(String name) {
        return new Logger(java.util.logging.Logger.getLogger(name));
    }

    /**
     * See {@link org.apache.log4j.Logger#}
     *
     * @param clazz Class<?> whose name is to be used as the logger name
     * @return Logger instance
     */
    public static Logger getLogger(Class<?> clazz) {
        return getLogger(clazz.getName());
    }

    /**
     * See {@link org.apache.log4j.Category#debug}
     *
     * @param message msg to be logged
     */
    public void debug(Object message) {
        mDelegate.log(java.util.logging.Level.FINE,
            message == null ? null : message.toString());
    }

    /**
     * See {@link org.apache.log4j.Category#debug}
     *
     * @param message msg to be logged
     * @param t exception
     */
    public void debug(Object message, Throwable t) {
        mDelegate.log(java.util.logging.Level.FINE,
            message == null ? null : message.toString(), t);
    }

    /**
     * See {@link org.apache.log4j.Category#error}
     *
     * @param message msg to be logged
     */
    public void error(LocalizedString message) {
        mDelegate.log(java.util.logging.Level.SEVERE,
            message == null ? null : message.toString());
    }

    /**
     * See {@link org.apache.log4j.Category#error}
     *
     * @param message msg to be logged
     * @param t exception to be logged
     */
    public void error(LocalizedString message, Throwable t) {
        mDelegate.log(java.util.logging.Level.SEVERE,
            message == null ? null : message.toString(), t);
    }

    /**
     * See {@link org.apache.log4j.Category#error}
     *
     * @param message msg to be logged
     * @param t exception to be logged
     */
    public void errorNoloc(String message, Throwable t) {
        mDelegate.log(java.util.logging.Level.SEVERE,
            message == null ? null : message.toString(), t);
    }

    /**
     * See {@link org.apache.log4j.Category#fatal}
     *
     * @param message msg to be logged
     */
    public void fatal(LocalizedString message) {
        mDelegate.log(java.util.logging.Level.SEVERE,
            message == null ? null : message.toString());
    }

    /**
     * See {@link org.apache.log4j.Category#fatal}
     *
     * @param message msg to be logged
     * @param t exception to be logged
     */
    public void fatal(LocalizedString message, Throwable t) {
        mDelegate.log(java.util.logging.Level.SEVERE,
            message == null ? null : message.toString(), t);
    }

    /**
     * See {@link org.apache.log4j.Category#fatal}
     *
     * @param message msg to be logged
     * @param t exception to be logged
     */
    public void fatalNoloc(String message, Throwable t) {
        mDelegate.log(java.util.logging.Level.SEVERE,
            message == null ? null : message.toString(), t);
    }

    /**
     * See {@link org.apache.log4j.Category#info}
     *
     * @param message msg to be logged
     */
    public void info(LocalizedString message) {
        mDelegate.log(java.util.logging.Level.INFO,
            message == null ? null : message.toString());
    }

    /**
     * See {@link org.apache.log4j.Category#info}
     *
     * @param message msg to be logged
     */
    public void infoNoloc(String message) {
        mDelegate.log(java.util.logging.Level.INFO,
            message == null ? null : message.toString());
    }

    /**
     * See {@link org.apache.log4j.Category#info}
     *
     * @param message msg to be logged
     * @param t exception to be logged
     */
    public void infoNoloc(String message, Throwable t) {
        mDelegate.log(java.util.logging.Level.INFO,
            message == null ? null : message.toString(), t);
    }

    /**
     * See {@link org.apache.log4j.Category#info}
     *
     * @param message msg to be logged
     * @param t exception to be logged
     */
    public void info(LocalizedString message, Throwable t) {
        mDelegate.log(java.util.logging.Level.INFO,
            message == null ? null : message.toString(), t);
    }

    /**
     * See {@link org.apache.log4j.Category#isDebugEnabled}
     *
     * @return if debug logging is enabled
     */
    public boolean isDebugEnabled() {
        return mDelegate.isLoggable(java.util.logging.Level.FINE);
    }

    /**
     * See {@link org.apache.log4j.Category#warn}
     *
     * @param message msg to be logged
     */
    public void warn(LocalizedString message) {
        mDelegate.log(java.util.logging.Level.WARNING,
            message == null ? null : message.toString());
    }

    /**
     * See {@link org.apache.log4j.Category#warn}
     *
     * @param message msg to be logged
     * @param t exception to be logged
     */
    public void warn(LocalizedString message, Throwable t) {
        mDelegate.log(java.util.logging.Level.WARNING,
            message == null ? null : message.toString(), t);
    }

    /**
     * See {@link org.apache.log4j.Category#warn}
     *
     * @param message msg to be logged
     */
    public void warnNoloc(String message) {
        mDelegate.log(java.util.logging.Level.WARNING,
            message == null ? null : message.toString());
    }

    /**
     * See {@link org.apache.log4j.Category#getName}
     *
     * @return String
     */
    public String getName() {
        return mDelegate.getName();
    }

    /**
     * See {@link org.apache.log4j.Category#getLevel}
     *
     * @return Level
     */
    public java.util.logging.Level getLevel() {
        return mDelegate.getLevel();
    }

    /**
     * See {@link org.apache.log4j.Category#getResourceBundle}
     *
     * @return ResourceBundle
     */
    public ResourceBundle getResourceBundle() {
        return mDelegate.getResourceBundle();
    }

    /**
     * See {@link org.apache.log4j.Category#isEnabledFor}
     *
     * @param level msg to be logged
     * @return boolean
     */
    public boolean isEnabledFor(java.util.logging.Level level) {
        return mDelegate.isLoggable(level);
    }

    /**
     * See {@link org.apache.log4j.Category#isInfoEnabled}
     *
     * @return boolean
     */
    public boolean isInfoEnabled() {
        return mDelegate.isLoggable(java.util.logging.Level.INFO);
    }

    /**
     * See {@link org.apache.log4j.Category#setLevel}
     *
     * @param level msg to be logged
     */
    public void setLevel(java.util.logging.Level level) {
        mDelegate.setLevel(level);
    }
}
