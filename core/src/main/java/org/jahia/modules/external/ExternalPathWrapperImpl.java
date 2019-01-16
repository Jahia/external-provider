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
package org.jahia.modules.external;

import com.google.common.base.Objects;
import org.apache.commons.lang.StringUtils;
import org.jahia.utils.security.PathWrapper;

import javax.jcr.Item;
import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * PathWrapper implementation for External provider access control API
 * Path representation use {@link String}
 *
 * @author jkevan
 */
public class ExternalPathWrapperImpl implements PathWrapper{
    private String jcrPath;
    private Session session;

    public ExternalPathWrapperImpl(String jcrPath, Session session) {
        this.jcrPath = jcrPath;
        this.session = session;
    }

    @Override
    public Object getInnerObject() {
        return jcrPath;
    }

    @Override
    public int getLength() {
        return jcrPath.length();
    }

    @Override
    public boolean isRoot() {
        return "/".equals(jcrPath);
    }

    @Override
    public String getPathStr() {
        return jcrPath;
    }

    @Override
    public String getNodeName() throws NamespaceException {
        return StringUtils.substringAfterLast(jcrPath, "/");
    }

    @Override
    public boolean itemExist() throws RepositoryException {
        return session.itemExists(jcrPath);
    }

    @Override
    public Item getItem() throws RepositoryException {
        return session.getItem(jcrPath);
    }

    @Override
    public PathWrapper getAncestor() throws RepositoryException {
        if (jcrPath.lastIndexOf('/') > 0) {
            return new ExternalPathWrapperImpl(jcrPath.substring(0, jcrPath.lastIndexOf('/')), session);
        } else {
            return new ExternalPathWrapperImpl("/", session);
        }
    }

    @Override
    public PathWrapper getNewPathWrapper(String path) throws RepositoryException {
        return new ExternalPathWrapperImpl(path, session);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExternalPathWrapperImpl that = (ExternalPathWrapperImpl) o;
        return Objects.equal(jcrPath, that.jcrPath);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(jcrPath);
    }
}
