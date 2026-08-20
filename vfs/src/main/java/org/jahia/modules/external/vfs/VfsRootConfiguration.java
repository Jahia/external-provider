package org.jahia.modules.external.vfs;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import java.util.Arrays;
import java.util.Map;

/**
 * Reads the schemes a VFS mount point root may use from the {@value #PID} configuration and hands them to
 * {@link VfsRootResolver}. Without a configuration the resolver keeps its own default, the local filesystem alone.
 */
@Component(service = {}, immediate = true, configurationPid = VfsRootConfiguration.PID)
public class VfsRootConfiguration {

    static final String PID = "org.jahia.modules.external.vfs";

    /** Comma-separated list of URI schemes a mount point root may use. */
    static final String ALLOWED_SCHEMES = "vfsMountPoint.allowedSchemes";

    @Activate
    public void activate(Map<String, ?> properties) {
        apply(properties);
    }

    @Modified
    public void modified(Map<String, ?> properties) {
        apply(properties);
    }

    @Deactivate
    public void deactivate() {
        VfsRootResolver.setAllowedSchemes(null);
    }

    private static void apply(Map<String, ?> properties) {
        Object schemes = properties != null ? properties.get(ALLOWED_SCHEMES) : null;
        VfsRootResolver.setAllowedSchemes(schemes != null ? Arrays.asList(schemes.toString().split(",")) : null);
    }
}
