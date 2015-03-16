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
    private List<String> overridableItems;
    private List<String> nonOverridableItems;
    private List<String> reservedNodes = Arrays.asList("j:acl", "j:workflowRules", "thumbnail");

    private List<String> ignorePropertiesForExport = Arrays.asList("j:extendedType","j:isExternalProviderRoot","j:externalNodeIdentifier");

    private boolean slowConnection = true;
    private boolean lockSupport = false;

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
                        || (property instanceof ExtensionProperty && !ignorePropertiesForExport.contains(property.getName())));
        } catch (RepositoryException e) {
            logger.error("Error while checking property name", e);
        }
        return false;
    }
}
