/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2019 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.external.query;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.commons.iterator.NodeIteratorAdapter;
import org.apache.jackrabbit.commons.query.sql2.Parser;
import org.apache.jackrabbit.core.query.JahiaSimpleQueryResult;
import org.apache.jackrabbit.core.query.lucene.CountRow;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.query.qom.QueryObjectModelFactoryImpl;
import org.apache.jackrabbit.spi.commons.query.qom.QueryObjectModelTree;
import org.jahia.modules.external.*;
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
    private static final String[] SUPPORTED_LANGUAGES = new String[]{Query.JCR_SQL2};
    private static final String FACET_MARKER = "rep:facet(";
    private static final String COUNT_MARKER = "rep:count(";
    private static final String EXTENSION_MIXIN = "jmix:externalProviderExtension";
    private static final String EXTENDED_TYPE_PROPERTY = "j:extendedType";
    private static final String EXTENSION_TYPE = "jnt:externalProviderExtension";

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
        return new ExternalQOMFactory(workspace.getSession().getRepository().getNamePathResolver());
    }

    public Query getQuery(Node node) throws InvalidQueryException, RepositoryException {
        return null;
    }

    public String[] getSupportedQueryLanguages() throws RepositoryException {
        return Arrays.copyOf(SUPPORTED_LANGUAGES, SUPPORTED_LANGUAGES.length);
    }

    /**
     * QOM Factory for external provider
     */
    class ExternalQOMFactory extends QueryObjectModelFactoryImpl implements QueryObjectModelFactory {
        ExternalQOMFactory(NamePathResolver resolver) {
            super(resolver);
        }

        @Override
        protected QueryObjectModel createQuery(QueryObjectModelTree qomTree) throws InvalidQueryException, RepositoryException {
            boolean nodeTypeSupported = isNodeTypeSupported(qomTree);
            boolean hasExtension = workspace.getSession().getExtensionSession() != null;
            if (!nodeTypeSupported && !hasExtension) {
                return null;
            }
            return new ExecutableExternalQuery(qomTree.getSource(), qomTree.getConstraint(), qomTree.getOrderings(), qomTree.getColumns(), nodeTypeSupported, hasExtension);
        }

        private boolean isNodeTypeSupported(QueryObjectModelTree qomTree) throws NoSuchNodeTypeException {
            if (!(qomTree.getSource() instanceof Selector)) {
                return false;
            }

            NodeTypeRegistry ntRegistry = NodeTypeRegistry.getInstance();
            ExtendedNodeType type = null;
            try {
                type = ntRegistry.getNodeType(((Selector) qomTree.getSource()).getNodeTypeName());
            } catch (NoSuchNodeTypeException e) {
                return false;
            }

            // check supported node types
            String nodeType = type.getName();
            Set<String> supportedNodeTypes = workspace.getSession().getRepository().getDataSource()
                    .getSupportedNodeTypes();
            if (supportedNodeTypes.contains(nodeType)) {
                return true;
            }
            for (String supportedNodeType : supportedNodeTypes) {
                try {
                    if (ntRegistry.getNodeType(supportedNodeType).isNodeType(nodeType)) {
                        return true;
                    }
                } catch (NoSuchNodeTypeException e) {
                    logger.error("no such node type", e);
                }
            }

            return false;
        }

    }

    /**
     * Query implementation for QOM Factory
     */
    class ExecutableExternalQuery extends ExternalQuery {
        private boolean nodeTypeSupported;
        private boolean hasExtension;


        ExecutableExternalQuery(Source source, Constraint constraints, Ordering[] orderings, Column[] columns, boolean nodeTypeSupported, boolean hasExtension) {
            super(source, constraints, orderings, columns);
            this.nodeTypeSupported = nodeTypeSupported;
            this.hasExtension = hasExtension;
        }

        @Override
        public QueryResult execute() throws InvalidQueryException, RepositoryException {
            List<String> results = new ArrayList<>();
            final ExternalSessionImpl session = workspace.getSession();
            final ExternalDataSource dataSource = session.getRepository().getDataSource();

            boolean noConstraints = isNoConstraints();

            Source source = getSource();
            boolean isMixinOrFacet = false;
            boolean isCount = false;
            long count = 0;
            long originalLimit = getLimit();
            String selectorType = null;
            String selectorName = null;
            if (source instanceof Selector) {
                selectorType = ((Selector) source).getNodeTypeName();
                selectorName = ((Selector) source).getSelectorName();
                isMixinOrFacet = NodeTypeRegistry.getInstance().getNodeType(selectorType).isMixin();
                for (Column c : getColumns()) {
                    final String columnName = c.getColumnName();
                    if (StringUtils.startsWith(columnName, FACET_MARKER)) {
                        isMixinOrFacet = true;
                        break;
                    }
                    if (StringUtils.startsWith(columnName, COUNT_MARKER)) {
                        isCount = true;
                        break;
                    }
                }
            }
            long lastItemIndex = getOffset() + getLimit();

            // Check first for extensions
            if (hasExtension) {
                Session extSession = session.getExtensionSession();
                QueryManager queryManager = extSession.getWorkspace().getQueryManager();

                final QueryObjectModelFactory qomFactory = queryManager.getQOMFactory();

                if (source instanceof Selector) {
                    // for extension node,but not mixin , change the type to jnt:externalProviderExtension
                    String selector = isMixinOrFacet || isCount ? selectorType : EXTENSION_MIXIN;
                    source = qomFactory.selector(selector, selectorName);

                }

                final ExternalContentStoreProvider storeProvider = session.getRepository().getStoreProvider();
                String mountPoint = storeProvider.getMountPoint();
                Constraint convertedConstraint = convertExistingPathConstraints(getConstraint(), mountPoint, qomFactory);
                if (!hasDescendantNode(convertedConstraint)) {
                    // Multiple IsDescendantNode queries are not supported
                    convertedConstraint = addPathConstraints(convertedConstraint, source, mountPoint, qomFactory);
                }

                // in case of normal (neither facet or count) query, search in extended type property types.
                if (!isCount && !isMixinOrFacet && selectorName != null && selectorType != null) {
                    Comparison c = qomFactory.comparison(qomFactory.propertyValue(selectorName, EXTENDED_TYPE_PROPERTY), QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, qomFactory.literal(extSession.getValueFactory().createValue(selectorType)));
                    convertedConstraint = qomFactory.and(c, convertedConstraint);
                }

                Query q = qomFactory.createQuery(source, convertedConstraint, getOrderings(), getColumns());
                boolean hasLimit = getLimit() > -1;
                if (!nodeTypeSupported) {
                    // Query is only done in JCR, directly pass limit and offset
                    if (hasLimit) {
                        q.setLimit(getLimit());
                    }
                    q.setOffset(getOffset());


                    final QueryResult result = q.execute();
                    if (!isCount) {
                        NodeIterator nodes = new QueryResultAdapter(result).getNodes();
                        while (nodes.hasNext()) {
                            Node node = (Node) nodes.next();
                            if (node == null) {
                                final String warnMsg = String.format("A null node is returned for the statement %s, the "
                                        + "Lucene indexes might be corrupted", q.getStatement());
                                logger.warn(warnMsg);
                            } else {
                                results.add(node.getPath().substring(mountPoint.length()));
                            }
                        }
                    } else {
                        count = getCount(result);
                    }
                    // As the node type is not supported by the DataSource, return the result directly
                    return buildQueryResult(results, dataSource, isCount, count);
                } else {
                    // Need to get all results to prepare merge
                    final QueryResult queryResult = q.execute();
                    if (!isCount) {
                        NodeIterator nodes = new QueryResultAdapter(queryResult).getNodes();
                        while (nodes.hasNext()) {
                            Node node = (Node) nodes.next();
                            if (node == null) {
                                final String warnMsg = String.format("A null node is returned for the statement %s, the "
                                        + "Lucene indexes might be corrupted", q.getStatement());
                                logger.warn(warnMsg);
                            } else {
                                String path = node.getPath().substring(mountPoint.length());
                                // If no constraint was set, only take extended nodes, as the datasource will return them all anyway
                                boolean isNotExtension = !node.isNodeType(EXTENSION_TYPE);
                                boolean matchExtensionConstraint = !noConstraints && session.itemExists(path);
                                if (isNotExtension || matchExtensionConstraint) {
                                    results.add(path);
                                    if (hasLimit && results.size() > lastItemIndex) {
                                        // stop to add item in the list, continue with the merge
                                        break;
                                    }
                                }
                            }
                        }
                    } else {
                        count = getCount(queryResult);
                    }

                    if (results.isEmpty()) {
                        // No results at all, ignore search in extension
                        results = null;
                    } else {
                        boolean offsetNotReached = getOffset() >= results.size();
                        if (offsetNotReached) {
                            // Offset greater than results here - return an empty result list, still need to merge results
                            // Change offset to the remaining results to get
                            setOffset(getOffset() - results.size());
                            results.clear();
                        } else if (hasLimit) {
                            // Strip results to limit and offset
                            int resultsSize = results.size();
                            results = results.subList((int) getOffset(), Math.min((int) getOffset() + (int) getLimit(), resultsSize));
                            //  set the offset and the limit for the external query (starting at 0 to the current limit minus the results from the extensions
                            setOffset(0);
                            setLimit(getLimit() - results.size());
                        } else {
                            boolean hasOffset = getOffset() > 0;
                            if (hasOffset) {
                                // Use offset
                                results = results.subList((int) getOffset(), results.size());
                                // set back the Offset to 0 has it has been consumed
                                setOffset(0);
                            }
                        }
                    }
                    // if the list contains all items
                    if (getLimit() == 0) {
                        return buildQueryResult(results, dataSource, isCount, count);
                    }

                }
            }

            // Add Provider's results

            ExternalContentStoreProvider.setCurrentSession(session);
            try {
                if (isCount && dataSource instanceof ExternalDataSource.SupportCount) {
                     count += ((ExternalDataSource.SupportCount) dataSource).count(this);
                } else if (!isCount) {
                    if (results == null) {
                        // No previous results, no merge to do
                        results = ((ExternalDataSource.Searchable) dataSource).search(this);
                    } else if (noConstraints) {
                        // Previous results, but only in extended nodes, no merge required - concat only
                        results.addAll(((ExternalDataSource.Searchable) dataSource).search(this));
                    } else {
                        List<String> providerResult = ((ExternalDataSource.Searchable) dataSource).search(this);
                        for (String s : providerResult) {
                            // Skip duplicate result
                            if (!results.contains(s)) {
                                results.add(s);
                                // if results size match the original limit, return them ..
                                if (originalLimit > -1 && results.size() >= originalLimit) {
                                    break;
                                }
                            }
                        }
                    }
                }
            } catch (UnsupportedRepositoryOperationException e) {
                logger.debug("Unsupported query ", e);
            } finally {
                ExternalContentStoreProvider.removeCurrentSession();
            }

            return buildQueryResult(results, dataSource, isCount, count);
        }

        private boolean isNoConstraints() throws RepositoryException {
            boolean noConstraints = false;
            try {
                // Check if query has
                if (QueryHelper.getSimpleAndConstraints(getConstraint()).size() == 0) {
                    noConstraints = true;
                }
            } catch (UnsupportedRepositoryOperationException e) {
                // Query has complex constraints, continue
            }
            return noConstraints;
        }

        private QueryResult buildQueryResult(List<String> results, ExternalDataSource dataSource, boolean isCount, long count) throws RepositoryException {
            if (isCount) {
                QueryResult rowCountResult = new ExternalCountRowResult(this, count, workspace);
                return new JahiaSimpleQueryResult(rowCountResult.getColumnNames(), rowCountResult.getSelectorNames(), rowCountResult.getRows());
            }
            if (results == null) {
                results = Collections.emptyList();
            }
            return new ExternalQueryResult(this, results, workspace);
        }

        private long getCount(QueryResult result) throws RepositoryException {
            Row row = result.getRows().hasNext() ? result.getRows().nextRow() : null;
            // Add null check as the RowIterator can return a null entry
            if (row instanceof CountRow) {
                return row.getValue(StringUtils.EMPTY).getLong();
            }
            return 0;
        }

        private boolean hasDescendantNode(Constraint convertedConstraint) {
            if (convertedConstraint instanceof DescendantNode) {
                return true;
            }
            if (convertedConstraint instanceof And) {
                return hasDescendantNode(((And) convertedConstraint).getConstraint1()) || hasDescendantNode(((And) convertedConstraint).getConstraint2());
            }
            return false;
        }

        private Constraint addPathConstraints(Constraint constraint, Source source, String mountPoint, QueryObjectModelFactory f) throws RepositoryException {
            Constraint result = constraint;
            if (source instanceof Selector) {
                DescendantNode descendantNode = f.descendantNode(((Selector) source).getSelectorName(), mountPoint);
                if (result == null) {
                    result = descendantNode;
                } else {
                    result = f.and(result, descendantNode);
                }
            } else if (source instanceof Join) {
                result = addPathConstraints(result, ((Join) source).getLeft(), mountPoint, f);
                result = addPathConstraints(result, ((Join) source).getRight(), mountPoint, f);
            }
            return result;
        }

        private Constraint convertExistingPathConstraints(Constraint constraint, String mountPoint, QueryObjectModelFactory f) throws RepositoryException {
            if (constraint instanceof ChildNode) {
                String root = ((ChildNode) constraint).getParentPath();
                // Path constraint is under mount point -> create new constraint with local path
                return f.childNode(((ChildNode) constraint).getSelectorName(), mountPoint + root);
            } else if (constraint instanceof DescendantNode) {
                String root = ((DescendantNode) constraint).getAncestorPath();
                return f.descendantNode(((DescendantNode) constraint).getSelectorName(), mountPoint + root);
            } else if (constraint instanceof And) {
                Constraint c1 = convertExistingPathConstraints(((And) constraint).getConstraint1(), mountPoint, f);
                Constraint c2 = convertExistingPathConstraints(((And) constraint).getConstraint2(), mountPoint, f);
                return f.and(c1, c2);
            } else if (constraint instanceof Or) {
                Constraint c1 = convertExistingPathConstraints(((Or) constraint).getConstraint1(), mountPoint, f);
                Constraint c2 = convertExistingPathConstraints(((Or) constraint).getConstraint2(), mountPoint, f);
                return f.or(c1, c2);
            } else if (constraint instanceof Not) {
                return f.not(convertExistingPathConstraints(((Not) constraint).getConstraint(), mountPoint, f));
            }
            return constraint;
        }


        private class QueryResultAdapter implements QueryResult {
            private final QueryResult result;

            public QueryResultAdapter(QueryResult result) {
                this.result = result;
            }

            @Override
            public String[] getColumnNames() throws RepositoryException {
                return result.getColumnNames();
            }

            @Override
            public RowIterator getRows() throws RepositoryException {
                return result.getRows();
            }

            @Override
            public NodeIterator getNodes() throws RepositoryException {
                if (result.getSelectorNames().length <= 1) {
                    return result.getNodes();
                } else {
                    return new NodeIteratorAdapter(result.getRows()) {
                        @Override
                        public Object next() {
                            Row row = (Row) super.next();
                            try {
                                return row.getNode(result.getSelectorNames()[0]);
                            } catch (RepositoryException e) {
                                throw new UnsupportedOperationException("Unable to access the node in " + row, e);
                            }
                        }
                    };
                }
            }

            @Override
            public String[] getSelectorNames() throws RepositoryException {
                return result.getSelectorNames();
            }
        }

    }
}
