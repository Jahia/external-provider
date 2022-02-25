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
        return new ExternalQueryNodeIterator(it);
    }

    @Override
    public RowIterator getRows() throws RepositoryException {
        final Iterator<String> it = results.iterator();
        return new ExternalQueryRowIterator(it);
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

    private class ExternalQueryNodeIterator implements NodeIterator {
        private final Iterator<String> it;
        private int pos;

        public ExternalQueryNodeIterator(Iterator<String> it) {
            this.it = it;
            pos = 0;
        }

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
    }

    private class ExternalQueryRowIterator implements RowIterator {
        private final Iterator<String> it;
        private int pos;

        public ExternalQueryRowIterator(Iterator<String> it) {
            this.it = it;
            pos = 0;
        }

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
                return new ExternalQueryRow(n);
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

        private class ExternalQueryRow implements Row {
            private final Node n;

            public ExternalQueryRow(Node n) {
                this.n = n;
            }

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
        }
    }
}
