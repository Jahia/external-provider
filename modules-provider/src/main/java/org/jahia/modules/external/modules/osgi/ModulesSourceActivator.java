/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2019 Jahia Solutions Group SA. All rights reserved.
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

import org.jahia.bin.listeners.JahiaContextLoaderListener;
import org.osgi.framework.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Activator for modules data provider.
 * Mount and unmount sources at startup/stop of modules
 */
public class ModulesSourceActivator implements BundleActivator {
    private static Logger logger = LoggerFactory.getLogger(ModulesSourceActivator.class);
    private BundleContext context;
    private SynchronousBundleListener bundleListener;


    /**
     * Called when this bundle is started so the Framework can perform the
     * bundle-specific activities necessary to start this bundle. This method
     * can be used to register services or to allocate any resources that this
     * bundle needs.
     * <br>
     * <br>
     * This method must complete and return to its caller in a timely manner.
     *
     * @param context The execution context of the bundle being started.
     */
    @Override
    public void start(BundleContext context) {
        if (this.context == null) {
            this.context = context;
        }
        logger.info("Starting external provider bundle.");
        bundleListener = new SynchronousBundleListener() {
            @Override
            public void bundleChanged(BundleEvent event) {
                if (event.getType() == BundleEvent.STARTED) {
                    ModulesSourceSpringInitializer.getInstance().mountBundle(event.getBundle());
                } else if (event.getType() == BundleEvent.STOPPING) {
                    ModulesSourceSpringInitializer.getInstance().unmountBundle(event.getBundle());
                }
            }
        };
        context.addBundleListener(bundleListener);
        Bundle[] bundles = context.getBundles();
        for (Bundle bundle : bundles) {
            if (bundle.getState() == Bundle.ACTIVE) {
                ModulesSourceSpringInitializer.getInstance().mountBundle(bundle);
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
    @Override
    public void stop(BundleContext context) {
        if (!JahiaContextLoaderListener.isRunning()) {
            return;
        }
        if (bundleListener != null) {
            context.removeBundleListener(bundleListener);
        }
        bundleListener = null;
        Bundle[] bundles = context.getBundles();
        for (Bundle bundle : bundles) {
            if (bundle.getState() == Bundle.ACTIVE || context.getBundle().getBundleId() == bundle.getBundleId()) {
                ModulesSourceSpringInitializer.getInstance().unmountBundle(bundle);
            }
        }
    }

}
