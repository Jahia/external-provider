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

import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of the {@link javax.jcr.Item} for the {@link org.jahia.modules.external.ExternalData}.
 *
 * @author Thomas Draier
 */
public abstract class ExternalItemImpl implements Item {

    protected ExternalSessionImpl session;
    protected ExternalAccessControlManager controlManager;

    protected boolean isNew = false;

    public ExternalItemImpl(ExternalSessionImpl session) throws RepositoryException {
        this.session = session;
        this.controlManager = session.getAccessControlManager();
    }

    public ExternalSessionImpl getSession() {
        return session;
    }

    /**
     * Returns the underlying instance of the store provider.
     *
     * @return the underlying instance of the store provider
     */
    protected ExternalContentStoreProvider getStoreProvider() {
        return getSession().getRepository().getStoreProvider();
    }

    public boolean isNode() {
        return false;
    }

    void setNew(boolean isNew) throws RepositoryException {
        this.isNew = isNew;
    }

    public boolean isNew() {
        return isNew;
    }

    public boolean isModified() {
        return false;
    }

    public boolean isSame(Item item) throws RepositoryException {
        return false;
    }

    public void accept(ItemVisitor itemVisitor) throws RepositoryException {

    }

    @Override
    public Item getAncestor(int depth) throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        if (depth == 0) {
            return session.getItem("/");
        }
        Matcher matcher = Pattern.compile("(/[^/]+){" + depth + "}").matcher(getPath());
        if (matcher.find()) {
            return session.getItem(matcher.group(0));
        }
        throw new ItemNotFoundException();
    }

    @Override
    public int getDepth() throws RepositoryException {
        if (getPath().equals("/")) {
            return 0;
        }
        return getPath().split("/").length - 1;
    }

    /**
     * {@inheritDoc}
     */
    public void save() throws AccessDeniedException, ItemExistsException, ConstraintViolationException, InvalidItemStateException, ReferentialIntegrityException, VersionException, LockException, NoSuchNodeTypeException, RepositoryException {
        session.save();
    }

    /**
     * {@inheritDoc}
     */
    public void refresh(boolean b) throws InvalidItemStateException, RepositoryException {
        session.refresh(b);
    }

    protected void checkModify() throws RepositoryException {
        controlManager.checkModify(isNode() ? getPath() : getParent().getPath());
    }

    protected void checkRead() throws RepositoryException {
        controlManager.checkRead(isNode() ? getPath() : getParent().getPath());
    }

    protected void checkAddChildNodes() throws RepositoryException {
        controlManager.checkAddChildNodes(isNode() ? getPath() : getParent().getPath());
    }

    protected boolean canManageNodeTypes() throws RepositoryException {
        return controlManager.canManageNodeTypes(isNode()?getPath():getParent().getPath());
    }

    protected boolean canRemoveNode() throws RepositoryException {
        try {
            controlManager.checkRemoveNode(isNode() ? getPath() : getParent().getPath());
        } catch (AccessDeniedException e) {
            return false;
        }
        return true;
    }
}
