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
package org.jahia.modules.external.modules;

import org.apache.commons.lang.StringUtils;
import org.jahia.api.Constants;
import org.jahia.services.content.DefaultEventListener;
import org.jahia.services.content.JCREventIterator;
import org.jahia.settings.SettingsBean;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import java.util.HashSet;
import java.util.Set;

/**
 * JCR event listener to detect changes in modules content.
 */
@Component(service = {DefaultEventListener.class, ModulesListener.class}, immediate = true)
public class ModulesListener extends DefaultEventListener {
    private static final Logger logger = LoggerFactory.getLogger(ModulesListener.class);

    private final Set<String> modules = new HashSet<String>();

    public Set<String> getModules() {
        return modules;
    }

    @Override
    public String getWorkspace() {
        return Constants.EDIT_WORKSPACE;
    }

    @Override
    public int getEventTypes() {
        return !SettingsBean.getInstance().isProductionMode()
                && !SettingsBean.getInstance().isDistantPublicationServerMode() ? Event.NODE_ADDED + Event.NODE_REMOVED
                + Event.PROPERTY_ADDED + Event.PROPERTY_CHANGED + Event.PROPERTY_REMOVED + Event.NODE_MOVED : 0;
    }

    @Override
    public String getPath() {
        return "/modules";
    }

    @Override
    public void onEvent(EventIterator events) {
        if (((JCREventIterator) events).getSession().isSystem()) {
            // skip internal module operations -> initial import and initialization / cleanup
            return;
        }

        synchronized (modules) {
            while (events.hasNext()) {
                Event event = (Event) events.next();
                if (!isExternal(event)) {
                    try {
                        String m = StringUtils.substringAfter(event.getPath(), "/modules/");
                        m = StringUtils.substringBefore(m, "/");
                        if (!StringUtils.isEmpty(m)) {
                            modules.add(m);
                        }
                    } catch (RepositoryException e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
        }
    }
}
