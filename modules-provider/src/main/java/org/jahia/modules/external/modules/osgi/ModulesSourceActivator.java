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
