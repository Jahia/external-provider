/*
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
package org.jahia.modules.external.events.rest;

import org.jahia.modules.external.ExternalContentStoreProvider;
import org.jahia.modules.external.events.EventServiceImpl;
import org.jahia.modules.external.events.model.ApiEventImpl;
import org.jahia.modules.external.events.validation.ValidList;
import org.jahia.modules.external.events.validation.ValidProvider;
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
                               @ValidProvider @PathParam("providerKey") String providerKey,
                               @HeaderParam("apiKey") String apiKey) throws RepositoryException {

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
