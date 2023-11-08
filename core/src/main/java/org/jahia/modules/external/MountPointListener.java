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
package org.jahia.modules.external;

import org.jahia.api.Constants;
import org.jahia.services.content.DefaultEventListener;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;

/**
 * Listener for the deletion of a mount point nodes to be able to clean up external provider ID mappings.
 *
 * @author Sergiy Shyrkov
 */
@Component(service = DefaultEventListener.class, immediate = true)
public class MountPointListener extends DefaultEventListener {

    private static final Logger logger = LoggerFactory.getLogger(MountPointListener.class);

    private ExternalProviderInitializerService externalProviderInitializerService;

    @Reference
    public void setExternalProviderInitializerService(ExternalProviderInitializerService mappingService) {
        this.externalProviderInitializerService = mappingService;
    }

    @Override
    public String getWorkspace() {
        return Constants.EDIT_WORKSPACE;
    }

    @Override
    public int getEventTypes() {
        return Event.NODE_REMOVED;
    }

    @Override
    public String getPath() {
        return "/mounts";
    }

    @Override
    public void onEvent(EventIterator events) {
        while (events.hasNext()) {
            Event evt = events.nextEvent();
            try {
                externalProviderInitializerService.removeProvider(evt.getIdentifier());
            } catch (RepositoryException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }
}
