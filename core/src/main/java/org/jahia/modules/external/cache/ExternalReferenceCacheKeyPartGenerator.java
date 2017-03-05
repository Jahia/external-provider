/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2017 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.modules.external.cache;

import org.jahia.modules.external.ExternalContentStoreProvider;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRStoreProvider;
import org.jahia.services.content.JCRStoreService;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.filter.cache.CacheKeyPartGenerator;

import javax.jcr.RepositoryException;
import java.util.Properties;

/**
 * Cache key part generator that add an entry for referenced content from an external source
 * it tests if they are readable or not for a user
 */
public class ExternalReferenceCacheKeyPartGenerator implements CacheKeyPartGenerator {

    @Override
    public String getKey() {
        return "refToExternalContent";
    }

    @Override
    public String getValue(Resource resource, RenderContext renderContext, Properties properties) {
        try {
            JCRNodeWrapper resourceNode = resource.getNode();
            if (resourceNode.isNodeType("jmix:nodeReference") && resourceNode.hasProperty("j:node")) {
                String uuid = resourceNode.getProperty("j:node").getString();
                for (JCRStoreProvider p : JCRStoreService.getInstance().getSessionFactory().getProviderList()) {
                    if (p instanceof ExternalContentStoreProvider && ((ExternalContentStoreProvider) p).isCacheKeyOnReferenceSupport() && uuid.startsWith(((ExternalContentStoreProvider) p).getId())) {
                        return uuid;
                    }
                }
            }
        } catch (RepositoryException e) {
            return "";
        }
        return "";
    }

    @Override
    public String replacePlaceholders(RenderContext renderContext, String s) {
        if (s.equals("")) {
            return "";
        }
        try {
            renderContext.getMainResource().getNode().getSession().getNodeByIdentifier(s);
        } catch (RepositoryException e) {
            return "0";
        }
        return "1";
    }
}
