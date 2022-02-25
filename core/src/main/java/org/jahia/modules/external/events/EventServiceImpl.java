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

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, null, new JCRCallback<Object>() {
            @Override
            public Object doInJCR(JCRSessionWrapper jcrSessionWrapper) throws RepositoryException {
                logger.debug("Processing API events for default");
                JCRObservationManager.doWorkspaceWriteCall(jcrSessionWrapper, JCRObservationManager.API, callback);
                return null;
            }
        });

        JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.LIVE_WORKSPACE, null, new JCRCallback<Object>() {
            @Override
            public Object doInJCR(JCRSessionWrapper jcrSessionWrapper) throws RepositoryException {
                logger.debug("Processing API events for live");
                JCRObservationManager.doWorkspaceWriteCall(jcrSessionWrapper, JCRObservationManager.API, callback);
                return null;
            }
        });

        logger.info("API events processed");
    }

}
