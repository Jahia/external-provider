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

import org.jahia.modules.external.ExternalContentStoreProvider;
import org.jahia.modules.external.events.EventServiceImpl;
import org.jahia.modules.external.events.model.ApiEventImpl;
import org.jahia.modules.external.events.validation.ValidList;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRStoreProvider;

import javax.inject.Inject;
import javax.jcr.RepositoryException;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * REST endpoint for external-provider events
 */
@Path("/external-provider/events")
@Produces({ MediaType.APPLICATION_JSON })
public class EventResource {

    private EventApiConfig eventApiConfig;

    @Inject
    public EventResource(EventApiConfig eventApiConfig) {
        this.eventApiConfig = eventApiConfig;
    }

    @POST
    @Path("/{providerKey:.*}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postEvents(@Valid final ValidList<ApiEventImpl> events,
                               @PathParam("providerKey") String providerKey,
                               @HeaderParam("apiKey") String apiKey) throws RepositoryException {

        if (!(JCRSessionFactory.getInstance().getProviders().get(providerKey) instanceof ExternalContentStoreProvider)) {
            Response.ResponseBuilder response = Response
                    .status(Response.Status.BAD_REQUEST)
                    .type(MediaType.TEXT_PLAIN_TYPE)
                    .entity("No external provider found (invalidValue = " + providerKey +  ")");
            return response.build();
        }

        final JCRStoreProvider provider = JCRSessionFactory.getInstance().getProviders().get(providerKey);

        if (!eventApiConfig.checkApiKey(apiKey, providerKey)) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }

        if (provider instanceof ExternalContentStoreProvider) {
            EventServiceImpl.doSendEvents(events, provider);
        }

        return Response.ok().build();
    }
}
