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
