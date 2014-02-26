/**
 * This file is part of Jahia, next-generation open source CMS:
 * Jahia's next-generation, open source CMS stems from a widely acknowledged vision
 * of enterprise application convergence - web, search, document, social and portal -
 * unified by the simplicity of web content management.
 *
 * For more information, please visit http://www.jahia.com.
 *
 * Copyright (C) 2002-2014 Jahia Solutions Group SA. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * As a special exception to the terms and conditions of version 2.0 of
 * the GPL (or any later version), you may redistribute this Program in connection
 * with Free/Libre and Open Source Software ("FLOSS") applications as described
 * in Jahia's FLOSS exception. You should have received a copy of the text
 * describing the FLOSS exception, and it is also available here:
 * http://www.jahia.com/license
 *
 * Commercial and Supported Versions of the program (dual licensing):
 * alternatively, commercial and supported versions of the program may be used
 * in accordance with the terms and conditions contained in a separate
 * written agreement between you and Jahia Solutions Group SA.
 *
 * If you are unsure which license is appropriate for your use,
 * please contact the sales department at sales@jahia.com.
 */

package org.jahia.modules.external.modules;

import java.util.HashSet;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;

import org.apache.commons.lang.StringUtils;
import org.jahia.services.content.DefaultEventListener;
import org.jahia.services.content.JCREventIterator;
import org.jahia.settings.SettingsBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JCR event listener to detect changes in modules content.
 */
public class ModulesListener extends DefaultEventListener {
    private static final Logger logger = LoggerFactory.getLogger(ModulesListener.class);
    private static ModulesListener instance;

    private final Set<String> modules = new HashSet<String>();

    public ModulesListener() {
        setWorkspace("default");
    }

    public static ModulesListener getInstance() {
        if (instance== null) {
            instance = new ModulesListener();
        }
        return instance;
    }

    public Set<String> getModules() {
        return modules;
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
        if (((JCREventIterator)events).getSession().isSystem()) {
            // skip internal module operations -> initial import and initialization / cleanup
            return;
        }

        synchronized (modules) {
            while (events.hasNext()) {
                Event event = (Event) events.next();
                if (!isExternal(event)) {
                    try {
                        String m = StringUtils.substringAfter(event.getPath(), "/modules/");
                        m  = StringUtils.substringBefore(m, "/");
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
