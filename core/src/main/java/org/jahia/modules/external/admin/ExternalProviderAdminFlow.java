/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2014 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.modules.external.admin;

import org.apache.jackrabbit.core.query.QOMQueryFactory;
import org.jahia.modules.external.*;
import org.jahia.services.content.*;
import org.jahia.services.content.nodetypes.ExtendedNodeType;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.jahia.services.query.QueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.query.Query;
import javax.jcr.query.qom.*;
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

    public void unmountProvider(String mountpoint) throws RepositoryException {
        JCRStoreProvider provider =  jcrStoreService.getSessionFactory().getMountPoints().get(mountpoint);
        if (provider != null && provider.isDynamicallyMounted()) {
            provider.stop();
        }
        JCRSessionWrapper session = jcrStoreService.getSessionFactory().getCurrentUserSession();
        JCRNodeWrapper node = session.getNode(mountpoint);
        node.remove();
        session.save();
    }

    public DataSourceInfo getDatasourceInfo(String mountpoint) throws RepositoryException {
        ExternalContentStoreProvider provider = (ExternalContentStoreProvider) jcrStoreService.getSessionFactory().getMountPoints().get(mountpoint);

        ExternalDataSource dataSource = provider.getDataSource();
        DataSourceInfo dataSourceInfo = getBaseDataSourceInfo(dataSource);

        ExternalData data = dataSource.getItemByPath("/");
        dataSourceInfo.setRootNodeType(data.getType());

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
