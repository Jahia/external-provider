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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves the root path a VFS mount point points at, restricted to the URI schemes a mount point is allowed to use.
 *
 * <p>Every caller that turns a {@code j:rootPath} value into a {@link FileObject} goes through here, so a mount point
 * is validated and mounted against the same set of schemes.
 */
public final class VfsRootResolver {

    /** The scheme of the local filesystem, and the only one allowed unless a configuration widens the set. */
    public static final String LOCAL_SCHEME = "file";

    private static final Set<String> LOCAL_ONLY = Collections.singleton(LOCAL_SCHEME);

    /**
     * Matches a root path that opens with a URI scheme naming a VFS provider. A single leading letter is deliberately
     * not matched, so a Windows drive ({@code d:/data}) stays a filesystem path rather than reading as a scheme.
     */
    private static final Pattern SCHEME_PREFIX = Pattern.compile("^([a-zA-Z][a-zA-Z0-9+.-]+):");

    private static volatile Set<String> allowedSchemes = LOCAL_ONLY;

    private static volatile FileSystemManager localOnlyManager;

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
        return managerFor(allowedSchemes).resolveFile(rootPath);
    }

    /**
     * @return the schemes a mount point root may currently use
     */
    public static Set<String> getAllowedSchemes() {
        return allowedSchemes;
    }

    /**
     * Sets the schemes a mount point root may use. A null or empty collection restores the local filesystem alone.
     */
    static void setAllowedSchemes(Collection<String> schemes) {
        Set<String> normalized = new LinkedHashSet<>();
        if (schemes != null) {
            for (String scheme : schemes) {
                if (StringUtils.isNotBlank(scheme)) {
                    normalized.add(scheme.trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        allowedSchemes = normalized.isEmpty() ? LOCAL_ONLY : Collections.unmodifiableSet(normalized);
    }

    static void checkSchemeAllowed(String rootPath) throws FileSystemException {
        if (StringUtils.isBlank(rootPath)) {
            throw new VfsRootNotAllowedException("The root path of a VFS mount point must not be blank");
        }
        String scheme = schemeOf(rootPath);
        Set<String> allowed = allowedSchemes;
        if (scheme != null && !allowed.contains(scheme)) {
            throw new VfsRootNotAllowedException(String.format(
                    "The URI scheme \"%s\" is not among the schemes a VFS mount point root may use: %s",
                    scheme, allowed));
        }
    }

    private static String schemeOf(String rootPath) {
        Matcher matcher = SCHEME_PREFIX.matcher(rootPath);
        return matcher.find() ? matcher.group(1).toLowerCase(Locale.ROOT) : null;
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
            manager.setFilesCache(new SoftRefFilesCache());
            manager.setCacheStrategy(CacheStrategy.ON_RESOLVE);
            manager.addProvider(LOCAL_SCHEME, new DefaultLocalFileProvider());
            // Neither a default provider nor a base file is set, so a root resolves only when the local provider
            // claims it.
            manager.init();
            localOnlyManager = manager;
        }
        return localOnlyManager;
    }
}
