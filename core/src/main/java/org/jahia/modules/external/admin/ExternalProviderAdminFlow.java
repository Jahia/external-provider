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
package org.jahia.modules.external.admin;

import org.jahia.modules.external.ExternalContentStoreProvider;
import org.jahia.modules.external.ExternalDataSource;
import org.jahia.modules.external.ExternalProviderInitializerService;
import org.jahia.modules.external.ExternalQuery;
import org.jahia.services.content.JCRStoreProvider;
import org.jahia.services.content.JCRStoreService;
import org.jahia.services.content.ProviderFactory;
import org.jahia.services.content.QueryManagerWrapper;
import org.jahia.services.content.nodetypes.ExtendedNodeType;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.jahia.services.query.QueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.query.Query;
import java.io.Serializable;
import java.util.*;

public class ExternalProviderAdminFlow implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(ExternalProviderAdminFlow.class);

    private transient JCRStoreService jcrStoreService;
    private transient ExternalProviderInitializerService initializationService;

    @Autowired
    public void setInitializationService(ExternalProviderInitializerService initializationService) {
        this.initializationService = initializationService;
    }

    @Autowired
    public void setJcrStoreService(JCRStoreService jcrStoreService) {
        this.jcrStoreService = jcrStoreService;
    }

    public List<MountInfo> getMountedProviders() throws RepositoryException {
        List<MountInfo> l = new ArrayList<MountInfo>();

        Map<String, DataSourceInfo> infos = new HashMap<String, DataSourceInfo>();

        for (Map.Entry<String, JCRStoreProvider> entry : jcrStoreService.getSessionFactory().getMountPoints().entrySet()) {
            if (entry.getValue() instanceof ExternalContentStoreProvider) {
                ExternalContentStoreProvider jcrStoreProvider = (ExternalContentStoreProvider) entry.getValue();
                MountInfo m = new MountInfo();
                m.setKey(jcrStoreProvider.getKey());
                m.setId(initializationService.getProviderId(jcrStoreProvider.getKey()));
                m.setMountPoint(entry.getKey());
                final ExternalDataSource dataSource = jcrStoreProvider.getDataSource();
                if (!infos.containsKey(dataSource.getClass().getName())) {
                    DataSourceInfo p = getBaseDataSourceInfo(dataSource);
                    infos.put(dataSource.getClass().getName(), p);
                }
                m.setDataSource(infos.get(dataSource.getClass().getName()));
                m.setDynamic(jcrStoreProvider.isDynamicallyMounted());
                l.add(m);
            }
        }
        return l;
    }

    private DataSourceInfo getBaseDataSourceInfo(ExternalDataSource dataSource) {
        DataSourceInfo info = new DataSourceInfo();
        info.setClazz(dataSource.getClass().getName());
        info.setSearchable(dataSource instanceof ExternalDataSource.Searchable);
        info.setSupportsLazy(dataSource instanceof ExternalDataSource.LazyProperty);
        info.setWriteable(dataSource instanceof ExternalDataSource.Writable);
        info.setInitializable(dataSource instanceof ExternalDataSource.Initializable);
        info.setSupportsUuid(dataSource.isSupportsUuid());
        info.setSupportsHierarchicalIdentifiers(dataSource.isSupportsHierarchicalIdentifiers());
        info.setSupportedTypes(dataSource.getSupportedNodeTypes());
        return info;
    }

    public List<String> getProviderFactories() {
        List<String> l = new ArrayList<String>();

        for (ProviderFactory factory : jcrStoreService.getProviderFactories().values()) {
            try {
                ExtendedNodeType type = NodeTypeRegistry.getInstance().getNodeType(factory.getNodeTypeName());
                l.add(factory.getNodeTypeName());
            } catch (NoSuchNodeTypeException e) {
                logger.error("Cannot find factory type",e);
            }

        }
        return l;
    }

    public DataSourceInfo getDatasourceInfo(String mountpoint) throws RepositoryException {
        ExternalContentStoreProvider provider = (ExternalContentStoreProvider) jcrStoreService.getSessionFactory().getMountPoints().get(mountpoint);

        ExternalDataSource dataSource = provider.getDataSource();
        DataSourceInfo dataSourceInfo = getBaseDataSourceInfo(dataSource);
        dataSourceInfo.setRootNodeType(jcrStoreService.getSessionFactory().getCurrentUserSession().getNode(provider.getMountPoint()).getPrimaryNodeType().getName());

        dataSourceInfo.setExtendable(provider.getExtensionProvider() != null);
        dataSourceInfo.setOverridableItems(provider.getOverridableItems());
        dataSourceInfo.setNonOverridableItems(provider.getNonOverridableItems());
        dataSourceInfo.setExtendableTypes(provider.getExtendableTypes());

        if (dataSourceInfo.isSearchable()) {
            dataSourceInfo.setSupportedQueries(new LinkedHashMap<String, Boolean>());

            final QueryManagerWrapper queryManager = jcrStoreService.getSessionFactory().getCurrentUserSession().getWorkspace().getQueryManager();

            final ExternalDataSource.Searchable searchable = (ExternalDataSource.Searchable) dataSource;

            testQuery(provider, searchable, dataSourceInfo, queryManager.createQuery("SELECT n.* FROM [nt:base] AS n", Query.JCR_SQL2));
            testQuery(provider, searchable, dataSourceInfo, queryManager.createQuery("SELECT n.* FROM [nt:base] AS n WHERE CONTAINS(n.n, 'test')", Query.JCR_SQL2));
            testQuery(provider, searchable, dataSourceInfo, queryManager.createQuery("SELECT n.* FROM [nt:base] AS n WHERE ISDESCENDANTNODE(n, [/test])", Query.JCR_SQL2));
            testQuery(provider, searchable, dataSourceInfo, queryManager.createQuery("SELECT n.* FROM [nt:base] AS n WHERE (CONTAINS(n.n, 'test') OR CONTAINS(n.[jcr:title], 'test')) AND (n.[jcr:language] = 'test' OR NOT n.[jcr:language] IS NOT NULL) ORDER BY SCORE(n) DESC", Query.JCR_SQL2));
            testQuery(provider, searchable, dataSourceInfo, queryManager.createQuery("SELECT * from [nt:hierarchyNode] as n WHERE ((CONTAINS(n.[jcr:content],'acme') OR CONTAINS(n.[jcr:description],'acme') OR CONTAINS(n.[jcr:title],'acme') OR CONTAINS(n.[jcr:keywords],'acme') OR LOCALNAME(n) LIKE '%acme%')) ORDER BY SCORE() DESC", Query.JCR_SQL2));
        }


        return dataSourceInfo;
    }

    private void testQuery(ExternalContentStoreProvider provider, ExternalDataSource.Searchable dataSource, DataSourceInfo dataSourceInfo, Query query) {
        try {
            ExternalQuery q = (ExternalQuery) ((QueryWrapper)query).getQueries().get(provider);
            if (q != null) {
                dataSource.search(new ExternalQuery(q.getSource(), q.getConstraint(), q.getOrderings(), q.getColumns()));
                dataSourceInfo.getSupportedQueries().put(query.getStatement(), true);
            } else {
                dataSourceInfo.getSupportedQueries().put(query.getStatement(), false);
            }
        } catch (Exception e) {
            dataSourceInfo.getSupportedQueries().put(query.getStatement(), false);
        }
    }
}
