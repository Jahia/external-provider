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
import org.jahia.modules.external.ExternalDataSource.AdvancedSearchable;
import org.jahia.modules.external.ExternalDataSource.SimpleSearchable;
import org.jahia.services.content.nodetypes.ExtendedNodeType;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.*;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.query.*;
import javax.jcr.query.qom.*;
import java.util.*;

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
            return new ExecutableExternalQuery(qomTree.getSource(), qomTree.getConstraint(), qomTree.getOrderings(), qomTree.getColumns());
        }
    }

    class ExecutableExternalQuery extends ExternalQuery {
        
        ExecutableExternalQuery(Source source, Constraint constraints, Ordering[] orderings, Column[] columns) {
            super(source, constraints, orderings, columns);
        }
        
        private void addConstraints(Map<String, String> search, Constraint constraint) throws RepositoryException {
            if (constraint instanceof And) {
                addConstraints(search, ((And) constraint).getConstraint1());
                addConstraints(search, ((And) constraint).getConstraint2());
            } else if (constraint instanceof Comparison) {
                Comparison comparison = (Comparison) constraint;
                if (comparison.getOperand1() instanceof PropertyValue &&
                    comparison.getOperand2() instanceof Literal &&
                        comparison.getOperator().equals(QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO)) {
                    search.put(((PropertyValue) comparison.getOperand1()).getPropertyName(), ((Literal) comparison.getOperand2()).getLiteralValue().getString());
                } else {
                    throw new UnsupportedRepositoryOperationException("Unsupported constraint : " + constraint.toString());
                }
            } else if (constraint instanceof DescendantNode) {
                String root = ((DescendantNode) constraint).getAncestorPath();
                search.put("__rootPath", root);
                search.put("__searchSubNodes", "true");
            } else if (constraint instanceof ChildNode) {
                String root = ((ChildNode) constraint).getParentPath();
                search.put("__rootPath", root);
                search.put("__searchSubNodes", "false");
            } else {
                throw new UnsupportedRepositoryOperationException("Unsupported constraint : " + constraint.toString());
            }
        }

        @Override
        public QueryResult execute() throws InvalidQueryException, RepositoryException {
            // do a check for supported node types
            if (!isNodeTypeSupported()) {
                return new ExternalQueryResult(this, Collections.<String> emptyList(), workspace);
            }

            ExternalDataSource dataSource = workspace.getSession().getRepository().getDataSource();
            List<String> results = null;
            if (dataSource instanceof AdvancedSearchable) {
                results = ((ExternalDataSource.AdvancedSearchable) dataSource).search(this);
            } else if (dataSource instanceof SimpleSearchable) {
                results = getSimpleSearchResults();
            } else {
                throw new UnsupportedOperationException("Unknown implementation of Searchable external data source");
            }

            return new ExternalQueryResult(this, results, workspace);
        }

        private List<String> getSimpleSearchResults() throws RepositoryException {
            Map<String, String> search = new HashMap<String, String>();

            try {
                if (getConstraint() != null) {
                    addConstraints(search, getConstraint());
                }
            } catch (RepositoryException e) {
                logger.error("Error when executing query on external provider:" + e.getMessage());
                return Collections.emptyList();
            }
            String root = search.get("__rootpath");
            if (root != null) {
                String mountPoint = workspace.getSession().getRepository().getStoreProvider().getMountPoint();
                if (!mountPoint.startsWith(root) || !root.startsWith(mountPoint)) {
                    return Collections.emptyList();
                }
            }

            return ((ExternalDataSource.SimpleSearchable) workspace.getSession().getRepository().getDataSource())
                    .search(root, ((Selector) getSource()).getNodeTypeName(), search, null, true, getOffset(), getLimit());
        }

        private boolean isNodeTypeSupported() throws NoSuchNodeTypeException {
            if (!(getSource() instanceof Selector)) {
                return false;
            }

            NodeTypeRegistry ntRegistry = NodeTypeRegistry.getInstance();
            ExtendedNodeType type = ntRegistry.getNodeType(((Selector) getSource()).getNodeTypeName());

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
}
