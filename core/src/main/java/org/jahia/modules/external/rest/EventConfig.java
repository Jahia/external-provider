package org.jahia.modules.external.rest;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * Configuration for /external-provider/events end point
 */
public class EventConfig extends ResourceConfig {
    public EventConfig() {
        super(
                EventResource.class,
                JacksonJaxbJsonProvider.class
        );
    }
}
