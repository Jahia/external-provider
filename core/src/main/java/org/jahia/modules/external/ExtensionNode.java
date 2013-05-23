package org.jahia.modules.external;

import javax.jcr.*;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.ActivityViolationException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Calendar;

/**
 * Wrapper for extension Node
 */
public class ExtensionNode extends ExtensionItem implements Node {
    private Node node;
    private String uuid;



    public ExtensionNode(Node node,String path, ExternalSessionImpl session) {
        super(node,path,session);
        this.node = node;
    }

    @Override
    public Node addNode(String relPath) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException, RepositoryException {
        return node.addNode(relPath);
    }

    @Override
    public Node addNode(String relPath, String primaryNodeTypeName) throws ItemExistsException, PathNotFoundException, NoSuchNodeTypeException, LockException, VersionException, ConstraintViolationException, RepositoryException {
        return node.addNode(relPath, primaryNodeTypeName);
    }

    @Override
    public void orderBefore(String srcChildRelPath, String destChildRelPath) throws UnsupportedRepositoryOperationException, VersionException, ConstraintViolationException, ItemNotFoundException, LockException, RepositoryException {
        node.orderBefore(srcChildRelPath, destChildRelPath);
    }

    @Override
    public Property setProperty(String name, Value value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return node.setProperty(name, value);
    }

    @Override
    public Property setProperty(String name, Value value, int type) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return node.setProperty(name, value, type);
    }

    @Override
    public Property setProperty(String name, Value[] values) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return node.setProperty(name, values);
    }

    @Override
    public Property setProperty(String name, Value[] values, int type) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return node.setProperty(name, values, type);
    }

    @Override
    public Property setProperty(String name, String[] values) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return node.setProperty(name, values);
    }

    @Override
    public Property setProperty(String name, String[] values, int type) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return node.setProperty(name, values, type);
    }

    @Override
    public Property setProperty(String name, String value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return node.setProperty(name, value);
    }

    @Override
    public Property setProperty(String name, String value, int type) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return node.setProperty(name, value, type);
    }

    @Override
    public Property setProperty(String name, InputStream value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return node.setProperty(name, value);
    }

    @Override
    public Property setProperty(String name, Binary value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return node.setProperty(name, value);
    }

    @Override
    public Property setProperty(String name, boolean value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return node.setProperty(name, value);
    }

    @Override
    public Property setProperty(String name, double value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return node.setProperty(name, value);
    }

    @Override
    public Property setProperty(String name, BigDecimal value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return node.setProperty(name, value);
    }

    @Override
    public Property setProperty(String name, long value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return node.setProperty(name, value);
    }

    @Override
    public Property setProperty(String name, Calendar value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return node.setProperty(name, value);
    }

    @Override
    public Property setProperty(String name, Node value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return node.setProperty(name, value);
    }

    @Override
    public Node getNode(String relPath) throws PathNotFoundException, RepositoryException {
        return node.getNode(relPath);
    }

    @Override
    public NodeIterator getNodes() throws RepositoryException {
        return node.getNodes();
    }

    @Override
    public NodeIterator getNodes(String namePattern) throws RepositoryException {
        return node.getNodes(namePattern);
    }

    @Override
    public NodeIterator getNodes(String[] nameGlobs) throws RepositoryException {
        return node.getNodes(nameGlobs);
    }

    @Override
    public Property getProperty(String relPath) throws PathNotFoundException, RepositoryException {
        return node.getProperty(relPath);
    }

    @Override
    public PropertyIterator getProperties() throws RepositoryException {
        return node.getProperties();
    }

    @Override
    public PropertyIterator getProperties(String namePattern) throws RepositoryException {
        return node.getProperties(namePattern);
    }

    @Override
    public PropertyIterator getProperties(String[] nameGlobs) throws RepositoryException {
        return node.getProperties(nameGlobs);
    }

    @Override
    public Item getPrimaryItem() throws ItemNotFoundException, RepositoryException {
        return node.getPrimaryItem();
    }

    @Override
    public String getUUID() throws UnsupportedRepositoryOperationException, RepositoryException {
        return node.getUUID();
    }

    @Override
    public String getIdentifier() throws RepositoryException {
        if (uuid == null) {
            ExternalContentStoreProvider storeProvider = getSession().getRepository().getStoreProvider();
            uuid = storeProvider.getInternalIdentifier(node.getIdentifier());
            if (uuid == null) {
                // not mapped yet -> store mapping
                uuid = storeProvider.mapInternalIdentifier(node.getIdentifier());
            }
        }

        return uuid;
    }

    @Override
    public int getIndex() throws RepositoryException {
        return node.getIndex();
    }

    @Override
    public PropertyIterator getReferences() throws RepositoryException {
        return node.getReferences();
    }

    @Override
    public PropertyIterator getReferences(String name) throws RepositoryException {
        return node.getReferences(name);
    }

    @Override
    public PropertyIterator getWeakReferences() throws RepositoryException {
        return node.getWeakReferences();
    }

    @Override
    public PropertyIterator getWeakReferences(String name) throws RepositoryException {
        return node.getWeakReferences(name);
    }

    @Override
    public boolean hasNode(String relPath) throws RepositoryException {
        return node.hasNode(relPath);
    }

    @Override
    public boolean hasProperty(String relPath) throws RepositoryException {
        return node.hasProperty(relPath);
    }

    @Override
    public boolean hasNodes() throws RepositoryException {
        return node.hasNodes();
    }

    @Override
    public boolean hasProperties() throws RepositoryException {
        return node.hasProperties();
    }

    @Override
    public NodeType getPrimaryNodeType() throws RepositoryException {
        return node.getPrimaryNodeType();
    }

    @Override
    public NodeType[] getMixinNodeTypes() throws RepositoryException {
        return node.getMixinNodeTypes();
    }

    @Override
    public boolean isNodeType(String nodeTypeName) throws RepositoryException {
        return node.isNodeType(nodeTypeName);
    }

    @Override
    public void setPrimaryType(String nodeTypeName) throws NoSuchNodeTypeException, VersionException, ConstraintViolationException, LockException, RepositoryException {
        node.setPrimaryType(nodeTypeName);
    }

    @Override
    public void addMixin(String mixinName) throws NoSuchNodeTypeException, VersionException, ConstraintViolationException, LockException, RepositoryException {
        node.addMixin(mixinName);
    }

    @Override
    public void removeMixin(String mixinName) throws NoSuchNodeTypeException, VersionException, ConstraintViolationException, LockException, RepositoryException {
        node.removeMixin(mixinName);
    }

    @Override
    public boolean canAddMixin(String mixinName) throws NoSuchNodeTypeException, RepositoryException {
        return node.canAddMixin(mixinName);
    }

    @Override
    public NodeDefinition getDefinition() throws RepositoryException {
        return node.getDefinition();
    }

    @Override
    public Version checkin() throws VersionException, UnsupportedRepositoryOperationException, InvalidItemStateException, LockException, RepositoryException {
        return node.checkin();
    }

    @Override
    public void checkout() throws UnsupportedRepositoryOperationException, LockException, ActivityViolationException, RepositoryException {
        node.checkout();
    }

    @Override
    public void doneMerge(Version version) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
        node.doneMerge(version);
    }

    @Override
    public void cancelMerge(Version version) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
        node.cancelMerge(version);
    }

    @Override
    public void update(String srcWorkspace) throws NoSuchWorkspaceException, AccessDeniedException, LockException, InvalidItemStateException, RepositoryException {
        node.update(srcWorkspace);
    }

    @Override
    public NodeIterator merge(String srcWorkspace, boolean bestEffort) throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
        return node.merge(srcWorkspace, bestEffort);
    }

    @Override
    public String getCorrespondingNodePath(String workspaceName) throws ItemNotFoundException, NoSuchWorkspaceException, AccessDeniedException, RepositoryException {
        return node.getCorrespondingNodePath(workspaceName);
    }

    @Override
    public NodeIterator getSharedSet() throws RepositoryException {
        return node.getSharedSet();
    }

    @Override
    public void removeSharedSet() throws VersionException, LockException, ConstraintViolationException, RepositoryException {
        node.removeSharedSet();
    }

    @Override
    public void removeShare() throws VersionException, LockException, ConstraintViolationException, RepositoryException {
        node.removeShare();
    }

    @Override
    public boolean isCheckedOut() throws RepositoryException {
        return node.isCheckedOut();
    }

    @Override
    public void restore(String versionName, boolean removeExisting) throws VersionException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        node.restore(versionName, removeExisting);
    }

    @Override
    public void restore(Version version, boolean removeExisting) throws VersionException, ItemExistsException, InvalidItemStateException, UnsupportedRepositoryOperationException, LockException, RepositoryException {
        node.restore(version, removeExisting);
    }

    @Override
    public void restore(Version version, String relPath, boolean removeExisting) throws PathNotFoundException, ItemExistsException, VersionException, ConstraintViolationException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        node.restore(version, relPath, removeExisting);
    }

    @Override
    public void restoreByLabel(String versionLabel, boolean removeExisting) throws VersionException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        node.restoreByLabel(versionLabel, removeExisting);
    }

    @Override
    public VersionHistory getVersionHistory() throws UnsupportedRepositoryOperationException, RepositoryException {
        return node.getVersionHistory();
    }

    @Override
    public Version getBaseVersion() throws UnsupportedRepositoryOperationException, RepositoryException {
        return node.getBaseVersion();
    }

    @Override
    public Lock lock(boolean isDeep, boolean isSessionScoped) throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, InvalidItemStateException, RepositoryException {
        return node.lock(isDeep, isSessionScoped);
    }

    @Override
    public Lock getLock() throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, RepositoryException {
        return node.getLock();
    }

    @Override
    public void unlock() throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, InvalidItemStateException, RepositoryException {
        node.unlock();
    }

    @Override
    public boolean holdsLock() throws RepositoryException {
        return node.holdsLock();
    }

    @Override
    public boolean isLocked() throws RepositoryException {
        return node.isLocked();
    }

    @Override
    public void followLifecycleTransition(String transition) throws UnsupportedRepositoryOperationException, InvalidLifecycleTransitionException, RepositoryException {
        node.followLifecycleTransition(transition);
    }

    @Override
    public String[] getAllowedLifecycleTransistions() throws UnsupportedRepositoryOperationException, RepositoryException {
        return node.getAllowedLifecycleTransistions();
    }
}
