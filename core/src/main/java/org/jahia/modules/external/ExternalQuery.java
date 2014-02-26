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
}
