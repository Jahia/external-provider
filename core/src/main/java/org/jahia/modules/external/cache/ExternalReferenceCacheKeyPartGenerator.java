package org.jahia.modules.external.cache;

import org.jahia.modules.external.ExternalContentStoreProvider;
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
        return "CMISObject";
    }

    @Override
    public String getValue(Resource resource, RenderContext renderContext, Properties properties) {
        try {
            if (resource.getNode().isNodeType("jmix:nodeReference")) {
                String uuid = resource.getNode().getProperty("j:node").getString();
                for (JCRStoreProvider p : JCRStoreService.getInstance().getSessionFactory().getProviderList()) {
                    if (p instanceof ExternalContentStoreProvider && ((ExternalContentStoreProvider) p).isCacheKeyOnReferenceSupport() && uuid.startsWith(((ExternalContentStoreProvider) p).getId())) {
                        return resource.getNode().getProperty("j:node").getString();
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
