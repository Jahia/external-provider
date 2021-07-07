/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2019 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.modules.external.test.configuration;

import junit.framework.AssertionFailedError;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.test.JahiaTestCase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import java.util.Dictionary;
import java.util.Hashtable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test mount point creation from configuration file
 */

public class ConfigurationTest extends JahiaTestCase {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationTest.class);

    private static final String MOUNTS_TEST_RENAMED_MOUNT = "/mounts/test-renamed-mount";
    private static final String CONFIGURATION_MOUNT_J_NODENAME = "mount.j_nodename";
    private static final String CONFIGURATION_MOUNT_J_ROOT_PATH = "mount.j_rootPath";
    private static final String CONFIGURATION_MOUNT_JCR_PRIMARY_TYPE = "mount.jcr_primaryType";
    private static final String SERVICE_PID = "service.pid";
    private static final String J_NODENAME_PROPERTY = "j:nodename";
    private static final String JNT_VFS_MOUNT_POINT_TYPE = "jnt:vfsMountPoint";
    private static final String J_ROOT_PATH_VALUE = "/Users/Username/Desktop";
    private static final String TEST_MOUNT_PATH = "/mounts/test-mount";
    private Dictionary<String, Object> properties;
    private static final String PID = "eaa8dfb2-3158-11ea-978f-2e728ce88125";
    private JCRSessionWrapper session;

    private ManagedServiceFactory mountPointConfigFactory;

    @Before public void setUp() {
        mountPointConfigFactory = BundleUtils.getOsgiService(ManagedServiceFactory.class, "(service.pid=org.jahia.modules.external.mount)");
        JCRSessionFactory.getInstance().closeAllSessions();
        properties = new Hashtable();
    }

    @After public void cleanRemainingMountPoints(){
        JCRSessionFactory.getInstance().closeAllSessions();
        try {
            session = JCRSessionFactory.getInstance().getCurrentUserSession();
            JCRNodeWrapper mountPoint =  session.getNode(TEST_MOUNT_PATH);
            try{
                deleteNode(mountPoint.getIdentifier());
            }catch(RepositoryException e){
                Assert.fail(String.format("Failed to delete the node %s", TEST_MOUNT_PATH));
            }
        } catch (PathNotFoundException e) {
            logger.info(String.format("Node %s correctly deleted", TEST_MOUNT_PATH));
        } catch (RepositoryException e) {
            logger.error(String.format("Failed to delete the node %s", TEST_MOUNT_PATH),e);
        }

        JCRSessionFactory.getInstance().closeAllSessions();
        try {
            session = JCRSessionFactory.getInstance().getCurrentUserSession();
            JCRNodeWrapper mountPoint =  session.getNode(MOUNTS_TEST_RENAMED_MOUNT);
            try{
                deleteNode(mountPoint.getIdentifier());
            }catch(RepositoryException e){
                Assert.fail(String.format("Failed to delete the node %s", MOUNTS_TEST_RENAMED_MOUNT));
            }
        } catch (PathNotFoundException e) {
            logger.info(String.format("Node %s correctly deleted", MOUNTS_TEST_RENAMED_MOUNT));
        } catch (RepositoryException e) {
            logger.error(String.format("Failed to delete the node %s", TEST_MOUNT_PATH),e);
        }
    }
    @Test public void testCreate() throws ConfigurationException, RepositoryException {
        JCRNodeWrapper mountPoint = createTestMountPoint();

        assertEquals(J_ROOT_PATH_VALUE, mountPoint.getPropertyAsString("j:rootPath"));
        assertEquals(JNT_VFS_MOUNT_POINT_TYPE, mountPoint.getPropertyAsString("jcr:primaryType"));
        assertEquals("test-mount", mountPoint.getPropertyAsString(J_NODENAME_PROPERTY));
        assertTrue(mountPoint.isNodeType("jmix:configPid"));
        assertEquals(PID, mountPoint.getPropertyAsString("configPid"));

        deleteNode(mountPoint.getIdentifier());
    }

    @Test(expected = RepositoryException.class) public void testUpdateWithoutTypeProperty()
            throws ConfigurationException, RepositoryException {
        properties.put(CONFIGURATION_MOUNT_J_NODENAME, "test");
        properties.put(CONFIGURATION_MOUNT_J_ROOT_PATH, J_ROOT_PATH_VALUE);
        properties.put(SERVICE_PID, PID);

        mountPointConfigFactory.updated(PID, properties);
        session = JCRSessionFactory.getInstance().getCurrentUserSession();
        session.getNode(TEST_MOUNT_PATH);
    }

    private JCRNodeWrapper createTestMountPoint() throws ConfigurationException, RepositoryException {
        properties.put(CONFIGURATION_MOUNT_J_NODENAME, "test");
        properties.put(CONFIGURATION_MOUNT_J_ROOT_PATH, J_ROOT_PATH_VALUE);
        properties.put(CONFIGURATION_MOUNT_JCR_PRIMARY_TYPE, JNT_VFS_MOUNT_POINT_TYPE);
        properties.put(SERVICE_PID, PID);

        mountPointConfigFactory.updated(PID, properties);

        return  JCRSessionFactory.getInstance().getCurrentUserSession().getNode(TEST_MOUNT_PATH);
    }

    @Test public void testUpdate() throws ConfigurationException, RepositoryException {
        createTestMountPoint();

        properties = new Hashtable<>();
        properties.put(CONFIGURATION_MOUNT_J_NODENAME, "test-renamed");
        properties.put(CONFIGURATION_MOUNT_J_ROOT_PATH, J_ROOT_PATH_VALUE);
        properties.put(CONFIGURATION_MOUNT_JCR_PRIMARY_TYPE, JNT_VFS_MOUNT_POINT_TYPE);
        properties.put(SERVICE_PID, PID);

        mountPointConfigFactory.updated(PID, properties);
        session = JCRSessionFactory.getInstance().getCurrentUserSession();
        JCRNodeWrapper mountPoint = session.getNode(MOUNTS_TEST_RENAMED_MOUNT);
        assertEquals("test-renamed-mount", mountPoint.getPropertyAsString(J_NODENAME_PROPERTY));

        deleteNode(mountPoint.getIdentifier());
    }

    @Test public void testDelete() throws ConfigurationException, RepositoryException {
        properties.put(CONFIGURATION_MOUNT_J_NODENAME, "test");
        properties.put(CONFIGURATION_MOUNT_J_ROOT_PATH, J_ROOT_PATH_VALUE);
        properties.put(CONFIGURATION_MOUNT_JCR_PRIMARY_TYPE, JNT_VFS_MOUNT_POINT_TYPE);
        properties.put(SERVICE_PID, PID);

        // mountPointConfigFactory.updated creates a mount point if it does not exist
        mountPointConfigFactory.updated(PID, properties);
        session = JCRSessionFactory.getInstance().getCurrentUserSession();
        session.getNode(TEST_MOUNT_PATH);

        mountPointConfigFactory.deleted(PID);
    }

    private static void deleteNode(String nodeId) throws RepositoryException {
        JCRTemplate.getInstance().doExecuteWithSystemSession(session -> {
            JCRNodeWrapper node = session.getNodeByIdentifier(nodeId);
            node.remove();
            session.save();
            return null;
        });
    }

}
