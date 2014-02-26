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

package org.jahia.modules.external;

import javax.jcr.*;
import javax.jcr.query.Query;

import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;

import java.util.*;

/**
 * Implementation of the {@link javax.jcr.Repository} for the {@link org.jahia.modules.external.ExternalData}.
 * 
 * @author toto
 */
public class ExternalRepositoryImpl implements Repository {

    @SuppressWarnings("deprecation")
    private static final Set<String> STANDARD_KEYS = new HashSet<String>() {
        private static final long serialVersionUID = -7206797627602056140L;
        {
        add(Repository.QUERY_FULL_TEXT_SEARCH_SUPPORTED);
        add(Repository.QUERY_JOINS);
        add(Repository.QUERY_LANGUAGES);
        add(Repository.QUERY_STORED_QUERIES_SUPPORTED);
        add(Repository.QUERY_XPATH_DOC_ORDER);
        add(Repository.QUERY_XPATH_POS_INDEX);
        add(Repository.REP_NAME_DESC);
        add(Repository.REP_VENDOR_DESC);
        add(Repository.REP_VENDOR_URL_DESC);
        add(Repository.SPEC_NAME_DESC);
        add(Repository.SPEC_VERSION_DESC);
        add(Repository.WRITE_SUPPORTED);
        add(Repository.IDENTIFIER_STABILITY);
        add(Repository.LEVEL_1_SUPPORTED);
        add(Repository.LEVEL_2_SUPPORTED);

        add(Repository.OPTION_NODE_TYPE_MANAGEMENT_SUPPORTED);
        add(Repository.NODE_TYPE_MANAGEMENT_AUTOCREATED_DEFINITIONS_SUPPORTED);
        add(Repository.NODE_TYPE_MANAGEMENT_INHERITANCE);
        add(Repository.NODE_TYPE_MANAGEMENT_MULTIPLE_BINARY_PROPERTIES_SUPPORTED);
        add(Repository.NODE_TYPE_MANAGEMENT_MULTIVALUED_PROPERTIES_SUPPORTED);
        add(Repository.NODE_TYPE_MANAGEMENT_ORDERABLE_CHILD_NODES_SUPPORTED);
        add(Repository.NODE_TYPE_MANAGEMENT_OVERRIDES_SUPPORTED);
        add(Repository.NODE_TYPE_MANAGEMENT_PRIMARY_ITEM_NAME_SUPPORTED);
        add(Repository.NODE_TYPE_MANAGEMENT_PROPERTY_TYPES);
        add(Repository.NODE_TYPE_MANAGEMENT_RESIDUAL_DEFINITIONS_SUPPORTED);
        add(Repository.NODE_TYPE_MANAGEMENT_SAME_NAME_SIBLINGS_SUPPORTED);
        add(Repository.NODE_TYPE_MANAGEMENT_VALUE_CONSTRAINTS_SUPPORTED);
        add(Repository.NODE_TYPE_MANAGEMENT_UPDATE_IN_USE_SUPORTED);
        add(Repository.OPTION_ACCESS_CONTROL_SUPPORTED);
        add(Repository.OPTION_JOURNALED_OBSERVATION_SUPPORTED);
        add(Repository.OPTION_LIFECYCLE_SUPPORTED);
        add(Repository.OPTION_LOCKING_SUPPORTED);
        add(Repository.OPTION_OBSERVATION_SUPPORTED);
        add(Repository.OPTION_NODE_AND_PROPERTY_WITH_SAME_NAME_SUPPORTED);
        add(Repository.OPTION_QUERY_SQL_SUPPORTED);
        add(Repository.OPTION_RETENTION_SUPPORTED);
        add(Repository.OPTION_SHAREABLE_NODES_SUPPORTED);
        add(Repository.OPTION_SIMPLE_VERSIONING_SUPPORTED);
        add(Repository.OPTION_TRANSACTIONS_SUPPORTED);
        add(Repository.OPTION_UNFILED_CONTENT_SUPPORTED);
        add(Repository.OPTION_UPDATE_MIXIN_NODE_TYPES_SUPPORTED);
        add(Repository.OPTION_UPDATE_PRIMARY_NODE_TYPE_SUPPORTED);
        add(Repository.OPTION_VERSIONING_SUPPORTED);
        add(Repository.OPTION_WORKSPACE_MANAGEMENT_SUPPORTED);
        add(Repository.OPTION_XML_EXPORT_SUPPORTED);
        add(Repository.OPTION_XML_IMPORT_SUPPORTED);
        add(Repository.OPTION_ACTIVITIES_SUPPORTED);
        add(Repository.OPTION_BASELINES_SUPPORTED);

    }};
    private ExternalAccessControlManager accessControlManager;

    private ExternalDataSource dataSource;

    private DefaultNamePathResolver namePathResolver;

    private NamespaceRegistry namespaceRegistry;

    private String providerKey;
    
    private Map<String, Object> repositoryDescriptors = new HashMap<String, Object>();
    private ExternalContentStoreProvider storeProvider;

    public ExternalRepositoryImpl(ExternalContentStoreProvider storeProvider, ExternalDataSource dataSource, ExternalAccessControlManager accessControlManager, NamespaceRegistry nsRegistry) {
        this.storeProvider = storeProvider;
        this.dataSource = dataSource;
        this.accessControlManager = accessControlManager;
        this.namespaceRegistry = nsRegistry;
        this.namePathResolver = new DefaultNamePathResolver(nsRegistry);
        initDescriptors();
    }

    protected ExternalAccessControlManager getAccessControlManager() {
        return accessControlManager;
    }

    public ExternalDataSource getDataSource() {
        return dataSource;
    }

    public String getDescriptor(String s) {
        Object descriptorObject = repositoryDescriptors.get(s);
        if (descriptorObject instanceof Value) {
            return descriptorObject.toString();
        } else {
            return null;
        }
    }

    public String[] getDescriptorKeys() {
        return repositoryDescriptors.keySet().toArray(new String[repositoryDescriptors.size()]);
    }

    public Value getDescriptorValue(String key) {
        final Object descriptorObject = repositoryDescriptors.get(key);
        if (descriptorObject instanceof Value) {
            return (Value) descriptorObject;
        }
        return null;
    }

    public Value[] getDescriptorValues(String key) {
        final Object descriptorObject = repositoryDescriptors.get(key);
        if (descriptorObject instanceof Value[]) {
            return (Value[]) descriptorObject;
        }
        return null;
    }

    public DefaultNamePathResolver getNamePathResolver() {
        return namePathResolver;
    }

    public NamespaceRegistry getNamespaceRegistry() {
        return namespaceRegistry;
    }

    public String getProviderKey() {
        return providerKey;
    }

    public ExternalContentStoreProvider getStoreProvider() {
        return storeProvider;
    }

    @SuppressWarnings("deprecation")
    private void initDescriptors() {

        repositoryDescriptors.put(Repository.SPEC_VERSION_DESC, new ExternalValueImpl("2.0"));
        repositoryDescriptors.put(Repository.SPEC_NAME_DESC, new ExternalValueImpl("Content Repository for Java Technology API"));
        repositoryDescriptors.put(Repository.REP_VENDOR_DESC, new ExternalValueImpl("Jahia"));
        repositoryDescriptors.put(Repository.REP_VENDOR_URL_DESC, new ExternalValueImpl("http://www.jahia.org"));
        repositoryDescriptors.put(Repository.REP_NAME_DESC, new ExternalValueImpl("The Web Integration Software"));
        repositoryDescriptors.put(Repository.REP_VERSION_DESC, new ExternalValueImpl("1.0"));
        repositoryDescriptors.put(Repository.WRITE_SUPPORTED, new ExternalValueImpl(true));
        repositoryDescriptors.put(Repository.IDENTIFIER_STABILITY, new ExternalValueImpl(Repository.IDENTIFIER_STABILITY_SESSION_DURATION));
        repositoryDescriptors.put(Repository.NODE_TYPE_MANAGEMENT_INHERITANCE, new ExternalValueImpl(Repository.NODE_TYPE_MANAGEMENT_INHERITANCE_MINIMAL));
        repositoryDescriptors.put(Repository.NODE_TYPE_MANAGEMENT_OVERRIDES_SUPPORTED, new ExternalValueImpl(false));
        repositoryDescriptors.put(Repository.NODE_TYPE_MANAGEMENT_PRIMARY_ITEM_NAME_SUPPORTED, new ExternalValueImpl(false));
        repositoryDescriptors.put(Repository.NODE_TYPE_MANAGEMENT_ORDERABLE_CHILD_NODES_SUPPORTED, new ExternalValueImpl(false));
        repositoryDescriptors.put(Repository.NODE_TYPE_MANAGEMENT_RESIDUAL_DEFINITIONS_SUPPORTED, new ExternalValueImpl(false));
        repositoryDescriptors.put(Repository.NODE_TYPE_MANAGEMENT_AUTOCREATED_DEFINITIONS_SUPPORTED, new ExternalValueImpl(false));
        repositoryDescriptors.put(Repository.NODE_TYPE_MANAGEMENT_SAME_NAME_SIBLINGS_SUPPORTED, new ExternalValueImpl(false));
        List<ExternalValueImpl> propertyTypes = new ArrayList<ExternalValueImpl>();
        propertyTypes.add(new ExternalValueImpl(PropertyType.BINARY));
        propertyTypes.add(new ExternalValueImpl(PropertyType.NAME));
        propertyTypes.add(new ExternalValueImpl(PropertyType.PATH));
        repositoryDescriptors.put(Repository.NODE_TYPE_MANAGEMENT_PROPERTY_TYPES, propertyTypes.toArray(new ExternalValueImpl[propertyTypes.size()]));
        repositoryDescriptors.put(Repository.NODE_TYPE_MANAGEMENT_MULTIVALUED_PROPERTIES_SUPPORTED, new ExternalValueImpl(false));
        repositoryDescriptors.put(Repository.NODE_TYPE_MANAGEMENT_MULTIPLE_BINARY_PROPERTIES_SUPPORTED, new ExternalValueImpl(false));
        repositoryDescriptors.put(Repository.NODE_TYPE_MANAGEMENT_VALUE_CONSTRAINTS_SUPPORTED, new ExternalValueImpl(false));
        repositoryDescriptors.put(Repository.NODE_TYPE_MANAGEMENT_UPDATE_IN_USE_SUPORTED, new ExternalValueImpl(false));
        if (dataSource instanceof ExternalDataSource.Searchable || getStoreProvider().getExtensionProvider() != null) {
            repositoryDescriptors.put(Repository.QUERY_LANGUAGES, new ExternalValueImpl[] { new ExternalValueImpl(Query.JCR_SQL2) } );
        } else {
            repositoryDescriptors.put(Repository.QUERY_LANGUAGES, new ExternalValueImpl[0]);
        }
        repositoryDescriptors.put(Repository.QUERY_STORED_QUERIES_SUPPORTED, new ExternalValueImpl(false));
        repositoryDescriptors.put(Repository.QUERY_FULL_TEXT_SEARCH_SUPPORTED, new ExternalValueImpl(false));
        repositoryDescriptors.put(Repository.QUERY_JOINS, new ExternalValueImpl(false));
        repositoryDescriptors.put(Repository.LEVEL_1_SUPPORTED, new ExternalValueImpl(true));
        repositoryDescriptors.put(Repository.LEVEL_2_SUPPORTED, new ExternalValueImpl(true));
        repositoryDescriptors.put(Repository.QUERY_XPATH_POS_INDEX, new ExternalValueImpl(false));
        repositoryDescriptors.put(Repository.QUERY_XPATH_DOC_ORDER, new ExternalValueImpl(false));

        repositoryDescriptors.put(Repository.OPTION_ACCESS_CONTROL_SUPPORTED, new ExternalValueImpl(false));
        repositoryDescriptors.put(Repository.OPTION_ACTIVITIES_SUPPORTED, new ExternalValueImpl(false));
        repositoryDescriptors.put(Repository.OPTION_BASELINES_SUPPORTED, new ExternalValueImpl(false));
        repositoryDescriptors.put(Repository.OPTION_JOURNALED_OBSERVATION_SUPPORTED, new ExternalValueImpl(false));
        repositoryDescriptors.put(Repository.OPTION_LIFECYCLE_SUPPORTED, new ExternalValueImpl(false));
        repositoryDescriptors.put(Repository.OPTION_LOCKING_SUPPORTED, new ExternalValueImpl(false));
        repositoryDescriptors.put(Repository.OPTION_NODE_AND_PROPERTY_WITH_SAME_NAME_SUPPORTED, new ExternalValueImpl(false));
        repositoryDescriptors.put(Repository.OPTION_NODE_TYPE_MANAGEMENT_SUPPORTED, new ExternalValueImpl(false));
        repositoryDescriptors.put(Repository.OPTION_OBSERVATION_SUPPORTED, new ExternalValueImpl(false));
        repositoryDescriptors.put(Repository.OPTION_QUERY_SQL_SUPPORTED, new ExternalValueImpl(false));
        repositoryDescriptors.put(Repository.OPTION_RETENTION_SUPPORTED, new ExternalValueImpl(false));
        repositoryDescriptors.put(Repository.OPTION_SHAREABLE_NODES_SUPPORTED, new ExternalValueImpl(false));
        repositoryDescriptors.put(Repository.OPTION_SIMPLE_VERSIONING_SUPPORTED, new ExternalValueImpl(false));
        repositoryDescriptors.put(Repository.OPTION_TRANSACTIONS_SUPPORTED, new ExternalValueImpl(false));
        repositoryDescriptors.put(Repository.OPTION_UNFILED_CONTENT_SUPPORTED, new ExternalValueImpl(false));
        repositoryDescriptors.put(Repository.OPTION_UPDATE_MIXIN_NODE_TYPES_SUPPORTED, new ExternalValueImpl(false));
        repositoryDescriptors.put(Repository.OPTION_UPDATE_PRIMARY_NODE_TYPE_SUPPORTED, new ExternalValueImpl(false));
        repositoryDescriptors.put(Repository.OPTION_VERSIONING_SUPPORTED, new ExternalValueImpl(false));
        repositoryDescriptors.put(Repository.OPTION_WORKSPACE_MANAGEMENT_SUPPORTED, new ExternalValueImpl(false));
        repositoryDescriptors.put(Repository.OPTION_XML_EXPORT_SUPPORTED, new ExternalValueImpl(false));
        repositoryDescriptors.put(Repository.OPTION_XML_IMPORT_SUPPORTED, new ExternalValueImpl(false));
    }

    public boolean isSingleValueDescriptor(String key) {
        return repositoryDescriptors.get(key) instanceof Value;
    }
    
    public boolean isStandardDescriptor(String key) {
        return STANDARD_KEYS.contains(key);
    }

    public Session login() throws LoginException, RepositoryException {
        return new ExternalSessionImpl(this, null, null);
    }

    public Session login(Credentials credentials) throws LoginException, RepositoryException {
        return new ExternalSessionImpl(this, credentials, null);
    }

    public Session login(Credentials credentials, String workspaceName) throws LoginException, NoSuchWorkspaceException, RepositoryException {
        return new ExternalSessionImpl(this, credentials, workspaceName);
    }

    public Session login(String workspaceName) throws LoginException, NoSuchWorkspaceException, RepositoryException {
        return new ExternalSessionImpl(this, null, workspaceName);
    }

    public void setProviderKey(String providerKey) {
        this.providerKey = providerKey;
    }

}
