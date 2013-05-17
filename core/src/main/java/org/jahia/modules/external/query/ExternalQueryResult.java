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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.*;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import javax.jcr.query.qom.Column;
import javax.jcr.query.qom.QueryObjectModel;
import javax.jcr.query.qom.Selector;

import org.apache.commons.lang.ArrayUtils;
import org.jahia.modules.external.ExternalWorkspaceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of the query result, returned by the external provider search.
 * 
 * @author Sergiy Shyrkov
 */
class ExternalQueryResult implements QueryResult {

    private static final Logger logger = LoggerFactory.getLogger(ExternalQueryResult.class);

    private String[] columns;

    private QueryObjectModel qom;

    private List<String> results;

    private String[] selectors;

    private ExternalWorkspaceImpl workspace;

    /**
     * Initializes an instance of this class.
     * 
     * @param qom
     *            the query object modules used for the search
     * @param results
     *            the list of result IDs matching the search criteria
     * @param workspace
     *            the current provider workspace
     */
    ExternalQueryResult(QueryObjectModel qom, List<String> results, ExternalWorkspaceImpl workspace) {
        super();
        this.qom = qom;
        this.results = results;
        this.workspace = workspace;
    }

    @Override
    public String[] getColumnNames() throws RepositoryException {
        if (columns == null) {
            if (qom.getColumns().length == 0) {
                columns = ArrayUtils.EMPTY_STRING_ARRAY;
            } else {
                List<String> columnList = new LinkedList<String>();
                for (Column c : qom.getColumns()) {
                    if (c.getColumnName() != null) {
                        columnList.add(c.getColumnName());
                    }
                }
                columns = columnList.toArray(new String[] {});
            }
        }

        return columns;
    }

    @Override
    public NodeIterator getNodes() throws RepositoryException {
        final Iterator<String> it = results.iterator();
        return new NodeIterator() {
            private int pos = 0;

            @Override
            public long getPosition() {
                return pos;
            }

            @Override
            public long getSize() {
                return results.size();
            }

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public Object next() {
                return nextNode();
            }

            @Override
            public Node nextNode() {
                try {
                    return workspace.getSession().getNode(it.next());
                } catch (RepositoryException e) {
                    logger.error(e.getMessage(), e);
                    return null;
                }
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            public void skip(long skipNum) {
                for (int i = 0; i < skipNum; i++) {
                    it.next();
                }
                pos += skipNum;
            }
        };
    }

    @Override
    public RowIterator getRows() throws RepositoryException {
        final Iterator<String> it = results.iterator();
        return new RowIterator() {
            private int pos = 0;

            @Override
            public long getPosition() {
                return pos;
            }

            @Override
            public long getSize() {
                return results.size();
            }

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public Object next() {
                return nextRow();
            }

            @Override
            public Row nextRow() {
                try {
                    final Node n = workspace.getSession().getNode(it.next());
                    return new Row() {
                        @Override
                        public Value[] getValues() throws RepositoryException {
                            return new Value[0];
                        }

                        @Override
                        public Value getValue(String columnName) throws ItemNotFoundException, RepositoryException {
                            return null;
                        }

                        @Override
                        public Node getNode() throws RepositoryException {
                            return n;
                        }

                        @Override
                        public Node getNode(String selectorName) throws RepositoryException {
                            return n;
                        }

                        @Override
                        public String getPath() throws RepositoryException {
                            return n.getPath();
                        }

                        @Override
                        public String getPath(String selectorName) throws RepositoryException {
                            return n.getPath();
                        }

                        @Override
                        public double getScore() throws RepositoryException {
                            return 0;
                        }

                        @Override
                        public double getScore(String selectorName) throws RepositoryException {
                            return 0;
                        }
                    };
                } catch (RepositoryException e) {
                    logger.error(e.getMessage(), e);
                    return null;
                }
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            public void skip(long skipNum) {
                for (int i = 0; i < skipNum; i++) {
                    it.next();
                }
                pos += skipNum;
            }
        };
    }

    @Override
    public String[] getSelectorNames() throws RepositoryException {
        if (selectors == null) {
            if (qom.getSource() instanceof Selector) {
                selectors = new String[] { ((Selector) qom.getSource()).getSelectorName() };
            } else {
                selectors = ArrayUtils.EMPTY_STRING_ARRAY;
            }

        }
        return selectors;
    }

}
