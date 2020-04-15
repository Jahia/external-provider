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
package org.jahia.modules.external.test.qom;

import static org.junit.Assert.assertEquals;

import java.util.Locale;

import javax.jcr.RepositoryException;
import javax.jcr.query.qom.QueryObjectModelFactory;

import org.jahia.api.Constants;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.query.QOMBuilder;
import org.jahia.test.JahiaTestCase;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for the escaping in queries using QOM and external provider.
 * 
 * @author Sergiy Shyrkov
 */
public class QOMTest extends JahiaTestCase {

    @BeforeClass
    public static void oneTimeSetup() {
        // do nothing
    }

    @AfterClass
    public static void oneTimeTearDown() {
        // do nothing
    }

    private JCRSessionWrapper session;

    private long pathQuery(String path) throws RepositoryException {
        QueryObjectModelFactory factory = session.getWorkspace().getQueryManager().getQOMFactory();
        QOMBuilder qomBuilder = new QOMBuilder(factory, session.getValueFactory());
        qomBuilder.setSource(factory.selector("jnt:folder", "aaa"));
        qomBuilder.andConstraint(factory.descendantNode("aaa", path));

        return qomBuilder.createQOM().execute().getNodes().getSize();
    }

    private void removeTestNode() throws RepositoryException {
        if (session.nodeExists("/sites/systemsite/files/qom-test")) {
            session.getNode("/sites/systemsite/files/qom-test").remove();
            session.save();
        }
    }

    @Before
    public void setUp() throws RepositoryException {
        session = JCRSessionFactory.getInstance().getCurrentUserSession(Constants.EDIT_WORKSPACE, Locale.ENGLISH);
        removeTestNode();
        session.getNode("/sites/systemsite/files").addNode("qom-test", "jnt:folder")
                .addNode("my path's value", "jnt:folder").addNode("test folder", "jnt:folder");
        session.getNode("/sites/systemsite/files/qom-test")
                .addNode("DLINK_DNS320L(LW).1.04b08(3.07.0822.2014)", "jnt:folder")
                .addNode("test folder", "jnt:folder");
        session.save();
    }

    @After
    public void tearDown() throws RepositoryException {
        removeTestNode();
        session.logout();
    }

    @Test
    public void testPathEscaping() throws RepositoryException {
        assertEquals(1, pathQuery("/sites/systemsite/files/qom-test/my path's value"));
        assertEquals(1, pathQuery("/sites/systemsite/files/qom-test/DLINK_DNS320L(LW).1.04b08(3.07.0822.2014)"));
    }
}
