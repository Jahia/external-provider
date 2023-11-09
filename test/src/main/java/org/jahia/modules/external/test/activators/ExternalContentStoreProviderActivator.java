package org.jahia.modules.external.test.activators;

import org.jahia.exceptions.JahiaInitializationException;
import org.jahia.modules.external.ExternalContentStoreProvider;
import org.jahia.modules.external.ExternalContentStoreProviderFactory;
import org.jahia.modules.external.test.db.*;
import org.jahia.modules.external.test.listener.ApiDataSource;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import java.util.*;

/**
 * An activator to setup all the ExternalContentStoreProvider instances used in the tests
 */
@Component(service= ExternalContentStoreProviderActivator.class, immediate=true)
public class ExternalContentStoreProviderActivator {

    @Reference
    private ExternalContentStoreProviderFactory providerFactory;

    @Reference
    private ApiDataSource apiDataSource;

    Map<String, ExternalContentStoreProvider> externalContentStoreProviders = new LinkedHashMap<>();

    @Activate
    public void activate(BundleContext bundleContext) throws JahiaInitializationException {

        ExternalContentStoreProvider externalContentStoreProvider = providerFactory.newProviderBuilder()
                .withKey("ExternalGenericDatabaseProvider")
                .withMountPoint("/external-database-generic")
                .withExtendableTypes(Arrays.asList("nt:base"))
                .withDataSource(new GenericDatabaseDataSource()).build();
        externalContentStoreProvider.start();
        externalContentStoreProviders.put(externalContentStoreProvider.getKey(), externalContentStoreProvider);

        ExternalContentStoreProvider.Builder externalMappedDatabaseProviderBuilder = providerFactory.newProviderBuilder()
                .withKey("ExternalMappedDatabaseProvider")
                .withMountPoint("/external-database-mapped")
                .withExtendableTypes(Arrays.asList("nt:base"))
                .withOverridableItems(Arrays.asList("jtestnt:directory.*", "jtestnt:airline.*", "jtestnt:city.language"))
                .withNonOverridableItems(Arrays.asList("jtestnt:airline.business_seats"))
                .withLockSupport(true)
                .withDataSource(new MappedDatabaseDataSource());

        externalContentStoreProvider = externalMappedDatabaseProviderBuilder.build();
        externalContentStoreProvider.start();
        externalContentStoreProviders.put(externalContentStoreProvider.getKey(), externalContentStoreProvider);

        ExternalContentStoreProvider.Builder externalMappedDatabaseProviderSupportCountProviderBuilder = externalMappedDatabaseProviderBuilder.copy()
                .withKey("ExternalMappedDatabaseProviderSupportCount")
                .withMountPoint("/external-database-mapped-support-count")
                .withDataSource(new SupportCountMappedDatabaseDataSource());

        externalContentStoreProvider = externalMappedDatabaseProviderSupportCountProviderBuilder.build();
        externalContentStoreProvider.start();
        externalContentStoreProviders.put(externalContentStoreProvider.getKey(), externalContentStoreProvider);

        ExternalContentStoreProvider.Builder externalMappedDatabaseProviderSupportCount2ProviderBuilder = externalMappedDatabaseProviderSupportCountProviderBuilder.copy()
                .withKey("ExternalMappedDatabaseProviderSupportCount2")
                .withMountPoint("/external-database-mapped-support-count-2");

        externalContentStoreProvider = externalMappedDatabaseProviderSupportCount2ProviderBuilder.build();
        externalContentStoreProvider.start();
        externalContentStoreProviders.put(externalContentStoreProvider.getKey(), externalContentStoreProvider);

        MappedDatabaseDataSource noAirlineTypeDataSource = new MappedDatabaseDataSource();
        noAirlineTypeDataSource.setNoAirlineType(true);
        ExternalContentStoreProvider.Builder externalMappedDatabaseProviderNoAirlineWithCountProviderBuilder = externalMappedDatabaseProviderSupportCountProviderBuilder.copy()
                .withKey("ExternalMappedDatabaseProviderNonExentable")
                .withMountPoint("/external-database-mapped-no-airlines-with-count")
                .withDataSource(noAirlineTypeDataSource);

        externalContentStoreProvider = externalMappedDatabaseProviderNoAirlineWithCountProviderBuilder.build();
        externalContentStoreProvider.start();
        externalContentStoreProviders.put(externalContentStoreProvider.getKey(), externalContentStoreProvider);

        ExternalContentStoreProvider.Builder externalMappedDatabaseProviderNoMixinProviderBuilder = externalMappedDatabaseProviderBuilder.copy()
                .withKey("ExternalMappedDatabaseProviderNoMixin")
                .withMountPoint("/external-database-mapped-no-mixin")
                .withNonExtendableMixins(Arrays.asList("*"));

        externalContentStoreProvider = externalMappedDatabaseProviderNoMixinProviderBuilder.build();
        externalContentStoreProvider.start();
        externalContentStoreProviders.put(externalContentStoreProvider.getKey(), externalContentStoreProvider);

        ExternalContentStoreProvider.Builder externalMappedDatabaseProviderNoNamedMixinProviderBuilder = externalMappedDatabaseProviderBuilder.copy()
                .withKey("ExternalMappedDatabaseProviderNoNamedMixin")
                .withMountPoint("/external-database-mapped-no-named-mixin")
                .withNonExtendableMixins(Arrays.asList("mix:title"));

        externalContentStoreProvider = externalMappedDatabaseProviderNoNamedMixinProviderBuilder.build();
        externalContentStoreProvider.start();
        externalContentStoreProviders.put(externalContentStoreProvider.getKey(), externalContentStoreProvider);

        ExternalContentStoreProvider.Builder externalCanLoadChildrenInBatchMappedDatabaseProviderBuilder = externalMappedDatabaseProviderBuilder.copy()
                .withKey("ExternalCanLoadChildrenInBatchMappedDatabaseProvider")
                .withMountPoint("/external-database-mapped-batch-children")
                .withDataSource(new CanLoadChildrenInBatchMappedDatabaseDataSource());

        externalContentStoreProvider = externalCanLoadChildrenInBatchMappedDatabaseProviderBuilder.build();
        externalContentStoreProvider.start();
        externalContentStoreProviders.put(externalContentStoreProvider.getKey(), externalContentStoreProvider);

        ExternalContentStoreProvider.Builder externalWriteableMappedDatabaseProviderBuilder = providerFactory.newProviderBuilder()
                .withKey("WritableDatabaseDataSource")
                .withMountPoint("/external-writeable-database-mapped")
                .withExtendableTypes(Arrays.asList("nt:base"))
                .withDataSource(new WriteableMappedDatabaseProvider(bundleContext));

        externalContentStoreProvider = externalWriteableMappedDatabaseProviderBuilder.build();
        externalContentStoreProvider.start();
        externalContentStoreProviders.put(externalContentStoreProvider.getKey(), externalContentStoreProvider);

        externalContentStoreProvider = providerFactory.newProviderBuilder()
                .withKey("staticProvider")
                .withMountPoint("/external-static")
                .withExtendableTypes(Arrays.asList("nt:base"))
                .withOverridableItems(Arrays.asList("jtestnt:directory.*", "jtestnt:airline.*", "jnt:contentFolder.*"))
                .withDataSource(apiDataSource)
                .build();
        externalContentStoreProvider.start();
        externalContentStoreProviders.put(externalContentStoreProvider.getKey(), externalContentStoreProvider);

        externalContentStoreProvider = providerFactory.newProviderBuilder()
                .withKey("tatatititoto")
                .withExtendableTypes(Arrays.asList("nt:base"))
                .withDataSource(apiDataSource)
                .build();
        // this provider is not started on purpose, it will be started and stopped by the edpTool.jsp
        externalContentStoreProviders.put(externalContentStoreProvider.getKey(), externalContentStoreProvider);

    }

    @Deactivate
    public void deactivate() {
        for (ExternalContentStoreProvider externalContentStoreProvider : externalContentStoreProviders.values()) {
            if (externalContentStoreProvider.isInitialized()) {
                externalContentStoreProvider.stop();
            }
        }
    }

    // Used by the edpTestTool.jsp
    public ExternalContentStoreProvider getExternalContentStoreProvider(String key) {
        return externalContentStoreProviders.get(key);
    }

}
