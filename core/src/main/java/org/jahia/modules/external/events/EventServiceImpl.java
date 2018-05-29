package org.jahia.modules.external.events;

import org.jahia.api.Constants;
import org.jahia.modules.external.ExternalData;
import org.jahia.modules.external.ExternalSessionImpl;
import org.jahia.modules.external.events.model.ApiEventImpl;
import org.jahia.services.content.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.stream.StreamSupport;

public class EventServiceImpl implements EventService {

    private static final Logger logger = LoggerFactory.getLogger(EventServiceImpl.class);

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
        logger.info("Received API events for " + provider.getKey());
        JCRCallback<Object> callback = jcrSessionWrapper -> {
            for (ApiEvent apiEvent : events) {
                logger.debug("Event {} for {}", apiEvent.getType(), apiEvent.getPath());
                ExternalData data = (ExternalData) apiEvent.getInfo().get("externalData");
                if (data != null) {
                    logger.debug("External data included for {}",data.getPath());
                    ExternalSessionImpl externalSession = (ExternalSessionImpl) jcrSessionWrapper.getProviderSession(provider);
                    externalSession.registerNode(data);
                }
                JCRObservationManager.addEvent(apiEvent, provider.getMountPoint(), "");
            }
            return null;
        };

        logger.debug("Processing API events for default");
        JCRObservationManager.doWorkspaceWriteCall(JCRSessionFactory.getInstance().getCurrentSystemSession(Constants.EDIT_WORKSPACE, null, null), JCRObservationManager.API, callback);
        logger.debug("Processing API events for live");
        JCRObservationManager.doWorkspaceWriteCall(JCRSessionFactory.getInstance().getCurrentSystemSession(Constants.LIVE_WORKSPACE, null, null), JCRObservationManager.API, callback);

        logger.info("API events processed");
    }

}
