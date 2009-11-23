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

package com.stc.jmsjca.unifiedjms;

import com.stc.jmsjca.localization.Localizer;
import com.stc.jmsjca.util.Exc;

import javax.resource.spi.InvalidPropertyException;

import java.util.ListIterator;

/**
 * This Custom Tokenizer class allows Custom command to break strings into tokens.
 * The tokenizer checks for the escape characters and the quotes to determine
 * the tokens.
 * Consider the following examples:
 * <li>
 *   string is <code>name1=value1:name2=value2</code> and the delimiter is :
 *   Custom tokenizer will tokenized the string to:
 *   <blockquote><pre>
 *   name1=value1
 *   name2=value2
 *   </pre></blockquote>
 * </li>
 * <li>
 *   string is <code>name1=abc\:def:name2=value2</code> and the delimiter is :
 *   Custom tokenizer will tokenized the string to:
 *   <blockquote><pre>
 *   name1=abc:def
 *   name2=value2
 *   </pre></blockquote>
 *   notice that abc\:def is not tokenized since it contains an escape character
 *   before the :.
 * </li>
 * <li>
 *   string is <code>name1="abc:def":name2=value2</code> and the delimiter is :
 *   Custom tokenizer will tokenized the string to:
 *   <blockquote><pre>
 *   name1=abc:def
 *   name2=value2
 *   </pre></blockquote>
 *   notice that "abc:def" is not not tokenized since it's in the quotes
 * </li>
 * <li>
 *   string is <code>name1="abc\:def":name2=value2</code> and the delimiter is :
 *   Custom tokenizer will tokenized the string to:
 *   <blockquote><pre>
 *   name1=abc\:def
 *   name2=value2
 *   </pre></blockquote>
 * </li>
 * @author  Jane Young
 */
public class GJRCustomTokenizer {
    static final Localizer L = Localizer.get();
    private static final char ESCAPE_CHAR  = '\\';
    private static final String QUOTE_STRING = "\"";
    private int size;
    private ListIterator<String> tokenIterator;

    /**
     *  constructor that calls popluateList to create the tokeIterator
     *  and size variables.
     *  @param stringToken - the string to tokenize.
     *  @param delimiter - the delimiter to tokenize.
     */
    public GJRCustomTokenizer(String stringToken, String delimiter) {
        if (!checkForMatchingQuotes(stringToken)) {
            throw Exc.rtexc(L.x("E721: The string [{0}] cannot be parsed: " 
                + "it is not properly closed. The delimiter is [{1}]"
                , stringToken, delimiter));
        }

        if (stringToken != null && delimiter != null) {
            tokenIterator = populateList(stringToken, delimiter);
        } else {
            throw Exc.rtexc(L.x("E722: The string [{0}] cannot be parsed: " 
                + "the string or its delimeter is null. The delimeter is [{1}]"
                , stringToken, delimiter));
        }
    }

    /**
     *  returns the number of tokens in the string.
     *  @return number of tokens
     */
    public int countTokens() {
        return size;
    }

    /**
     *  returns true is there are more token in the list
     *  @return true if there are more tokens else false.
     */
    public boolean hasMoreTokens() {
        return tokenIterator.hasNext();
    }

    /**
     *  returns the token string without the  escape characters
     *  @return the next string token without the escape characters.
     */
    public String nextTokenWithoutEscapeAndQuoteChars() throws InvalidPropertyException {
        final String strWOEscape = removeEscapeChars(tokenIterator.next());
        final String strWOQuotes = removeQuoteChars(strWOEscape);
        return removeEscapeCharsFromQuotes(strWOQuotes);
    }


    /**
     *  returns the next token string
     *  @return the next string token
     */
    public String nextToken() {
        return tokenIterator.next();
    }

    /**
     *  This method will check for matching quotes.  If quotes do not match then
     *  return false else return true.
     *  @param str - string to check for matching quotes
     *  @return boolean - true if quotes match else false.
     */
    private boolean checkForMatchingQuotes(String str) {
        //get index of the first quote in the string
        int beginQuote = getStringDelimiterIndex(str, QUOTE_STRING, 0);

        while (beginQuote != -1) {
            int endQuote = getStringDelimiterIndex(str, QUOTE_STRING, beginQuote + 1);
            if (endQuote == -1) {
                return false;
            }
            beginQuote = getStringDelimiterIndex(str, QUOTE_STRING, endQuote + 1);
        }
        return true;
    }


    /**
     *  this methos calls the getStringDelimiterIndex to determine the index
     *  of the delimiter and use that to populate the tokenIterator.
     *  @param strToken - string to tokenize
     *  @param delimiter - delimiter to tokenize the string
     *  @return ListIterator
     */
    private ListIterator<String> populateList(String strToken, String delimiter) {
        java.util.List<String> tokenList = new java.util.Vector<String>();
        int endIndex = getStringDelimiterIndex(strToken, delimiter, 0);
        if (endIndex == -1) {
            tokenList.add(strToken);
        } else {
            int beginIndex = 0;
            while (endIndex > -1) {
                //do not want to add to the list if the string is empty
                if (beginIndex != endIndex) {
                    tokenList.add(strToken.substring(beginIndex, endIndex));
                }
                beginIndex = endIndex + 1;
                endIndex = getStringDelimiterIndex(strToken, delimiter, beginIndex);
            }
            //do not want to add to the list if the begindIndex is the last index
            if (beginIndex != strToken.length()) {
                tokenList.add(strToken.substring(beginIndex));
            }
        }
        size = tokenList.size();
        try {
            return tokenList.listIterator();
        } catch (java.lang.IndexOutOfBoundsException ioe) {
            throw Exc.rtexc(L.x("E723: The string [{0}] could not be parsed: {1}", strToken, ioe), ioe);
        }
    }

    /**
     * Removes the escape characters from the property value
     * @param strValue - string value to remove the escape character
     * @return the string with escape character removed
     */
    private String removeEscapeChars(String strValue)
    throws InvalidPropertyException {
        int prefixIndex = 0;
        java.lang.StringBuffer strbuff = new java.lang.StringBuffer();

        while (prefixIndex < strValue.length()) {
            int delimeterIndex = getStringDelimiterIndex(strValue,
                String.valueOf(ESCAPE_CHAR), prefixIndex);
            if (delimeterIndex == -1) {
                strbuff.append(strValue.substring(prefixIndex));
                break;
            }

            //if a quote is follow by an esacpe then keep the escape character
            if (delimeterIndex + 1 < strValue.length() &&
                String.valueOf(strValue.charAt(delimeterIndex + 1)).equals(QUOTE_STRING)) {
                strbuff.append(strValue.substring(prefixIndex, delimeterIndex + 1));
            } else {
                strbuff.append(strValue.substring(prefixIndex, delimeterIndex));
            }

            prefixIndex = delimeterIndex + 1;
        }
        return strbuff.toString();
    }

    /**
     * Removes escape characters that precedes quotes
     * @param strValue - the string value to remove the escape characters
     * @return string value with escape characters removed
     */
    private String removeEscapeCharsFromQuotes(String strValue) throws InvalidPropertyException {
        int prefixIndex = 0;
        java.lang.StringBuffer strbuff = new java.lang.StringBuffer();

        while (prefixIndex < strValue.length()) {
            int delimeterIndex = strValue.indexOf(String.valueOf(ESCAPE_CHAR), prefixIndex);
            if (delimeterIndex == -1) {
                strbuff.append(strValue.substring(prefixIndex));
                break;
            }
            //if a quote is follow by an esacpe then remove the escape character
            if (String.valueOf(strValue.charAt(delimeterIndex + 1)).equals(QUOTE_STRING)) {
                strbuff.append(strValue.substring(prefixIndex, delimeterIndex));
            } else {
                strbuff.append(strValue.substring(prefixIndex, delimeterIndex + 1));
            }

            prefixIndex = delimeterIndex + 1;
        }
        return strbuff.toString();
    }


    /**
     * Removes the quote characters from the property value
     * @return string value with quotes removed
     */
    private String removeQuoteChars(String strValue)
    throws InvalidPropertyException {
        int prefixIndex = 0;
        java.lang.StringBuffer strbuff = new java.lang.StringBuffer();

        while (prefixIndex < strValue.length()) {
            int delimeterIndex = getStringDelimiterIndex(strValue,
                QUOTE_STRING, prefixIndex);
            if (delimeterIndex == -1) {
                strbuff.append(strValue.substring(prefixIndex));
                break;
            }
            strbuff.append(strValue.substring(prefixIndex, delimeterIndex));
            prefixIndex = delimeterIndex + 1;
        }
        return strbuff.toString();
    }


    /**
     *  This method returns the index of the delimiter.  It will factor out the
     *  escape and quote characters.
     *  @param strToken - string to token
     *  @param delimiter - the delimiter to tokenize
     *  @param fromIndex - the index to start the tokenize
     *  @return index - index of the delimiter in the strToken
     *  @throw CommandException if the end quote do not match.
     */
    private int getStringDelimiterIndex(String strToken, String delimiter, int fromIndex) {
        if (fromIndex > strToken.length() - 1) {
            return -1;
        }

        //get index of the delimiter
        final int hasDelimiter = strToken.indexOf(delimiter, fromIndex);

        //get index of the first quote in the string token
        final int quoteBeginIndex = strToken.indexOf(QUOTE_STRING, fromIndex);

        // ex: set server.ias1.jdbcurl="jdbc://oracle"
        // if there's is a quote and a delimiter, then find the end quote
        if ((quoteBeginIndex != -1) && (hasDelimiter != -1) &&
            (quoteBeginIndex < hasDelimiter)) {
            //get index of the end quote in the string token
            final int quoteEndIndex = strToken.indexOf(QUOTE_STRING, quoteBeginIndex + 1);

            if (quoteEndIndex == -1) {
                throw Exc.rtexc(L.x("E721: The string [{0}] cannot be parsed: " 
                    + "it is not properly closed. The delimiter is [{1}]"
                    , strToken, delimiter));
            }
            if (quoteEndIndex != (strToken.length() - 1)) {
                return getStringDelimiterIndex(strToken, delimiter, quoteEndIndex + 1);
            } else {
                return -1;
            }
        }
        if ((hasDelimiter > 0) && (strToken.charAt(hasDelimiter - 1) == ESCAPE_CHAR)) {
            return getStringDelimiterIndex(strToken, delimiter, hasDelimiter + 1);
        } else {
            return hasDelimiter;
        }
    }
}