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

package org.jahia.modules.external.query;

import org.apache.jackrabbit.commons.query.sql2.Parser;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.query.qom.QueryObjectModelFactoryImpl;
import org.apache.jackrabbit.spi.commons.query.qom.QueryObjectModelTree;
import org.jahia.modules.external.ExternalDataSource;
import org.jahia.modules.external.ExternalQuery;
import org.jahia.modules.external.ExternalWorkspaceImpl;
import org.jahia.services.content.nodetypes.ExtendedNodeType;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.qom.*;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Implementation of the {@link javax.jcr.query.QueryManager} for the {@link org.jahia.modules.external.ExternalData}.
 */
public class ExternalQueryManager implements QueryManager {
    private static final String[] SUPPORTED_LANGUAGES = new String[] { Query.JCR_SQL2 };

    private static Logger logger = LoggerFactory.getLogger(ExternalQueryManager.class);

    private ExternalWorkspaceImpl workspace;

    public ExternalQueryManager(ExternalWorkspaceImpl workspace) {
        this.workspace = workspace;
    }

    public Query createQuery(String statement, String language) throws InvalidQueryException, RepositoryException {
        if (!language.equals(Query.JCR_SQL2)) {
            throw new InvalidQueryException("Unsupported query language");
        }
        Parser p = new Parser(getQOMFactory(), workspace.getSession().getValueFactory());
        return p.createQueryObjectModel(statement);
    }

    public QueryObjectModelFactory getQOMFactory() {
        return new MyQOMFactory(workspace.getSession().getRepository().getNamePathResolver());
    }

    public Query getQuery(Node node) throws InvalidQueryException, RepositoryException {
        return null;  
    }

    public String[] getSupportedQueryLanguages() throws RepositoryException {
        return SUPPORTED_LANGUAGES;
    }

    class MyQOMFactory extends QueryObjectModelFactoryImpl implements QueryObjectModelFactory {
        MyQOMFactory(NamePathResolver resolver) {
            super(resolver);
        }

        @Override
        protected QueryObjectModel createQuery(QueryObjectModelTree qomTree) throws InvalidQueryException, RepositoryException {
            if (!isNodeTypeSupported(qomTree)) {
                return null;
            }
            return new ExecutableExternalQuery(qomTree.getSource(), qomTree.getConstraint(), qomTree.getOrderings(), qomTree.getColumns());
        }

        private boolean isNodeTypeSupported(QueryObjectModelTree qomTree) throws NoSuchNodeTypeException {
            if (!(qomTree.getSource() instanceof Selector)) {
                return false;
            }

            NodeTypeRegistry ntRegistry = NodeTypeRegistry.getInstance();
            ExtendedNodeType type = ntRegistry.getNodeType(((Selector) qomTree.getSource()).getNodeTypeName());

            // check supported node types
            String nodeType = type.getName();
            Set<String> supportedNodeTypes = workspace.getSession().getRepository().getDataSource()
                    .getSupportedNodeTypes();
            if (supportedNodeTypes.contains(nodeType)) {
                return true;
            }
            for (String supportedNodeType : supportedNodeTypes) {
                if (ntRegistry.getNodeType(supportedNodeType).isNodeType(nodeType)) {
                    return true;
                }
            }

            return false;
        }

    }

    class ExecutableExternalQuery extends ExternalQuery {
        
        ExecutableExternalQuery(Source source, Constraint constraints, Ordering[] orderings, Column[] columns) {
            super(source, constraints, orderings, columns);
        }
        
        @Override
        public QueryResult execute() throws InvalidQueryException, RepositoryException {
            ExternalDataSource dataSource = workspace.getSession().getRepository().getDataSource();
            List<String> results = null;
            try {
                results = ((ExternalDataSource.Searchable) dataSource).search(this);
                if (getLimit() > -1 && results.size() > getLimit()) {
                    results = results.subList(0, (int) getLimit());
                }
            } catch (UnsupportedRepositoryOperationException e) {
                logger.warn("Unsupported query ", e);
                results = Collections.emptyList();
            }
            return new ExternalQueryResult(this, results, workspace);
        }

    }
}
