/*
 * Copyright (C) 2002-2022 Jahia Solutions Group SA. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jahia.modules.external.vfs;

import org.jahia.exceptions.JahiaInitializationException;
import org.jahia.modules.external.ExternalContentStoreProviderFactory;
import org.jahia.services.SpringContextSingleton;
import org.jahia.services.content.*;
import org.jahia.modules.external.ExternalContentStoreProvider;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.jcr.RepositoryException;

/**
 * Mount external VFS Data store
 */
@Component(service = ProviderFactory.class, immediate = true)
public class VFSProviderFactory implements ProviderFactory {

    @Reference
    private ExternalContentStoreProviderFactory externalContentStoreProviderFactory;

    public void setExternalContentStoreProviderFactory(ExternalContentStoreProviderFactory externalContentStoreProviderFactory) {
        this.externalContentStoreProviderFactory = externalContentStoreProviderFactory;
    }

    /**
     * The node type which is supported by this factory
     * @return The node type name
     */
    @Override
    public String getNodeTypeName() {
        return "jnt:vfsMountPoint";
    }

    /**
     * Mount the provider in place of the mountPoint node passed in parameter. Use properties of
     * the mountPoint node to set parameters in the store provider
     *
     * @param mountPoint The jnt:mountPoint node
     * @return A new provider instance, mounted
     * @throws RepositoryException
     */
    @Override
    public JCRStoreProvider mountProvider(JCRNodeWrapper mountPoint) throws RepositoryException {
        ExternalContentStoreProvider provider = externalContentStoreProviderFactory.newProvider();
        provider.setKey(mountPoint.getIdentifier());
        provider.setMountPoint(mountPoint.getPath());

        VFSDataSource dataSource = new VFSDataSource();
        dataSource.setRoot(mountPoint.getProperty("j:rootPath").getString());
        provider.setDataSource(dataSource);
        provider.setDynamicallyMounted(true);
        provider.setSessionFactory(JCRSessionFactory.getInstance());
        try {
            provider.start();
        } catch (JahiaInitializationException e) {
            throw new RepositoryException(e);
        }
        return provider;

    }

}
