/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2018 Jahia Solutions Group SA. All rights reserved.
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

import org.jahia.modules.external.vfs.factory.VFSMountPointFactory;
import org.jahia.modules.external.vfs.factory.VFSMountPointFactoryHandler;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.content.*;
import org.jahia.services.content.decorator.JCRGroupNode;
import org.jahia.services.content.decorator.JCRUserNode;
import org.jahia.services.sites.JahiaSite;
import org.jahia.services.usermanager.JahiaGroupManagerService;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.jahia.test.TestHelper;
import org.jahia.test.services.content.ContentTest;
import org.junit.*;
import org.slf4j.Logger;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import java.io.File;
import java.util.Collections;
import java.util.Properties;

import static org.junit.Assert.*;

public class VFSAclTest {
    private static final transient Logger logger = org.slf4j.LoggerFactory.getLogger(ContentTest.class);

    private final static String TESTSITE_NAME = "aclTestSite";

    private static JCRUserNode user1;
    private static JCRUserNode user2;
    private static JCRUserNode user3;
    private static JCRUserNode user4;

    private static JCRGroupNode group1;
    private static JCRGroupNode group2;
    private static File dynamicMountDir;

    private static final String DYNAMIC_MOUNT_FILE_NAME = "dynamicMountDirectory";
    private static final String MOUNTS_DYNAMIC_MOUNT_POINT = "/mounts/dynamic-mount";
    private static final String MOUNTS_DYNAMIC_MOUNT_POINT_LOCAL =  "/sites/"+TESTSITE_NAME+"/files";
    private static final String MOUNTS_DYNAMIC_MOUNT_POINT_TARGET = MOUNTS_DYNAMIC_MOUNT_POINT_LOCAL + "/dynamic";
    private static final String MOUNTS_DYNAMIC_MOUNT_POINT_NAME = "dynamic";

    public static JCRPublicationService jcrService;

    private static JCRNodeWrapper root;
    private static JCRNodeWrapper content1;
    private static JCRNodeWrapper content11;
    private static JCRNodeWrapper content12;
    private static JCRNodeWrapper content2;
    private static JCRNodeWrapper content21;
    private static JCRNodeWrapper content22;
    private static String rootIdentifier;
    private JCRSessionWrapper session;
    static String content1Identifier;
    private static String content11Identifier;
    private static String content12Identifier;
    private static String content2Identifier;
    private static String content21Identifier;
    private static String content22Identifier;


    public VFSAclTest() {
    }

    @BeforeClass
    public static void oneTimeSetUp() throws Exception {
        JahiaSite site = TestHelper.createSite(TESTSITE_NAME);

        jcrService = ServicesRegistry.getInstance().getJCRPublicationService();

        JCRSessionWrapper session = jcrService.getSessionFactory().getCurrentUserSession();


        File sysTempDir = new File(System.getProperty("java.io.tmpdir"));

        dynamicMountDir = new File(sysTempDir, DYNAMIC_MOUNT_FILE_NAME);
        if (!dynamicMountDir.exists()) {
            dynamicMountDir.mkdir();
        }

        VFSMountPointFactory vfsMountPointFactory = new VFSMountPointFactory();
        vfsMountPointFactory.setName(MOUNTS_DYNAMIC_MOUNT_POINT_NAME);
        vfsMountPointFactory.setRoot("file://" + dynamicMountDir.getAbsolutePath());
        vfsMountPointFactory.setLocalPath(MOUNTS_DYNAMIC_MOUNT_POINT_LOCAL);
        new VFSMountPointFactoryHandler().save(vfsMountPointFactory);

        root = session.getNode(MOUNTS_DYNAMIC_MOUNT_POINT_TARGET);
        rootIdentifier = root.getIdentifier();
        content1 = root.addNode("content1", "jnt:folder");
        content1Identifier = content1.getIdentifier();
        content11 = content1.addNode("content1.1", "jnt:folder");
        content11Identifier = content11.getIdentifier();
        content12 = content1.addNode("content1.2", "jnt:folder");
        content12Identifier = content12.getIdentifier();
        content2 = root.addNode("content2", "jnt:folder");
        content2Identifier = content2.getIdentifier();
        content21 = content2.addNode("content2.1", "jnt:folder");
        content21Identifier = content21.getIdentifier();
        content22 = content2.addNode("content2.2", "jnt:folder");
        content22Identifier = content22.getIdentifier();
        session.save();

        JahiaUserManagerService userManager = ServicesRegistry.getInstance().getJahiaUserManagerService();
        assertNotNull("JahiaUserManagerService cannot be retrieved", userManager);

        user1 = userManager.createUser("user1", "password", new Properties(), session);
        user2 = userManager.createUser("user2", "password", new Properties(), session);
        user3 = userManager.createUser("user3", "password", new Properties(), session);
        user4 = userManager.createUser("user4", "password", new Properties(), session);

        JahiaGroupManagerService groupManager = ServicesRegistry.getInstance().getJahiaGroupManagerService();
        assertNotNull("JahiaGroupManagerService cannot be retrieved", groupManager);

        group1 = groupManager.createGroup(site.getSiteKey(), "group1", new Properties(), false, session);
        group2 = groupManager.createGroup(site.getSiteKey(), "group2", new Properties(), false, session);

        group1.addMember(user1);
        group1.addMember(user2);

        group2.addMember(user3);
        group2.addMember(user4);
        session.save();
    }

    @AfterClass
    public static void oneTimeTearDown() throws Exception {
        try {
            JCRSessionWrapper session = JCRSessionFactory.getInstance().getCurrentUserSession();
            if (session.nodeExists("/sites/"+TESTSITE_NAME)) {
                TestHelper.deleteSite(TESTSITE_NAME);
            }

            JahiaUserManagerService userManager = ServicesRegistry.getInstance().getJahiaUserManagerService();
            userManager.deleteUser(user1.getPath(), session);
            userManager.deleteUser(user2.getPath(), session);
            userManager.deleteUser(user3.getPath(), session);
            userManager.deleteUser(user4.getPath(), session);
            session.save();
        } catch (Exception ex) {
            logger.warn("Exception during test tearDown", ex);
        }
        JCRSessionFactory.getInstance().closeAllSessions();

        // unmount
        JCRStoreProvider provider = JCRStoreService.getInstance().getSessionFactory().getProvider(MOUNTS_DYNAMIC_MOUNT_POINT_TARGET, false);
        if (provider != null) {
            provider.stop();
        }
        JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<Object>() {
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
                } catch (PathNotFoundException e) {
                }
                try {
                    targetMountNode = session.getNode(MOUNTS_DYNAMIC_MOUNT_POINT_TARGET);
                } catch (PathNotFoundException e) {
                }

                if (mountNode != null) {
                    fail("Failed to remove mountnode: " + mountNode.getPath());
                }

                if (targetMountNode != null) {
                    fail("Failed to remove target mountnode: " + targetMountNode.getPath());
                }

                return null;
            }
        });
    }

    @Before
    public void setUp() throws RepositoryException {
        session = JCRSessionFactory.getInstance().getCurrentUserSession();
        root = session.getNodeByIdentifier(rootIdentifier);
        content1 = session.getNodeByIdentifier(content1Identifier);
        content11 = session.getNodeByIdentifier(content11Identifier);
        content12 = session.getNodeByIdentifier(content12Identifier);
        content2 = session.getNodeByIdentifier(content2Identifier);
        content21 = session.getNodeByIdentifier(content21Identifier);
        content22 = session.getNodeByIdentifier(content22Identifier);
        session.save();
    }

    @After
    public void tearDown() throws Exception {
        root.revokeAllRoles();
        content1.revokeAllRoles();
        content11.revokeAllRoles();
        content12.revokeAllRoles();
        content2.revokeAllRoles();
        content21.revokeAllRoles();
        content21.revokeAllRoles();
        session.save();
        JCRSessionFactory.getInstance().closeAllSessions();
    }

    @Test
    public void testDefaultReadRight() throws Exception {
        assertFalse((JCRTemplate.getInstance().doExecute("user1", null ,null, null, new CheckPermission(MOUNTS_DYNAMIC_MOUNT_POINT_LOCAL, "jcr:read"))));
    }

    @Test
    public void testGrantUser() throws Exception {
        content11.grantRoles("u:user1", Collections.singleton("owner"));
        session.save();

        assertTrue((JCRTemplate.getInstance().doExecute("user1", null ,null, null, new CheckPermission(content11.getPath(), "jcr:write"))));
        assertFalse((JCRTemplate.getInstance().doExecute("user2", null ,null, null, new CheckPermission(content11.getPath(), "jcr:write"))));
    }

    @Test
    public void testGrantGroup() throws Exception {
        content11.grantRoles("g:group1", Collections.singleton("owner"));
        session.save();

        assertTrue((JCRTemplate.getInstance().doExecute("user1", null ,null, null, new CheckPermission(content11.getPath(), "jcr:write"))));
        assertTrue((JCRTemplate.getInstance().doExecute("user2", null ,null, null, new CheckPermission(content11.getPath(), "jcr:write"))));
        assertFalse((JCRTemplate.getInstance().doExecute("user3", null ,null, null, new CheckPermission(content11.getPath(), "jcr:write"))));
        assertFalse((JCRTemplate.getInstance().doExecute("user4", null ,null, null, new CheckPermission(content11.getPath(), "jcr:write"))));
    }

    @Test
    public void testDenyUser() throws Exception {
        content1.grantRoles("u:user1", Collections.singleton("owner"));
        content11.denyRoles("u:user1", Collections.singleton("owner"));
        session.save();

        assertTrue((JCRTemplate.getInstance().doExecute("user1", null ,null, null, new CheckPermission(content1.getPath(), "jcr:write"))));
        assertFalse((JCRTemplate.getInstance().doExecute("user1", null ,null, null, new CheckPermission(content11.getPath(), "jcr:write"))));
    }

    @Test
    public void testAclBreak() throws Exception {
        content1.setAclInheritanceBreak(true);

        content11.grantRoles("u:user1", Collections.singleton("owner"));
        session.save();
        assertFalse((JCRTemplate.getInstance().doExecute("user1", null ,null, null, new CheckPermission(root.getPath(), "jcr:read"))));
        assertFalse((JCRTemplate.getInstance().doExecute("user1", null ,null, null, new CheckPermission(content1.getPath(), "jcr:read"))));
        assertTrue((JCRTemplate.getInstance().doExecute("user1", null ,null, null, new CheckPermission(content11.getPath(), "jcr:read"))));
        assertFalse((JCRTemplate.getInstance().doExecute("user1", null ,null, null, new CheckPermission(content12.getPath(), "jcr:read"))));
    }



    class CheckPermission implements JCRCallback<Boolean> {
        private String path;
        private String permission;

        CheckPermission(String path, String permission) {
            this.path = path;
            this.permission = permission;
        }

        public Boolean doInJCR(JCRSessionWrapper session) throws RepositoryException {
            try {
                return session.getNode(path).hasPermission(permission);
            } catch (PathNotFoundException e) {
                return false;
            }
        }
    }


}
