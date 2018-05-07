package org.jahia.modules.external.rest;

import org.jahia.api.Constants;
import org.jahia.services.content.*;

import javax.jcr.RepositoryException;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * REST endpoint for external-provider events
 */
@Path("/external-provider/events")
@Produces({ MediaType.APPLICATION_JSON })
public class EventResource {

    @POST
    @Path("/{providerKey:.*}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postEvents(final List<ExternalEvent> events, @PathParam("providerKey") String providerKey) throws RepositoryException {
        final JCRStoreProvider provider = JCRSessionFactory.getInstance().getProviders().get(providerKey);
        if (provider != null) {
            JCRObservationManager.doWorkspaceWriteCall(JCRSessionFactory.getInstance().getCurrentSystemSession(Constants.EDIT_WORKSPACE, null, null), 3, new JCRCallback<Object>() {
                @Override
                public Object doInJCR(JCRSessionWrapper jcrSessionWrapper) throws RepositoryException {
                    for (ExternalEvent externalEvent : events) {
                        JCRObservationManager.addEvent(externalEvent, provider.getMountPoint(), "");
                    }
                    return null;
                }
            });

        }
        return Response.ok().build();
    }

}
