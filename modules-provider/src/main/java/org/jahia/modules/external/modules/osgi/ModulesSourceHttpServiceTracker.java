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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang.StringUtils;
import org.jahia.api.Constants;
import org.jahia.bundles.extender.jahiamodules.BundleHttpResourcesTracker;
import org.jahia.data.templates.JahiaTemplatesPackage;
import org.jahia.modules.external.ExternalContentStoreProvider;
import org.jahia.modules.external.modules.ModulesUtils;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.SpringContextSingleton;
import org.jahia.services.content.JCRStoreService;
import org.jahia.services.render.scripting.bundle.BundleScriptResolver;
import org.jahia.services.templates.TemplatePackageRegistry;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;

import javax.jcr.PathNotFoundException;

public class ModulesSourceHttpServiceTracker extends ServiceTracker<HttpService, HttpService> {
    private static final Logger logger = LoggerFactory.getLogger(ModulesSourceHttpServiceTracker.class);

    private final Bundle bundle;
    private final String bundleName;
    private final JahiaTemplatesPackage module;
    private final BundleScriptResolver bundleScriptResolver;
    private final TemplatePackageRegistry templatePackageRegistry;
    private HttpService httpService;

    /**
     * Tracker for resource modifications
     * @param module the module package
     */
    public ModulesSourceHttpServiceTracker(JahiaTemplatesPackage module) {
        super(module.getBundle().getBundleContext(), HttpService.class.getName(), null);
        this.bundle = module.getBundle();
        this.bundleName = BundleUtils.getDisplayName(bundle);
        this.module = module;
        this.bundleScriptResolver = (BundleScriptResolver) SpringContextSingleton.getBean("BundleScriptResolver");
        this.templatePackageRegistry = (TemplatePackageRegistry) SpringContextSingleton.getBean("org.jahia.services.templates.TemplatePackageRegistry");
    }

    @Override
    public HttpService addingService(ServiceReference<HttpService> reference) {
        HttpService httpService = super.addingService(reference);
        this.httpService = httpService;

        registerMissingResources();

        return httpService;
    }

    /**
     * register the resource as a View
     * @param resource to register
     */
    public void registerResource(File resource) {
        String filePath = getResourcePath(resource);
        String fileServletAlias = "/" + bundle.getSymbolicName() + filePath;
        httpService.unregister(fileServletAlias);
        bundleScriptResolver.addBundleScript(bundle, filePath);
        templatePackageRegistry.addModuleWithViewsForComponent(StringUtils.substringBetween(filePath, "/", "/"), module);
        logger.debug("Register file {} in bundle {}", filePath, bundleName);
   }

    /**
     * Unregister the resource, remove it from the available Views
     * @param file is the resource to unregister
     */
    public void unregisterResouce(File file) {
        String filePath = getResourcePath(file);
        String fileServletAlias = "/" + bundle.getSymbolicName() + filePath;

        if (bundle.getEntry(filePath) != null) {
            // A jsp is still present in the bundle, don't unregister it
            return;
        }

        httpService.unregister(fileServletAlias);
        bundleScriptResolver.removeBundleScript(bundle, filePath);
        String propertiesFileName = FilenameUtils.removeExtension(file.getName()) + ".properties";
        File parentFile = file.getParentFile();
        File[] matching = parentFile != null ? parentFile.listFiles((FilenameFilter) FileFilterUtils.notFileFilter(FileFilterUtils.nameFileFilter(propertiesFileName))) : null;
        if (matching == null || matching.length == 0) {
            templatePackageRegistry.removeModuleWithViewsForComponent(StringUtils.substringBetween(filePath, "/", "/"), module);
        }
        logger.debug("Unregister file {} in bundle {}", filePath, bundleName);
    }

    /**
     * Register all resources from the current bundle
     */
    public void registerMissingResources() {
        File sourcesFolder = module.getSourcesFolder();
        if (sourcesFolder == null) {
            return;
        }
        File resourcesRoot = new File(sourcesFolder, "src/main/resources");
        if (!resourcesRoot.exists()) {
            return;
        }

        ExternalContentStoreProvider storeProvider = (ExternalContentStoreProvider) JCRStoreService.getInstance()
                .getSessionFactory().getProviders()
                .get(ModulesUtils.getSourcesProviderKey(module));

        if (storeProvider == null) {
            // the sources are not mounted
            return;
        }

        for (File resource : FileUtils.listFiles(resourcesRoot, new WildcardFileFilter("*"), TrueFileFilter.INSTANCE)) {
            String resourcePath = getResourcePath(resource);
            if (bundle.getResource(resourcePath) == null) {
                // resource is not present in the compiled module: check if it is a view file to be registered
                if (isViewFile(resourcePath, storeProvider)) {
                    registerResource(resource);
                }
            }
        }
    }

    private static boolean isViewFile(String resourcePath, ExternalContentStoreProvider storeProvider) {
        try {
            return StringUtils.equals(Constants.JAHIANT_VIEWFILE,
                    storeProvider.getDataSource().getItemByPath("src/main/resources" + resourcePath).getType());
        } catch (PathNotFoundException e) {
            // no such item
        }
        return false;
    }

    /**
     * flush the compiled jsp
     * @param jsp to flush
     */
    public void flushJspCache(File jsp) {
        String jspPath = getResourcePath(jsp);
        BundleHttpResourcesTracker.flushJspCache(bundle, jspPath);
    }

    protected String getResourcePath(File file) {
        return StringUtils.substringAfterLast(FilenameUtils.separatorsToUnix(file.getPath()), "/src/main/resources");
    }
}
