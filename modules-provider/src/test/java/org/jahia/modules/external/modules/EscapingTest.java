package org.jahia.modules.external.modules;

import org.junit.Test;

import java.util.function.UnaryOperator;

import static org.jahia.modules.external.modules.Escaping.*;
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
