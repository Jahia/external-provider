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
