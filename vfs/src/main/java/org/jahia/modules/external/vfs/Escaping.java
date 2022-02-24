/*
 * Copyright (C) 2002-2022 Jahia Solutions Group SA. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
