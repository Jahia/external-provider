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
