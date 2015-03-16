/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2015 Jahia Solutions Group SA. All rights reserved.
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
