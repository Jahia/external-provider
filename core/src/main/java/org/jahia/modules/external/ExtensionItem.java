/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2018 Jahia Solutions Group SA. All rights reserved.
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
