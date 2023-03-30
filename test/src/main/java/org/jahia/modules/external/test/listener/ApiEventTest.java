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
package org.jahia.modules.external.test.listener;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.jahia.bin.Jahia;
import org.jahia.modules.external.ExternalData;
import org.jahia.modules.external.events.EventService;
import org.jahia.osgi.BundleUtils;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.content.ApiEvent;
import org.jahia.services.content.JCREventIterator;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.test.JahiaTestCase;
import org.junit.*;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.springframework.context.support.AbstractApplicationContext;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.Assert.*;

/**
 * Tests for external events
 */
public class ApiEventTest  extends JahiaTestCase {
    private String user = "root";
    private String password = "root1234";

    private static Dictionary<String, Object> originalProperties;
    private static final String API_KEY = "42267ebc-f8d0-4f4d-ac98-21fb8eeda653";
    private static final String PROVIDER = "staticProvider";

    private TestApiEventListener apiListener;
    private TestEventListener listener;
    private static ApiDataSource dataSource;
    private EventService eventService;

    private static final Consumer<JCREventIterator> failCallback = it -> {
        fail("Listener that do not listen on API events should not be triggered");
    };

    private static final Consumer<JCREventIterator> eventWithExternalDataCB = it -> {

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
    };

    private static final Consumer<JCREventIterator> simpleEventCB = it -> {

        try {
            assertEquals(1,it.getSize());
            Event e = it.nextEvent();
            assertEquals(Event.NODE_ADDED, e.getType());
            assertEquals("/external-static/tata", e.getPath());

            JCRNodeWrapper node = it.getSession().getNode(e.getPath());
        } catch (RepositoryException e1) {
            fail(e1.getMessage());
        }

    };

    @BeforeClass
    public static void oneTimeSetUp() throws Exception {
        ConfigurationAdmin configurationAdmin = BundleUtils.getOsgiService(ConfigurationAdmin.class, null);
        Configuration configuration = configurationAdmin.getConfiguration("org.jahia.modules.api.external_provider.event");
        originalProperties = configuration.getProperties();
        List<String> keys = Collections.list(originalProperties.keys());
        Hashtable<String, Object> newProps = new Hashtable<>();
        keys.stream().forEach(k -> newProps.put(k, originalProperties.get(k)));
        newProps.put("providers.event.api.key", "42267ebc-f8d0-4f4d-ac98-21fb8eeda653");
        configuration.update(newProps);
    }

    @AfterClass
    public static void oneTearDown() throws Exception {
        ConfigurationAdmin configurationAdmin = BundleUtils.getOsgiService(ConfigurationAdmin.class, null);
        Configuration configuration = configurationAdmin.getConfiguration("org.jahia.modules.api.external_provider.event");
        configuration.update(originalProperties);
    }

    @Before
    public void setUp() {
        eventService = BundleUtils.getOsgiService(EventService.class, null);
        AbstractApplicationContext context = ServicesRegistry.getInstance().getJahiaTemplateManagerService().getTemplatePackageById("external-provider-test").getContext();
        apiListener = (TestApiEventListener) context.getBean("apiTestListener");
        listener = (TestEventListener) context.getBean("testListener");
        dataSource = (ApiDataSource) context.getBean("apiDataSource");
        JCRSessionFactory.getInstance().closeAllSessions();
        dataSource.getLog().clear();
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testSimpleEventREST() {
        executeCall("[{\n" +
                "    \"path\":\"/tata\"\n" +
                "  }]", simpleEventCB);
    }

    @Test
    public void testSimpleEventOSGI() {
        // try OSGI service call
        ApiEventImplTest apiEvent = new ApiEventImplTest();
        apiEvent.setPath("/tata");
        executeOSGICall(Collections.singleton(apiEvent), simpleEventCB);
    }

    @Test
    public void testRemoveEvent() {
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
        });
    }

    @Test
    public void testPathOfEventWhenEventOnRoot() {
        executeCall("[{\n" +
                "    \"path\":\"/\",\n" +
                "    \"userID\":\"root\",\n" +
                "    \"type\":\"NODE_REMOVED\"\n" +
                "  }]", it -> {

            try {
                assertEquals(1, it.getSize());
                Event e = it.nextEvent();
                assertEquals(Event.NODE_REMOVED, e.getType());
                assertEquals("/external-static", e.getPath());
            } catch (RepositoryException e1) {
                fail(e1.getMessage());
            }
        });
    }

    @Test
    public void testEventWithDataREST() {
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
                "  }]", eventWithExternalDataCB);
    }

    @Test
    public void testEventWithDataOSGI() {
        Map<String, String[]> properties = new HashMap<>();
        properties.put("jcr:created", new String[] {"2017-10-10T10:50:43.000+02:00"});
        properties.put("jcr:lastModified", new String[] {"2017-10-10T10:50:43.000+02:00"});

        Map<String, Map<String, String[]>> i18nProperties = new HashMap<>();
        Map<String, String[]> enProps = new HashMap<>();
        enProps.put("text", new String[] {"test title en"});
        Map<String, String[]> frProps = new HashMap<>();
        frProps.put("text", new String[] {"test title fr"});
        i18nProperties.put("en", enProps);
        i18nProperties.put("fr", frProps);

        ExternalData externalData = new ExternalData("/tata", "/tata", "jnt:bigText", properties);
        externalData.setI18nProperties(i18nProperties);
        executeOSGICallWithExternalData(Collections.singleton(externalData), eventWithExternalDataCB);
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
    public void testEventWithInvalidProvider() throws IOException  {
        int i = executeCall("[{\n" +
                "    \"userID\":\"root\",\n" +
                "    \"path\":\"/tata\"\n" +
                "  }]", "");
        assertEquals(400, i);

        i = executeCall("[{\n" +
                "    \"userID\":\"root\",\n" +
                "    \"path\":\"/tata\"\n" +
                "  }]", "invalidProvider");
        assertEquals(400, i);
    }

    @Test
    public void testEventWithInvalidAPIKey() throws IOException  {
        int i = executeCall("[{\n" +
                "    \"userID\":\"root\",\n" +
                "    \"path\":\"/tata\"\n" +
                "  }]", PROVIDER, "");
        assertEquals(403, i);

        i = executeCall("[{\n" +
                "    \"userID\":\"root\",\n" +
                "    \"path\":\"/tata\"\n" +
                "  }]", PROVIDER, "invalidApiKey");
        assertEquals(403, i);
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
        });
    }

    private int executeCall(String body, String provider, String apiKey) throws IOException {
        CloseableHttpClient client = getHttpClient();

        URL url = new URL(getBaseServerURL() + Jahia.getContextPath() + "/modules/external-provider/events/" + provider);

        HttpPost method = new HttpPost(url.toExternalForm());

        if (user != null && password != null) {
            method.addHeader("Authorization", "Basic " + org.apache.xerces.impl.dv.util.Base64.encode((user + ":" + password).getBytes()));
        }


        method.addHeader("Content-Type", "application/json");
        method.setEntity(new StringEntity(body, ContentType.create("application/json","UTF-8")));
        method.setHeader("apiKey", apiKey);

        try (CloseableHttpResponse httpResponse = client.execute(method)) {
            return httpResponse.getCode();
        }
    }

    private int executeCall(String body, String provider) throws IOException {
        return executeCall(body, provider, API_KEY);
    }

    private int executeCall(String body) throws IOException {
        return executeCall(body, PROVIDER);
    }

    private void executeCall(String body, Consumer<JCREventIterator> apiListenerCallback) {
        executeListeners(() -> {
            try {
                int i = executeCall(body);
                assertEquals(200, i);
            } catch (IOException e) {
                fail(e.getMessage());
            }
            return null;
        }, apiListenerCallback);
    }

    private void executeOSGICall(Iterable<ApiEvent> apiEvents, Consumer<JCREventIterator> apiListenerCallback) {
        executeListeners(() -> {
            try {
                eventService.sendEvents(apiEvents, JCRSessionFactory.getInstance().getProviders().get(PROVIDER));
            } catch (RepositoryException e) {
                fail(e.getMessage());
            }
            return null;
        }, apiListenerCallback);
    }

    private void executeOSGICallWithExternalData(Iterable<ExternalData> externalDatas, Consumer<JCREventIterator> apiListenerCallback) {
        executeListeners(() -> {
            try {
                eventService.sendAddedNodes(externalDatas, JCRSessionFactory.getInstance().getProviders().get(PROVIDER));
            } catch (RepositoryException e) {
                fail(e.getMessage());
            }
            return null;
        }, apiListenerCallback);
    }

    private void executeListeners(Supplier doAction, Consumer<JCREventIterator> apiListenerCallback) {
        apiListener.setCallback(apiListenerCallback);
        listener.setCallback(ApiEventTest.failCallback);

        try {
            doAction.get();

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
