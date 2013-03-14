/**
 * This file is part of Jahia, next-generation open source CMS:
 * Jahia's next-generation, open source CMS stems from a widely acknowledged vision
 * of enterprise application convergence - web, search, document, social and portal -
 * unified by the simplicity of web content management.
 *
 * For more information, please visit http://www.jahia.com.
 *
 * Copyright (C) 2002-2013 Jahia Solutions Group SA. All rights reserved.
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

package org.jahia.modules.external;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Workspace;
import javax.jcr.query.QueryManager;

import org.apache.commons.lang.StringUtils;
import org.jahia.exceptions.JahiaInitializationException;
import org.jahia.exceptions.JahiaRuntimeException;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRStoreProvider;
import org.springframework.beans.factory.InitializingBean;

/**
 * Implementation of the {@link org.jahia.services.content.JCRStoreProvider} for the {@link org.jahia.modules.external.ExternalData}.
 * 
 * @author Thomas Draier
 */
public class ExternalContentStoreProvider extends JCRStoreProvider implements InitializingBean {
    
    private boolean readOnly;

    private ExternalDataSource dataSource;
    
    private String id;

    private IdentifierMappingService identifierMappingService;

    @Override
    protected Repository createRepository() {
        ExternalRepositoryImpl instance = new ExternalRepositoryImpl(this, dataSource, new ExternalAccessControlManager(readOnly));
        instance.setProviderKey(getKey());

        return instance;
    }

    @Override
    public void start() throws JahiaInitializationException {
        getId(); // initialize ID
        if (dataSource instanceof ExternalDataSource.Initializable) {
            ((ExternalDataSource.Initializable) dataSource).start();
        }
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
        if (dataSource instanceof ExternalDataSource.Initializable) {
            ((ExternalDataSource.Initializable) dataSource).stop();
        }
    }

    @Override
    protected void initObservers() throws RepositoryException {
        // do nothing
    }

    @Override
    public QueryManager getQueryManager(JCRSessionWrapper session) throws RepositoryException {
        return dataSource instanceof ExternalDataSource.Searchable ? super.getQueryManager(session) : null;
    }

    @Override
    public boolean isExportable() {
        return false;
    }

    /**
     * Returns <code>true</code> if this is a read-only content provider.
     * 
     * @return <code>true</code> if this is a read-only content provider
     */
    public boolean isReadOnly() {
        return readOnly;
    }

    protected void registerNamespaces(Workspace workspace) throws RepositoryException {
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public void setDataSource(ExternalDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (getKey() != null) {
            init();
        }
    }

    /**
     * Initializes this provider instance. Should be called after the {@link #setKey(String)} was called to set the provider key.
     * 
     * @throws RepositoryException
     *             in case of an initialization error
     */
    protected void init() throws RepositoryException {
        if (getKey() == null) {
            throw new IllegalArgumentException("The key is not specified for the provider instance."
                    + " Unable to initialize this provider.");
        }
        id = StringUtils.leftPad(identifierMappingService.getProviderId(getKey()).toString(), 8, "f");
    }
    
    /**
     * The internal ID of this provider which is also used as a prefix for UUIDs, handled by this provider.
     * 
     * @return internal ID of this provider which is also used as a prefix for UUIDs, handled by this provider.
     */
    public String getId() {
        if (id == null) {
            try {
                init();
            } catch (RepositoryException e) {
                throw new JahiaRuntimeException(e.getMessage(), e);
            }
        }
        return id;
    }

    public IdentifierMappingService getIdentifierMappingService() {
        return identifierMappingService;
    }

    public void setIdentifierMappingService(IdentifierMappingService mappingService) {
        this.identifierMappingService = mappingService;
    }

    /**
     * Reads internal UUID of the specified node via mapping table, using external ID and provider key.
     * 
     * @param externalId
     *            the external ID to retrieve UUID for
     * @return an internal UUID of the specified node via mapping table or <code>null</code> if the mapping is not stored yet
     * @throws RepositoryException
     *             in case an internal identifier cannot be retrieved from the database
     */
    protected String getInternalIdentifier(String externalId) throws RepositoryException {
        return getIdentifierMappingService().getInternalIdentifier(externalId, getKey());
    }
    
    /**
     * Generates the internal UUID for the specified node in the mapping table, using external ID and this provider.
     * 
     * @param externalId
     *            the external ID to generate UUID for
     * @return an generated internal UUID
     * @throws RepositoryException
     *             in case an internal identifier cannot be stored into the database
     */
    protected String mapInternalIdentifier(String externalId) throws RepositoryException {
        return getIdentifierMappingService().mapInternalIdentifier(externalId, getKey(), getId());
    }
}
