package org.jahia.modules.external.vfs;

import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * OSGI configuration for VFS mount points
 */
@Component(service = ManagedService.class, immediate = true, property = {
        "service.pid=" + VfsRootConfiguration.PID,
        "service.description=VFS mount point configuration service",
        "service.vendor=Jahia Solutions Group SA"
})
public class VfsRootConfiguration implements ManagedService {

    static final String PID = "org.jahia.modules.external.vfs";

    // Configuration keys
    static final String KEY_ALLOWED_SCHEMES = "vfsMountPoint.allowedSchemes";

    private static final Logger logger = LoggerFactory.getLogger(VfsRootConfiguration.class);

    @Override
    public void updated(Dictionary<String, ?> properties) {
        if (properties == null) {
            logger.info("No configuration found for VFS mount points, using defaults");
            VfsRootResolver.setAllowedSchemes(null);
            return;
        }
        VfsRootResolver.setAllowedSchemes(getSchemesProperty(properties, KEY_ALLOWED_SCHEMES));
    }

    @Deactivate
    public void deactivate() {
        VfsRootResolver.close();
    }

    /**
     * Reads a scheme list, which a {@code .cfg} file carries as one comma-separated string and Config Admin carries
     * as a collection.
     */
    private static List<String> getSchemesProperty(Dictionary<String, ?> properties, String key) {
        Object value = properties.get(key);
        if (value == null) {
            return null;
        }
        Stream<?> values;
        if (value instanceof Object[]) {
            values = Arrays.stream((Object[]) value);
        } else if (value instanceof Collection) {
            values = ((Collection<?>) value).stream();
        } else {
            values = Stream.of(value);
        }
        return values.filter(Objects::nonNull)
                .flatMap(scheme -> Arrays.stream(scheme.toString().split(",")))
                .collect(Collectors.toList());
    }
}
