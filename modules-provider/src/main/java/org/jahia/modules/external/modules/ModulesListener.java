/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2016 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU General Public License
 *     as published by the Free Software Foundation; either version 2
 *     of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
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
package org.jahia.modules.external.modules;

import org.apache.commons.lang.StringUtils;
import org.jahia.services.content.DefaultEventListener;
import org.jahia.services.content.JCREventIterator;
import org.jahia.settings.SettingsBean;
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
public class ModulesListener extends DefaultEventListener {
    private static final Logger logger = LoggerFactory.getLogger(ModulesListener.class);

    private final Set<String> modules = new HashSet<String>();

    private ModulesListener() {
        setWorkspace("default");
    }

    // Initialization on demand holder idiom: thread-safe singleton initialization
    private static class Holder {
        static final ModulesListener INSTANCE = new ModulesListener();
    }

    public static ModulesListener getInstance() {
        return Holder.INSTANCE;
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
