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

@Component(immediate = true)
public class Activator {

    private static final Logger logger = LoggerFactory.getLogger(Activator.class);

    // 100 is the "fully booted" marker, as defined in org.jahia.osgi.FrameworkService.finalFrameworkStartLevel.
    private static final int FULLY_BOOTED_START_LEVEL = 100;

    // Root of the packages that were moved from external-provider to external-provider-ui
    // (org.jahia.modules.external.admin.mount and .mount.validator). Consumers wired to the old
    // external-provider revision that used to export them must be re-wired to this bundle.
    private static final String MOVED_PACKAGE_ROOT = "org.jahia.modules.external.admin";

    /**
     * One-time migration hook: the {@value #MOVED_PACKAGE_ROOT} packages were moved out of the
     * external-provider bundle into this one. Consumers (e.g. external-provider-vfs) that were
     * wired to the old exporting revision need to be refreshed so the framework re-resolves them
     * against this bundle.
     *
     * <p>This must behave as a <em>migration step</em>, not a lifecycle step: it fires only while a
     * consumer is still wired to a foreign revision of those packages, and stays out of the way on
     * plain stop/start, redeploy, or cold boot. Triggering a refresh at the wrong moment competes
     * with the refresh the framework/module-manager is already running (OSGi serialises refreshes)
     * and deadlocks — see Jahia/jahia-private#5156.</p>
     *
     * <p>No persistent flag is needed: the live wiring graph is the source of truth. If no consumer
     * is wired to a foreign revision, the migration is already done and this method is a no-op,
     * re-evaluated safely on every activation.</p>
     */
    @Activate
    public void start(BundleContext context) {

        // Guard 1 — cold boot. The resolver wires everything from scratch; no manual refresh needed.
        int currentLevel = BundleLifecycleUtils.getFrameworkStartLevel();
        if (currentLevel < FULLY_BOOTED_START_LEVEL) {
            logger.info("System is still booting (start level {}). Skipping migration refresh.", currentLevel);
            return;
        }

        Bundle bundle = context.getBundle();
        String symbolicName = bundle.getSymbolicName();

        // Guard 2 — a version transition is still in flight. If another revision of this bundle is
        // present in any state, the module-manager has not finished the upgrade. Refreshing now
        // would race the framework's own refresh and deadlock. Step aside; the migration is
        // re-evaluated on the next activation, once this is the sole revision.
        boolean otherRevisionPresent = Arrays.stream(context.getBundles())
                .filter(b -> symbolicName.equals(b.getSymbolicName()))
                .anyMatch(b -> b.getBundleId() != bundle.getBundleId());
        if (otherRevisionPresent) {
            logger.info("Another revision of {} is still present; deferring migration until the upgrade settles.", symbolicName);
            return;
        }

        // Sole revision, fully booted: refresh only the consumers still wired to a foreign revision
        // of the moved packages (i.e. the old external-provider exporter, not this bundle).
        Set<Bundle> toRefresh = Arrays.stream(context.getBundles())
                .filter(b -> b.getBundleId() != bundle.getBundleId())
                .filter(b -> b.getState() == Bundle.RESOLVED || b.getState() == Bundle.ACTIVE)
                .filter(b -> isWiredToForeignRevision(b, bundle))
                .collect(Collectors.toSet());

        if (toRefresh.isEmpty()) {
            logger.info("No consumers wired to a foreign revision of the moved packages. Migration already complete; nothing to refresh.");
            return;
        }

        logger.info("Refreshing {} consumer bundle(s) still wired to a foreign revision of {}:", toRefresh.size(), MOVED_PACKAGE_ROOT);
        toRefresh.forEach(b -> logger.info("  Refreshing bundle - {}:{}", b.getSymbolicName(), b.getBundleId()));
        BundleLifecycleUtils.refreshBundles(toRefresh);
    }

    /**
     * Returns {@code true} if {@code candidate} has a resolved package wire rooted at
     * {@link #MOVED_PACKAGE_ROOT} whose provider is a bundle other than {@code currentBundle}.
     * Such a wire points at a foreign (stale) revision that must be re-wired to this bundle.
     *
     * <p>Uses {@link BundleWiring#getRequiredWires(String)} — the resolver's live, authoritative
     * view — rather than matching {@code Import-Package} manifest headers as raw strings, which
     * gives false positives and cannot tell <em>who</em> provides the package.</p>
     */
    private static boolean isWiredToForeignRevision(Bundle candidate, Bundle currentBundle) {
        BundleWiring wiring = candidate.adapt(BundleWiring.class);
        // adapt() returns null when the bundle is INSTALLED or UNRESOLVED (no wiring exists yet).
        if (wiring == null) {
            return false;
        }

        List<BundleWire> requiredWires = wiring.getRequiredWires("osgi.wiring.package");
        if (requiredWires == null) {
            return false;
        }

        return requiredWires.stream().anyMatch(wire -> {
            // The standard OSGi package capability attribute name equals the namespace itself.
            Object pkgAttr = wire.getCapability().getAttributes().get("osgi.wiring.package");
            if (!(pkgAttr instanceof String)) {
                return false;
            }
            String pkg = (String) pkgAttr;

            // Match the exact root and any sub-package (e.g. .mount, .mount.validator).
            if (!pkg.equals(MOVED_PACKAGE_ROOT) && !pkg.startsWith(MOVED_PACKAGE_ROOT + ".")) {
                return false;
            }

            // Foreign when the wire resolves to any bundle other than this (newest) one.
            BundleWiring providerWiring = wire.getProviderWiring();
            return providerWiring != null && !currentBundle.equals(providerWiring.getBundle());
        });
    }
}
