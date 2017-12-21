/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2018 Jahia Solutions Group SA. All rights reserved.
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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang.StringUtils;
import org.jahia.bundles.extender.jahiamodules.BundleHttpResourcesTracker;
import org.jahia.data.templates.JahiaTemplatesPackage;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.SpringContextSingleton;
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

public class ModulesSourceHttpServiceTracker extends ServiceTracker {
    private static Logger logger = LoggerFactory.getLogger(ModulesSourceHttpServiceTracker.class);

    private final Bundle bundle;
    private final String bundleName;
    private final JahiaTemplatesPackage module;
    private final BundleScriptResolver bundleScriptResolver;
    private final TemplatePackageRegistry templatePackageRegistry;
    private HttpService httpService;

    /**
     * Tracker for resource modifications
     * @param module
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
    public Object addingService(ServiceReference reference) {
        HttpService httpService = (HttpService) super.addingService(reference);
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
        if (logger.isDebugEnabled()) {
            logger.debug("Register file {} in bundle {}", filePath, bundleName);
        }
   }

    /**
     * Unregister the resource, remove it from the availables Views
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
        if (logger.isDebugEnabled()) {
            logger.debug("Unregister file {} in bundle {}", filePath, bundleName);
        }
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
        // todo: use value of Jahia-Module-Scripting-Views header for the filter if available to register resources?
        // todo: avoid scanning if Jahia-Module-Has-Views header is set to no?
        for (File resource : FileUtils.listFiles(resourcesRoot, new WildcardFileFilter("*"), TrueFileFilter.INSTANCE)) {
            if (bundle.getResource(getResourcePath(resource)) == null) {
                registerResource(resource);
            }
        }
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
        return StringUtils.substringAfterLast(FilenameUtils.separatorsToUnix(file.getPath()),"/src/main/resources");
    }
}
