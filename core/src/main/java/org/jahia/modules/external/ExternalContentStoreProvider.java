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
package org.jahia.modules.external;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.commons.iterator.PropertyIteratorAdapter;
import org.jahia.api.Constants;
import org.jahia.exceptions.JahiaInitializationException;
import org.jahia.exceptions.JahiaRuntimeException;
import org.jahia.services.content.*;
import org.jahia.services.content.nodetypes.Name;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import javax.jcr.*;
import javax.jcr.query.QueryManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implementation of the {@link org.jahia.services.content.JCRStoreProvider} for the {@link org.jahia.modules.external.ExternalData}.
 *
 * @author Thomas Draier
 */
public class ExternalContentStoreProvider extends JCRStoreProvider implements InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(ExternalContentStoreProvider.class);

    private static final ThreadLocal<ExternalSessionImpl> currentSession = new ThreadLocal<ExternalSessionImpl>();

    private boolean readOnly;

    private ExternalDataSource dataSource;

    private String id;

    private ExternalProviderInitializerService externalProviderInitializerService;

    private List<String> extendableTypes;
    private List<String> nonExtendableMixins;
    private List<String> overridableItems;
    private List<String> nonOverridableItems;
    private List<String> reservedNodes = Arrays.asList("j:acl", "j:workflowRules", "j:conditionalVisibility", "thumbnail");

    private List<String> ignorePropertiesForExport = Arrays.asList("j:extendedType","j:isExternalProviderRoot","j:externalNodeIdentifier");

    private boolean slowConnection = true;
    private boolean lockSupport = false;
    private boolean cacheKeyOnReferenceSupport = false;
    private boolean aclSupport = true;

    public static ExternalSessionImpl getCurrentSession() {
        return currentSession.get();
    }

    public static void setCurrentSession(ExternalSessionImpl session) {
        currentSession.set(session);
    }

    public static void removeCurrentSession() {
        currentSession.remove();
    }

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
        ExternalRepositoryImpl instance = new ExternalRepositoryImpl(this, dataSource, namespaceRegistry);
        instance.setProviderKey(getKey());

        return instance;
    }

    @Override
    public boolean start(boolean checkAvailability) throws JahiaInitializationException {

        // Enable acl
        if(aclSupport) {
            if (overridableItems != null) {
                List<String> l = new ArrayList<>();
                l.addAll(overridableItems);
                l.addAll(externalProviderInitializerService.getOverridableItemsForACLs());
                overridableItems = l;
            } else {
                overridableItems = new ArrayList<>(externalProviderInitializerService.getOverridableItemsForACLs());
            }
        }

        // Enable lock
        if (lockSupport) {
            if (overridableItems != null) {
                List<String> l = new ArrayList<>();
                l.addAll(overridableItems);
                l.addAll(externalProviderInitializerService.getOverridableItemsForLocks());
                overridableItems = l;
            } else {
                overridableItems = new ArrayList<>(externalProviderInitializerService.getOverridableItemsForLocks());
            }
        }

        getId(); // initialize ID
        if (dataSource instanceof ExternalDataSource.Initializable) {
            ((ExternalDataSource.Initializable) dataSource).start();
        }
        return super.start(checkAvailability);
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

    public List<String> getNonOverridableItems() {
        return nonOverridableItems;
    }

    public void setNonOverridableItems(List<String> nonOverridableItems) {
        this.nonOverridableItems = nonOverridableItems;
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

    public void setAclSupport(boolean aclSupport) {
        this.aclSupport = aclSupport;
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
     * @return a generated internal UUID
     * @throws RepositoryException
     *             in case an internal identifier cannot be stored into the database
     */
    protected String mapInternalIdentifier(String externalId) throws RepositoryException {
        return getExternalProviderInitializerService().mapInternalIdentifier(externalId, getKey(), getId());
    }

    /**
     * Get internal UUID of the specified node or generate a new if it doesn't exist
     *
     * @param externalId
     *            the external ID to generate UUID for
     * @return an internal UUID
     * @throws RepositoryException
     *             in case an internal identifier cannot be stored into the database
     */
    public String getOrCreateInternalIdentifier(String externalId) throws RepositoryException {
        String internalId = getInternalIdentifier(externalId);
        if (internalId == null) {
            // not mapped yet -> store mapping
            internalId = mapInternalIdentifier(externalId);
        }
        return internalId;
    }

    public PropertyIterator getWeakReferences(JCRNodeWrapper node, String propertyName, Session session) throws RepositoryException {
        if (dataSource instanceof ExternalDataSource.Referenceable && session instanceof ExternalSessionImpl) {
            String identifier = node.getIdentifier();
            if (this.equals(node.getProvider())) {
                identifier = ((ExternalNodeImpl) node.getRealNode()).getData().getId();
            }
            setCurrentSession((ExternalSessionImpl) session);
            List<String> referringProperties = null;
            try {
                referringProperties = ((ExternalDataSource.Referenceable) dataSource).getReferringProperties(identifier, propertyName);
            } finally {
                ExternalContentStoreProvider.removeCurrentSession();
            }
            if (referringProperties == null) {
                return null;
            }
            if (referringProperties.isEmpty()) {
                return PropertyIteratorAdapter.EMPTY;
            }
            List<Property> l = new ArrayList<Property>();
            for (String propertyPath : referringProperties) {
                String nodePath = StringUtils.substringBeforeLast(propertyPath, "/");
                if (nodePath.isEmpty()) {
                    nodePath = "/";
                }
                ExternalNodeImpl referringNode = (ExternalNodeImpl) session.getNode(nodePath);
                if (referringNode != null) {
                    l.add(new ExternalPropertyImpl(
                            new Name(StringUtils.substringAfterLast(propertyPath, "/"), NodeTypeRegistry.getInstance().getNamespaces()),
                            referringNode, (ExternalSessionImpl) session));
                }
            }
            return new PropertyIteratorAdapter(l);
        }
        return null;
    }

    @Override
    public boolean canExportNode(Node node) {
        try {
            if (node instanceof JCRNodeWrapper) {
                node = ((JCRNodeWrapper) node).getRealNode();
            }
            return Constants.EDIT_WORKSPACE.equals(node.getSession().getWorkspace().getName())
                    && (node instanceof ExtensionNode
                        || (node instanceof ExternalNodeImpl && ((ExternalNodeImpl) node).getExtensionNode(false) != null));
        } catch (RepositoryException e) {
            logger.error("Error while checking if an extension node exists", e);
        }
        return false;
    }

    @Override
    public boolean canExportProperty(Property property) {
        try {
            if (property instanceof JCRPropertyWrapper) {
                property = ((JCRNodeWrapper) property.getParent()).getRealNode().getProperty(property.getName());
            }
            return Constants.EDIT_WORKSPACE.equals(property.getSession().getWorkspace().getName())
                    && ("jcr:primaryType".equals(property.getName()) || "jcr:mixinTypes".equals(property.getName())
                        || ((property instanceof ExternalPropertyImpl || property instanceof ExtensionProperty) && !ignorePropertiesForExport.contains(property.getName())));
        } catch (RepositoryException e) {
            logger.error("Error while checking property name", e);
        }
        return false;
    }

    /**
     * if true,if a content from the provider is referenced, it will be added to the cache key
     * For example it is used in the Alfresco CMIS provider as a content may not be available for all users
     * by default is false
     * @return
     */
    public boolean isCacheKeyOnReferenceSupport() {
        return cacheKeyOnReferenceSupport;
    }

    public void setCacheKeyOnReferenceSupport(boolean cacheKeyOnReferenceSupport) {
        this.cacheKeyOnReferenceSupport = cacheKeyOnReferenceSupport;
    }
}
