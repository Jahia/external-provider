/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group SA. All rights reserved.
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
