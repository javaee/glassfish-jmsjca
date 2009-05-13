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

import com.stc.jmsjca.core.Options;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * Provides for easy string formatting
 *
 * @author Frank Kieviet
 * @version $Revision: 1.8 $
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
            return b == null;
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
     * Splits up a single line into multiple lines using a delimiter indicated with mark.
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
    
    private static final String MARK = Options.SEP;
    
    /**
     * Packs a multi-line string into a single line string.
     * 
     * Example:
     *   original=null
     *   toPack=
     *     a=b
     *     c=d,e
     *     
     *   results
     *     JMSJCA.sep=,a=b,c=d\,e
     * 
     * @param original String to take the separator from; may be null; "," is default
     * @param toPack multiline string to convert into a single line string
     * @return singleline string
     */
    public static String packOptions(String original, String toPack) {
        // Determine separator
        char sep = original == null || !original.startsWith(MARK) || original.length() <= MARK.length() 
        ? ',' : original.charAt(MARK.length());
        
        // Escape separators
        toPack = toPack.replace("" + sep, "\\" + sep);
        
        // Normalize line endings
        toPack = toPack.replace("\r\n", "\n");
        toPack = toPack.replace('\r', '\n');
        
        // Fix line endings
        toPack = toPack.replace('\n', sep);
        
        return MARK + sep + toPack;
    }
    
    /**
     * Unpacks a single-line into a multi-line string. A packed string should start
     * with mark=s where s is the separator. Separators are replaced with line
     * endings; separators can be escaped with a backslash.
     * 
     * Example: 
     *    JMSJCA.sep=,a=b,c=d\,e 
     * becomes 
     *    a=b
     *    c=d,e
     * 
     * @param toUnpack Single line string
     * @return multi line string
     */
    public static String unpackOptions(String toUnpack) {
        if (toUnpack == null || !toUnpack.startsWith(MARK) || toUnpack.length() <= MARK.length()) {
            return toUnpack;
        } else {
            StringBuffer ret = new StringBuffer();
            char sep = toUnpack.charAt(MARK.length());
            for (int i = MARK.length() + 1; i < toUnpack.length(); i++) {
                char c = toUnpack.charAt(i);
                if (c == sep) {
                    ret.append("\r\n");
                } else if (c == '\\') {
                    // Is next char a delimiter?
                    if (i < toUnpack.length() - 1 && toUnpack.charAt(i + 1) == sep) {
                        ret.append(sep);
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
            for (Iterator<?> iter = p.entrySet().iterator(); iter.hasNext();) {
                Map.Entry<?, ?> element = (Map.Entry<?, ?>) iter.next();
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

    private static final String PW_PREFIX = "__:ENC:08:";
    
    /**
     * Decodes a password if it is encoded. An encoded password is a base64 encoded
     * password with a special prefix (see code).
     * 
     * @param todecode possibly encoded password, null and empty acceptable
     * @return decoded password, or original if not encoded
     */
    public static String pwdecode(String todecode) {
        // Allowed sanity checking
        if (todecode == null) {
            return null;
        }
        if (todecode.length() == 0) {
            return "";
        }

        // Not encoded?
        if (!todecode.startsWith(PW_PREFIX)) {
            return todecode;
        }
        
        // Encoded
        String encoded = todecode.substring(PW_PREFIX.length());
        
        return Base64Coder.decode(encoded);
    }

    /**
     * For testing only: encodes a password to be used with pwdecode()
     * 
     * @param toencode not-null String
     * @return encoded password
     */
    public static String pwencode(String toencode) {
        return PW_PREFIX + Base64Coder.encode(toencode);
    }

    /**
     * Base64 encoder and decoder implemented as defined by RFC 2045
     * 
     * @author unattributed
     */
    private static final class Base64Coder {
        /** Symbol that represents the end of an input stream */
        private static final int END_OF_INPUT = -1;
        
        /** A character that is not a valid base 64 character. */
        private static final int NON_BASE_64 = -1;
        
        /**
         * Table of the sixty-four characters that are used as the Base64 alphabet:
         * [A-Za-z0-9+/]
         */
        protected static final byte[] BASE64CARS = {
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
            'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b',
            'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p',
            'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3',
            '4', '5', '6', '7', '8', '9', '+', '/',
        };
        
        /**
         * Reverse lookup table for the Base64 alphabet. reversebase64Chars[byte]
         * gives n for the nth Base64 character or negative if a character is not
         * a Base64 character.
         */
        protected static final byte[] DECODETABLE = new byte[0x100];

        static {
            // Fill in NON_BASE_64 for all characters to start with
            for (int i = 0; i < DECODETABLE.length; i++) {
                DECODETABLE[i] = NON_BASE_64;
            }

            // For characters that are BASE64CARS, adjust
            // the reverse lookup table.
            for (byte i = 0; i < BASE64CARS.length; i++) {
                DECODETABLE[BASE64CARS[i]] = i;
            }
        }
        
        private Base64Coder() {
            
        }
        
        /**
         * Encode a String in Base64. The String is converted to and from bytes
         * according to the platform's default character encoding. No line breaks
         * or other white space are inserted into the encoded data.
         *
         * @param string The data to encode.
         *
         * @return An encoded String.
         */
        public static String encode(String string) {
            return new String(encode(string.getBytes()));
        }

        /**
         * Encode a String in Base64. No line breaks or other white space are
         * inserted into the encoded data.
         *
         * @param string The data to encode.
         * @param enc Character encoding to use when converting to and from bytes.
         *
         * @return An encoded String.
         *
         * @throws UnsupportedEncodingException if the character encoding specified
         *         is not supported.
         */
        public static String encode(String string, String enc)
            throws UnsupportedEncodingException {
            return new String(encode(string.getBytes(enc)), enc);
        }

        /**
         * Encode bytes in Base64. No line breaks or other white space are
         * inserted into the encoded data.
         * 
         * @param bytes The data to encode.
         * 
         * @return Encoded bytes.
         */
        public static byte[] encode(byte[] bytes) {
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);

            // calculate the length of the resulting output.
            // in general it will be 4/3 the size of the input
            // but the input length must be divisible by three.
            // If it isn't the next largest size that is divisible
            // by three is used.
            int mod;
            int length = bytes.length;

            if ((mod = length % 3) != 0) {
                length += 3 - mod;
            }

            length = (length * 4) / 3;

            ByteArrayOutputStream out = new ByteArrayOutputStream(length);

            try {
                encode(in, out, false);
            } catch (IOException x) {
                // This can't happen.
                // The input and output streams were constructed
                // on memory structures that don't actually use IO.
            }

            return out.toByteArray();
        }

        /**
         * Encode data from the InputStream to the OutputStream in Base64. Line
         * breaks are inserted every 76 characters in the output.
         * 
         * @param in Stream from which to read data that needs to be encoded.
         * @param out Stream to which to write encoded data.
         * 
         * @throws IOException if there is a problem reading or writing.
         */
        public static void encode(InputStream in, OutputStream out) throws IOException {
            encode(in, out, true);
        }

        /**
         * Encode data from the InputStream to the OutputStream in Base64.
         * 
         * @param in Stream from which to read data that needs to be encoded.
         * @param out Stream to which to write encoded data.
         * @param lineBreaks Whether to insert line breaks every 76 characters
         *            in the output.
         * 
         * @throws IOException if there is a problem reading or writing.
         */
        public static void encode(InputStream in, OutputStream out, boolean lineBreaks)
            throws IOException {
            // Base64 encoding converts three bytes of input to
            // four bytes of output
            int[] inBuffer = new int[3];
            int lineCount = 0;

            boolean done = false;

            while (!done && ((inBuffer[0] = in.read()) != END_OF_INPUT)) {
                // Fill the buffer
                inBuffer[1] = in.read();
                inBuffer[2] = in.read();

                // Calculate the out Buffer
                // The first byte of our in buffer will always be valid
                // but we must check to make sure the other two bytes
                // are not END_OF_INPUT before using them.
                // The basic idea is that the three bytes get split into
                // four bytes along these lines:
                //      [AAAAAABB] [BBBBCCCC] [CCDDDDDD]
                // [xxAAAAAA] [xxBBBBBB] [xxCCCCCC] [xxDDDDDD]
                // bytes are considered to be zero when absent.
                // the four bytes are then mapped to common ASCII symbols
                // A's: first six bits of first byte
                out.write(BASE64CARS[inBuffer[0] >> 2]);

                if (inBuffer[1] != END_OF_INPUT) {
                    // B's: last two bits of first byte, first four bits of second byte
                    out.write(
                            BASE64CARS[((inBuffer[0] << 4) & 0x30)
                                       | (inBuffer[1] >> 4)]);

                    if (inBuffer[2] != END_OF_INPUT) {
                        // C's: last four bits of second byte, first two bits of third byte
                        out.write(
                                BASE64CARS[((inBuffer[1] << 2) & 0x3c)
                                           | (inBuffer[2] >> 6)]);
                        // D's: last six bits of third byte
                        out.write(BASE64CARS[inBuffer[2] & 0x3F]);
                    } else {
                        // C's: last four bits of second byte
                        out.write(BASE64CARS[(inBuffer[1] << 2) & 0x3c]);
                        // an equals sign for a character that is not a Base64 character
                        out.write('=');
                        done = true;
                    }
                } else {
                    // B's: last two bits of first byte
                    out.write(BASE64CARS[(inBuffer[0] << 4) & 0x30]);
                    // an equal signs for characters that is not a Base64 characters
                    out.write('=');
                    out.write('=');
                    done = true;
                }

                lineCount += 4;

                if (lineBreaks && (lineCount >= 76)) {
                    out.write('\n');
                    lineCount = 0;
                }
            }

            if (lineBreaks && (lineCount >= 1)) {
                out.write('\n');
                lineCount = 0;
            }

            out.flush();
        }

        /**
         * Decode a Base64 encoded String. Characters that are not part of the
         * Base64 alphabet are ignored in the input. The String is converted to
         * and from bytes according to the platform's default character encoding.
         *
         * @param string The data to decode.
         *
         * @return A decoded String.
         */
        public static String decode(String string) {
            return new String(decode(string.getBytes()));
        }

        /**
         * Decode a Base64 encoded String. Characters that are not part of the
         * Base64 alphabet are ignored in the input.
         *
         * @param string The data to decode.
         * @param enc Character encoding to use when converting to and from bytes.
         *
         * @return A decoded String.
         *
         * @throws UnsupportedEncodingException if the character encoding specified
         *         is not supported.
         */
        public static String decode(String string, String enc)
        throws UnsupportedEncodingException {
            return new String(decode(string.getBytes(enc)), enc);
        }

        /**
         * Decode Base64 encoded bytes. Characters that are not part of the
         * Base64 alphabet are ignored in the input.
         * 
         * @param bytes The data to decode.
         * 
         * @return Decoded bytes.
         */
        public static byte[] decode(byte[] bytes) {
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);

            // calculate the length of the resulting output.
            // in general it will be at most 3/4 the size of the input
            // but the input length must be divisible by four.
            // If it isn't the next largest size that is divisible
            // by four is used.
            int mod;
            int length = bytes.length;

            if ((mod = length % 4) != 0) {
                length += 4 - mod;
            }

            length = (length * 3) / 4;

            ByteArrayOutputStream out = new ByteArrayOutputStream(length);

            try {
                decode(in, out, false);
            } catch (IOException x) {
                // This can't happen.
                // The input and output streams were constructed
                // on memory structures that don't actually use IO.
                ;
            }

            return out.toByteArray();
        }

        /**
         * Reads the next (decoded) Base64 character from the input stream. Non
         * Base64 characters are skipped.
         * 
         * @param in Stream from which bytes are read.
         * @param throwExceptions Throw an exception if an unexpected character
         *            is encountered.
         * 
         * @return the next Base64 character from the stream or -1 if there are
         *         no more Base64 characters on the stream.
         * 
         * @throws IOException if an IO Error occurs.
         * @throws Base64DecodingException if unexpected data is encountered
         *             when throwExceptions is specified.
         */
        private static int readBase64(InputStream in, boolean throwExceptions)
        throws IOException {
            int read;

            do {
                read = in.read();

                if (read == END_OF_INPUT) {
                    return END_OF_INPUT;
                }
                read = DECODETABLE[(byte) read];
            } while (read <= NON_BASE_64);

            return read;
        }

        /**
         * Decode Base64 encoded data from the InputStream to the OutputStream.
         * Characters in the Base64 alphabet, white space and equals sign are
         * expected to be in urlencoded data.  The presence of other characters
         * could be a sign that the data is corrupted.
         *
         * @param in Stream from which to read data that needs to be decoded.
         * @param out Stream to which to write decoded data.
         *
         * @throws IOException if an IO error occurs.
         */
        public static void decode(InputStream in, OutputStream out)
        throws IOException {
            decode(in, out, true);
        }

        /**
         * Decode Base64 encoded data from the InputStream to the OutputStream.
         * Characters in the Base64 alphabet, white space and equals sign are
         * expected to be in urlencoded data. The presence of other characters
         * could be a sign that the data is corrupted.
         * 
         * @param in Stream from which to read data that needs to be decoded.
         * @param out Stream to which to write decoded data.
         * @param throwExceptions Whether to throw exceptions when unexpected
         *            data is encountered.
         * 
         * @throws IOException if an IO error occurs.
         */
        public static void decode(InputStream in, OutputStream out,
                boolean throwExceptions)
        throws IOException {
            // Base64 decoding converts four bytes of input to three bytes of output
            int[] inBuffer = new int[4];

            // read bytes unmapping them from their ASCII encoding in the process
            // we must read at least two bytes to be able to output anything
            boolean done = false;

            while (!done
                && ((inBuffer[0] = readBase64(in, throwExceptions)) != END_OF_INPUT)
                && ((inBuffer[1] = readBase64(in, throwExceptions)) != END_OF_INPUT)) {
                // Fill the buffer
                inBuffer[2] = readBase64(in, throwExceptions);
                inBuffer[3] = readBase64(in, throwExceptions);

                // Calculate the output
                // The first two bytes of our in buffer will always be valid
                // but we must check to make sure the other two bytes
                // are not END_OF_INPUT before using them.
                // The basic idea is that the four bytes will get reconstituted
                // into three bytes along these lines:
                // [xxAAAAAA] [xxBBBBBB] [xxCCCCCC] [xxDDDDDD]
                //      [AAAAAABB] [BBBBCCCC] [CCDDDDDD]
                // bytes are considered to be zero when absent.
                // six A and two B
                out.write((inBuffer[0] << 2) | (inBuffer[1] >> 4));

                if (inBuffer[2] != END_OF_INPUT) {
                    // four B and four C
                    out.write((inBuffer[1] << 4) | (inBuffer[2] >> 2));

                    if (inBuffer[3] != END_OF_INPUT) {
                        // two C and six D
                        out.write((inBuffer[2] << 6) | inBuffer[3]);
                    } else {
                        done = true;
                    }
                } else {
                    done = true;
                }
            }

            out.flush();
        }
    }
}
