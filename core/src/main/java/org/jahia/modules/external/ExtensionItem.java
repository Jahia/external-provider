package org.jahia.modules.external;

import org.apache.commons.lang.StringUtils;

import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

public class ExtensionItem extends ExternalItemImpl implements Item {
    private Item item;
    private String path;

    public ExtensionItem(Item item, String path, ExternalSessionImpl session) {
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
        return session.getNode(StringUtils.substringBeforeLast(path, "/"));
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
        item.remove();
    }
}
