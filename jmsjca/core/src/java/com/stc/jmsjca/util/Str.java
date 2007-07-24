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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * Provides for easy string formatting
 *
 * @author Frank Kieviet
 * @version $Revision: 1.6 $
 */
public class Str {
    /**
     * Formats a msg
     *
     * @return formatted string
     * @param msg String
     * @param args Object[]
     */
    public static String msg(String msg, Object[] args) {
        return MessageFormat.format(msg, args);
    }

    /**
     * Formats a msg
     *
     * @return formatted string
     * @param msg String
     */
    public static String msg(String msg) {
        return msg(msg, new Object[] {});
    }

    /**
     * Formats a msg
     *
     * @return formatted string
     * @param msg String
     * @param arg1 Object
     */
    public static String msg(String msg, Object arg1) {
        return msg(msg, new Object[] {arg1});
    }

    /**
     * Formats a msg
     *
     * @return formatted string
     * @param msg String
     * @param arg1 Object
     * @param arg2 Object
     */
    public static String msg(String msg, Object arg1, Object arg2) {
        return msg(msg, new Object[] {arg1, arg2});
    }

    /**
     * Formats a msg
     *
     * @return formatted string
     * @param msg String
     * @param arg1 Object
     * @param arg2 Object
     * @param arg3 Object
     */
    public static String msg(String msg, Object arg1, Object arg2, Object arg3) {
        return msg(msg, new Object[] {arg1, arg2, arg3});
    }

    /**
     * Formats a msg
     *
     * @return formatted string
     * @param msg String
     * @param arg1 Object
     * @param arg2 Object
     * @param arg3 Object
     * @param arg4 Object
     */
    public static String msg(String msg, Object arg1, Object arg2, Object arg3, Object arg4) {
        return msg(msg, new Object[] {arg1, arg2, arg3, arg4});
    }

    /**
     * Formats a msg
     *
     * @return formatted string
     * @param msg String
     * @param arg1 Object
     * @param arg2 Object
     * @param arg3 Object
     * @param arg4 Object
     * @param arg5 Object
     */
    public static String msg(String msg, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
        return msg(msg, new Object[] {arg1, arg2, arg3, arg4, arg5});
    }

    /**
     * Converts a password to a string suitable to display in log files
     *
     * @param inp password
     * @return neutralized string
     */
    public static String password(String inp) {
        if (inp == null) {
            return "null";
        } else if (inp.length() == 0) {
            return "zero-length";
        } else {
            return "###";
        }
    }

    /**
     * Returns true if the specified string is empty (null, "" or just spaces)
     *
     * @param s String
     * @return boolean true if empty
     */
    public static boolean empty(String s) {
        if (s == null || s.length() == 0) {
           return true;
        }

        for (int i = 0; i < s.length(); i++) {
            if (!Character.isSpaceChar(s.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * isEqual
     *
     * @param a String
     * @param b String
     * @return boolean
     */
    public static boolean isEqual(String a, String b) {
        if (a == null) {
            return (b == null);
        } else {
            return a.equals(b);
        }
    }

    /**
     * hash
     *
     * @param seed int
     * @param o Object
     * @return int
     */
    public static int hash(int seed, Object o) {
        if (o == null) {
            return seed + 17;
        }
        return seed * 37 + o.hashCode();
    }

    /**
     * Hash tool
     *
     * @param seed int
     * @param o boolean
     * @return int
     */
    public static int hash(int seed, boolean o) {
        return seed * 37 + (o ? 3 : 7);
    }

    /**
     * Splits up a single line into mulitple lines using a delimiter indicated with mark.
     * The separator can be escaped with a back slash
     * 
     * Example: mark="x=|" toparse="a=b|x=|c=d|e=\|\|f" yields
     *   a=b
     *   x=
     *   c=d
     *   e=||f
     * 
     * @param mark prefix for the separator
     * @param toParse String to parse
     * @return parsed string
     */
    public static String parseProperties(String mark, String toParse) {
        if (empty(toParse)) {
            return toParse;
        }

        int at = toParse.indexOf(mark); 
        if (at < 0) {
            return toParse;
        } else {
            if (at + mark.length() == toParse.length()) {
                throw new RuntimeException("Missing separator character in [" + toParse + "]");
            }
            
            char sep = toParse.charAt(at + mark.length());
            return parseProperties(sep, toParse);
        }
    }
    
    private static String parseProperties(char delimiter, String input) {
        StringBuffer ret = new StringBuffer();
        int n = input.length();
        for (int i = 0; i < n; i++) {
            char c = input.charAt(i);
            if (c == delimiter) {
                ret.append("\r\n");
            } else if (c == '\\') {
                // Is next char a delimiter?
                if (i < n - 1 && input.charAt(i + 1) == delimiter) {
                    ret.append(delimiter);
                    i++;
                } else {
                    ret.append(c);
                }
            } else {
                ret.append(c);
            }
        }
        return ret.toString();
    }

    /**
     * Parses the specified properties and merges them into the
     * specified properties set.
     *
     * @param s serialized properties; may be empty
     * @param toAdd properties set to merge into
     */
    public static void deserializeProperties(String s, Properties toAdd) {
        if (empty(s)) {
            return;
        }

        try {
            // Load
            Properties p = new Properties();
            ByteArrayInputStream inp = new ByteArrayInputStream(s.getBytes("ISO-8859-1"));
            p.load(inp);

            // Copy
            for (Iterator iter = p.entrySet().iterator(); iter.hasNext();) {
                Map.Entry element = (Map.Entry) iter.next();
                toAdd.put(element.getKey(), element.getValue());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load properties: " + e, e);
        }
    }

    /**
     * Serializes a properties set to a String
     *
     * @param p properties to serialize
     * @return String
     */
    public static String serializeProperties(Properties p) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            p.store(out, "");
            return out.toString("ISO-8859-1");
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize properties: " + e, e);
        }
    }

//    /**
//     * Serializes a properties set to a String
//     *
//     * @param p properties to serialize
//     * @return String
//     */
//    public static String propertiesToString(Properties p) {
//        StringBuffer ret = new StringBuffer();
//        for (Iterator iter = p.entrySet().iterator(); iter.hasNext();) {
//            Map.Entry x = (Map.Entry) iter.next();
//            ret.append(x.getKey()).append(" = ").append(x.getValue());
//        }
//        return ret.toString();
//    }
    
    /**
     * Concatenates string components
     * 
     * @param strs components
     * @param delim delimeter, e.g. ", "
     * @return concatenated string
     */
    public static String concat(Object[] strs, String delim) {
        StringBuffer ret = new StringBuffer();
        for (int i = 0; i < strs.length; i++) {
            if (i != 0) {
                ret.append(delim);
            }
            ret.append(strs[i]);
        }
        return ret.toString();
    }
    
    /**
     * Returns if a string is empty or null
     * 
     * @param s string to test
     * @return true if null or empty
     */
    public boolean isEmpty(String s) {
        return s == null || s.length() == 0;
    }
    
    /**
     * Utility to provide key/value pairs in lieu of a full-blown map
     */
    public interface Translator {
        /**
         * Looks up a key/value
         *
         * @param key key
         * @return value
         */
        String get(String key);
    }

    /**
     * Changes all ${names} to their values using the specified Translator;
     * returns the number of substitutions and unresolved values. This
     * code is baesd on the PropetyHelper code in Apache Ant
     *
     * @param value String to process
     * @param substitutions Translator
     * @param nResolved must be int[1]
     * @param nUnresolved must be int[1]
     * @throws java.lang.Exception on failure
     * @return sustituted string
     */
    public static String substituteAntProperty(String value, Translator substitutions, int[] nResolved, 
            int[] nUnresolved) throws Exception {

        StringBuffer ret = new StringBuffer();

        int prev = 0;
        int pos;
        nResolved[0] = 0;
        nUnresolved[0] = 0;

        // search for the next instance of $ from the 'prev' position
        while ((pos = value.indexOf("$", prev)) >= 0) {

            // if there was any text before this, add it as a fragment
            if (pos > 0) {
                ret.append(value.substring(prev, pos));
            }
            
            // if we are at the end of the string, we tack on a $ then move past it
            if (pos == (value.length() - 1)) {
                ret.append("$");
                prev = pos + 1;
            } else if (value.charAt(pos + 1) != '{') {
                // peek ahead to see if the next char is a property or not
                // not a property: insert the char as a literal
                ret.append(value.substring(pos, pos + 2));
                prev = pos + 2;
            } else {
                // property found, extract its name or bail on a typo
                int endName = value.indexOf('}', pos);
                if (endName < 0) {
                    throw new Exception("Syntax error in property in " + value + " at index " + pos);
                }
                String propertyName = value.substring(pos + 2, endName);
                String replacement = substitutions.get(propertyName);
                if (replacement != null) {
                    ret.append(replacement);
                    nResolved[0]++;
                } else {
                    ret.append("${").append(propertyName).append("}");
                    nUnresolved[0]++;
                }
                prev = endName + 1;
            }
        }

        // no more $ signs found
        // if there is any tail to the file, append it
        if (prev < value.length()) {
            ret.append(value.substring(prev));
        }
        
        return ret.toString();
    }
}
