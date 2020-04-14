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
 *     Copyright (C) 2002-2020 Jahia Solutions Group. All rights reserved.
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

import org.junit.Test;

import java.util.function.UnaryOperator;

import static org.jahia.modules.external.vfs.Escaping.*;
import static org.junit.Assert.assertEquals;

/**
 * Unit testing of {@link Escaping}
 *
 * @author cmoitrier
 */
public final class EscapingTest {

    @Test
    public void testEscapeIllegalJcrChars() {
        assertEquals("/foo/bar", escapeIllegalJcrChars("/foo/bar"));
        assertEquals("foo%5Bbar%5D", escapeIllegalJcrChars("foo[bar]"));
        assertEquals("foo%7Cbar", escapeIllegalJcrChars("foo|bar"));
        assertEquals("foo%2Abar", escapeIllegalJcrChars("foo*bar"));
        assertEquals("foo%3Abar", escapeIllegalJcrChars("foo:bar"));
    }

    @Test
    public void testUnescapeIllegalJcrChars() {
        testUnescapeIllegalJcrChars("/foo[]|*/bar::test", Escaping::escapeIllegalJcrChars);
    }

    private static void testUnescapeIllegalJcrChars(String s, UnaryOperator<String> escape) {
        assertEquals(s, unescapeIllegalJcrChars(escape.apply(s)));
    }

}
