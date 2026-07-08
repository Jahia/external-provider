package org.jahia.modules.external.admin;

import org.jahia.osgi.BundleLifecycleUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component(immediate = true)
public class Activator {

    private static final Logger logger = LoggerFactory.getLogger(Activator.class);

    // 100 is the "fully booted" marker, as defined in org.jahia.osgi.FrameworkService.finalFrameworkStartLevel.
    private static final int FULLY_BOOTED_START_LEVEL = 100;

    private static final String PACKAGE_NAMESPACE = "osgi.wiring.package";
    private static final String IMPORT_PACKAGE_HEADER = "Import-Package";

    // Root of the packages that were moved from external-provider to external-provider-ui
    // (org.jahia.modules.external.admin.mount and .mount.validator). Consumers previously wired to
    // the external-provider revision that exported them must be re-wired to this bundle.
    private static final String MOVED_PACKAGE_ROOT = "org.jahia.modules.external.admin";

    /**
     * One-time migration hook: the {@value #MOVED_PACKAGE_ROOT} packages were moved out of the
     * external-provider bundle into this one. Consumers (e.g. external-provider-vfs) must end up
     * wired to this bundle for those packages.
     *
     * <p>This must behave as a <em>migration step</em>, not a lifecycle step. The decision is taken
     * per consumer, from the live wiring graph, so it is self-correcting and re-evaluated on every
     * activation (no persistent flag). For each bundle that depends on the moved packages:</p>
     * <ul>
     *   <li>wired to <b>this</b> bundle → nothing to do;</li>
     *   <li>wired to <b>another revision of this module</b> (same symbolic name, different bundle id)
     *       → nothing to do: an upgrade is in flight and the module manager owns the re-wire (its own
     *       refresh cascade re-resolves consumers). Refreshing here would race that in-flight refresh
     *       and deadlock — see Jahia/jahia-private#5156. Leaving it alone is what keeps this safe;</li>
     *   <li>wired to a <b>foreign</b> bundle (the old external-provider exporter) → refresh, so the
     *       framework re-resolves it against this bundle;</li>
     *   <li>not wired at all but its manifest <b>declares</b> the import → refresh: this catches an
     *       optional import that did not resolve because this bundle was installed after the consumer
     *       (there is no wire to inspect in that case).</li>
     * </ul>
     *
     * <p>Because a consumer wired to any revision of this module (this one or an older sibling still
     * present during an upgrade) is left untouched, plain stop/start, redeploy and cold boot are
     * no-ops, and version transitions are deferred to the module manager without a separate guard.</p>
     */
    @Activate
    public void start(BundleContext context) {

        // Cold boot: the resolver wires everything from scratch; no manual refresh needed.
        int currentLevel = BundleLifecycleUtils.getFrameworkStartLevel();
        if (currentLevel < FULLY_BOOTED_START_LEVEL) {
            logger.info("System is still booting (start level {}). Skipping migration refresh.", currentLevel);
            return;
        }

        Bundle bundle = context.getBundle();
        String symbolicName = bundle.getSymbolicName();

        Set<Bundle> toRefresh = Arrays.stream(context.getBundles())
                .filter(b -> b.getBundleId() != bundle.getBundleId())
                .filter(b -> b.getState() == Bundle.RESOLVED || b.getState() == Bundle.ACTIVE)
                .filter(b -> needsRewireToThisBundle(b, symbolicName))
                .collect(Collectors.toSet());

        if (toRefresh.isEmpty()) {
            logger.info("No consumers depend on the moved packages without being wired to {}. Migration already complete; nothing to refresh.", symbolicName);
            return;
        }

        logger.info("Refreshing {} consumer bundle(s) to re-wire the moved packages onto {}:", toRefresh.size(), symbolicName);
        toRefresh.forEach(b -> logger.info("  Refreshing bundle - {}:{}", b.getSymbolicName(), b.getBundleId()));
        BundleLifecycleUtils.refreshBundles(toRefresh);
    }

    /**
     * Decides, from {@code candidate}'s live wiring for the moved packages, whether it must be
     * refreshed to (re-)wire onto this module. See {@link #start(BundleContext)} for the rules.
     *
     * @param ourSymbolicName the symbolic name of this module — any revision of it (this bundle or an
     *                        older sibling still present during an upgrade) counts as "the family".
     */
    private static boolean needsRewireToThisBundle(Bundle candidate, String ourSymbolicName) {
        boolean wiredToFamily = false;
        boolean wiredToForeign = false;

        List<BundleWire> wires = movedPackageWires(candidate).collect(Collectors.toList());
        for (BundleWire wire : wires) {
            BundleWiring providerWiring = wire.getProviderWiring();
            if (providerWiring == null) {
                continue;
            }
            if (ourSymbolicName.equals(providerWiring.getBundle().getSymbolicName())) {
                wiredToFamily = true;
            } else {
                wiredToForeign = true;
            }
        }

        // Wired to us or to an older revision of us → leave it: the module manager owns the re-wire
        // during a version transition. This subsumes a "defer while another revision is present" guard.
        if (wiredToFamily) {
            return false;
        }
        // Wired to a foreign (stale) revision → refresh so it re-resolves against this bundle.
        if (wiredToForeign) {
            return true;
        }
        // No wire at all: refresh only if it declares the import (e.g. an optional import that did not
        // resolve because this bundle was installed after the consumer).
        return declaresMovedPackageImport(candidate);
    }

    /**
     * {@code true} if {@code candidate} declares an {@code Import-Package} of the moved packages in
     * its manifest — regardless of whether that import is currently wired. Complements the wire
     * inspection: it catches consumers whose (optional) import is unresolved and has no wire.
     */
    private static boolean declaresMovedPackageImport(Bundle candidate) {
        Object importPackage = candidate.getHeaders().get(IMPORT_PACKAGE_HEADER);
        return importPackage instanceof String && ((String) importPackage).contains(MOVED_PACKAGE_ROOT);
    }

    /**
     * Resolved package wires of {@code candidate} whose exported package is rooted at
     * {@link #MOVED_PACKAGE_ROOT} (the exact root or any sub-package, e.g. {@code .mount}).
     * Uses {@link BundleWiring#getRequiredWires(String)} — the resolver's live, authoritative view.
     */
    private static Stream<BundleWire> movedPackageWires(Bundle candidate) {
        BundleWiring wiring = candidate.adapt(BundleWiring.class);
        // adapt() returns null when the bundle is INSTALLED/UNRESOLVED (no wiring yet).
        if (wiring == null) {
            return Stream.empty();
        }
        List<BundleWire> requiredWires = wiring.getRequiredWires(PACKAGE_NAMESPACE);
        if (requiredWires == null) {
            return Stream.empty();
        }
        return requiredWires.stream().filter(wire -> {
            Object pkgAttr = wire.getCapability().getAttributes().get(PACKAGE_NAMESPACE);
            if (!(pkgAttr instanceof String)) {
                return false;
            }
            String pkg = (String) pkgAttr;
            return pkg.equals(MOVED_PACKAGE_ROOT) || pkg.startsWith(MOVED_PACKAGE_ROOT + ".");
        });
    }
}
