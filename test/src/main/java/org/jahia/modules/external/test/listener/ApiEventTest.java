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
package org.jahia.modules.external.test.listener;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.jahia.bin.Jahia;
import org.jahia.modules.external.ExternalData;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.content.JCREventIterator;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.test.JahiaTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.AbstractApplicationContext;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.Consumer;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Tests for external events
 */
public class ApiEventTest  extends JahiaTestCase {
    private String user = "root";
    private String password = "root1234";

    private TestApiEventListener apiListener;
    private TestEventListener listener;
    private ApiDataSource dataSource;

    @Before
    public void setUp() throws RepositoryException {
        AbstractApplicationContext context = ServicesRegistry.getInstance().getJahiaTemplateManagerService().getTemplatePackageById("external-provider-test").getContext();
        apiListener = (TestApiEventListener) context.getBean("apiTestListener");
        listener = (TestEventListener) context.getBean("testListener");
        dataSource = (ApiDataSource) context.getBean("apiDataSource");
        JCRSessionFactory.getInstance().closeAllSessions();
        dataSource.getLog().clear();
    }

    @After
    public void tearDown() throws RepositoryException {
    }

    @Test
    public void testSimpleEvent() throws IOException {
        executeCall("[{\n" +
                "    \"path\":\"/tata\"\n" +
                "  }]", it -> {

            try {
                assertEquals(1,it.getSize());
                Event e = it.nextEvent();
                assertEquals(Event.NODE_ADDED, e.getType());
                assertEquals("/external-static/tata", e.getPath());

                JCRNodeWrapper node = it.getSession().getNode(e.getPath());
            } catch (RepositoryException e1) {
                fail(e1.getMessage());
            }

        }, it -> {
            fail("Listener that do not listen on API events should not be triggered");
        });

    }

    @Test
    public void testRemoveEvent() throws IOException {
        executeCall("[{\n" +
                "    \"path\":\"/tata\",\n" +
                "    \"userID\":\"root\",\n" +
                "    \"type\":\"NODE_REMOVED\"\n" +
                "  }]", it -> {

            try {
                assertEquals(1, it.getSize());
                Event e = it.nextEvent();
                assertEquals(Event.NODE_REMOVED, e.getType());
                assertEquals("/external-static/tata", e.getPath());
            } catch (RepositoryException e1) {
                fail(e1.getMessage());
            }
        }, it -> {
            fail("Listener that do not listen on API events should not be triggered");
        });
    }

    @Test
    public void testEventWithData() throws IOException  {
        executeCall("[{\n" +
                "    \"path\":\"/tata\",\n" +
                "    \"userID\":\"root\",\n" +
                "    \"info\": {\n" +
                "      \"externalData\":{\n" +
                "        \"id\":\"/tata\",\n" +
                "        \"path\":\"/tata\",\n" +
                "        \"type\":\"jnt:bigText\",\n" +
                "        \"properties\": {\n" +
                "          \"jcr:created\": [\"2017-10-10T10:50:43.000+02:00\"],\n" +
                "          \"jcr:lastModified\": [\"2017-10-10T10:50:43.000+02:00\"]\n" +
                "        },\n" +
                "        \"i18nProperties\":{\n" +
                "          \"en\":{\n" +
                "            \"text\":[\"test title en\"]\n" +
                "          },\n" +
                "          \"fr\":{\n" +
                "            \"text\":[\"test title fr\"]\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }]", it -> {

            try {
                assertEquals(1,it.getSize());
                Event e = it.nextEvent();
                assertEquals(Event.NODE_ADDED, e.getType());
                assertEquals("/external-static/tata", e.getPath());

                ExternalData externalData = (ExternalData) e.getInfo().get("externalData");
                assertNotNull(externalData);
                assertEquals("/tata", externalData.getPath());

                JCRNodeWrapper node = it.getSession().getNode(e.getPath());
                assertEquals("2017-10-10T10:50:43.000+02:00", node.getProperty("jcr:created").getString());
                assertEquals("Should not go to datasource if externaldata is provided", 0, dataSource.getLog().size());
            } catch (RepositoryException e1) {
                fail(e1.getMessage());
            }
        }, it -> {
            fail("Listener that do not listen on API events should not be triggered");
        });
    }

    @Test
    public void testEventWithIncompleteExternalData() throws IOException  {
        int i = executeCall("[{\n" +
                "    \"path\":\"/tata\",\n" +
                "    \"userID\":\"root\",\n" +
                "    \"info\": {\n" +
                "      \"externalData\":{\n" +
                "        \"id\":\"/tata\",\n" +
                "        \"type\":\"jnt:bigText\"\n" +
                "      }\n" +
                "    }\n" +
                "  }]");
        assertEquals(400, i);

        i = executeCall("[{\n" +
                "    \"path\":\"/tata\",\n" +
                "    \"userID\":\"root\",\n" +
                "    \"info\": {\n" +
                "      \"externalData\":{\n" +
                "        \"path\":\"/tata\",\n" +
                "        \"type\":\"jnt:bigText\"\n" +
                "      }\n" +
                "    }\n" +
                "  }]");
        assertEquals(400, i);

        i = executeCall("[{\n" +
                "    \"path\":\"/tata\",\n" +
                "    \"userID\":\"root\",\n" +
                "    \"info\": {\n" +
                "      \"externalData\":{\n" +
                "        \"path\":\"/tata\",\n" +
                "        \"id\":\"/tata\"\n" +
                "      }\n" +
                "    }\n" +
                "  }]");
        assertEquals(400, i);
    }

    @Test
    public void testEventWithIncompleteProperties() throws IOException  {
        int i = executeCall("[{\n" +
                "    \"userID\":\"root\"\n" +
                "  }]");
        assertEquals(400, i);

        i = executeCall("[{\n" +
                "    \"path\":\"\"\n" +
                "  }]");
        assertEquals(400, i);
    }

    @Test
    public void testEventWithBinary() throws IOException  {
        executeCall("[{\n" +
                "    \"path\":\"/tutu\",\n" +
                "    \"userID\":\"root\",\n" +
                "    \"info\": {\n" +
                "      \"externalData\":{\n" +
                "        \"id\":\"/tutu\",\n" +
                "        \"path\":\"/tutu\",\n" +
                "        \"type\":\"jtestnt:binary\",\n" +
                "        \"properties\": {\n" +
                "          \"jcr:created\": [\"2017-10-10T10:50:43.000+02:00\"],\n" +
                "          \"jcr:lastModified\": [\"2017-10-10T10:50:43.000+02:00\"]\n" +
                "        },\n" +
                "        \"binaryProperties\":{\n" +
                "          \"jcr:data\": [\"" + Base64.getEncoder().encodeToString("tutu file content".getBytes()) + "\"]\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }]", it -> {

            try {
                assertEquals(1,it.getSize());
                Event e = it.nextEvent();
                assertEquals(Event.NODE_ADDED, e.getType());
                assertEquals("/external-static/tutu", e.getPath());

                ExternalData externalData = (ExternalData) e.getInfo().get("externalData");
                assertNotNull(externalData);
                assertEquals("/tutu", externalData.getPath());

                JCRNodeWrapper node = it.getSession().getNode(e.getPath());
                assertEquals("2017-10-10T10:50:43.000+02:00", node.getProperty("jcr:created").getString());
                assertEquals("tutu file content", node.getProperty("jcr:data").getString());
                assertEquals("Should not go to datasource if externaldata is provided", 0, dataSource.getLog().size());
            } catch (RepositoryException e1) {
                fail(e1.getMessage());
            }
        }, it -> {
            fail("Listener that do not listen on API events should not be triggered");
        });
    }

    private int executeCall(String body) throws IOException {
        HttpClient client = new HttpClient();

        URL url = new URL(getBaseServerURL() + Jahia.getContextPath() + "/modules/external-provider/events/staticProvider");

        client.getParams().setAuthenticationPreemptive(true);
        if (user != null && password != null) {
            Credentials defaultcreds = new UsernamePasswordCredentials(user, password);
            client.getState().setCredentials(new AuthScope(url.getHost(), url.getPort(), AuthScope.ANY_REALM), defaultcreds);
        }

        client.getHostConfiguration().setHost(url.getHost(), url.getPort(), url.getProtocol());

        PostMethod method = new PostMethod(url.toExternalForm());
        method.addRequestHeader("Content-Type", "application/json");
        method.setRequestEntity(new StringRequestEntity(body, "application/json","UTF-8"));

        return client.executeMethod(method);
    }

    private void executeCall(String body, Consumer<JCREventIterator> apiListenerCallback, Consumer<JCREventIterator> listenerCallback) throws IOException {
        apiListener.setCallback(apiListenerCallback);
        listener.setCallback(listenerCallback);

        try {
            int i = executeCall(body);
            assertEquals(200, i);

            if (apiListener.getAssertionError() != null) {
                throw apiListener.getAssertionError();
            }
            if (listener.getAssertionError() != null) {
                throw listener.getAssertionError();
            }
        } finally {
            apiListener.setCallback(null);
            apiListener.setAssertionError(null);
            listener.setCallback(null);
            listener.setAssertionError(null);
        }
    }
}
