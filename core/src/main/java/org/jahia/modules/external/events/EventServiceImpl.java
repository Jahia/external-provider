package org.jahia.modules.external.events;

import org.jahia.api.Constants;
import org.jahia.modules.external.ExternalData;
import org.jahia.modules.external.ExternalSessionImpl;
import org.jahia.modules.external.events.model.ApiEventImpl;
import org.jahia.services.content.*;

import javax.jcr.RepositoryException;
import java.util.stream.StreamSupport;

public class EventServiceImpl implements EventService {

    public void sendEvents(Iterable<? extends ApiEvent> events, JCRStoreProvider provider) throws RepositoryException {
        doSendEvents(events, provider);
    }

    @Override
    public void sendAddedNodes(Iterable<ExternalData> data, JCRStoreProvider provider) throws RepositoryException {
        doSendEvents(() -> StreamSupport.stream(data.spliterator(), false).map(this::addedEventFromData).iterator(), provider);
    }

    public ApiEvent addedEventFromData(ExternalData data) {
        return new ApiEventImpl(data);
    }

    public static void doSendEvents(Iterable<? extends ApiEvent> events, JCRStoreProvider provider) throws RepositoryException {
        JCRCallback<Object> callback = jcrSessionWrapper -> {
            for (ApiEvent apiEvent : events) {
                ExternalData data = (ExternalData) apiEvent.getInfo().get("externalData");
                if (data != null) {
                    ExternalSessionImpl externalSession = (ExternalSessionImpl) jcrSessionWrapper.getProviderSession(provider);
                    externalSession.registerNode(data);
                }
                JCRObservationManager.addEvent(apiEvent, provider.getMountPoint(), "");
            }
            return null;
        };

        JCRObservationManager.doWorkspaceWriteCall(JCRSessionFactory.getInstance().getCurrentSystemSession(Constants.EDIT_WORKSPACE, null, null), JCRObservationManager.API, callback);
        JCRObservationManager.doWorkspaceWriteCall(JCRSessionFactory.getInstance().getCurrentSystemSession(Constants.LIVE_WORKSPACE, null, null), JCRObservationManager.API, callback);
    }

}
