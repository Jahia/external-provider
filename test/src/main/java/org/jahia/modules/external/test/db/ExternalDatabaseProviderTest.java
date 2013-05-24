/**
 * This file is part of Jahia, next-generation open source CMS:
 * Jahia's next-generation, open source CMS stems from a widely acknowledged vision
 * of enterprise application convergence - web, search, document, social and portal -
 * unified by the simplicity of web content management.
 *
 * For more information, please visit http://www.jahia.com.
 *
 * Copyright (C) 2002-2013 Jahia Solutions Group SA. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * As a special exception to the terms and conditions of version 2.0 of
 * the GPL (or any later version), you may redistribute this Program in connection
 * with Free/Libre and Open Source Software ("FLOSS") applications as described
 * in Jahia's FLOSS exception. You should have received a copy of the text
 * describing the FLOSS exception, and it is also available here:
 * http://www.jahia.com/license
 *
 * Commercial and Supported Versions of the program (dual licensing):
 * alternatively, commercial and supported versions of the program may be used
 * in accordance with the terms and conditions contained in a separate
 * written agreement between you and Jahia Solutions Group SA.
 *
 * If you are unsure which license is appropriate for your use,
 * please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.external.test.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Locale;

import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.jahia.api.Constants;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRPropertyWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.test.JahiaTestCase;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for the external provider implementation.
 * 
 * @author Sergiy Shyrkov
 */
public class ExternalDatabaseProviderTest extends JahiaTestCase {

    private static String GENERIC_PROVIDER_MOUNTPOINT = "/external-database-generic";

    private static String MAPPED_PROVIDER_MOUNTPOINT = "/external-database-mapped";

    @BeforeClass
    public static void oneTimeSetup() throws Exception {
        // do nothing
    }

    @AfterClass
    public static void oneTimeTearDown() throws Exception {
        // do nothing
    }

    private JCRSessionWrapper session;

    public void checkProperties(JCRNodeWrapper amsterdam) throws Exception {
        // property existence
        assertTrue(amsterdam.hasProperty("city_id"));
        assertTrue(amsterdam.hasProperty("airport"));
        assertTrue(amsterdam.hasProperty("city_name"));
        assertTrue(amsterdam.hasProperty("country_iso_code"));
        assertTrue(amsterdam.hasProperty("language"));
        assertTrue(amsterdam.hasProperty("country"));
        assertTrue(amsterdam.hasProperty("jcr:uuid"));
        assertFalse(amsterdam.hasProperty("jcr:test"));
        assertFalse(amsterdam.hasProperty("city_main_post_code"));

        // property values
        assertEquals("1", amsterdam.getProperty("city_id").getString());
        assertEquals("AMS", amsterdam.getProperty("airport").getString());
        assertEquals("Amsterdam", amsterdam.getProperty("city_name").getString());
        assertEquals("NL", amsterdam.getProperty("country_iso_code").getString());
        assertEquals("Dutch", amsterdam.getProperty("language").getString());
        assertEquals("Netherlands", amsterdam.getProperty("country").getString());

        try {
            amsterdam.getProperty("city_main_post_code");
            fail("Property city_main_post_code should not have been found");
        } catch (PathNotFoundException e) {
            // property is not present
        }
    }

    private long getResultCount(String query) throws RepositoryException, InvalidQueryException {
        return getResultCount(query, 0, 0);
    }

    private long getResultCount(String query, long limit, long offset) throws RepositoryException,
            InvalidQueryException {
        return query(query, limit, offset).getNodes().getSize();
    }

    private QueryResult query(String query, long limit, long offset) throws RepositoryException, InvalidQueryException {
        Query queryObject = session.getWorkspace().getQueryManager().createQuery(query, Query.JCR_SQL2);
        if (limit > 0) {
            queryObject.setLimit(limit);
        }
        if (offset > 0) {
            queryObject.setOffset(offset);
        }
        return queryObject.execute();
    }

    @Before
    public void setUp() throws RepositoryException {
        session = JCRSessionFactory.getInstance().getCurrentUserSession(Constants.EDIT_WORKSPACE, Locale.ENGLISH);

        assertTrue("Cannot find mounted provider at " + GENERIC_PROVIDER_MOUNTPOINT,
                session.nodeExists(GENERIC_PROVIDER_MOUNTPOINT));
        assertTrue("Cannot find mounted provider at " + MAPPED_PROVIDER_MOUNTPOINT,
                session.nodeExists(MAPPED_PROVIDER_MOUNTPOINT));
    }

    @After
    public void tearDown() {
        session.logout();
    }

    @Test
    public void testGenericNodes() throws Exception {
        JCRNodeWrapper root = session.getNode(GENERIC_PROVIDER_MOUNTPOINT);

        // node existence
        assertTrue(session.nodeExists(GENERIC_PROVIDER_MOUNTPOINT + "/CITIES"));
        assertNotNull(session.getNode(GENERIC_PROVIDER_MOUNTPOINT + "/CITIES"));
        assertFalse(session.nodeExists(GENERIC_PROVIDER_MOUNTPOINT + "/CITIES2"));
        try {
            session.getNode(GENERIC_PROVIDER_MOUNTPOINT + "/CITIES2");
            fail("Node " + GENERIC_PROVIDER_MOUNTPOINT + "/CITIES2" + " should not have been found");
        } catch (PathNotFoundException e) {

        }

        // hasNodes()
        assertTrue(root.hasNodes());
        assertTrue(root.hasNode("AIRLINES"));

        // getNodes()
        assertEquals(7, root.getNodes().getSize());
        assertEquals(1, root.getNodes("AIRLINES").getSize());
        assertEquals(2, root.getNodes("AIRLINES | MAPS").getSize());
        assertEquals(1, root.getNodes("AIR*").getSize());
        assertEquals(2, root.getNodes("FLIGHTS*").getSize());
        assertEquals(3, root.getNodes("FLIGHT*").getSize());
        assertEquals(4, root.getNodes("AIR* | FLIGHT*").getSize());
        assertEquals(1, root.getNodes(new String[] { "AIRLINES" }).getSize());
        assertEquals(2, root.getNodes(new String[] { "AIRLINES", "MAPS" }).getSize());
        assertEquals(1, root.getNodes(new String[] { "AIR*" }).getSize());
        assertEquals(2, root.getNodes(new String[] { "FLIGHTS*" }).getSize());
        assertEquals(3, root.getNodes(new String[] { "FLIGHT*" }).getSize());
        assertEquals(4, root.getNodes(new String[] { "AIR*", "FLIGHT*" }).getSize());

        // node type mapping
        assertTrue(session.getNode(GENERIC_PROVIDER_MOUNTPOINT).isNodeType(GenericDatabaseDataSource.DATA_TYPE_SCHEMA));
        assertTrue(session.getNode(GENERIC_PROVIDER_MOUNTPOINT + "/CITIES").isNodeType(
                GenericDatabaseDataSource.DATA_TYPE_TABLE));
        assertTrue(session.getNode(GENERIC_PROVIDER_MOUNTPOINT + "/CITIES/MQ").isNodeType(
                GenericDatabaseDataSource.DATA_TYPE_ROW));

        // UUID and path
        JCRNodeWrapper amsterdam = root.getNode("CITIES").getNode("MQ");
        String id = amsterdam.getIdentifier();
        String path = amsterdam.getPath();
        assertEquals(id, session.getNodeByIdentifier(id).getIdentifier());
        assertEquals(id, session.getNode(GENERIC_PROVIDER_MOUNTPOINT + "/CITIES/MQ").getIdentifier());
        assertEquals(path, session.getNode(GENERIC_PROVIDER_MOUNTPOINT + "/CITIES/MQ").getPath());
        assertEquals(path, root.getNode("CITIES").getNode("MQ").getPath());
        assertEquals(root.getNode("CITIES").getNode("MQ").getIdentifier(),
                session.getNode(GENERIC_PROVIDER_MOUNTPOINT + "/CITIES/MQ").getIdentifier());
        assertEquals(root.getNode("CITIES").getNode("MQ"), session.getNode(GENERIC_PROVIDER_MOUNTPOINT + "/CITIES/MQ"));
    }

    @Test
    public void testGenericProperties() throws Exception {
        checkProperties(session.getNode(GENERIC_PROVIDER_MOUNTPOINT + "/CITIES/MQ"));
    }

    @Test
    public void testMappedNodes() throws Exception {
        JCRNodeWrapper root = session.getNode(MAPPED_PROVIDER_MOUNTPOINT);

        // node existence
        assertTrue(session.nodeExists(MAPPED_PROVIDER_MOUNTPOINT + "/CITIES"));
        assertNotNull(session.getNode(MAPPED_PROVIDER_MOUNTPOINT + "/CITIES"));
        assertFalse(session.nodeExists(MAPPED_PROVIDER_MOUNTPOINT + "/CITIES2"));
        try {
            session.getNode(MAPPED_PROVIDER_MOUNTPOINT + "/CITIES2");
            fail("Node " + MAPPED_PROVIDER_MOUNTPOINT + "/CITIES2" + " should not have been found");
        } catch (PathNotFoundException e) {

        }

        // hasNodes()
        assertTrue(root.hasNodes());
        assertTrue(root.hasNode("AIRLINES"));

        // getNodes()
        assertEquals(4, root.getNodes().getSize());
        assertEquals(1, root.getNodes("AIRLINES").getSize());
        assertEquals(2, root.getNodes("AIRLINES | CITIES").getSize());
        assertEquals(1, root.getNodes("AIR*").getSize());
        assertEquals(2, root.getNodes("C*").getSize());
        assertEquals(1, root.getNodes("FLIGHT*").getSize());
        assertEquals(3, root.getNodes("AIR* | C*").getSize());
        assertEquals(1, root.getNodes(new String[] { "AIRLINES" }).getSize());
        assertEquals(2, root.getNodes(new String[] { "AIRLINES", "CITIES" }).getSize());
        assertEquals(1, root.getNodes(new String[] { "AIR*" }).getSize());
        assertEquals(2, root.getNodes(new String[] { "C*" }).getSize());
        assertEquals(1, root.getNodes(new String[] { "FLIGHT*" }).getSize());
        assertEquals(3, root.getNodes(new String[] { "AIR*", "C*" }).getSize());

        // node type mapping
        assertTrue(session.getNode(MAPPED_PROVIDER_MOUNTPOINT).isNodeType(MappedDatabaseDataSource.DATA_TYPE_CATALOG));
        assertTrue(session.getNode(MAPPED_PROVIDER_MOUNTPOINT + "/CITIES").isNodeType(
                MappedDatabaseDataSource.DATA_TYPE_DIRECTORY));
        assertTrue(session.getNode(MAPPED_PROVIDER_MOUNTPOINT + "/CITIES/1").isNodeType(
                MappedDatabaseDataSource.DATA_TYPE_CITY));

        // UUID and path
        JCRNodeWrapper amsterdam = root.getNode("CITIES").getNode("1");
        String id = amsterdam.getIdentifier();
        String path = amsterdam.getPath();
        assertEquals(id, session.getNodeByIdentifier(id).getIdentifier());
        assertEquals(id, session.getNode(MAPPED_PROVIDER_MOUNTPOINT + "/CITIES/1").getIdentifier());
        assertEquals(path, session.getNode(MAPPED_PROVIDER_MOUNTPOINT + "/CITIES/1").getPath());
        assertEquals(path, root.getNode("CITIES").getNode("1").getPath());
        assertEquals(root.getNode("CITIES").getNode("1").getIdentifier(),
                session.getNode(MAPPED_PROVIDER_MOUNTPOINT + "/CITIES/1").getIdentifier());
        assertEquals(root.getNode("CITIES").getNode("1"), session.getNode(MAPPED_PROVIDER_MOUNTPOINT + "/CITIES/1"));
    }

    @Test
    public void testMappedProperties() throws Exception {
        checkProperties(session.getNode(MAPPED_PROVIDER_MOUNTPOINT + "/CITIES/1"));
    }

    @Test
    public void testQueryConstraints() throws Exception {
        assertEquals(1, getResultCount("select * from [" + MappedDatabaseDataSource.DATA_TYPE_CITY
                + "] where [language] = 'Dutch'"));

        assertEquals(3, getResultCount("select * from [" + MappedDatabaseDataSource.DATA_TYPE_CITY
                + "] where [language] = 'Arabic'"));
        assertEquals(1, getResultCount("select * from [" + MappedDatabaseDataSource.DATA_TYPE_CITY
                + "] where [language] = 'Arabic' and [city_name] = 'Cairo'"));
        assertEquals(0, getResultCount("select * from [" + MappedDatabaseDataSource.DATA_TYPE_CITY
                + "] where [language] = 'Arabic' and [city_name] = 'Amstaredam'"));

        assertEquals(37, getResultCount("select * from [" + MappedDatabaseDataSource.DATA_TYPE_CITY
                + "] where [country_iso_code] = 'US'"));
        assertEquals(
                17,
                getResultCount("select * from [" + MappedDatabaseDataSource.DATA_TYPE_CITY
                        + "] where [country_iso_code] = 'US'", 0, 20));
        assertEquals(
                3,
                getResultCount("select * from [" + MappedDatabaseDataSource.DATA_TYPE_CITY
                        + "] where [country_iso_code] = 'US'", 3, 20));
        assertEquals(
                0,
                getResultCount("select * from [" + MappedDatabaseDataSource.DATA_TYPE_CITY
                        + "] where [country_iso_code] = 'US'", 3, 100));
    }

    @Test
    public void testQueryLimitAndOffset() throws Exception {
        String queryDirs = "select * from [" + MappedDatabaseDataSource.DATA_TYPE_DIRECTORY + "]";

        assertEquals(4, getResultCount(queryDirs, 0, 0));

        assertEquals(1, getResultCount(queryDirs, 1, 0));
        assertEquals(2, getResultCount(queryDirs, 2, 0));
        assertEquals(3, getResultCount(queryDirs, 3, 0));
        assertEquals(4, getResultCount(queryDirs, 4, 0));

        assertEquals(3, getResultCount(queryDirs, 0, 1));
        assertEquals(2, getResultCount(queryDirs, 0, 2));
        assertEquals(1, getResultCount(queryDirs, 0, 3));
        assertEquals(0, getResultCount(queryDirs, 0, 4));
        assertEquals(0, getResultCount(queryDirs, 0, 10));

        assertEquals(3, getResultCount(queryDirs, 3, 1));
        assertEquals(2, getResultCount(queryDirs, 3, 2));
        assertEquals(1, getResultCount(queryDirs, 3, 3));
        assertEquals(0, getResultCount(queryDirs, 3, 4));
        assertEquals(0, getResultCount(queryDirs, 3, 5));
    }

    @Test
    public void testQueryLimitAndOffsetMultipleProviders() throws Exception {
        String query = "select * from [nt:base]";
        long total = getResultCount(query, 0, 0);

        assertEquals(200, getResultCount(query, 0, total - 200));
    }

    @Test
    public void testQueryNodeType() throws Exception {
        // count
        assertEquals(4, getResultCount("select * from [" + MappedDatabaseDataSource.DATA_TYPE_DIRECTORY + "]"));
        assertEquals(2, getResultCount("select * from [" + MappedDatabaseDataSource.DATA_TYPE_AIRLINE + "]"));
        assertEquals(0, getResultCount("select * from [" + GenericDatabaseDataSource.DATA_TYPE_TABLE + "]"));

        for (NodeIterator ni = query("select * from [" + MappedDatabaseDataSource.DATA_TYPE_AIRLINE + "]", 0, 0)
                .getNodes(); ni.hasNext();) {
            assertTrue(ni.nextNode().isNodeType(MappedDatabaseDataSource.DATA_TYPE_AIRLINE));
        }
    }

    @Test
    public void testExtensionProperty() throws Exception {
        JCRNodeWrapper root = session.getNode(MAPPED_PROVIDER_MOUNTPOINT);
        JCRNodeWrapper AA = root.getNode("AIRLINES").getNode("AA");
        assertEquals("Previous value invalid", 5, AA.getProperty("firstclass_seats").getLong());
        AA.setProperty("firstclass_seats", 10);
        assertEquals("Property not updated", 10, AA.getProperty("firstclass_seats").getLong());
    }

    @Test
    public void testMixin() throws Exception {
        JCRNodeWrapper root = session.getNode(MAPPED_PROVIDER_MOUNTPOINT);
        JCRNodeWrapper AA = root.getNode("AIRLINES").getNode("AA");
        AA.addMixin("jmix:comments");
        AA.setProperty("shortView",true);

        assertTrue("Mixin not set", AA.isNodeType("jmix:comments"));
        assertEquals("Property not updated", true, AA.getProperty("shortView").getBoolean());

        AA.removeMixin("jmix:comments");
        assertFalse("Mixin not removed", AA.isNodeType("jmix:comments"));
        assertFalse("Property not removed", AA.hasProperty("shortView"));
    }

    @Test
    public void testAddNode() throws Exception {
        JCRNodeWrapper root = session.getNode(MAPPED_PROVIDER_MOUNTPOINT);
        JCRNodeWrapper AA = root.getNode("AIRLINES").getNode("AA");

        if (AA.isNodeType("jmix:comments")) {
            AA.removeMixin("jmix:comments");
            session.save();
        }

        AA.addMixin("jmix:comments");
        session.save();

        assertEquals(1,AA.getNodes().getSize());

        JCRNodeWrapper comments = AA.getNode("comments");

        assertEquals(comments.getPath(), session.getNodeByIdentifier(comments.getIdentifier()).getPath());
        assertEquals(comments.getIdentifier(), session.getNode(comments.getPath()).getIdentifier());
        assertEquals(AA.getIdentifier(), comments.getParent().getIdentifier());
        assertEquals(4,comments.getDepth());
        assertTrue(comments.isNodeType("jnt:topic"));

        JCRPropertyWrapper propertyWrapper = comments.setProperty("topicSubject", "testSubject");
        assertEquals(comments.getPath() + "/topicSubject", propertyWrapper.getPath());
        assertEquals("testSubject", propertyWrapper.getValue().getString());

        propertyWrapper = comments.getProperty("topicSubject");
        assertEquals(comments.getPath() + "/topicSubject", propertyWrapper.getPath());
        assertEquals("testSubject", propertyWrapper.getValue().getString());

        JCRNodeWrapper post1 = comments.addNode("post1","jnt:post");
        assertEquals(post1.getPath(), session.getNodeByIdentifier(post1.getIdentifier()).getPath());
        assertEquals(post1.getIdentifier(), session.getNode(post1.getPath()).getIdentifier());
        assertEquals(comments.getIdentifier(), post1.getParent().getIdentifier());

        AA.removeMixin("jmix:comments");
        session.save();
    }

    @Test
    public void testSearchOnExtension() throws Exception {
        JCRNodeWrapper root = session.getNode(MAPPED_PROVIDER_MOUNTPOINT);
        JCRNodeWrapper AA = root.getNode("AIRLINES").getNode("AA");
        AA.addMixin("jmix:comments");
        AA.setProperty("shortView",true);
        session.save();

        assertEquals(1, getResultCount("select * from [jmix:comments] as c where isdescendantnode(c, '"+MAPPED_PROVIDER_MOUNTPOINT+"')",0,0));

        AA.removeMixin("jmix:comments");
        session.save();
    }
}
