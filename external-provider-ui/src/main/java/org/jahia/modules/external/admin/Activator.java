package org.jahia.modules.external.admin;

import org.jahia.osgi.BundleLifecycleUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component(immediate = true)
public class Activator {

    private static final Logger logger = LoggerFactory.getLogger(Activator.class);

    // Root package exported by this bundle. Sub-packages (e.g. .components, .impl) are also matched
    // by the wire inspection logic below so that any consumer of this API surface is detected.
    private static final String EXPORTED_PACKAGE = "org.jahia.modules.external.admin";

    @Activate
    public void start(BundleContext context) {

        // ── Guard 1: only act during hot-upgrades ────────────────────────────────────────────
        // During a cold boot the OSGi resolver wires all bundles from scratch; no manual refresh
        // is needed. Level 100 is the "fully booted" marker defined in FrameworkService.finalFrameworkStartLevel.
        // Anything below that means we are still in the initial start sequence.
        int currentLevel = BundleLifecycleUtils.getFrameworkStartLevel();
        if (currentLevel < 100) {
            logger.info("System is still booting (start level {}). Skipping hot-upgrade refresh.", currentLevel);
            return;
        }

        Bundle bundle = context.getBundle();
        Version myVersion = bundle.getVersion();
        String symbolicName = bundle.getSymbolicName();

        // ── Guard 2: highest active version wins ─────────────────────────────────────────────
        // In a Cellar-managed cluster, several versions of this bundle can be ACTIVE simultaneously
        // for a short window (e.g. v4.8.0 [178] and v4.10.0 [205] both running). Each activation
        // would otherwise bounce the same consumer bundles back and forth between revisions.
        //
        // Rule: only the highest ACTIVE version takes ownership of the refresh. An older revision
        // that activates after the new one is already running must step aside immediately.
        boolean newerVersionActive = Arrays.stream(context.getBundles())
                .filter(b -> symbolicName.equals(b.getSymbolicName()))
                .filter(b -> b.getBundleId() != bundle.getBundleId())  // exclude ourselves
                .filter(b -> b.getState() == Bundle.ACTIVE)
                .anyMatch(b -> b.getVersion().compareTo(myVersion) > 0);

        if (newerVersionActive) {
            logger.info("A newer active version of {} is already running. Skipping refresh for {}.", symbolicName, myVersion);
            return;
        }

        // ── Guard 3: one-time migration flag ─────────────────────────────────────────────────
        // This Activator exists to re-wire consumers after the org.jahia.modules.external.admin
        // package was moved into this bundle. That move is a one-time event on each node: once
        // consumers have been refreshed and are wired to this bundle, there is nothing left to do
        // on subsequent restarts or upgrades.
        //
        // The flag has no version in its name because the migration is node-scoped, not
        // version-scoped: once done it must not run again regardless of which future version
        // of this bundle activates next.
        //
        // Stored under karaf.data (not felix-cache/bundle<ID>/data/) so that a re-install of
        // any version (which gets a new bundle-ID and therefore a new Felix data directory)
        // still finds the flag.
        File migrationFlag = resolveMigrationFlag(context, symbolicName);

        if (migrationFlag != null && migrationFlag.exists()) {
            logger.debug("Package migration already completed on this node. Skipping.");
            return;
        }

        // ── Guard 4: older version still present — do not interfere ──────────────────────────
        // If an older version of this bundle is still RESOLVED or ACTIVE, the module manager has
        // not yet completed the transition. Triggering a refresh here would bounce consumer bundles
        // before the upgrade sequence is finished. The framework will re-wire consumers naturally
        // once the old version is uninstalled or refreshed by the module manager.
        boolean olderVersionPresent = Arrays.stream(context.getBundles())
                .filter(b -> symbolicName.equals(b.getSymbolicName()))
                .filter(b -> b.getBundleId() != bundle.getBundleId())
                .filter(b -> b.getVersion().compareTo(myVersion) < 0)
                .anyMatch(b -> b.getState() == Bundle.RESOLVED || b.getState() == Bundle.ACTIVE);

        if (olderVersionPresent) {
            logger.info("Older version(s) of {} still present. Skipping — module manager will complete the transition.", symbolicName);
            logger.info("Uninstall the previous version to re-wire the bundles that depend on {}.", symbolicName);

            // The previous version's Activator may have already re-wired consumers into the
            // external-provider-ui bundle family. Detect this by checking whether any consumer
            // still imports EXPORTED_PACKAGE from a bundle *outside* the family (i.e. from a
            // bundle that is not any version of external-provider-ui).
            // If no such outside wire exists, the migration is complete: write the flag now so
            // the next activation (after the old version is finally removed) skips immediately
            // without scanning or refreshing anything.
            Set<Long> familyIds = Arrays.stream(context.getBundles())
                    .filter(b -> symbolicName.equals(b.getSymbolicName()))
                    .map(Bundle::getBundleId)
                    .collect(Collectors.toSet());

            boolean migrationPending = Arrays.stream(context.getBundles())
                    .filter(b -> b.getState() == Bundle.RESOLVED || b.getState() == Bundle.ACTIVE)
                    .filter(b -> !familyIds.contains(b.getBundleId()))
                    .anyMatch(b -> isWiredOutsideFamily(b, familyIds));

            if (!migrationPending) {
                logger.info("Consumers are already wired within the {} family. Recording migration as complete.", symbolicName);
                writeMigrationFlag(migrationFlag, symbolicName);
            }

            return;
        }

        // ── Refresh stale consumers ───────────────────────────────────────────────────────────
        // All guards passed: this is the sole active version and the system is fully booted.
        // The old version may have been uninstalled but not yet refreshed by the framework,
        // leaving consumers (e.g. external-provider-vfs) still wired to the old bundle's
        // package exports. Find them via live wire inspection and refresh them so the framework
        // re-resolves them against this bundle.
        Set<Bundle> toRefresh = Arrays.stream(context.getBundles())
                .filter(b -> b.getState() == Bundle.RESOLVED || b.getState() == Bundle.ACTIVE)
                .filter(b -> b.getBundleId() != bundle.getBundleId())  // skip ourselves
                .filter(b -> isWiredToStaleRevision(b, bundle))
                .collect(Collectors.toSet());

        if (toRefresh.isEmpty()) {
            logger.info("No stale consumers found for {}. No refresh needed.", symbolicName);
        } else {
            logger.info("Refreshing {} bundle(s) still wired to an older revision of {}:",
                    toRefresh.size(), symbolicName);
            toRefresh.forEach(b -> logger.info("  Refreshing bundle - {}:{}", b.getSymbolicName(), b.getBundleId()));
            BundleLifecycleUtils.refreshBundles(toRefresh);
        }

        // ── Write migration flag ──────────────────────────────────────────────────────────────
        // Written after the refresh so that a JVM crash mid-refresh does not leave the flag in
        // place and suppress a necessary re-run on the next start.
        writeMigrationFlag(migrationFlag, symbolicName);
    }

    /**
     * Returns {@code true} if {@code candidate} has at least one resolved import of a package
     * rooted at {@link #EXPORTED_PACKAGE} that is provided by a bundle other than
     * {@code currentBundle} (i.e. the wire points to a stale, older revision).
     *
     * <p>Uses {@link BundleWiring#getRequiredWires(String)} — the resolver's live, authoritative
     * view — rather than scanning manifest headers as strings.</p>
     */
    private static boolean isWiredToStaleRevision(Bundle candidate, Bundle currentBundle) {
        BundleWiring wiring = candidate.adapt(BundleWiring.class);
        // adapt() returns null when the bundle is INSTALLED or UNRESOLVED (no wiring exists yet).
        if (wiring == null) {
            return false;
        }

        List<BundleWire> requiredWires = wiring.getRequiredWires("osgi.wiring.package");
        // getRequiredWires() can return null when the bundle has no wiring data at all.
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

            // Match the exact root package and any sub-package depth (e.g. .components, .impl).
            boolean exportsOurPackage = pkg.equals(EXPORTED_PACKAGE)
                    || pkg.startsWith(EXPORTED_PACKAGE + ".");

            if (!exportsOurPackage) {
                return false;
            }

            // The wire is stale when it resolves to any bundle other than the current (newest) one.
            // getProviderWiring() is non-null for a live, resolved wire.
            BundleWiring providerWiring = wire.getProviderWiring();
            return providerWiring != null && !currentBundle.equals(providerWiring.getBundle());
        });
    }

    /**
     * Returns {@code true} if {@code candidate} has at least one resolved import of a package
     * rooted at {@link #EXPORTED_PACKAGE} that is provided by a bundle whose ID is <em>not</em>
     * in {@code familyIds} (i.e. the wire points outside the {@code external-provider-ui} bundle
     * family entirely).
     *
     * <p>Used in Guard 4 to detect whether a previous version already re-wired all consumers
     * into the family, making the migration effectively complete.</p>
     */
    private static boolean isWiredOutsideFamily(Bundle candidate, Set<Long> familyIds) {
        BundleWiring wiring = candidate.adapt(BundleWiring.class);
        if (wiring == null) {
            return false;
        }

        List<BundleWire> requiredWires = wiring.getRequiredWires("osgi.wiring.package");
        if (requiredWires == null) {
            return false;
        }

        return requiredWires.stream().anyMatch(wire -> {
            Object pkgAttr = wire.getCapability().getAttributes().get("osgi.wiring.package");
            if (!(pkgAttr instanceof String)) {
                return false;
            }
            String pkg = (String) pkgAttr;

            if (!pkg.equals(EXPORTED_PACKAGE) && !pkg.startsWith(EXPORTED_PACKAGE + ".")) {
                return false;
            }

            BundleWiring providerWiring = wire.getProviderWiring();
            return providerWiring != null && !familyIds.contains(providerWiring.getBundle().getBundleId());
        });
    }

    /**
     * Writes the migration flag file, logging a warning on concurrent writes and an error if the
     * filesystem operation fails. Safe to call with a {@code null} flag (no persistent storage).
     */
    private static void writeMigrationFlag(File migrationFlag, String symbolicName) {
        if (migrationFlag == null) {
            return;
        }
        try {
            if (!migrationFlag.createNewFile()) {
                // Can happen if two threads race through @Activate concurrently (e.g.
                // FelixFrameworkWiring and FelixStartLevel both activating DS components
                // at the same instant). Harmless — both performed the same idempotent check.
                logger.warn("Migration flag already exists for {} — concurrent activation detected.", symbolicName);
            }
        } catch (IOException e) {
            logger.error("Could not write migration flag for {}; check may re-run on next hot-deploy. Error: {}", symbolicName, e.getMessage());
            logger.debug("Full stack trace", e);
        }
    }

    /**
     * Resolves the path for the one-time per-node migration flag file.
     *
     * <p>Preferred location: {@code <karaf.data>/<symbolicName>-migration.done}.
     * {@code karaf.data} is a stable directory on the node filesystem, independent of the
     * Felix bundle-ID assigned at install time, so any future version of this bundle finds
     * the same flag.</p>
     *
     * <p>Fallback: Felix bundle data area ({@code felix-cache/bundle<ID>/data/}). This is
     * bundle-ID–scoped; a re-install bypasses it. Guards 1 and 2 remain effective even in
     * this degraded case.</p>
     */
    private static File resolveMigrationFlag(BundleContext context, String symbolicName) {
        // BundleContext.getProperty() checks OSGi framework properties first, then System properties.
        // karaf.data is set by the Karaf launcher (e.g. /data/digital-factory-data/karaf).
        String karafData = context.getProperty("karaf.data");
        if (karafData != null) {
            return new File(karafData, symbolicName + "-migration.done");
        }

        logger.debug("karaf.data property not available; falling back to Felix bundle data area for migration flag.");
        // Returns null if the framework has no persistent storage configured for this bundle.
        return context.getDataFile("migration.done");
    }
}
