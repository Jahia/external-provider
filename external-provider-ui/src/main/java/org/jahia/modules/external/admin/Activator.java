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
     * <p>This must behave as a <em>migration step</em>, not a lifecycle step. Two things make it safe:</p>
     * <ul>
     *   <li><b>Guard against transitions.</b> While another revision of this bundle is still
     *       {@code RESOLVED}/{@code ACTIVE}, an upgrade is in flight and the module manager owns the
     *       re-wire (its own refresh cascade re-resolves consumers). Refreshing here would race that
     *       in-flight refresh and deadlock — see Jahia/jahia-private#5156. We step aside and let it
     *       settle; the migration is re-evaluated, safely, on the next activation.</li>
     *   <li><b>No spurious work.</b> A consumer already wired to <em>this</em> bundle is left alone,
     *       so plain stop/start, redeploy and cold boot are no-ops. Only consumers that depend on the
     *       moved packages but are <em>not</em> wired to us are refreshed.</li>
     * </ul>
     *
     * <p>A consumer needs a refresh when it depends on the moved packages yet is not wired to this
     * bundle — detected two complementary ways: a live wire pointing at a foreign (stale) revision,
     * or a declared {@code Import-Package} that is currently unwired (e.g. an optional import that
     * did not resolve because this bundle was installed after the consumer). No persistent flag is
     * needed: the live wiring graph is the source of truth, re-evaluated on every activation.</p>
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
        // RESOLVED or ACTIVE, the module manager has not finished the upgrade; refreshing now would
        // race its in-flight refresh and deadlock. Step aside — re-evaluated on the next activation.
        boolean otherRevisionPresent = Arrays.stream(context.getBundles())
                .filter(b -> symbolicName.equals(b.getSymbolicName()))
                .filter(b -> b.getBundleId() != bundle.getBundleId())
                .anyMatch(b -> b.getState() == Bundle.RESOLVED || b.getState() == Bundle.ACTIVE);
        if (otherRevisionPresent) {
            logger.info("Another revision of {} is still present; deferring migration until the upgrade settles.", symbolicName);
            return;
        }

        // Sole revision, fully booted: refresh the consumers that depend on the moved packages but
        // are not wired to this bundle (foreign/stale wire, or a declared-but-unwired import).
        Set<Bundle> toRefresh = Arrays.stream(context.getBundles())
                .filter(b -> b.getBundleId() != bundle.getBundleId())
                .filter(b -> b.getState() == Bundle.RESOLVED || b.getState() == Bundle.ACTIVE)
                .filter(b -> needsRewireToThisBundle(b, bundle))
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
     * A consumer needs re-wiring when it depends on the moved packages but is not (yet) wired to
     * {@code currentBundle}. Already-correctly-wired consumers are skipped so redeploy/stop-start
     * stay no-ops.
     */
    private static boolean needsRewireToThisBundle(Bundle candidate, Bundle currentBundle) {
        if (isWiredToThisBundle(candidate, currentBundle)) {
            return false;
        }
        // Not wired to us: refresh if it has a live wire to a foreign revision, or if it declares
        // an import of the moved packages that is currently unwired (covers optional imports that
        // did not resolve because this bundle was installed after the consumer).
        return isWiredToForeignRevision(candidate, currentBundle) || declaresMovedPackageImport(candidate);
    }

    /**
     * {@code true} if {@code candidate} has a resolved package wire rooted at
     * {@link #MOVED_PACKAGE_ROOT} whose provider is {@code target}.
     */
    private static boolean isWiredToThisBundle(Bundle candidate, Bundle target) {
        return movedPackageWires(candidate).anyMatch(wire -> {
            BundleWiring providerWiring = wire.getProviderWiring();
            return providerWiring != null && target.equals(providerWiring.getBundle());
        });
    }

    /**
     * {@code true} if {@code candidate} has a resolved package wire rooted at
     * {@link #MOVED_PACKAGE_ROOT} whose provider is a bundle other than {@code currentBundle}
     * (i.e. a stale revision). Uses the resolver's live view rather than manifest strings.
     */
    private static boolean isWiredToForeignRevision(Bundle candidate, Bundle currentBundle) {
        return movedPackageWires(candidate).anyMatch(wire -> {
            BundleWiring providerWiring = wire.getProviderWiring();
            return providerWiring != null && !currentBundle.equals(providerWiring.getBundle());
        });
    }

    /**
     * {@code true} if {@code candidate} declares an {@code Import-Package} of the moved packages in
     * its manifest — regardless of whether that import is currently wired. This complements the wire
     * inspection: it catches consumers whose (optional) import is unresolved and therefore has no
     * wire to inspect.
     */
    private static boolean declaresMovedPackageImport(Bundle candidate) {
        Object importPackage = candidate.getHeaders().get(IMPORT_PACKAGE_HEADER);
        return importPackage instanceof String && ((String) importPackage).contains(MOVED_PACKAGE_ROOT);
    }

    /**
     * Resolved package wires of {@code candidate} whose exported package is rooted at
     * {@link #MOVED_PACKAGE_ROOT} (the exact root or any sub-package, e.g. {@code .mount}).
     */
    private static java.util.stream.Stream<BundleWire> movedPackageWires(Bundle candidate) {
        BundleWiring wiring = candidate.adapt(BundleWiring.class);
        // adapt() returns null when the bundle is INSTALLED/UNRESOLVED (no wiring yet).
        if (wiring == null) {
            return java.util.stream.Stream.empty();
        }
        List<BundleWire> requiredWires = wiring.getRequiredWires(PACKAGE_NAMESPACE);
        if (requiredWires == null) {
            return java.util.stream.Stream.empty();
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
