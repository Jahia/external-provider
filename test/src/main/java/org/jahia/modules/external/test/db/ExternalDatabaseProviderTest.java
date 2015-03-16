/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2015 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ======================================================================================
 *
 *     IF YOU DECIDE TO CHOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     "This program is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU General Public License
 *     as published by the Free Software Foundation; either version 2
 *     of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 *     As a special exception to the terms and conditions of version 2.0 of
 *     the GPL (or any later version), you may redistribute this Program in connection
 *     with Free/Libre and Open Source Software ("FLOSS") applications as described
 *     in Jahia's FLOSS exception. You should have received a copy of the text
 *     describing the FLOSS exception, also available here:
 *     http://www.jahia.com/license"
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ======================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 *
 *
 * ==========================================================================================
 * =                                   ABOUT JAHIA                                          =
 * ==========================================================================================
 *
 *     Rooted in Open Source CMS, Jahia’s Digital Industrialization paradigm is about
 *     streamlining Enterprise digital projects across channels to truly control
 *     time-to-market and TCO, project after project.
 *     Putting an end to “the Tunnel effect”, the Jahia Studio enables IT and
 *     marketing teams to collaboratively and iteratively build cutting-edge
 *     online business solutions.
 *     These, in turn, are securely and easily deployed as modules and apps,
 *     reusable across any digital projects, thanks to the Jahia Private App Store Software.
 *     Each solution provided by Jahia stems from this overarching vision:
 *     Digital Factory, Workspace Factory, Portal Factory and eCommerce Factory.
 *     Founded in 2002 and headquartered in Geneva, Switzerland,
 *     Jahia Solutions Group has its North American headquarters in Washington DC,
 *     with offices in Chicago, Toronto and throughout Europe.
 *     Jahia counts hundreds of global brands and governmental organizations
 *     among its loyal customers, in more than 20 countries across the globe.
 *
 *     For more information, please visit http://www.jahia.com
 */
package org.jahia.modules.external.test.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Locale;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.jahia.api.Constants;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRPropertyWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRValueWrapper;
import org.jahia.services.sites.JahiaSite;
import org.jahia.test.JahiaTestCase;
import org.jahia.test.TestHelper;
import org.jahia.test.services.importexport.ImportExportTest;
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

    private final static String GENERIC_PROVIDER_MOUNTPOINT = "/external-database-generic";

    private final static String MAPPED_PROVIDER_MOUNTPOINT = "/external-database-mapped";

    private final static String BATCH_CHILDREN_PROVIDER_MOUNTPOINT = "/external-database-mapped-batch-children";

    private static final String TESTSITE_NAME = "externalProviderExportTest";

    @BeforeClass
    public static void oneTimeSetup() {
        // do nothing
    }

    @AfterClass
    public static void oneTimeTearDown() {
        // do nothing
    }

    private JCRSessionWrapper session;

    public void checkProperties(JCRNodeWrapper amsterdam, boolean mapped) throws RepositoryException {
        // property existence
        assertTrue(amsterdam.hasProperty("city_id"));
        assertTrue(amsterdam.hasProperty("airport"));
        assertTrue(amsterdam.hasProperty(mapped ? "city_name" : "city_name__en"));
        assertTrue(amsterdam.hasProperty("country_iso_code"));
        assertTrue(amsterdam.hasProperty("language"));
        assertTrue(amsterdam.hasProperty(mapped ? "country" : "country__en"));
        assertTrue(amsterdam.hasProperty("jcr:uuid"));
        assertFalse(amsterdam.hasProperty("jcr:test"));
        assertFalse(amsterdam.hasProperty("city_main_post_code"));

        // property values
        assertEquals("1", amsterdam.getProperty("city_id").getString());
        assertEquals("AMS", amsterdam.getProperty("airport").getString());
        assertEquals("Amsterdam", amsterdam.getProperty(mapped ? "city_name" : "city_name__en").getString());
        assertEquals("NL", amsterdam.getProperty("country_iso_code").getString());
        assertEquals("Dutch", amsterdam.getProperty("language").getString());
        assertEquals("Netherlands", amsterdam.getProperty(mapped ? "country" : "country__en").getString());

        try {
            amsterdam.getProperty("city_main_post_code");
            fail("Property city_main_post_code should not have been found");
        } catch (PathNotFoundException e) {
            // property is not present
        }
    }
    
    public void checkMultipleI18nProperties(JCRNodeWrapper amazonAirline) throws RepositoryException {
    	assertTrue(amazonAirline.hasProperty("maintenance_center"));
    	JCRPropertyWrapper centers = amazonAirline.getProperty("maintenance_center");
    	JCRValueWrapper[] center_names = centers.getValues();
    	assertTrue(center_names.length == 2);
    	for (int i = 0; i < center_names.length; i++) {
    		assertTrue(center_names[i].getString().equals( "Centre Technique de Washington DC") || center_names[i].getString().equals("Centre Technique de Portland"));
    	}
    	
    	// should throw a ValueFormatException
    	amazonAirline.getProperty("maintenance_center").getValue();
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

        assertTrue("Cannot find mounted provider at " + BATCH_CHILDREN_PROVIDER_MOUNTPOINT,
                session.nodeExists(BATCH_CHILDREN_PROVIDER_MOUNTPOINT));
    }

    @After
    public void tearDown() {
        session.logout();
    }

    @Test
    public void testGenericNodes() throws RepositoryException {
        testGenericNodes(GENERIC_PROVIDER_MOUNTPOINT);
    }

    public void testGenericNodes(String mountpoint) throws RepositoryException {
        JCRNodeWrapper root = session.getNode(mountpoint);

        // node existence
        assertTrue(session.nodeExists(mountpoint + "/CITIES"));
        assertNotNull(session.getNode(mountpoint + "/CITIES"));
        assertFalse(session.nodeExists(mountpoint + "/CITIES2"));
        try {
            session.getNode(mountpoint + "/CITIES2");
            fail("Node " + mountpoint + "/CITIES2" + " should not have been found");
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
        assertTrue(session.getNode(mountpoint).isNodeType(GenericDatabaseDataSource.DATA_TYPE_SCHEMA));
        assertTrue(session.getNode(mountpoint + "/CITIES").isNodeType(GenericDatabaseDataSource.DATA_TYPE_TABLE));
        assertTrue(session.getNode(mountpoint + "/CITIES/MQ").isNodeType(GenericDatabaseDataSource.DATA_TYPE_ROW));

        // UUID and path
        JCRNodeWrapper amsterdam = root.getNode("CITIES").getNode("MQ");
        String id = amsterdam.getIdentifier();
        String path = amsterdam.getPath();
        assertEquals(id, session.getNodeByIdentifier(id).getIdentifier());
        assertEquals(id, session.getNode(mountpoint + "/CITIES/MQ").getIdentifier());
        assertEquals(path, session.getNode(mountpoint + "/CITIES/MQ").getPath());
        assertEquals(path, root.getNode("CITIES").getNode("MQ").getPath());
        assertEquals(root.getNode("CITIES").getNode("MQ").getIdentifier(),
                session.getNode(mountpoint + "/CITIES/MQ").getIdentifier());
        assertEquals(root.getNode("CITIES").getNode("MQ"), session.getNode(mountpoint + "/CITIES/MQ"));
    }

    @Test
    public void testGenericProperties() throws RepositoryException {
        checkProperties(session.getNode(GENERIC_PROVIDER_MOUNTPOINT + "/CITIES/MQ"), false);
    }

    @Test
    public void testMappedNodes() throws RepositoryException {
        testMappedNodes(MAPPED_PROVIDER_MOUNTPOINT);
        testMappedNodes(BATCH_CHILDREN_PROVIDER_MOUNTPOINT);
    }

    public void testMappedNodes(String mountpoint) throws RepositoryException {
        JCRNodeWrapper root = session.getNode(mountpoint);

        // node existence
        assertTrue(session.nodeExists(mountpoint + "/CITIES"));
        assertNotNull(session.getNode(mountpoint + "/CITIES"));
        assertFalse(session.nodeExists(mountpoint + "/CITIES2"));
        try {
            session.getNode(mountpoint + "/CITIES2");
            fail("Node " + mountpoint + "/CITIES2" + " should not have been found");
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
        assertTrue(session.getNode(mountpoint).isNodeType(MappedDatabaseDataSource.DATA_TYPE_CATALOG));
        assertTrue(session.getNode(mountpoint + "/CITIES").isNodeType(
                MappedDatabaseDataSource.DATA_TYPE_DIRECTORY));
        assertTrue(session.getNode(mountpoint + "/CITIES/1").isNodeType(
                MappedDatabaseDataSource.DATA_TYPE_CITY));

        // UUID and path
        JCRNodeWrapper amsterdam = root.getNode("CITIES").getNode("1");
        String id = amsterdam.getIdentifier();
        String path = amsterdam.getPath();
        assertEquals(id, session.getNodeByIdentifier(id).getIdentifier());
        assertEquals(id, session.getNode(mountpoint + "/CITIES/1").getIdentifier());
        assertEquals(path, session.getNode(mountpoint + "/CITIES/1").getPath());
        assertEquals(path, root.getNode("CITIES").getNode("1").getPath());
        assertEquals(root.getNode("CITIES").getNode("1").getIdentifier(),
                session.getNode(mountpoint + "/CITIES/1").getIdentifier());
        assertEquals(root.getNode("CITIES").getNode("1"), session.getNode(mountpoint + "/CITIES/1"));
    }

    @Test
    public void testMappedProperties() throws RepositoryException {
        testMappedProperties(MAPPED_PROVIDER_MOUNTPOINT);
        testMappedProperties(BATCH_CHILDREN_PROVIDER_MOUNTPOINT);
    }

    public void testMappedProperties(String mountpoint) throws RepositoryException {
        checkProperties(session.getNode(mountpoint + "/CITIES/1"), true);
    }

    /**
    * QA-6426
    * @throws RepositoryException
    */
    @Test(expected=ValueFormatException.class)
    public void testMultipleI18nMappedProperties() throws RepositoryException {
    	JCRSessionWrapper frenchSession = JCRSessionFactory.getInstance().getCurrentUserSession(Constants.EDIT_WORKSPACE, Locale.FRENCH);
    	JCRNodeWrapper root = frenchSession.getNode(MAPPED_PROVIDER_MOUNTPOINT);
    	JCRNodeWrapper US = root.getNode("AIRLINES").getNode("US");
    	checkMultipleI18nProperties(US);
    }
    
    @Test
    public void testQueryConstraints() throws RepositoryException {
        testQueryConstraints(MAPPED_PROVIDER_MOUNTPOINT);
        testQueryConstraints(BATCH_CHILDREN_PROVIDER_MOUNTPOINT);
    }

    public void testQueryConstraints(String mountpoint) throws RepositoryException {
        assertEquals(1, getResultCount("select * from [" + MappedDatabaseDataSource.DATA_TYPE_CITY
                + "] where [language] = 'Dutch' and isdescendantnode('" + mountpoint + "')"));

        assertEquals(3, getResultCount("select * from [" + MappedDatabaseDataSource.DATA_TYPE_CITY
                + "] where [language] = 'Arabic' and isdescendantnode('" + mountpoint + "')"));
        assertEquals(1, getResultCount("select * from [" + MappedDatabaseDataSource.DATA_TYPE_CITY
                + "] where [language] = 'Arabic' and [city_name] = 'Cairo' and isdescendantnode('" + mountpoint + "')"));
        assertEquals(0, getResultCount("select * from [" + MappedDatabaseDataSource.DATA_TYPE_CITY
                + "] where [language] = 'Arabic' and [city_name] = 'Amstaredam' and isdescendantnode('" + mountpoint + "')"));

        assertEquals(37, getResultCount("select * from [" + MappedDatabaseDataSource.DATA_TYPE_CITY
                + "] where [country_iso_code] = 'US' and isdescendantnode('" + mountpoint + "')"));
        assertEquals(
                17,
                getResultCount("select * from [" + MappedDatabaseDataSource.DATA_TYPE_CITY
                        + "] where [country_iso_code] = 'US' and isdescendantnode('" + mountpoint + "')", 0, 20));
        assertEquals(
                3,
                getResultCount("select * from [" + MappedDatabaseDataSource.DATA_TYPE_CITY
                        + "] where [country_iso_code] = 'US' and isdescendantnode('" + mountpoint + "')", 3, 20));
        assertEquals(
                0,
                getResultCount("select * from [" + MappedDatabaseDataSource.DATA_TYPE_CITY
                        + "] where [country_iso_code] = 'US' and isdescendantnode('" + mountpoint + "')", 3, 100));
    }

    @Test
    public void testQueryPaths() throws RepositoryException {
        testQueryPaths(MAPPED_PROVIDER_MOUNTPOINT);
        testQueryPaths(BATCH_CHILDREN_PROVIDER_MOUNTPOINT);
    }

    public void testQueryPaths(String mountpoint) throws RepositoryException {
        assertEquals(87, getResultCount("select * from [jtestnt:city] where ischildnode('" + mountpoint + "/CITIES')"));
        assertEquals(0, getResultCount("select * from [jtestnt:city] where ischildnode('" + mountpoint + "/AIRLINES')"));
        assertEquals(0, getResultCount("select * from [jtestnt:city] where ischildnode('" + mountpoint + "')"));
        assertEquals(0, getResultCount("select * from [jtestnt:city] where ischildnode('/sites/systemsite')"));
        assertEquals(87, getResultCount("select * from [jtestnt:city] where isdescendantnode('" + mountpoint + "/CITIES')"));
        assertEquals(0, getResultCount("select * from [jtestnt:city] where isdescendantnode('" + mountpoint + "/AIRLINES')"));
        assertEquals(87, getResultCount("select * from [jtestnt:city] where isdescendantnode('" + mountpoint + "/')"));
        assertEquals(0, getResultCount("select * from [jtestnt:city] where isdescendantnode('/sites/systemsite')"));
    }

    @Test
    public void testQueryLimitAndOffset() throws RepositoryException {
        testQueryLimitAndOffset(MAPPED_PROVIDER_MOUNTPOINT);
        testQueryLimitAndOffset(BATCH_CHILDREN_PROVIDER_MOUNTPOINT);
    }

    public void testQueryLimitAndOffset(String mountpoint) throws RepositoryException {
        String queryDirs = "select * from [" + MappedDatabaseDataSource.DATA_TYPE_DIRECTORY + "] where isdescendantnode('" + mountpoint + "')";

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
    public void testQueryLimitAndOffsetMultipleProviders() throws RepositoryException {
        String query = "select * from [nt:base]";
        long total = getResultCount(query, 0, 0);

        assertEquals(200, getResultCount(query, 0, total - 200));
    }

    @Test
    public void testQueryNodeType() throws RepositoryException {
        testQueryNodeType(MAPPED_PROVIDER_MOUNTPOINT);
        testQueryNodeType(BATCH_CHILDREN_PROVIDER_MOUNTPOINT);
    }

    public void testQueryNodeType(String mountpoint) throws RepositoryException {
        // count
        assertEquals(4, getResultCount("select * from [" + MappedDatabaseDataSource.DATA_TYPE_DIRECTORY + "] where isdescendantnode('" + mountpoint + "')"));
        assertEquals(2, getResultCount("select * from [" + MappedDatabaseDataSource.DATA_TYPE_AIRLINE + "] where isdescendantnode('" + mountpoint + "')"));
        assertEquals(0, getResultCount("select * from [" + GenericDatabaseDataSource.DATA_TYPE_TABLE + "] where isdescendantnode('" + mountpoint + "')"));

        for (NodeIterator ni = query("select * from [" + MappedDatabaseDataSource.DATA_TYPE_AIRLINE + "] where isdescendantnode('" + mountpoint + "')", 0, 0)
                .getNodes(); ni.hasNext();) {
            assertTrue(ni.nextNode().isNodeType(MappedDatabaseDataSource.DATA_TYPE_AIRLINE));
        }
    }

    @Test
    public void testExtensionProperty() throws RepositoryException {
        testExtensionProperty(MAPPED_PROVIDER_MOUNTPOINT);
        testExtensionProperty(BATCH_CHILDREN_PROVIDER_MOUNTPOINT);
    }

    public void testExtensionProperty(String mountpoint) throws RepositoryException {
        JCRNodeWrapper root = session.getNode(mountpoint);
        JCRNodeWrapper AA = root.getNode("AIRLINES").getNode("AA");
        AA.setProperty("firstclass_seats", 10);
        assertEquals("Property not updated", 10, AA.getProperty("firstclass_seats").getLong());
        boolean threwException = false;
        try {
            AA.setProperty("business_seats", 50);
        } catch (UnsupportedRepositoryOperationException e) {
            threwException = true;
        }
        assertTrue("Setting a non-overridable property shouldn't be possible", threwException);
    }

    @Test
    public void testMixin() throws RepositoryException {
        testMixin(MAPPED_PROVIDER_MOUNTPOINT);
        testMixin(BATCH_CHILDREN_PROVIDER_MOUNTPOINT);
    }

    public void testMixin(String mountpoint) throws RepositoryException {
        JCRNodeWrapper root = session.getNode(mountpoint);
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
    public void testAddNode() throws RepositoryException {
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
        session.save();

        AA.removeMixin("jmix:comments");
        session.save();
    }

    @Test
    public void testSearchOnExtension() throws RepositoryException {
        testSearchOnExtension(MAPPED_PROVIDER_MOUNTPOINT);
        testSearchOnExtension(BATCH_CHILDREN_PROVIDER_MOUNTPOINT);
    }

    public void testSearchOnExtension(String mountpoint) throws RepositoryException {
        JCRNodeWrapper root = session.getNode(mountpoint);
        JCRNodeWrapper AA = root.getNode("AIRLINES").getNode("AA");
        AA.addMixin("jmix:comments");
        AA.setProperty("shortView",true);
        session.save();

        assertEquals(1, getResultCount("select * from [jmix:comments] as c where isdescendantnode(c, '" + mountpoint + "')", 0, 0));

        AA.removeMixin("jmix:comments");
        session.save();
    }

    @Test
    public void testI18nAndLazyProperties() throws RepositoryException {
        JCRNodeWrapper city = session.getNode(MAPPED_PROVIDER_MOUNTPOINT + "/CITIES/16");
        assertTrue(city.hasI18N(Locale.ENGLISH));
        assertTrue(city.hasI18N(Locale.FRENCH));
        assertFalse(city.hasI18N(Locale.GERMAN));
        assertTrue(city.hasProperty("country"));
        assertTrue(city.hasProperty("city_name"));
        assertFalse(city.hasProperty("city_name_en"));
        assertTrue(city.hasProperty("airport"));
        assertEquals("GVA", city.getPropertyAsString("airport"));
        assertEquals("Switzerland", city.getPropertyAsString("country"));
        assertEquals("Geneva", city.getPropertyAsString("city_name"));

        JCRSessionWrapper frenchSession = JCRSessionFactory.getInstance().getCurrentUserSession(Constants.EDIT_WORKSPACE, Locale.FRENCH);
        JCRNodeWrapper frenchCity = frenchSession.getNode(MAPPED_PROVIDER_MOUNTPOINT + "/CITIES/16");
        assertEquals("Suisse", frenchCity.getProperty("country").getString());
        assertEquals("Genève", frenchCity.getPropertyAsString("city_name"));
        frenchSession.logout();
    }

    @Test
    public void testLock() throws RepositoryException {

        JCRNodeWrapper city = session.getNode(MAPPED_PROVIDER_MOUNTPOINT + "/CITIES/16");


        if (city.isNodeType("jmix:lockable")) {
            boolean result = city.lockAndStoreToken("user");
            session.save();

            assertTrue(city + " : lockAndStoreToken returned false",
                    result);

            Lock lock = city.getLock();
            assertNotNull(city + " : lock is null", lock);

            try {
                city.unlock();
            } catch (LockException e) {
                fail(city + " : unlock failed");
            }
        }

        Lock lock = city.lock(false,false);
        assertNotNull(city.getPath() + " : Lock is null", lock);
        assertTrue(city.getPath() + " : Node not locked", city.isLocked());
        city.unlock();
        assertFalse(city.getPath() + " : Node not unlocked", city.isLocked());
    }

    @Test
    public void testWritableAddNode() throws RepositoryException {
        final JCRNodeWrapper parent = session.getNode("/external-writeable-database-mapped/AIRLINES");
        JCRNodeWrapper n = parent.addNode("TS", "jtestnt:airline");
//        JCRNodeWrapper n = session.getNode("/external-writeable-database-mapped/AIRLINES/AT")
        n.setProperty("airline","TS");
        n.setProperty("airline_full","air transat");
        n.setProperty("basic_rate","0.15");
        n.setProperty("distance_discount","0.01");

        assertEquals(3,parent.getNodes().getSize());
        assertEquals(1,parent.getNodes("TS*").getSize());
        JCRNodeWrapper n2 = parent.getNode("TS");
        assertTrue(n.equals(n2));

        session.save();

        assertEquals(3,parent.getNodes().getSize());
        assertEquals(1,parent.getNodes("TS*").getSize());
        n2 = parent.getNode("TS");
        assertTrue(n.equals(n2));

        n.setProperty("basic_rate","0.20");
        session.save();

        n.remove();
        assertEquals(2,parent.getNodes().getSize());
        assertEquals(0,parent.getNodes("TS*").getSize());

        try {
            parent.getNode("TS");
            fail("node still exists");
        } catch (PathNotFoundException e) {
            // ok
        }

        session.save();
        assertEquals(2,parent.getNodes().getSize());
        assertEquals(0,parent.getNodes("TS").getSize());
        try {
            parent.getNode("TS");
            fail("node still exists");
        } catch (PathNotFoundException e) {
            // ok
        }

    }

    @Test
    public void testImportExport() throws Exception {
        try {
            JahiaSite providerExportTest = TestHelper.createSite(TESTSITE_NAME);
            JCRNodeWrapper root = session.getNode(MAPPED_PROVIDER_MOUNTPOINT);
            JCRNodeWrapper airlines = root.getNode("AIRLINES");
            JCRNodeWrapper aa = airlines.getNode("AA");
            JCRNodeWrapper us = airlines.getNode("US");
            aa.setProperty("firstclass_seats", 10);
            JCRNodeWrapper site = session.getNode(providerExportTest.getJCRLocalPath());
            JCRNodeWrapper aaReference = site.addNode("aaReference", "jnt:contentReference");
            aaReference.setProperty("j:node", aa);
            JCRNodeWrapper usReference = site.addNode("usReference", "jnt:contentReference");
            usReference.setProperty("j:node", us);
            session.save();

            File zip = ImportExportTest.exportSite(TESTSITE_NAME);
            cleanExtension();

            TestHelper.createSite(TESTSITE_NAME, "localhost", TestHelper.WEB_TEMPLATES, zip.getAbsolutePath(), TESTSITE_NAME + ".zip");
            session.save();

            JCRNodeWrapper AA = session.getNode(MAPPED_PROVIDER_MOUNTPOINT).getNode("AIRLINES").getNode("AA");
            assertEquals("Property not imported", 10, AA.getProperty("firstclass_seats").getLong());
        } finally {
            try {
                cleanExtension();
                TestHelper.deleteSite(TESTSITE_NAME);
                session.save();
            } catch (Exception e) {}
        }
    }

    private void cleanExtension() throws Exception {
        Session jcrSession = session.getNode("/").getRealNode().getSession();
        if (jcrSession.nodeExists(MAPPED_PROVIDER_MOUNTPOINT)) {
            jcrSession.getNode(MAPPED_PROVIDER_MOUNTPOINT).remove();
        }
        Node rootNode = jcrSession.getNode("/");
        if (rootNode.isNodeType("jmix:hasExternalProviderExtension")) {
            rootNode.removeMixin("jmix:hasExternalProviderExtension");
        }
        jcrSession.save();
    }

}
