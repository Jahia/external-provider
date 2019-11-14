/*
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2019 Jahia Solutions Group. All rights reserved.
 *
 *     This file is part of a Jahia's Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance with the terms
 *     contained in the Jahia Solutions Group Terms &amp; Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     For questions regarding licensing, support, production usage...
 *     please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *
 * ==========================================================================================
 */
package org.jahia.modules.external.vfs;

import org.jahia.services.content.JCRContentUtils;

/**
 * Utilities to escape illegal JCR characters from file names and paths.
 *
 * @author cmoitrier
 */
final class Escaping {

    // Characters to escape
    private static final String ILLEGAL_JCR_CHARS = "[]*|:";

    /**
     * Escapes characters not supported by JCR from file names and paths
     *
     * <p>Escaped characters are the following ones: {@value #ILLEGAL_JCR_CHARS}.
     * <p>Use {@link #unescapeIllegalJcrChars(String)} for unescaping
     *
     * @param s a string to escape illegal characters from
     * @return a compliant version of {@code s}
     */
    static String escapeIllegalJcrChars(String s) {
        StringBuilder buffer = new StringBuilder(s.length() * 2);
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ILLEGAL_JCR_CHARS.indexOf(ch) != -1) {
                buffer.append('%');
                buffer.append(Character.toUpperCase(Character.forDigit(ch / 16, 16)));
                buffer.append(Character.toUpperCase(Character.forDigit(ch % 16, 16)));
            } else {
                buffer.append(ch);
            }
        }
        return buffer.toString();
    }

    /**
     * Unescapes all characters that have been escaped by {@link #escapeIllegalJcrChars(String)}
     * 
     * @param s a string to unescape characters from
     * @return the unescaped string
     */
    static String unescapeIllegalJcrChars(String s) {
        return JCRContentUtils.unescapeLocalNodeName(s);
    }

    private Escaping() {
        throw new AssertionError();
    }

}
