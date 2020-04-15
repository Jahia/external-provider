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
package org.jahia.modules.external.modules.osgi;

import org.apache.commons.beanutils.BeanUtils;
import org.eclipse.gemini.blueprint.context.BundleContextAware;
import org.jahia.data.templates.JahiaTemplatesPackage;
import org.jahia.exceptions.JahiaInitializationException;
import org.jahia.modules.external.ExternalContentStoreProvider;
import org.jahia.modules.external.modules.ModulesUtils;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.JahiaAfterInitializationService;
import org.jahia.services.SpringContextSingleton;
import org.jahia.services.content.JCRStoreProvider;
import org.jahia.services.content.JCRStoreService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Service to mount/unmount module sources
 */
public class ModulesSourceSpringInitializer implements JahiaAfterInitializationService, BundleContextAware {
    private static final Logger logger = LoggerFactory.getLogger(ModulesSourceSpringInitializer.class);
    private BundleContext context;
    private static ModulesSourceSpringInitializer instance;
    private JCRStoreService jcrStoreService;
    private Map<String, ModulesSourceHttpServiceTracker> httpServiceTrackers = new HashMap<String, ModulesSourceHttpServiceTracker>();

    public static synchronized ModulesSourceSpringInitializer getInstance() {
        if (instance == null) {
            instance = new ModulesSourceSpringInitializer();
        }
        return instance;
    }

    @Override
    public void initAfterAllServicesAreStarted() throws JahiaInitializationException {
        try {
            logger.info("All services are started. Started mounting modules sources.");
            Bundle[] bundles = context.getBundles();
            for (Bundle bundle : bundles) {
                if (bundle.getState() == Bundle.ACTIVE) {
                    mountBundle(bundle);
                }
            }
            logger.info("Done mounting modules sources.");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void setBundleContext(BundleContext bundleContext) {
        this.context = bundleContext;
    }

    /**
     * Mounts the sources provider for the specified module package.
     *
     * @param templatePackage the module package to mount sources for
     * @return <code>true</code> if the sources provider was successfully mounted or is already mounted
     */
    public boolean mountSourcesProvider(JahiaTemplatesPackage templatePackage) {
        if (context != null) {
            String providerKey = ModulesUtils.getSourcesProviderKey(templatePackage);
            JCRStoreProvider provider = jcrStoreService.getSessionFactory().getProviders().get(providerKey);
            if (provider == null) {
                try {
                    Object dataSource = SpringContextSingleton.getBeanInModulesContext("ModulesDataSourcePrototype");
                    logger.info("Mounting source for bundle {}", templatePackage.getName());
                    Map<String, Object> properties = new LinkedHashMap<String, Object>();
                    properties.put("root", templatePackage.getSourcesFolder().toURI().toString());
                    properties.put("module", templatePackage);

                    BeanUtils.populate(dataSource, properties);

                    ExternalContentStoreProvider ex = (ExternalContentStoreProvider) SpringContextSingleton.getBeanInModulesContext(
                            "ExternalStoreProviderPrototype");
                    properties.clear();
                    properties.put("key", providerKey);
                    properties.put("mountPoint", "/modules/" + templatePackage.getIdWithVersion() + "/sources");
                    properties.put("dataSource", dataSource);
                    properties.put("lockSupport", true);
                    properties.put("slowConnection", false);

                    BeanUtils.populate(ex, properties);

                    ex.start();

                    return true;
                } catch (IllegalAccessException e) {
                    logger.error(e.getMessage(), e);
                } catch (InvocationTargetException e) {
                    logger.error(e.getMessage(), e);
                } catch (JahiaInitializationException e) {
                    logger.error(e.getMessage(), e);
                } catch (NoSuchBeanDefinitionException e) {
                    logger.debug(e.getMessage(), e);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            } else {
                return true;
            }
        }

        return false;
    }

    public void unmountSourcesProvider(JahiaTemplatesPackage templatePackage) {
        if (context != null) {
            JCRStoreProvider provider = jcrStoreService.getSessionFactory().getProviders().get(ModulesUtils.getSourcesProviderKey(templatePackage));
            if (provider != null) {
                logger.info("Unmounting source for bundle {}", templatePackage.getName());
                provider.stop();
            }
        }
    }

    public void mountBundle(Bundle bundle) {
        final JahiaTemplatesPackage pkg = BundleUtils.isJahiaModuleBundle(bundle) ? BundleUtils.getModule(
                bundle) : null;
        if (null != pkg && pkg.getSourcesFolder() != null) {
            if (mountSourcesProvider(pkg) && !httpServiceTrackers.containsKey(bundle.getSymbolicName())) {
                ModulesSourceHttpServiceTracker modulesSourceHttpServiceTracker = new ModulesSourceHttpServiceTracker(pkg);
                modulesSourceHttpServiceTracker.open(true);
                httpServiceTrackers.put(bundle.getSymbolicName(), modulesSourceHttpServiceTracker);
            }
        }
    }

    public void unmountBundle(Bundle bundle) {
        final JahiaTemplatesPackage pkg = BundleUtils.isJahiaModuleBundle(bundle) ? BundleUtils.getModule(
                bundle) : null;
        try {
            if (null != pkg && pkg.getSourcesFolder() != null) {
                unmountSourcesProvider(pkg);
            }
        } catch (Exception e) {
            logger.error("Cannot unmount sources provider for " + pkg.getId(), e);
        }
        ModulesSourceHttpServiceTracker httpServiceTracker = httpServiceTrackers.remove(bundle.getSymbolicName());
        if (httpServiceTracker != null) {
            httpServiceTracker.close();
        }
    }

    public ModulesSourceHttpServiceTracker getHttpServiceTracker(String bundleSymbolicName) {
        return httpServiceTrackers.get(bundleSymbolicName);
    }

    public void setJcrStoreService(JCRStoreService jcrStoreService) {
        this.jcrStoreService = jcrStoreService;
    }
}