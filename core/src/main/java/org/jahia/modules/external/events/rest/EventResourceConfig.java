/*
 * Copyright (C) 2002-2022 Jahia Solutions Group SA. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jahia.modules.external.events.rest;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

/**
 * Configuration for /external-provider/events end point
 */
public class EventResourceConfig extends ResourceConfig {

    public EventResourceConfig() {
        this(EventApiConfigServiceFactory.class);
    }

    private EventResourceConfig(final Class<? extends Factory<EventApiConfig>> eventApiConfigFactoryClass) {
        super(
                EventResource.class,
                JacksonJaxbJsonProvider.class,
                SimpleValidationFeature.class
        );
        register(new AbstractBinder() {
            @Override
            protected void configure() {
                bindFactory(eventApiConfigFactoryClass).to(EventApiConfig.class);
            }
        });
        property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
    }
}
