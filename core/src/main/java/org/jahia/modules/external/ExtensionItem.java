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

import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

/**
 * Wrapper for extension Item
 */
public class ExtensionItem extends ExternalItemImpl implements Item {
    private Item item;
    private String path;

    public ExtensionItem(Item item, String path, ExternalSessionImpl session) throws RepositoryException {
        super(session);
        this.item = item;
        this.path = path;
        this.session = session;
    }

    @Override
    public String getPath() throws RepositoryException {
        return path;
    }

    @Override
    public String getName() throws RepositoryException {
        return item.getName();
    }

    @Override
    public Node getParent() throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        if (path.equals("/")) {
            throw new ItemNotFoundException();
        }
        String parentPath = StringUtils.substringBeforeLast(path, "/");
        try {
            controlManager.checkRead(parentPath);
        } catch (PathNotFoundException e) {
            throw new AccessDeniedException(parentPath);
        }
        try {
            return session.getNode(parentPath);
        } catch (PathNotFoundException e) {
            throw new ItemNotFoundException(parentPath);
        }
    }

    @Override
    public boolean isNode() {
        return item.isNode();
    }

    @Override
    public boolean isNew() {
        return item.isNew();
    }

    @Override
    public boolean isModified() {
        return item.isModified();
    }

    @Override
    public boolean isSame(Item otherItem) throws RepositoryException {
        return item.isSame(otherItem);
    }

    @Override
    public void accept(ItemVisitor visitor) throws RepositoryException {
        item.accept(visitor);
    }

    @Override
    public void refresh(boolean keepChanges) throws InvalidItemStateException, RepositoryException {
        item.refresh(keepChanges);
    }

    @Override
    public void remove() throws VersionException, LockException, ConstraintViolationException, AccessDeniedException, RepositoryException {
        if (isNode()) {
            if (!canRemoveNode()) {
                throw new AccessDeniedException(getPath());
            }
        } else {
            checkModify();
        }
        item.remove();
    }
}
