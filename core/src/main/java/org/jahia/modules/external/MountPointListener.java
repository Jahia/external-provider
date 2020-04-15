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
package org.jahia.modules.external;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;

import org.jahia.api.Constants;
import org.jahia.services.content.DefaultEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listener for the deletion of a mount point nodes to be able to clean up external provider ID mappings.
 *
 * @author Sergiy Shyrkov
 */
public class MountPointListener extends DefaultEventListener {

    private static final Logger logger = LoggerFactory.getLogger(MountPointListener.class);

    private ExternalProviderInitializerService externalProviderInitializerService;

    public MountPointListener() {
        super();
        setWorkspace(Constants.EDIT_WORKSPACE);
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

    public void setExternalProviderInitializerService(ExternalProviderInitializerService mappingService) {
        this.externalProviderInitializerService = mappingService;
    }

}
