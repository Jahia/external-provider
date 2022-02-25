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

import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.qom.Column;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.Ordering;
import javax.jcr.query.qom.QueryObjectModel;
import javax.jcr.query.qom.Source;
import javax.jcr.version.VersionException;

import org.apache.commons.lang.ArrayUtils;

import java.util.Arrays;

/**
 * Implementation of the {@link QueryObjectModel} for an external provider.
 */
public class ExternalQuery implements QueryObjectModel {
    private Column[] columns;
    private Constraint constraints;
    private long limit = -1;
    private long offset = 0;
    private Ordering[] orderings;
    private Source source;

    public ExternalQuery(Source source, Constraint constraints, Ordering[] orderings, Column[] columns) {
        this.source = source;
        this.constraints = constraints;
        this.orderings = Arrays.copyOf(orderings, orderings.length);
        this.columns = Arrays.copyOf(columns, columns.length);
    }

    @Override
    public void bindValue(String varName, Value value) throws IllegalArgumentException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    @Override
    public QueryResult execute() throws InvalidQueryException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getBindVariableNames() throws RepositoryException {
        return ArrayUtils.EMPTY_STRING_ARRAY;
    }

    @Override
    public Column[] getColumns() {
        return Arrays.copyOf(columns, columns.length);
    }

    @Override
    public Constraint getConstraint() {
        return constraints;
    }

    @Override
    public String getLanguage() {
        return Query.JCR_SQL2;
    }

    /**
     * Returns the maximum size of the result set.
     *
     * @return the maximum size of the result set
     */
    public long getLimit() {
        return limit;
    }

    /**
     * Returns the start offset of the result set.
     *
     * @return the start offset of the result set
     */
    public long getOffset() {
        return offset;
    }

    @Override
    public Ordering[] getOrderings() {
        return Arrays.copyOf(orderings, orderings.length);
    }

    @Override
    public Source getSource() {
        return source;
    }

    @Override
    public String getStatement() {
        return null;
    }

    @Override
    public String getStoredQueryPath() throws ItemNotFoundException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    @Override
    public void setLimit(long limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("limit must not be negative");
        }
        this.limit = limit;
    }

    @Override
    public void setOffset(long offset) {
        if (offset < 0) {
            throw new IllegalArgumentException("offset must not be negative");
        }
        this.offset = offset;
    }

    @Override
    public Node storeAsNode(String absPath) throws ItemExistsException, PathNotFoundException, VersionException,
            ConstraintViolationException, LockException, UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ExternalQuery{");
        sb.append("columns=").append(Arrays.toString(columns));
        sb.append(", constraints=").append(constraints);
        sb.append(", limit=").append(limit);
        sb.append(", offset=").append(offset);
        sb.append(", orderings=").append(Arrays.toString(orderings));
        sb.append(", source=").append(source);
        sb.append('}');
        return sb.toString();
    }
}
