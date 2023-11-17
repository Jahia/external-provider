package org.jahia.modules.external.admin;

import org.jahia.osgi.BundleLifecycleUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component(immediate = true)
public class Activator {

    @Activate
    public void start(BundleContext context) {
        Set<Bundle> wiredBundles = context.getBundle().adapt(BundleWiring.class).getProvidedWires("osgi.wiring.package").stream().map(x -> x.getRequirer().getBundle()).collect(Collectors.toSet());
        Set<Bundle> toRefresh = Arrays.stream(context.getBundles())
                .filter(b -> b.getHeaders().get("Import-Package") != null && b.getHeaders().get("Import-Package").contains("org.jahia.modules.external.admin"))
                .filter(b -> !wiredBundles.contains(b))
                .collect(Collectors.toSet());
        if (!toRefresh.isEmpty()) {
            BundleLifecycleUtils.refreshBundles(toRefresh);
        }
    }
}
