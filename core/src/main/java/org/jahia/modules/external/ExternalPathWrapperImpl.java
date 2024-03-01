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

import org.apache.commons.lang.StringUtils;
import org.jahia.utils.security.PathWrapper;

import javax.jcr.Item;
import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Objects;

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
        return Objects.equals(jcrPath, that.jcrPath);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(jcrPath);
    }
}
