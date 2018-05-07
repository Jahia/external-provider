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
import java.net.URL;
import java.util.function.Consumer;

import static org.junit.Assert.*;

public class ApiEventTest  extends JahiaTestCase {
    private String user = "root";
    private String password = "root1234";

    private TestApiEventListener listener;
    private ApiDataSource dataSource;

    @Before
    public void setUp() throws RepositoryException {
        AbstractApplicationContext context = ServicesRegistry.getInstance().getJahiaTemplateManagerService().getTemplatePackageById("external-provider-test").getContext();
        listener = (TestApiEventListener) context.getBean("testListener");
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
                "    \"path\":\"/tata\",\n" +
                "    \"userID\":\"root\"\n" +
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

        });

    }

    @Test
    public void testRemoveEvent() throws IOException {
        executeCall("[{\n" +
                "    \"path\":\"/tata\",\n" +
                "    \"userID\":\"root\",\n" +
                "    \"type\":2\n" +
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
        });
    }

    private void executeCall(String body, Consumer<JCREventIterator> callback) throws IOException {
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
        listener.setCallback(callback);
        try {
            int i = client.executeMethod(method);
            assertEquals(200, i);

            if (listener.getAssertionError() != null) {
                throw listener.getAssertionError();
            }
        } finally {
            listener.setCallback(null);
            listener.setAssertionError(null);
        }
    }

}
