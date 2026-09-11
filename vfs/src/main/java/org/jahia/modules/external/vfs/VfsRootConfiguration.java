package org.jahia.modules.external.vfs;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * OSGi configuration for VFS mount points.
 *
 * <p>The policy is {@link ConfigurationPolicy#OPTIONAL}, so the component activates on the declared defaults when no
 * configuration is present and the module keeps the local file system alone. {@link Modified} is wired to the same
 * path as {@link Activate}, so a change to the configuration reaches the mount points already mounted instead of
 * waiting for the component to be built again.
 */
@Component(service = VfsRootConfiguration.class, immediate = true,
        configurationPid = VfsRootConfiguration.PID,
        configurationPolicy = ConfigurationPolicy.OPTIONAL,
        property = {
                "service.description=VFS mount point configuration service",
                "service.vendor=Jahia Solutions Group SA"
        })
@Designate(ocd = VfsRootConfiguration.Config.class)
public class VfsRootConfiguration {

    static final String PID = "org.jahia.modules.external.vfs";

    @ObjectClassDefinition(name = "%configName", description = "%configDesc",
            localization = "OSGI-INF/l10n/external-provider-vfs/config")
    public @interface Config {

        /**
         * Metatype maps each {@code _} of the accessor name to a {@code .} in the property key, so this declares the
         * {@code vfsMountPoint.allowedSchemes} key a {@code .cfg} file carries.
         */
        @AttributeDefinition(name = "%vfsMountPoint_allowedSchemes",
                description = "%vfsMountPoint_allowedSchemes.desc")
        String[] vfsMountPoint_allowedSchemes() default {VfsRootResolver.LOCAL_SCHEME};
    }

    @Activate
    public void activate(Config config) {
        apply(config);
    }

    @Modified
    public void modified(Config config) {
        apply(config);
    }

    private static void apply(Config config) {
        VfsRootResolver.setAllowedSchemes(schemesOf(config.vfsMountPoint_allowedSchemes()));
    }

    /**
     * A modelled array arrives as one entry per value, while a {@code .cfg} file may carry the whole list in one
     * comma-separated entry. Both are read, so a configuration written either way names the same set.
     */
    private static List<String> schemesOf(String[] schemes) {
        if (schemes == null) {
            return null;
        }
        return Arrays.stream(schemes)
                .filter(Objects::nonNull)
                .flatMap(scheme -> Stream.of(scheme.split(",")))
                .collect(Collectors.toList());
    }
}
