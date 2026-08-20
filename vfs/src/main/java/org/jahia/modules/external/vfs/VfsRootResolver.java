package org.jahia.modules.external.vfs;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.vfs2.CacheStrategy;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.cache.SoftRefFilesCache;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.apache.commons.vfs2.provider.local.DefaultLocalFileProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves the root path a VFS mount point points at, restricted to the URI schemes a mount point is allowed to use.
 *
 * <p>Every caller that turns a {@code j:rootPath} value into a {@link FileObject} goes through here, so a mount point
 * is validated and mounted against the same set of schemes.
 */
public final class VfsRootResolver {

    private static final Logger logger = LoggerFactory.getLogger(VfsRootResolver.class);

    /** The scheme of the local filesystem, and the only one allowed unless a configuration widens the set. */
    public static final String LOCAL_SCHEME = "file";

    private static final Set<String> LOCAL_ONLY = Collections.singleton(LOCAL_SCHEME);

    /**
     * Matches a root path that opens with a URI scheme naming a VFS provider. A single leading letter is deliberately
     * not matched, so a Windows drive ({@code d:/data}) stays a filesystem path rather than reading as a scheme.
     */
    private static final Pattern SCHEME_PREFIX = Pattern.compile("^([a-zA-Z][a-zA-Z0-9+.-]+):");

    /** The shape of a scheme name, applied to a configured value before it joins the allowed set. */
    private static final Pattern SCHEME_NAME = Pattern.compile("[a-z][a-z0-9+.-]+");

    private static final AtomicReference<Set<String>> allowedSchemes = new AtomicReference<>(LOCAL_ONLY);

    /** Only ever touched from the synchronized methods below, so it needs no memory barrier of its own. */
    private static DefaultFileSystemManager localOnlyManager;

    private VfsRootResolver() {
        throw new IllegalStateException("Utility class is not meant to be instantiated");
    }

    /**
     * Resolves the root path of a VFS mount point.
     *
     * @param rootPath the {@code j:rootPath} value of the mount point
     * @return the resolved root
     * @throws FileSystemException if the root path is blank, uses a scheme that is not allowed, or cannot be resolved
     */
    public static FileObject resolveRoot(String rootPath) throws FileSystemException {
        checkSchemeAllowed(rootPath);
        return managerFor(allowedSchemes.get()).resolveFile(rootPath);
    }

    /**
     * @return the schemes a mount point root may currently use
     */
    static Set<String> getAllowedSchemes() {
        return allowedSchemes.get();
    }

    /**
     * Sets the schemes a mount point root may use. A value that is not the name of a scheme is reported and dropped,
     * and a set left with nothing readable in it restores the local filesystem alone: the set also decides which
     * providers the resolving manager carries, so it must not widen on a value it cannot read.
     */
    static void setAllowedSchemes(Collection<String> schemes) {
        Set<String> normalized = new LinkedHashSet<>();
        if (schemes != null) {
            for (String scheme : schemes) {
                String candidate = StringUtils.trimToEmpty(scheme).toLowerCase(Locale.ROOT);
                if (SCHEME_NAME.matcher(candidate).matches()) {
                    normalized.add(candidate);
                } else if (StringUtils.isNotBlank(candidate)) {
                    logger.warn("Ignoring \"{}\", which is not the name of a URI scheme, in the schemes a VFS mount"
                            + " point root may use", candidate);
                }
            }
        }
        Set<String> applied = normalized.isEmpty() ? LOCAL_ONLY : Collections.unmodifiableSet(normalized);
        allowedSchemes.set(applied);
        if (LOCAL_ONLY.equals(applied)) {
            logger.info("A VFS mount point root may name the local file system");
        } else {
            logger.info("A VFS mount point root may name any of {}", applied);
        }
    }

    static void checkSchemeAllowed(String rootPath) throws FileSystemException {
        if (StringUtils.isBlank(rootPath)) {
            throw new VfsRootNotAllowedException("The root path of a VFS mount point must not be blank");
        }
        String scheme = schemeOf(rootPath);
        Set<String> allowed = allowedSchemes.get();
        if (!allowed.contains(scheme)) {
            throw new VfsRootNotAllowedException(String.format(
                    "The URI scheme \"%s\" is not among the schemes a VFS mount point root may use: %s",
                    scheme, allowed));
        }
    }

    /**
     * The scheme a root names, which is the local file system when it names none — a plain path and a {@code file:}
     * URI reach the same place, so the allowed set answers for both alike.
     */
    private static String schemeOf(String rootPath) {
        Matcher matcher = SCHEME_PREFIX.matcher(rootPath);
        return matcher.find() ? matcher.group(1).toLowerCase(Locale.ROOT) : LOCAL_SCHEME;
    }

    /**
     * On the local-only default, a manager carrying just the local provider. A configuration naming further schemes
     * needs the shared manager, which is the one carrying their providers.
     */
    private static FileSystemManager managerFor(Set<String> allowed) throws FileSystemException {
        return LOCAL_ONLY.containsAll(allowed) ? localOnlyManager() : VFS.getManager();
    }

    private static synchronized FileSystemManager localOnlyManager() throws FileSystemException {
        if (localOnlyManager == null) {
            DefaultFileSystemManager manager = new DefaultFileSystemManager();
            try {
                manager.setFilesCache(new SoftRefFilesCache());
                manager.setCacheStrategy(CacheStrategy.ON_RESOLVE);
                manager.addProvider(LOCAL_SCHEME, new DefaultLocalFileProvider());
                // Neither a default provider nor a base file is set, so a root resolves only when the local provider
                // claims it.
                manager.init();
            } catch (FileSystemException e) {
                manager.close();
                throw e;
            }
            localOnlyManager = manager;
        }
        return localOnlyManager;
    }

    /**
     * Releases the manager this resolver holds. Called when the bundle stops, so a redeployment starts from a new one.
     */
    static synchronized void close() {
        if (localOnlyManager != null) {
            localOnlyManager.close();
            localOnlyManager = null;
        }
    }
}
