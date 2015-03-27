/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2015 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ======================================================================================
 *
 *     IF YOU DECIDE TO CHOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     "This program is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU General Public License
 *     as published by the Free Software Foundation; either version 2
 *     of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 *     As a special exception to the terms and conditions of version 2.0 of
 *     the GPL (or any later version), you may redistribute this Program in connection
 *     with Free/Libre and Open Source Software ("FLOSS") applications as described
 *     in Jahia's FLOSS exception. You should have received a copy of the text
 *     describing the FLOSS exception, also available here:
 *     http://www.jahia.com/license"
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ======================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 *
 *
 * ==========================================================================================
 * =                                   ABOUT JAHIA                                          =
 * ==========================================================================================
 *
 *     Rooted in Open Source CMS, Jahia’s Digital Industrialization paradigm is about
 *     streamlining Enterprise digital projects across channels to truly control
 *     time-to-market and TCO, project after project.
 *     Putting an end to “the Tunnel effect”, the Jahia Studio enables IT and
 *     marketing teams to collaboratively and iteratively build cutting-edge
 *     online business solutions.
 *     These, in turn, are securely and easily deployed as modules and apps,
 *     reusable across any digital projects, thanks to the Jahia Private App Store Software.
 *     Each solution provided by Jahia stems from this overarching vision:
 *     Digital Factory, Workspace Factory, Portal Factory and eCommerce Factory.
 *     Founded in 2002 and headquartered in Geneva, Switzerland,
 *     Jahia Solutions Group has its North American headquarters in Washington DC,
 *     with offices in Chicago, Toronto and throughout Europe.
 *     Jahia counts hundreds of global brands and governmental organizations
 *     among its loyal customers, in more than 20 countries across the globe.
 *
 *     For more information, please visit http://www.jahia.com
 */
package org.jahia.modules.external;

import javax.jcr.*;
import javax.jcr.query.QueryManager;

import org.apache.commons.lang.StringUtils;
import org.jahia.exceptions.JahiaInitializationException;
import org.jahia.exceptions.JahiaRuntimeException;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRStoreProvider;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.util.Arrays;
import java.util.List;

/**
 * Implementation of the {@link org.jahia.services.content.JCRStoreProvider} for the {@link org.jahia.modules.external.ExternalData}.
 *
 * @author Thomas Draier
 */
public class ExternalContentStoreProvider extends JCRStoreProvider implements InitializingBean {

    private boolean readOnly;

    private ExternalDataSource dataSource;

    private String id;

    private ExternalProviderInitializerService externalProviderInitializerService;

    private List<String> extendableTypes;
    private List<String> nonExtendableMixins;
    private List<String> overridableItems;
    private List<String> reservedNodes = Arrays.asList("j:acl", "j:workflowRules", "thumbnail");

    private boolean slowConnection = true;
    private boolean lockSupport = false;

    @Override
    protected Repository createRepository() {
        JCRStoreProvider defaultProvider = JCRSessionFactory.getInstance().getDefaultProvider();
        NamespaceRegistry namespaceRegistry;
        JCRSessionWrapper systemSession = null;
        try {
            systemSession = defaultProvider.getSystemSession();
            namespaceRegistry = systemSession.getProviderSession(defaultProvider).getWorkspace().getNamespaceRegistry();
        } catch (RepositoryException e) {
            throw new JahiaRuntimeException(e);
        } finally {
            if (systemSession != null) {
                systemSession.logout();
            }
        }
        ExternalRepositoryImpl instance = new ExternalRepositoryImpl(this, dataSource,
                new ExternalAccessControlManager(namespaceRegistry, readOnly), namespaceRegistry);
        instance.setProviderKey(getKey());

        return instance;
    }

    @Override
    public void start() throws JahiaInitializationException {
        // Enable lock
        if (lockSupport) {
            if (overridableItems != null) {
                overridableItems.addAll(externalProviderInitializerService.getOverridableItemsForLocks());
            } else {
                overridableItems = externalProviderInitializerService.getOverridableItemsForLocks();
            }
        }

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

    public ExternalDataSource getDataSource() {
        return dataSource;
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
        id = StringUtils.leftPad(externalProviderInitializerService.getProviderId(getKey()).toString(), 8, "f");
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

    public ExternalProviderInitializerService getExternalProviderInitializerService() {
        return externalProviderInitializerService;
    }

    public void setExternalProviderInitializerService(ExternalProviderInitializerService mappingService) {
        this.externalProviderInitializerService = mappingService;
    }

    public JCRStoreProvider getExtensionProvider() {
        return extendableTypes != null || overridableItems != null ? externalProviderInitializerService.getExtensionProvider():null;
    }

    public List<String> getExtendableTypes() {
        return extendableTypes;
    }

    public void setExtendableTypes(List<String> extendableTypes) {
        this.extendableTypes = extendableTypes;
    }

    public List<String> getOverridableItems() {
        return overridableItems;
    }

    public void setOverridableItems(List<String> overridableItems) {
        this.overridableItems = overridableItems;
    }

    public List<String> getNonExtendableMixins() {
        return nonExtendableMixins;
    }

    public void setNonExtendableMixins(List<String> nonExtendableMixins) {
        this.nonExtendableMixins = nonExtendableMixins;
    }

    public void setLockSupport(boolean lockSupport) {
        this.lockSupport = lockSupport;
    }

    public boolean isSlowConnection() {
        return slowConnection;
    }

    public void setSlowConnection(boolean slowConnection) {
        this.slowConnection = slowConnection;
    }

    public List<String> getReservedNodes() {
        return reservedNodes;
    }

    public void setReservedNodes(List<String> reservedNodes) {
        this.reservedNodes = reservedNodes;
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
        return getExternalProviderInitializerService().getInternalIdentifier(externalId, getKey());
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
        return getExternalProviderInitializerService().mapInternalIdentifier(externalId, getKey(), getId());
    }
}
