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
import org.jahia.services.content.nodetypes.ExtendedNodeType;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Wrapper for extension Node
 */
@SuppressWarnings("deprecation")
public class ExtensionNode extends ExtensionItem implements Node {
    private static final Logger logger = LoggerFactory.getLogger(ExtensionNode.class);

    private Node node;
    private String uuid;
    private String path;
    private ExternalSessionImpl session;


    public ExtensionNode(Node node,String path, ExternalSessionImpl session) throws RepositoryException {
        super(node,path,session);
        this.node = node;
        this.path = path;
        this.session = session;
    }

    public Node getJcrNode() {
        return node;
    }

    @Override
    public Node addNode(String relPath) throws RepositoryException {
        return addNode(relPath,null);
    }

    @Override
    public Node addNode(String relPath, String primaryNodeTypeName) throws ItemExistsException, PathNotFoundException, NoSuchNodeTypeException, LockException, VersionException, ConstraintViolationException, RepositoryException {
        checkAddChildNodes();
        Node subNode = node.addNode(relPath, primaryNodeTypeName);
        subNode.addMixin("jmix:externalProviderExtension");
        subNode.setProperty("j:isExternalProviderRoot", false);
        List<Value> values = createNodeTypeValues(session.getValueFactory(), primaryNodeTypeName);
        subNode.setProperty("j:extendedType",values.toArray(new Value[values.size()]));
        return new ExtensionNode(subNode, path + "/" + relPath,session);
    }

    /**
     * Create a list of values containing all inherited node type names
     * @param primaryNodeTypeName base type
     * @return
     * @throws RepositoryException
     */
    public static List<Value> createNodeTypeValues(ValueFactory valueFactory, String primaryNodeTypeName) throws RepositoryException {
        List<Value> values = new ArrayList<Value>();
        values.add(valueFactory.createValue(primaryNodeTypeName));
        for (ExtendedNodeType type : NodeTypeRegistry.getInstance().getNodeType(primaryNodeTypeName).getSupertypes()) {
            values.add(valueFactory.createValue(type.getName()));
        }
        return values;
    }

    @Override
    public void orderBefore(String srcChildRelPath, String destChildRelPath) throws UnsupportedRepositoryOperationException, VersionException, ConstraintViolationException, ItemNotFoundException, LockException, RepositoryException {
        node.orderBefore(srcChildRelPath, destChildRelPath);
    }

    @Override
    public Property setProperty(String name, Value value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkModify();
        return new ExtensionProperty(node.setProperty(name, value), path + "/" + name, session, this);
    }

    @Override
    public Property setProperty(String name, Value value, int type) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkModify();
        return new ExtensionProperty(node.setProperty(name, value, type), path + "/" + name, session, this);
    }

    @Override
    public Property setProperty(String name, Value[] values) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkModify();
        return new ExtensionProperty(node.setProperty(name, values), path + "/" + name, session, this);
    }

    @Override
    public Property setProperty(String name, Value[] values, int type) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkModify();
        return new ExtensionProperty(node.setProperty(name, values, type), path + "/" + name, session, this);
    }

    @Override
    public Property setProperty(String name, String[] values) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkModify();
        return new ExtensionProperty(node.setProperty(name,values), path + "/" + name, session, this);
    }

    @Override
    public Property setProperty(String name, String[] values, int type) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkModify();
        return new ExtensionProperty(node.setProperty(name, values, type), path + "/" + name, session, this);
    }

    @Override
    public Property setProperty(String name, String value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkModify();
        return new ExtensionProperty(node.setProperty(name, value), path + "/" + name, session, this);
    }

    @Override
    public Property setProperty(String name, String value, int type) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkModify();
        return new ExtensionProperty(node.setProperty(name, value, type), path + "/" + name, session, this);
    }

    @Override
    public Property setProperty(String name, InputStream value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkModify();
        return new ExtensionProperty(node.setProperty(name, value), path + "/" + name, session, this);
    }

    @Override
    public Property setProperty(String name, Binary value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkModify();
        return new ExtensionProperty(node.setProperty(name, value), path + "/" + name, session, this);
    }

    @Override
    public Property setProperty(String name, boolean value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkModify();
        return new ExtensionProperty(node.setProperty(name, value), path + "/" + name, session, this);
    }

    @Override
    public Property setProperty(String name, double value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkModify();
        return new ExtensionProperty(node.setProperty(name, value), path + "/" + name, session, this);
    }

    @Override
    public Property setProperty(String name, BigDecimal value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkModify();
        return new ExtensionProperty(node.setProperty(name, value), path + "/" + name, session, this);
    }

    @Override
    public Property setProperty(String name, long value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkModify();
        return new ExtensionProperty(node.setProperty(name, value), path + "/" + name, session, this);
    }

    @Override
    public Property setProperty(String name, Calendar value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkModify();
        return new ExtensionProperty(node.setProperty(name, value), path + "/" + name, session, this);
    }

    @Override
    public Property setProperty(String name, Node value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkModify();
        return new ExtensionProperty(node.setProperty(name, value), path + "/" + name, session, this);
    }

    @Override
    public Node getNode(String relPath) throws PathNotFoundException, RepositoryException {
        return new ExtensionNode(node.getNode(relPath),path + "/" + relPath,session);
    }

    @Override
    public NodeIterator getNodes() throws RepositoryException {
        return new ExtensionNodeIterator(node.getNodes());
    }

    @Override
    public NodeIterator getNodes(String namePattern) throws RepositoryException {
        return new ExtensionNodeIterator(node.getNodes(namePattern));
    }

    @Override
    public NodeIterator getNodes(String[] nameGlobs) throws RepositoryException {
        return new ExtensionNodeIterator(node.getNodes(nameGlobs));
    }

    @Override
    public Property getProperty(String relPath) throws PathNotFoundException, RepositoryException {
        return new ExtensionProperty(node.getProperty(relPath),path + "/" + relPath,session, this);
    }

    @Override
    public PropertyIterator getProperties() throws RepositoryException {
        return new ExtensionPropertyIterator(node.getProperties());
    }

    @Override
    public PropertyIterator getProperties(String namePattern) throws RepositoryException {
        return new ExtensionPropertyIterator(node.getProperties(namePattern));
    }

    @Override
    public PropertyIterator getProperties(String[] nameGlobs) throws RepositoryException {
        return new ExtensionPropertyIterator(node.getProperties(nameGlobs));
    }

    @Override
    public Item getPrimaryItem() throws ItemNotFoundException, RepositoryException {
        return new ExtensionItem(node.getPrimaryItem(), path, session);
    }

    @Override
    public String getUUID() throws UnsupportedRepositoryOperationException, RepositoryException {
        return getIdentifier();
    }

    @Override
    public String getIdentifier() throws RepositoryException {
        if (uuid == null) {
            ExternalContentStoreProvider storeProvider = getSession().getRepository().getStoreProvider();
            uuid = storeProvider.getOrCreateInternalIdentifier(node.getIdentifier());
        }

        return uuid;
    }

    @Override
    public int getIndex() throws RepositoryException {
        return node.getIndex();
    }

    @Override
    public PropertyIterator getReferences() throws RepositoryException {
        return new ExtensionPropertyIterator(node.getReferences());
    }

    @Override
    public PropertyIterator getReferences(String name) throws RepositoryException {
        return new ExtensionPropertyIterator(node.getReferences(name));
    }

    @Override
    public PropertyIterator getWeakReferences() throws RepositoryException {
        return new ExtensionPropertyIterator(node.getWeakReferences());
    }

    @Override
    public PropertyIterator getWeakReferences(String name) throws RepositoryException {
        return new ExtensionPropertyIterator(node.getWeakReferences(name));
    }

    @Override
    public boolean hasNode(String relPath) throws RepositoryException {
        try {
            String p = StringUtils.equals(relPath,"/") ? "" : relPath;
            controlManager.checkRead(getPath() + p);
        } catch (PathNotFoundException e) {
            return false;
        }
        return node.hasNode(relPath);
    }

    @Override
    public boolean hasProperty(String relPath) throws RepositoryException {
        return node.hasProperty(relPath);
    }

    @Override
    public boolean hasNodes() throws RepositoryException {
        return node.getNodes().hasNext();
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
        List<NodeType> nt = new ArrayList<NodeType>();
        for (NodeType nodeType : node.getMixinNodeTypes()) {
            if (!nodeType.getName().equals("jmix:externalProviderExtension")) {
                nt.add(NodeTypeRegistry.getInstance().getNodeType(nodeType.getName()));
            }
        }
        return nt.toArray(new NodeType[nt.size()]);
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
        if (!canManageNodeTypes()) {
            return;
        }
        node.addMixin(mixinName);
    }

    @Override
    public void removeMixin(String mixinName) throws NoSuchNodeTypeException, VersionException, ConstraintViolationException, LockException, RepositoryException {
        if (!canManageNodeTypes()) {
            return;
        }
        node.removeMixin(mixinName);
    }

    @Override
    public boolean canAddMixin(String mixinName) throws NoSuchNodeTypeException, RepositoryException {
        return canManageNodeTypes() && node.canAddMixin(mixinName);
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
    public void update(String srcWorkspace) throws NoSuchWorkspaceException, PathNotFoundException, LockException, InvalidItemStateException, RepositoryException {
        node.update(srcWorkspace);
    }

    @Override
    public NodeIterator merge(String srcWorkspace, boolean bestEffort) throws NoSuchWorkspaceException, PathNotFoundException, MergeException, LockException, InvalidItemStateException, RepositoryException {
        return new ExtensionNodeIterator(node.merge(srcWorkspace, bestEffort));
    }

    @Override
    public String getCorrespondingNodePath(String workspaceName) throws ItemNotFoundException, NoSuchWorkspaceException, PathNotFoundException, RepositoryException {
        return node.getCorrespondingNodePath(workspaceName);
    }

    @Override
    public NodeIterator getSharedSet() throws RepositoryException {
        return new ExtensionNodeIterator(node.getSharedSet());
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
    public Lock lock(boolean isDeep, boolean isSessionScoped) throws UnsupportedRepositoryOperationException, LockException, PathNotFoundException, InvalidItemStateException, RepositoryException {
        return node.lock(isDeep, isSessionScoped);
    }

    @Override
    public Lock getLock() throws UnsupportedRepositoryOperationException, LockException, PathNotFoundException, RepositoryException {
        return node.getLock();
    }

    @Override
    public void unlock() throws UnsupportedRepositoryOperationException, LockException, PathNotFoundException, InvalidItemStateException, RepositoryException {
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

    /**
     * Implementation for property iterator
     */
    private class ExtensionPropertyIterator implements PropertyIterator {
        private int pos = 0;
        private PropertyIterator extensionPropertiesIterator;
        private Property nextProperty = null;

        ExtensionPropertyIterator(PropertyIterator extensionPropertiesIterator) {
            this.extensionPropertiesIterator = extensionPropertiesIterator;
            fetchNext();
        }

        private Property fetchNext() {
            nextProperty = null;
            if (extensionPropertiesIterator != null) {
                while (extensionPropertiesIterator.hasNext()) {
                    Property next = extensionPropertiesIterator.nextProperty();
                    try {
                        nextProperty = new ExtensionProperty(next, node.getPath() + "/" + next.getName(), getSession(), ExtensionNode.this);
                        return nextProperty;
                    } catch (RepositoryException e) {
                        // go to next property
                    }
                }
            }
            return null;
        }

        @Override
        public Property nextProperty() {
            if (nextProperty == null) {
                throw new NoSuchElementException();
            }
            Property next = nextProperty;
            fetchNext();
            pos ++;
            return next;
        }

        public void skip(long skipNum) {
            for (int i=0; i<skipNum; i++) {
                nextProperty();
            }
        }

        @Override
        public long getSize() {
            return extensionPropertiesIterator.getSize();
        }

        @Override
        public long getPosition() {
            return pos;
        }

        @Override
        public boolean hasNext() {
            return nextProperty != null;
        }

        @Override
        public Object next() {
            return nextProperty();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove");
        }
    }

    /**
     * Implementation for node iterator
     */
    private class ExtensionNodeIterator implements NodeIterator {
        private int pos = 0;
        private NodeIterator extensionNodeIterator;
        private Node nextNode;

        public ExtensionNodeIterator(NodeIterator extensionNodeIterator) {
            this.extensionNodeIterator = extensionNodeIterator;
            fetchNext();
        }

        private Node fetchNext() {
            nextNode = null;
            if (extensionNodeIterator != null && extensionNodeIterator.hasNext()) {
                do {
                    try {
                        Node n = extensionNodeIterator.nextNode();
                        session.getAccessControlManager().checkRead(StringUtils.substringAfter(n.getPath(), session.getRepository().getStoreProvider().getMountPoint()));
                        nextNode = new ExtensionNode(n,getPath() + "/" + n.getName(),getSession());
                        return  nextNode;
                    } catch (RepositoryException e) {
                        logger.debug("Cannot get node", e);
                    }
                } while (nextNode == null || extensionNodeIterator.hasNext());
            }
            return null;
        }

        public Node nextNode() {
            if (nextNode == null) {
                throw new NoSuchElementException();
            }
            Node next = nextNode;
            fetchNext();
            pos++;
            return next;
        }

        public void skip(long skipNum) {
            for (int i = 0; i<skipNum ; i++) {
                nextNode();
            }
        }

        public long getSize() {
            return -1;
        }

        public long getPosition() {
            return pos;
        }

        public boolean hasNext() {
            return nextNode != null;
        }

        public Object next() {
            return nextNode();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
