/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.external.test.vfs;

import org.apache.commons.io.FileUtils;
import org.jahia.ajax.gwt.client.data.definition.GWTJahiaNodeProperty;
import org.jahia.ajax.gwt.client.data.node.GWTJahiaNode;
import org.jahia.ajax.gwt.client.service.GWTJahiaServiceException;
import org.jahia.ajax.gwt.helper.NavigationHelper;
import org.jahia.api.Constants;
import org.jahia.modules.external.vfs.factory.VFSMountPointFactory;
import org.jahia.modules.external.vfs.factory.VFSMountPointFactoryHandler;
import org.jahia.services.SpringContextSingleton;
import org.jahia.services.content.*;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.sites.JahiaSite;
import org.jahia.services.usermanager.JahiaUser;
import org.jahia.test.JahiaAdminUser;
import org.jahia.test.TestHelper;
import org.jahia.utils.LanguageCodeConverters;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;

import javax.jcr.*;
import javax.jcr.nodetype.NodeType;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Unit test for VFS content store provider.
 *
 * @author loom
 * Date: Aug 20, 2010
 * Time: 3:29:57 PM
 */
public class VFSContentStoreProviderTest {
    private static Logger logger = org.slf4j.LoggerFactory.getLogger(VFSContentStoreProviderTest.class);
    private static final String TESTSITE_NAME = "vfsContentProviderTest";
    private static final String SITECONTENT_ROOT_NODE = "/sites/" + TESTSITE_NAME;
    private static File dynamicMountDir;
    private static File staticMountDir;
    private static final String STATIC_MOUNT_FILE_NAME = "staticMountDirectory";
    private static final String DYNAMIC_MOUNT_FILE_NAME = "dynamicMountDirectory";
    private static final String MOUNTS_DYNAMIC_MOUNT_POINT = "/mounts/dynamic-mount";
    private static final String MOUNTS_DYNAMIC_MOUNT_POINT_TARGET = "/mounts/dynamic";
    private static final String MOUNTS_DYNAMIC_MOUNT_POINT_NAME = "dynamic";

    private static final String SIMPLE_WEAKREFERENCE_PROPERTY_NAME = "test:simpleNode";
    private static final String MULTIPLE_WEAKREFERENCE_PROPERTY_NAME = "test:multipleNode";
    private static final String MULTIPLE_I18N_WEAKREFERENCE_PROPERTY_NAME = "test:multipleI18NNode";
    private static final String TEST_EXTERNAL_WEAKREFERENCE_NODE_TYPE = "test:externalWeakReference";

    private static final String DELETION_MESSAGE = "Deleted in unit test";

    private static JahiaSite site;

    JCRSessionWrapper englishEditSession;
    JCRSessionWrapper frenchEditSession;
    VFSMountPointFactoryHandler vfsMountPointFactoryHandler = new VFSMountPointFactoryHandler();

    private void getCleanSession() throws Exception {
        String defaultLanguage = site.getDefaultLanguage();
        JCRSessionFactory sessionFactory = JCRSessionFactory.getInstance();
        sessionFactory.closeAllSessions();
        englishEditSession = sessionFactory.getCurrentUserSession(Constants.EDIT_WORKSPACE, Locale.ENGLISH,
                LanguageCodeConverters.languageCodeToLocale(defaultLanguage));
        frenchEditSession = sessionFactory.getCurrentUserSession(Constants.EDIT_WORKSPACE, Locale.FRENCH,
                LanguageCodeConverters.languageCodeToLocale(defaultLanguage));
    }

    @BeforeClass
    public static void oneTimeSetUp()
            throws Exception {
        site = TestHelper.createSite(TESTSITE_NAME);

        File sysTempDir = new File(System.getProperty("java.io.tmpdir"));

        staticMountDir = new File(sysTempDir, STATIC_MOUNT_FILE_NAME);
        if (!staticMountDir.exists()) {
            staticMountDir.mkdir();
        }

        dynamicMountDir = new File(sysTempDir, DYNAMIC_MOUNT_FILE_NAME);
        if (!dynamicMountDir.exists()) {
            dynamicMountDir.mkdir();
        }
        JahiaUser jahiaRootUser = JahiaAdminUser.getAdminUser(null);
        unMountDynamicMountPoint();
        removeDynamicMountPoint(jahiaRootUser);
    }

    @AfterClass
    public static void oneTimeTearDown() throws Exception {
        JahiaUser jahiaRootUser = JahiaAdminUser.getAdminUser(null);
        unMountDynamicMountPoint();
        removeDynamicMountPoint(jahiaRootUser);
        TestHelper.deleteSite(TESTSITE_NAME);
        try {
            FileUtils.deleteDirectory(dynamicMountDir);
            FileUtils.deleteDirectory(staticMountDir);
        } catch (Exception ex) {
            logger.warn("Exception during test tearDown", ex);
        }
    }

    private void assertRootNavigation(JCRSessionWrapper session) throws RepositoryException, GWTJahiaServiceException {
        JCRSiteNode siteNode = (JCRSiteNode) session.getNode(SITECONTENT_ROOT_NODE);
        NavigationHelper navigationHelper = (NavigationHelper) SpringContextSingleton.getInstance().getContext().getBean("NavigationHelper");
        Locale locale = LanguageCodeConverters.languageCodeToLocale("en");
        List<String> paths = new ArrayList<String>();
        paths.add("/mounts");
        List<GWTJahiaNode> rootNodes = navigationHelper.retrieveRoot(paths, null,null,null,null,
                null,null,siteNode, session, locale);
        List<String> nodeTypes = new ArrayList<String>();
        nodeTypes.add("nt:file");
        nodeTypes.add("nt:folder");
        nodeTypes.add("jnt:mounts");
        List<String> fields = new ArrayList<String>();
        fields.add("providerKey");
        fields.add("icon");
        fields.add("name");
        fields.add("locked");
        fields.add("size");
        fields.add("jcr:lastModified");
        for (GWTJahiaNode rootNode : rootNodes) {
            assertGWTJahiaNode(rootNode, "/mounts");
            List<GWTJahiaNode> childNodes = navigationHelper.ls(rootNode.getPath(), nodeTypes, new ArrayList<String>(), new ArrayList<String>(), fields, session, Locale.getDefault());
            for (GWTJahiaNode childNode : childNodes) {
                assertGWTJahiaNode(childNode, "/mounts/" + childNode.getName());
                List<GWTJahiaNode> childChildNodes = navigationHelper.ls(childNode.getPath(), nodeTypes, new ArrayList<String>(), new ArrayList<String>(), fields, session, Locale.getDefault());
            }
        }
    }

    private void assertGWTJahiaNode(GWTJahiaNode jahiaGWTNode, String expectedPath) {
        assertEquals("Expected path and actual GWT node path are not equal !", expectedPath, jahiaGWTNode.getPath());
        int lastSlashPosInPath = jahiaGWTNode.getPath().lastIndexOf("/");
        if (lastSlashPosInPath > -1)
            assertEquals("Last part of path and name are not equal !", jahiaGWTNode.getPath().substring(lastSlashPosInPath + 1), jahiaGWTNode.getName());
        else {
            assertEquals("Last part of path and name are not equal !", jahiaGWTNode.getPath(), jahiaGWTNode.getName());
        }
    }

    @Test
    public void testDynamicMount() throws Exception, GWTJahiaServiceException, RepositoryException {
        JahiaUser jahiaRootUser = JahiaAdminUser.getAdminUser(null);
        boolean mountNodeStillExists = true;
        try {
            JCRSessionWrapper session = JCRSessionFactory.getInstance().getCurrentUserSession();
            VFSMountPointFactory vfsMountPointFactory = new VFSMountPointFactory();
            vfsMountPointFactory.setName(MOUNTS_DYNAMIC_MOUNT_POINT_NAME);
            vfsMountPointFactory.setRoot("file://" + dynamicMountDir.getAbsolutePath());
            vfsMountPointFactoryHandler.save(vfsMountPointFactory);
            assertRootNavigation(session);

            JCRNodeWrapper targetMountNode = getNode(session, MOUNTS_DYNAMIC_MOUNT_POINT_TARGET);
            assertNode(targetMountNode, 0);
            createFolder(session, "folder1", targetMountNode);
            JCRNodeWrapper folder1Node = getNode(session, MOUNTS_DYNAMIC_MOUNT_POINT_TARGET + "/folder1");
            assertNode(folder1Node, 0);
            
            assertTrue("Node schould exist using session.nodeExists", session.nodeExists(MOUNTS_DYNAMIC_MOUNT_POINT_TARGET + "/folder1"));
            assertTrue("Node schould exist using node.hasNode(simpleName)", session.getNode(MOUNTS_DYNAMIC_MOUNT_POINT_TARGET).hasNode("folder1"));
            assertTrue("Node schould exist using parent.hasNode(relativePath)", session.getNode(targetMountNode.getParent().getPath()).hasNode(targetMountNode.getName() + "/folder1"));
            
            session.checkout(folder1Node);
            folder1Node.remove();
            session.save();
            
            // get mount point node from parent
            JCRNodeWrapper sameMountNode = targetMountNode.getParent().getNode(targetMountNode.getName());
            assertEquals("Node objects are not the same", targetMountNode, sameMountNode);
            assertEquals("Node wrappers/decorators are not of the same class", targetMountNode.getClass(),
                    sameMountNode.getClass());
            assertEquals("Real nodes are not the same", targetMountNode.getRealNode(), sameMountNode.getRealNode());

            unMountDynamicMountPoint();
            removeDynamicMountPoint(jahiaRootUser);

            // we must recycle session because of internal session caches.
            getCleanSession();

            session = JCRSessionFactory.getInstance().getCurrentUserSession();

            mountNodeStillExists = false;
            try {
                session.getNode(MOUNTS_DYNAMIC_MOUNT_POINT_TARGET);
                mountNodeStillExists = true;
            } catch (PathNotFoundException pnfe) {
            }
            assertFalse("Dynamic mount node should have been removed but is still present in repository !", mountNodeStillExists);
        } finally {
            if (mountNodeStillExists) {
                unMountDynamicMountPoint();
            }
        }
    }

    private static void unMountDynamicMountPoint() throws RepositoryException {
        // now let's unmount.
        JCRStoreProvider provider = JCRStoreService.getInstance().getSessionFactory().getProvider(MOUNTS_DYNAMIC_MOUNT_POINT_TARGET, false);
        if (provider != null) {
            provider.stop();
        }
    }

    private static void removeDynamicMountPoint(JahiaUser jahiaRootUser) throws RepositoryException {
        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(jahiaRootUser, null, null, new JCRCallback<Object>() {
            public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                JCRNodeWrapper mountNode;
                JCRNodeWrapper targetMountNode = null;
                try {
                    mountNode = session.getNode(MOUNTS_DYNAMIC_MOUNT_POINT);
                    mountNode.remove();
                    session.save();
                } catch (PathNotFoundException pnfe) {
                }

                mountNode = null;
                try {
                    mountNode = session.getNode(MOUNTS_DYNAMIC_MOUNT_POINT);
                } catch(PathNotFoundException e){}
                try {
                    targetMountNode = session.getNode(MOUNTS_DYNAMIC_MOUNT_POINT_TARGET);
                } catch(PathNotFoundException e){}

                if(mountNode != null) {
                    fail("Failed to remove mountnode: " + mountNode.getPath());
                }

                if(targetMountNode != null) {
                    fail("Failed to remove target mountnode: " + targetMountNode.getPath());
                }

                return null;
            }
        });
    }

    @Test
    public void testReferencing() throws Exception, RepositoryException, UnsupportedEncodingException {
        JahiaUser jahiaRootUser = JahiaAdminUser.getAdminUser(null);
        try {

            JCRSessionWrapper session = JCRSessionFactory.getInstance().getCurrentUserSession();
            VFSMountPointFactory vfsMountPointFactory = new VFSMountPointFactory();
            vfsMountPointFactory.setName(MOUNTS_DYNAMIC_MOUNT_POINT_NAME);
            vfsMountPointFactory.setRoot("file://" + dynamicMountDir.getAbsolutePath());
            vfsMountPointFactoryHandler.save(vfsMountPointFactory);

            JCRNodeWrapper mountNode = getNode(session, MOUNTS_DYNAMIC_MOUNT_POINT_TARGET);
            assertNode(mountNode, 0);

            String value = "This is a test";
            String mimeType = "text/plain";

            InputStream is = new ByteArrayInputStream(value.getBytes("UTF-8"));

            String name1 = "test1_" + System.currentTimeMillis() + ".txt";
            JCRNodeWrapper vfsTestFile1 = mountNode.uploadFile(name1, is, mimeType);

            is = new ByteArrayInputStream(value.getBytes("UTF-8"));

            String name2 = "test2_" + System.currentTimeMillis() + ".txt";
            JCRNodeWrapper vfsTestFile2 = mountNode.uploadFile(name2, is, mimeType);

            session.save();

            JCRSiteNode siteNode = (JCRSiteNode) session.getNode(SITECONTENT_ROOT_NODE);

            // simple external referencing testing, with no language specified...

            JCRNodeWrapper fileReferenceNode = siteNode.addNode("externalReferenceNode", "jnt:fileReference");
            fileReferenceNode.setProperty(Constants.NODE, vfsTestFile1);
            session.save();

            Property externalReferenceProperty = fileReferenceNode.getProperty(Constants.NODE);
            Node externalNode = externalReferenceProperty.getNode();
            assertEquals("External node identifier retrieved from reference do not match", vfsTestFile1.getIdentifier(),
                    externalNode.getIdentifier());
            PropertyIterator weakReferenceProperties = vfsTestFile1.getWeakReferences(Constants.NODE);
            boolean foundWeakReferenceProperty = false;
            while (weakReferenceProperties.hasNext()) {
                Property property = weakReferenceProperties.nextProperty();
                if (property.getName().equals(Constants.NODE) && property.getParent().getIdentifier().equals(fileReferenceNode.getIdentifier())) {
                    foundWeakReferenceProperty = true;
                    break;
                }
            }
            assertTrue("Expected to find weak reference property j:node but it wasn't found !", foundWeakReferenceProperty);
            assertTrue("Expected to find j:node property when testing for it's presence but it wasn't found.",
                    fileReferenceNode.hasProperty(Constants.NODE));

            // Now let's test accessing using property iterator

            boolean foundReferenceProperty = false;
            PropertyIterator fileReferenceProperties = fileReferenceNode.getProperties();
            while (fileReferenceProperties.hasNext()) {
                Property property = fileReferenceProperties.nextProperty();
                if (property.getName().equals(Constants.NODE)) {
                    foundReferenceProperty = true;
                    break;
                }
            }
            assertTrue("Couldn't find property j:node using property iterators", foundReferenceProperty);

            fileReferenceProperties = fileReferenceNode.getProperties("j:nod* | j:*ode");
            while (fileReferenceProperties.hasNext()) {
                Property property = fileReferenceProperties.nextProperty();
                if (property.getName().equals(Constants.NODE)) {
                    foundReferenceProperty = true;
                    break;
                }
            }
            assertTrue("Couldn't find property j:node using property iterators and name patterns", foundReferenceProperty);

            // as our own property iterators also support the Map interface, we will test that now.
            Map fileReferencePropertiesMap = (Map) fileReferenceNode.getProperties("j:nod* | j:*ode");
            assertTrue("Properties used as a map do not have the reference property j:node",
                    fileReferencePropertiesMap.containsKey(Constants.NODE));
            Value refValue = (Value) fileReferencePropertiesMap.get(Constants.NODE);
            assertTrue("Reference property could not be found in properties used as a map", refValue != null);
            assertEquals("Reference property retrieved from properties used as a map does not contain proper reference",
                    vfsTestFile1.getIdentifier(), refValue.getString());

            // TODO add tests where we mix internal references AND external references in the same multi-valued property in different
            // languages.
            getCleanSession();
            siteNode = (JCRSiteNode) englishEditSession.getNode(SITECONTENT_ROOT_NODE);
            vfsTestFile1 = englishEditSession.getNode(MOUNTS_DYNAMIC_MOUNT_POINT_TARGET + "/" + name1);
            vfsTestFile2 = englishEditSession.getNode(MOUNTS_DYNAMIC_MOUNT_POINT_TARGET + "/" + name2);

            JCRNodeWrapper mixedFileReferenceNode = siteNode.addNode("externalMixedReferenceNode", TEST_EXTERNAL_WEAKREFERENCE_NODE_TYPE);
            mixedFileReferenceNode.setProperty(SIMPLE_WEAKREFERENCE_PROPERTY_NAME, vfsTestFile1);
            ValueFactory valueFactory = englishEditSession.getValueFactory();

            List<Value> values = new ArrayList<Value>();
            values.add(session.getValueFactory().createValue(vfsTestFile2,true));

            is = new ByteArrayInputStream(value.getBytes("UTF-8"));

            JCRNodeWrapper siteFile1 = siteNode.uploadFile(name1, is, mimeType);
            values.add(valueFactory.createValue(siteFile1));

            Value[] multipleWeakRefs = values.toArray(new Value[values.size()]);

            mixedFileReferenceNode.setProperty(MULTIPLE_WEAKREFERENCE_PROPERTY_NAME, multipleWeakRefs);
            englishEditSession.save();

            // let's get another session to make sure we don't have cache issues
            getCleanSession();

            mixedFileReferenceNode = englishEditSession.getNode(SITECONTENT_ROOT_NODE + "/externalMixedReferenceNode");

            assertTrue("Couldn't find property when testing for it's presence with the hasProperty method",
                    mixedFileReferenceNode.hasProperty(SIMPLE_WEAKREFERENCE_PROPERTY_NAME));
            Property simpleRefProperty = mixedFileReferenceNode.getProperty(SIMPLE_WEAKREFERENCE_PROPERTY_NAME);
            assertTrue("Reference property does not have proper value",
                    simpleRefProperty.getNode().getIdentifier().equals(vfsTestFile1.getIdentifier()));

            Property multipleRefProperty = mixedFileReferenceNode.getProperty(MULTIPLE_WEAKREFERENCE_PROPERTY_NAME);
            assertTrue("Expected multiple property but it is not multi-valued", multipleRefProperty.isMultiple());
            Value[] multipleRefPropertyValues = multipleRefProperty.getValues();
            assertTrue("First property value type is not correct", multipleRefPropertyValues[0].getType() == PropertyType.WEAKREFERENCE);
            assertTrue("First property value does not match VFS test file 2",
                    multipleRefPropertyValues[0].getString().equals(vfsTestFile2.getIdentifier()));
            assertTrue("Second property value type is not correct", multipleRefPropertyValues[1].getType() == PropertyType.WEAKREFERENCE);
            assertTrue("Second property value does not match site test file 1",
                    multipleRefPropertyValues[1].getString().equals(siteFile1.getIdentifier()));

            // TODO we will have to set the last property in multiple languages. We will need multiple
            // session objects for this.

            // TODO add tests for reference removal, making sure we don't have dangling references.

            // TODO add tests for handling missing reference targets.
        } finally {
            unMountDynamicMountPoint();
            removeDynamicMountPoint(jahiaRootUser);
        }
    }

    @Test
    public void testMarkForDeletion() throws Exception, RepositoryException, UnsupportedEncodingException {
        JahiaUser jahiaRootUser = JahiaAdminUser.getAdminUser(null);
        try {
            JCRSessionWrapper session = JCRSessionFactory.getInstance().getCurrentUserSession();
            VFSMountPointFactory vfsMountPointFactory = new VFSMountPointFactory();
            vfsMountPointFactory.setName(MOUNTS_DYNAMIC_MOUNT_POINT_NAME);
            vfsMountPointFactory.setRoot("file://" + dynamicMountDir.getAbsolutePath());
            vfsMountPointFactoryHandler.save(vfsMountPointFactory);

            JCRNodeWrapper mountNode = getNode(session, MOUNTS_DYNAMIC_MOUNT_POINT_TARGET);
            assertNode(mountNode, 0);

            String value = "This is a test";
            String mimeType = "text/plain";

            InputStream is = new ByteArrayInputStream(value.getBytes("UTF-8"));

            String name1 = "test1_" + System.currentTimeMillis() + ".txt";
            JCRNodeWrapper vfsTestFile1 = mountNode.uploadFile(name1, is, mimeType);
            assertNotNull(vfsTestFile1);

            is = new ByteArrayInputStream(value.getBytes("UTF-8"));

            String name2 = "test2_" + System.currentTimeMillis() + ".txt";
            JCRNodeWrapper vfsTestFile2 = mountNode.uploadFile(name2, is, mimeType);
            assertNotNull(vfsTestFile2);

            session.save();

            getCleanSession();

            JCRSiteNode siteNode = (JCRSiteNode) englishEditSession.getNode(SITECONTENT_ROOT_NODE);
            assertNotNull(siteNode);
            vfsTestFile1 = getNode(englishEditSession, MOUNTS_DYNAMIC_MOUNT_POINT_TARGET + "/" + name1);
            assertFalse("Node should not allow mark for deletion", vfsTestFile1.canMarkForDeletion());

            boolean unsupportedRepositoryOperation = false;
            try {
                vfsTestFile1.markForDeletion(DELETION_MESSAGE);
            } catch (UnsupportedRepositoryOperationException uroe) {
                unsupportedRepositoryOperation = true;
            }
            assertTrue("Mark for deletion should not be allowed", unsupportedRepositoryOperation);
            englishEditSession.save();

            assertFalse("jmix:markedForDeletionRoot set", vfsTestFile1.isNodeType(Constants.JAHIAMIX_MARKED_FOR_DELETION_ROOT));
            assertFalse("jmix:markedForDeletion set", vfsTestFile1.isNodeType(Constants.JAHIAMIX_MARKED_FOR_DELETION));
            assertFalse("marked for deletion comment not set",
                    DELETION_MESSAGE.equals(vfsTestFile1.getPropertyAsString(Constants.MARKED_FOR_DELETION_MESSAGE)));
            assertFalse("j:deletionUser not set", vfsTestFile1.hasProperty(Constants.MARKED_FOR_DELETION_USER));
            assertFalse("j:deletionDate not set", vfsTestFile1.hasProperty(Constants.MARKED_FOR_DELETION_DATE));

            unsupportedRepositoryOperation = false;
            try {
                vfsTestFile1.unmarkForDeletion();
            } catch (UnsupportedRepositoryOperationException uroe) {
                unsupportedRepositoryOperation = true;
            }
            assertTrue("Unmark for deletion should not be allowed", unsupportedRepositoryOperation);
        } finally {
            unMountDynamicMountPoint();
            removeDynamicMountPoint(jahiaRootUser);
        }
    }

    private JCRNodeWrapper getNode(JCRSessionWrapper session, String path) throws RepositoryException {
        try {
            JCRNodeWrapper node = session.getNode(path);
            return node;
        } catch (PathNotFoundException pnfe) {
            logger.error("Mount point not available", pnfe);
            assertTrue("Node at " + path + " could not be found !", false);
        }
        return null;
    }

    private void assertNode(Node node, int depth)
            throws RepositoryException {
        NodeType primaryNodeType = node.getPrimaryNodeType();
        assertNotNull("Primary node type is null !", primaryNodeType);
        String nodeIdentifier = node.getIdentifier();
        assertNotNull("Node identifier is null!", nodeIdentifier);
        NodeType[] nodeMixinNodeTypes = node.getMixinNodeTypes();
        assertNotNull("Mixin types are null !", nodeMixinNodeTypes);
        assertNotNull("Node path is null!", node.getPath());
        assertNotNull("Node name is null!", node.getName());
        int lastSlashPosInPath = node.getPath().lastIndexOf("/");
        if (lastSlashPosInPath > -1)
            assertEquals("Last part of path and name are not equal !", node.getPath().substring(lastSlashPosInPath + 1), node.getName());
        else {
            assertEquals("Last part of path and name are not equal !", node.getPath(), node.getName());
        }

        PropertyIterator propertyIterator = node.getProperties();
        assertNotNull("Property iterator is null !", propertyIterator);
        while (propertyIterator.hasNext()) {
            Property property = propertyIterator.nextProperty();
            property.isMultiple();
            property.isNew();
            property.isNode();
        }
        NodeIterator nodeIterator = node.getNodes();
        assertNotNull("Child node iterator is null !", nodeIterator);
        while (nodeIterator.hasNext()) {
            Node childNode = nodeIterator.nextNode();
            assertNode(childNode, depth + 1);
        }
    }

    private void createFolder(JCRSessionWrapper session, String name, Node node) throws RepositoryException {
        session.checkout(node);
        node.addNode(name, "jnt:folder");
        session.save();
    }
}
