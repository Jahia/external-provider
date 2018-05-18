package org.jahia.modules.external.events;

import org.jahia.modules.external.ExternalData;
import org.jahia.services.content.ApiEvent;
import org.jahia.services.content.JCRStoreProvider;

import javax.jcr.RepositoryException;

public interface EventService {

     void sendEvents(Iterable<? extends ApiEvent> events, JCRStoreProvider provider) throws RepositoryException;

     void sendAddedNodes(Iterable<ExternalData> data, JCRStoreProvider provider) throws RepositoryException;

     ApiEvent addedEventFromData(ExternalData data);

}
