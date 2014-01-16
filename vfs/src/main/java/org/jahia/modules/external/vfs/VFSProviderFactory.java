/**
 * This file is part of Jahia, next-generation open source CMS:
 * Jahia's next-generation, open source CMS stems from a widely acknowledged vision
 * of enterprise application convergence - web, search, document, social and portal -
 * unified by the simplicity of web content management.
 *
 * For more information, please visit http://www.jahia.com.
 *
 * Copyright (C) 2002-2014 Jahia Solutions Group SA. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * As a special exception to the terms and conditions of version 2.0 of
 * the GPL (or any later version), you may redistribute this Program in connection
 * with Free/Libre and Open Source Software ("FLOSS") applications as described
 * in Jahia's FLOSS exception. You should have received a copy of the text
 * describing the FLOSS exception, and it is also available here:
 * http://www.jahia.com/license
 *
 * Commercial and Supported Versions of the program (dual licensing):
 * alternatively, commercial and supported versions of the program may be used
 * in accordance with the terms and conditions contained in a separate
 * written agreement between you and Jahia Solutions Group SA.
 *
 * If you are unsure which license is appropriate for your use,
 * please contact the sales department at sales@jahia.com.
 */

package org.jahia.modules.external.vfs;

import org.jahia.exceptions.JahiaInitializationException;
import org.jahia.services.SpringContextSingleton;
import org.jahia.services.content.*;
import org.jahia.modules.external.ExternalContentStoreProvider;

import javax.jcr.RepositoryException;

/**
 * Mount external VFS Data store
 */
public class VFSProviderFactory implements ProviderFactory {
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
        ExternalContentStoreProvider provider = (ExternalContentStoreProvider) SpringContextSingleton.getBean("ExternalStoreProviderPrototype");
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
