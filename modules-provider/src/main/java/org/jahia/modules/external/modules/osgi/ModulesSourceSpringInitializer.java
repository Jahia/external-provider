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
package org.jahia.modules.external.modules.osgi;

import org.apache.commons.beanutils.BeanUtils;
import org.jahia.bin.listeners.JahiaContextLoaderListener;
import org.jahia.data.templates.JahiaTemplatesPackage;
import org.jahia.exceptions.JahiaInitializationException;
import org.jahia.modules.external.ExternalContentStoreProvider;
import org.jahia.modules.external.modules.ModulesDataSource;
import org.jahia.modules.external.modules.ModulesUtils;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.SpringContextSingleton;
import org.jahia.services.content.JCRStoreProvider;
import org.jahia.services.content.JCRStoreService;
import org.jahia.services.templates.SourceControlFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service to mount/unmount module sources
 */
@Component(immediate = true)
public class ModulesSourceSpringInitializer implements SynchronousBundleListener {
    private static final Logger logger = LoggerFactory.getLogger(ModulesSourceSpringInitializer.class);
    private BundleContext context;
    private JCRStoreService jcrStoreService;
    private Map<String, ModulesSourceHttpServiceTracker> httpServiceTrackers = new HashMap<String, ModulesSourceHttpServiceTracker>();
    private final List<ModulesSourceMonitor> sourceMonitors = new ArrayList<>();

    @Reference
    public void setJcrStoreService(JCRStoreService jcrStoreService) {
        this.jcrStoreService = jcrStoreService;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    public void bindSourceMonitors(ModulesSourceMonitor sourceMonitor) {
        this.sourceMonitors.add(sourceMonitor);
    }

    public void unbindSourceMonitors(ModulesSourceMonitor sourceMonitor) {
        this.sourceMonitors.remove(sourceMonitor);
    }

    @Activate
    public void start(BundleContext context) {
        if (this.context == null) {
            this.context = context;
        }
        logger.info("Starting external provider bundle.");

        context.addBundleListener(this);
        Bundle[] bundles = context.getBundles();
        for (Bundle bundle : bundles) {
            if (bundle.getState() == Bundle.ACTIVE) {
                mountBundle(bundle);
            }
        }

    }

    /**
     * Called when this bundle is stopped so the Framework can perform the
     * bundle-specific activities necessary to stop the bundle. In general, this
     * method should undo the work that the <code>BundleActivator.start</code>
     * method started. There should be no active threads that were started by
     * this bundle when this bundle returns. A stopped bundle must not call any
     * Framework objects.
     * <br>
     * <br>
     * This method must complete and return to its caller in a timely manner.
     *
     * @param context The execution context of the bundle being stopped.
     */
    @Deactivate
    public void stop(BundleContext context) {
        if (!JahiaContextLoaderListener.isRunning()) {
            return;
        }
        context.removeBundleListener(this);
        Bundle[] bundles = context.getBundles();
        for (Bundle bundle : bundles) {
            if (bundle.getState() == Bundle.ACTIVE || context.getBundle().getBundleId() == bundle.getBundleId()) {
                unmountBundle(bundle);
            }
        }
    }

    public void bundleChanged(BundleEvent event) {
        if (event.getType() == BundleEvent.STARTED) {
            mountBundle(event.getBundle());
        } else if (event.getType() == BundleEvent.STOPPING) {
            unmountBundle(event.getBundle());
        }
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
                    ModulesDataSource dataSource = new ModulesDataSource();
                    dataSource.setSourceControlFactory((SourceControlFactory) SpringContextSingleton.getBean("SourceControlFactory"));
                    dataSource.setSupportedNodeTypes(new HashSet<>(Arrays.asList(ModulesDataSource.SUPPORTED_NODE_TYPES)));
                    dataSource.setFolderTypeMapping(Arrays.stream(ModulesDataSource.FOLDER_TYPE_MAPPING).collect(Collectors.toMap(s -> s[0], s -> s[1])));
                    dataSource.setFileTypeMapping(Arrays.stream(ModulesDataSource.FILE_TYPE_MAPPING).collect(Collectors.toMap(s -> s[0], s -> s[1])));
                    dataSource.setJcrStoreService(jcrStoreService);
                    dataSource.setModulesSourceSpringInitializer(this);
                    dataSource.setSourceMonitors(sourceMonitors);

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

}
