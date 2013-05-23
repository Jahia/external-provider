package org.jahia.modules.external;

import org.apache.commons.lang.StringUtils;

import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExtensionItem implements Item {
    private Item item;
    private ExternalSessionImpl session;
    private String path;

    public ExtensionItem(Item item, String path, ExternalSessionImpl session) {
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
    public Item getAncestor(int depth) throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        if (depth == 0) {
            return session.getItem("/");
        }
        Matcher matcher = Pattern.compile("(/[^/]+){"+depth+"}").matcher(path);
        if (matcher.find()) {
            return session.getItem(matcher.group(0));
        }
        throw new ItemNotFoundException();
    }

    @Override
    public Node getParent() throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        return session.getNode(StringUtils.substringBeforeLast(path, "/"));
    }

    @Override
    public int getDepth() throws RepositoryException {
        if (path.equals("/")) {
            return 0;
        }
        return path.split("/").length - 1;
    }

    @Override
    public ExternalSessionImpl getSession() throws RepositoryException {
        return session;
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
    public void save() throws AccessDeniedException, ItemExistsException, ConstraintViolationException, InvalidItemStateException, ReferentialIntegrityException, VersionException, LockException, NoSuchNodeTypeException, RepositoryException {
        session.save();
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
