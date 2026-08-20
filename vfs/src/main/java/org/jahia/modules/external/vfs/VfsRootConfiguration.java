package org.jahia.modules.external.vfs;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Reads the schemes a VFS mount point root may use from the {@value #PID} configuration and hands them to
 * {@link VfsRootResolver}.
 */
@Component(service = {}, immediate = true, configurationPid = VfsRootConfiguration.PID)
@Designate(ocd = VfsRootConfiguration.Config.class)
public class VfsRootConfiguration {

    static final String PID = "org.jahia.modules.external.vfs";

    static final String ALLOWED_SCHEMES = "vfsMountPoint.allowedSchemes";

    @ObjectClassDefinition(name = "Jahia VFS mount points", description = "Settings for VFS mount points")
    public @interface Config {

        @AttributeDefinition(name = "Allowed root schemes",
                description = "URI schemes the root path of a VFS mount point may use. The local file system is the"
                        + " default; name a further scheme here when a mount point needs it.")
        String[] vfsMountPoint_allowedSchemes() default {VfsRootResolver.LOCAL_SCHEME};
    }

    @Activate
    public void activate(Config config, Map<String, Object> properties) {
        apply(config, properties);
    }

    @Modified
    public void modified(Config config, Map<String, Object> properties) {
        apply(config, properties);
    }

    @Deactivate
    public void deactivate() {
        VfsRootResolver.setAllowedSchemes(null);
    }

    private static void apply(Config config, Map<String, Object> properties) {
        Object value = properties != null ? properties.get(ALLOWED_SCHEMES) : null;
        VfsRootResolver.setAllowedSchemes(
                value != null ? schemesOf(value) : Arrays.asList(config.vfsMountPoint_allowedSchemes()));
    }

    /**
     * Reads the configured schemes off the raw value rather than the typed accessor, because a {@code .cfg} file
     * carries them as one comma-separated string while Config Admin carries them as a collection.
     */
    private static List<String> schemesOf(Object value) {
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
