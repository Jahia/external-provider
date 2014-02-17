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

package org.jahia.modules.external.modules.osgi;

import com.phloc.commons.io.file.filter.FilenameFilterNotEquals;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.jahia.bundles.extender.jahiamodules.BundleHttpResourcesTracker;
import org.jahia.bundles.extender.jahiamodules.FileHttpContext;
import org.jahia.data.templates.JahiaTemplatesPackage;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.SpringContextSingleton;
import org.jahia.services.render.scripting.bundle.BundleScriptResolver;
import org.jahia.services.templates.TemplatePackageRegistry;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class ModulesSourceHttpServiceTracker extends ServiceTracker {
    private static Logger logger = LoggerFactory.getLogger(ModulesSourceHttpServiceTracker.class);

    private final Bundle bundle;
    private final String bundleName;
    private final JahiaTemplatesPackage module;
    private final BundleScriptResolver bundleScriptResolver;
    private final TemplatePackageRegistry templatePackageRegistry;
    private HttpService httpService;

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
        return httpService;
    }

    public void registerJsp(File jsp) {
        String jspPath = getJspPath(jsp);
        if (bundle.getEntry(jspPath) != null) {
            return;
        }
        unregisterJsp(jsp);
        String jspServletAlias = "/" + bundle.getSymbolicName() + jspPath;
        HttpContext httpContext = new FileHttpContext(FileHttpContext.getSourceURLs(bundle),
                httpService.createDefaultHttpContext());
        BundleHttpResourcesTracker.registerJspServlet(httpService, httpContext, bundle, bundleName, jspServletAlias, jspPath, null);
        bundleScriptResolver.addBundleScript(bundle, jspPath);
        templatePackageRegistry.addModuleWithViewsForComponent(StringUtils.substringBetween(jspPath, "/", "/"), module);
        if (logger.isDebugEnabled()) {
            logger.debug("Register JSP {} in bundle {}", jspPath, bundleName);
        }
    }

    public void unregisterJsp(File jsp) {
        String jspPath = getJspPath(jsp);
        String jspServletAlias = "/" + bundle.getSymbolicName() + jspPath;
        httpService.unregister(jspServletAlias);
        bundleScriptResolver.removeBundleScript(bundle, jspPath);
        String propertiesFileName = FilenameUtils.removeExtension(jsp.getName()) + ".properties";
        File parentFile = jsp.getParentFile();
        File[] matching = parentFile != null ? parentFile.listFiles(new FilenameFilterNotEquals(propertiesFileName)) : null;
        if (matching == null || matching.length == 0) {
            templatePackageRegistry.removeModuleWithViewsForComponent(StringUtils.substringBetween(jspPath, "/", "/"), module);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Unregister JSP {} in bundle {}", jspPath, bundleName);
        }
    }

    protected String getJspPath(File jsp) {
        return StringUtils.substringAfterLast(FilenameUtils.separatorsToUnix(jsp.getPath()),"/src/main/resources");
    }
}
