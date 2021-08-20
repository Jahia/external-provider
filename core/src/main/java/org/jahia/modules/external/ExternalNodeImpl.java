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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.util.ChildrenCollectorFilter;
import org.apache.jackrabbit.value.BinaryImpl;
import org.jahia.api.Constants;
import org.jahia.modules.external.acl.ExternalDataAce;
import org.jahia.modules.external.acl.ExternalDataAcl;
import org.jahia.services.content.nodetypes.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.*;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.*;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;

/**
 * Implementation of the {@link javax.jcr.Node} for the {@link org.jahia.modules.external.ExternalData}.
 *
 * @author Thomas Draier
 */
public class ExternalNodeImpl extends ExternalItemImpl implements Node {

    public static final String MATCH_ALL_PATTERN = "*";

    private static final Logger logger = LoggerFactory.getLogger(ExternalNodeImpl.class);
    private static final String J_TRANSLATION = "j:translation_";

    private ExternalData data;
    private List<String> externalChildren;
    private Map<String, ExternalPropertyImpl> properties = null;
    private String uuid;

    public ExternalNodeImpl(ExternalData data, ExternalSessionImpl session) throws RepositoryException {

        super(session);
        this.data = data;
        this.properties = new HashMap<String, ExternalPropertyImpl>();

        for (Map.Entry<String, String[]> entry : data.getProperties().entrySet()) {
            ExtendedPropertyDefinition definition = getPropertyDefinition(entry.getKey());
            if (definition != null && definition.getName().equals(MATCH_ALL_PATTERN) && data.getType() != null && data.getType().equals(Constants.JAHIANT_TRANSLATION)) {
                definition = ((ExternalNodeImpl) getParent()).getPropertyDefinition(entry.getKey());
            }
            if (definition != null) {
                int requiredType = definition.getRequiredType();
                if (definition.isMultiple()) {
                    Value[] values = new Value[entry.getValue().length];
                    for (int i = 0; i < entry.getValue().length; i++) {
                        values[i] = session.getValueFactory().createValue(entry.getValue()[i], requiredType);
                    }
                    properties.put(entry.getKey(), new ExternalPropertyImpl(new Name(entry.getKey(), NodeTypeRegistry.getInstance().getNamespaces()), this, session, values));
                } else {
                    properties.put(entry.getKey(),
                            new ExternalPropertyImpl(new Name(entry.getKey(), NodeTypeRegistry.getInstance().getNamespaces()), this, session,
                                    session.getValueFactory().createValue(entry.getValue().length > 0 ? entry.getValue()[0] : null, requiredType)));
                }
            }
        }

        if (data.getBinaryProperties() != null) {
            for (Map.Entry<String, Binary[]> entry : data.getBinaryProperties().entrySet()) {
                ExtendedPropertyDefinition definition = getPropertyDefinition(entry.getKey());
                if (definition != null && definition.getRequiredType() == PropertyType.BINARY) {
                    if (definition.isMultiple()) {
                        Value[] values = new Value[entry.getValue().length];
                        for (int i = 0; i < entry.getValue().length; i++) {
                            values[i] = session.getValueFactory().createValue(entry.getValue()[i]);
                        }
                        properties.put(entry.getKey(), new ExternalPropertyImpl(new Name(entry.getKey(), NodeTypeRegistry.getInstance().getNamespaces()), this, session, values));
                    } else {
                        properties.put(entry.getKey(),
                                new ExternalPropertyImpl(new Name(entry.getKey(), NodeTypeRegistry.getInstance().getNamespaces()), this, session,
                                        session.getValueFactory().createValue(entry.getValue()[0])));
                    }
                }
            }
        }

        properties.put(Constants.JCR_UUID,
                new ExternalPropertyImpl(new Name(Constants.JCR_UUID, NodeTypeRegistry.getInstance().getNamespaces()), this, session,
                        session.getValueFactory().createValue(getIdentifier())));
        properties.put(Constants.JCR_PRIMARYTYPE,
                new ExternalPropertyImpl(new Name(Constants.JCR_PRIMARYTYPE, NodeTypeRegistry.getInstance().getNamespaces()), this, session,
                        session.getValueFactory().createValue(data.getType(), PropertyType.NAME)));
        ExtendedNodeType[] values = getMixinNodeTypes();
        if (values.length > 0) {
            List<Value> mixins = new ArrayList<Value>();
            for (ExtendedNodeType value : values) {
                mixins.add(session.getValueFactory().createValue(value.getName(), PropertyType.NAME));
            }
            properties.put(Constants.JCR_MIXINTYPES,
                    new ExternalPropertyImpl(new Name(Constants.JCR_MIXINTYPES, NodeTypeRegistry.getInstance().getNamespaces()), this, session,
                            mixins.toArray(new Value[mixins.size()])));
        }
    }

    private NodeDefinition getChildNodeDefinition(String name, String childType) throws RepositoryException {
        Map<String, ExtendedNodeDefinition> nodeDefinitionsAsMap = getExtendedPrimaryNodeType().getChildNodeDefinitionsAsMap();
        if (nodeDefinitionsAsMap.containsKey(name)) {
            return nodeDefinitionsAsMap.get(name);
        }
        ExtendedNodeType childTypeNT = NodeTypeRegistry.getInstance().getNodeType(childType);
        for (NodeType nodeType : getMixinNodeTypes()) {
            nodeDefinitionsAsMap = ((ExtendedNodeType) nodeType).getChildNodeDefinitionsAsMap();
            if (nodeDefinitionsAsMap.containsKey(name)) {
                return nodeDefinitionsAsMap.get(name);
            }
            for (Map.Entry<String, ExtendedNodeDefinition> entry : ((ExtendedNodeType) nodeType).getUnstructuredChildNodeDefinitions().entrySet()) {
                if (childTypeNT.isNodeType(entry.getKey())) {
                    return entry.getValue();
                }
            }
        }
        for (Map.Entry<String, ExtendedNodeDefinition> entry : getExtendedPrimaryNodeType().getUnstructuredChildNodeDefinitions().entrySet()) {
            if (childTypeNT.isNodeType(entry.getKey())) {
                return entry.getValue();
            }
        }
        Node extensionNode = getExtensionNode(false);
        if (extensionNode != null && extensionNode.isNodeType("jnt:externalProviderExtension")) {
            return extensionNode.getDefinition();

        }
        return null;
    }

    public ExtendedPropertyDefinition getPropertyDefinition(String name) throws RepositoryException {
        Map<String, ExtendedPropertyDefinition> propertyDefinitionsAsMap = getExtendedPrimaryNodeType().getPropertyDefinitionsAsMap();
        if (propertyDefinitionsAsMap.containsKey(name)) {
            return propertyDefinitionsAsMap.get(name);
        }
        for (NodeType nodeType : getMixinNodeTypes(false)) {
            propertyDefinitionsAsMap = ((ExtendedNodeType) nodeType).getPropertyDefinitionsAsMap();
            if (propertyDefinitionsAsMap.containsKey(name)) {
                return propertyDefinitionsAsMap.get(name);
            }
        }
        if (getExtensionNode(false) != null) {
            for (NodeType nodeType : getExtensionNode(false).getMixinNodeTypes()) {
                nodeType = NodeTypeRegistry.getInstance().getNodeType(nodeType.getName());
                propertyDefinitionsAsMap = ((ExtendedNodeType) nodeType).getPropertyDefinitionsAsMap();
                if (propertyDefinitionsAsMap.containsKey(name)) {
                    return propertyDefinitionsAsMap.get(name);
                }
            }
        }
        if (!getExtendedPrimaryNodeType().getUnstructuredPropertyDefinitions().isEmpty()) {
            return getExtendedPrimaryNodeType().getUnstructuredPropertyDefinitions().values().iterator().next();
        }
        return null;
    }

    public ExternalData getData() {
        return data;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPath() throws RepositoryException {
        return data.getPath();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() throws RepositoryException {
        return data.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Node getParent() throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        if (data.getPath().equals("/")) {
            throw new ItemNotFoundException();
        }
        String path = StringUtils.substringBeforeLast(data.getPath(), "/");
        try {
            controlManager.checkRead(path.isEmpty() ? "/" : path);
        } catch (PathNotFoundException e) {
            throw new AccessDeniedException(path);
        }
        return session.getNode(path.isEmpty() ? "/" : path);
    }

    public List<String> getExternalChildren() throws RepositoryException {
        if (externalChildren == null) {
            if (isNew) {
                externalChildren = new ArrayList<String>();
            } else {
                ExternalContentStoreProvider.setCurrentSession(session);
                try {
                    final ExternalDataSource dataSource = session.getRepository().getDataSource();
                    if (dataSource instanceof ExternalDataSource.CanLoadChildrenInBatch) {
                        ExternalDataSource.CanLoadChildrenInBatch childrenLoader = (ExternalDataSource.CanLoadChildrenInBatch) dataSource;
                        final List<ExternalData> childrenNodes = childrenLoader.getChildrenNodes(getPath());
                        if (externalChildren == null) {
                            externalChildren = new ArrayList<String>(childrenNodes.size());
                        }
                        for (ExternalData child : childrenNodes) {
                            String parentPath = StringUtils.substringBeforeLast(child.getPath(), "/");
                            if (parentPath.equals("")) {
                                parentPath = "/";
                            }
                            if (parentPath.equals(getPath())) {
                                externalChildren.add(child.getName());
                            }
                            final ExternalNodeImpl node = new ExternalNodeImpl(child, session);
                            session.registerNode(node);
                        }
                    } else {
                        externalChildren = new ArrayList<String>(dataSource.getChildren(getPath()));
                    }
                } finally {
                    ExternalContentStoreProvider.removeCurrentSession();
                }
            }
        }
        return externalChildren;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNode() {
        return true;
    }

    @Override
    void setNew(boolean isNew) throws RepositoryException {
        super.setNew(isNew);
        if (!isNew) {
            if (!data.getTmpId().equals(data.getId())) {
                getSession().getRepository().getStoreProvider().getExternalProviderInitializerService().updateExternalIdentifier(data.getTmpId(), data.getId(), getSession().getRepository().getProviderKey(), false);
            }
            data.markSaved();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove() throws VersionException, LockException, ConstraintViolationException, RepositoryException {

        if (!(session.getRepository().getDataSource() instanceof ExternalDataSource.Writable)) {
            throw new UnsupportedRepositoryOperationException();
        }

        if (!canRemoveNode()) {
            throw new AccessDeniedException(getPath());
        }

        if (!data.getPath().equals("/")) {
            ((ExternalNodeImpl) getParent()).getExternalChildren().remove(getName());
        }

        session.getDeletedData().put(getPath(), data);
        session.unregisterNode(this);

        Node extensionNode = getExtensionNode(false);
        if (extensionNode != null) {
            extensionNode.remove();
        }
    }

    protected void removeProperty(String name) throws RepositoryException {

        if (!(session.getRepository().getDataSource() instanceof ExternalDataSource.Writable)) {
            throw new UnsupportedRepositoryOperationException();
        }
        checkModify();

        boolean hasProperty = false;
        if (data.getBinaryProperties() != null && data.getBinaryProperties().containsKey(name)) {
            hasProperty = true;
            data.getBinaryProperties().remove(name);
            properties.remove(name);
        }
        if (data.getProperties() != null && data.getProperties().containsKey(name)) {
            hasProperty = true;
            data.getProperties().remove(name);
            properties.remove(name);
        }
        if (data.getLazyBinaryProperties() != null && data.getLazyBinaryProperties().contains(name)) {
            hasProperty = true;
            data.getLazyBinaryProperties().remove(name);
        }
        if (data.getLazyProperties() != null && data.getLazyProperties().contains(name)) {
            hasProperty = true;
            data.getLazyProperties().remove(name);
        }
        if (hasProperty) {
            session.getChangedData().put(getPath(), data);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Node addNode(String relPath) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException, RepositoryException {
        return addNode(relPath, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Node addNode(String relPath, String primaryNodeTypeName) throws ItemExistsException, PathNotFoundException, NoSuchNodeTypeException, LockException, VersionException, ConstraintViolationException, RepositoryException {

        checkAddChildNodes();

        if (canItemBeExtended(relPath, primaryNodeTypeName)) {

            if ((StringUtils.equals(primaryNodeTypeName, ExternalDataAcl.ACL_NODE_TYPE) || StringUtils.equals(primaryNodeTypeName, ExternalDataAce.ACE_NODE_TYPE))
                    && (session.getRepository().getDataSource() instanceof ExternalDataSource.AccessControllable ||
                        session.getRepository().getDataSource() instanceof ExternalDataSource.SupportPrivileges)) {
                throw new UnsupportedRepositoryOperationException("Acl and Ace are handle by DataSource");
            }

            Node extendedNode = getExtensionNode(true);

            if (extendedNode != null) {
                Node n = extendedNode.addNode(relPath, primaryNodeTypeName);
                n.addMixin("jmix:externalProviderExtension");
                List<Value> values = ExtensionNode.createNodeTypeValues(session.getValueFactory(), primaryNodeTypeName);
                n.setProperty("j:extendedType", values.toArray(new Value[values.size()]));
                n.setProperty("j:isExternalProviderRoot", false);
                String localPath = (getPath().equals("/") ? "/" : getPath() + "/") + relPath;
                return new ExtensionNode(n, localPath, getSession());
            }
        }

        if (!(session.getRepository().getDataSource() instanceof ExternalDataSource.Writable)) {
            throw new UnsupportedRepositoryOperationException();
        }

        String separator = StringUtils.equals(this.data.getId(), "/") ? "" : "/";
        ExternalData subNodeData = new ExternalData(this.data.getId() + separator + relPath, getPath() + (getPath().equals("/") ? "" : "/") + relPath, primaryNodeTypeName, new HashMap<String, String[]>(), true);
        final ExternalNodeImpl newNode = new ExternalNodeImpl(subNodeData, session);
        session.registerNode(newNode);
        session.getChangedData().put(subNodeData.getPath(), subNodeData);
        session.setNewItem(newNode);
        getExternalChildren().add(relPath);
        return newNode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void orderBefore(String srcChildRelPath, String destChildRelPath) throws UnsupportedRepositoryOperationException, VersionException, ConstraintViolationException, ItemNotFoundException, LockException, RepositoryException {
        if (srcChildRelPath.equals(destChildRelPath)) {
            return;
        }
        List<String> children = getExternalChildren();

        children.remove(srcChildRelPath);
        if (destChildRelPath == null || !children.contains(destChildRelPath)) {
            // put scrChildNode at the end of the list
            children.add(srcChildRelPath);
        } else {
            children.add(children.indexOf(destChildRelPath), srcChildRelPath);
        }
        session.getOrderedData().put(getPath(), children);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Property setProperty(String name, Value value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {

        checkModify();

        if (canItemBeExtended(getPropertyDefinition(name))) {
            final Node extensionNode = getExtensionNode(true);
            if (extensionNode != null) {
                return new ExtensionProperty(extensionNode.setProperty(name, value), getPath() + "/" + name, session, this);
            }
        }
        if (!(session.getRepository().getDataSource() instanceof ExternalDataSource.Writable)) {
            throw new UnsupportedRepositoryOperationException();
        }
        if (value == null) {
            if (hasProperty(name)) {
                removeProperty(name);
            }
            return null;
        }
        ExtendedPropertyDefinition epd = getPropertyDefinition(name);

        if (!hasProperty(name) || (hasProperty(name) && !getProperty(name).getValue().equals(value))) {
            if (epd.getRequiredType() == PropertyType.BINARY) {
                if (data.getBinaryProperties() == null) {
                    data.setBinaryProperties(new HashMap<String, Binary[]>());
                }
                data.getBinaryProperties().put(name, new Binary[]{value.getBinary()});
            } else {
                data.getProperties().put(name, new String[]{value.getString()});
            }
            final ExternalPropertyImpl newProperty = new ExternalPropertyImpl(new Name(name, NodeTypeRegistry.getInstance().getNamespaces()), this, session, value);
            properties.put(name, newProperty);
            session.setNewItem(newProperty);
            session.getChangedData().put(getPath(), data);
        }

        return getProperty(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Property setProperty(String name, Value value, int type) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return setProperty(name, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Property setProperty(String name, Value[] values) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {

        checkModify();

        if (canItemBeExtended(getPropertyDefinition(name))) {
            final Node extensionNode = getExtensionNode(true);
            if (extensionNode != null) {
                return new ExtensionProperty(extensionNode.setProperty(name, values), getPath() + "/" + name, session, this);
            }
        }
        if (!(session.getRepository().getDataSource() instanceof ExternalDataSource.Writable)) {
            throw new UnsupportedRepositoryOperationException();
        }
        if (values == null) {
            if (hasProperty(name)) {
                removeProperty(name);
            }
            return null;
        }

        ExtendedPropertyDefinition epd = getPropertyDefinition(name);

        if (!hasProperty(name) || (hasProperty(name) && !Arrays.equals(getProperty(name).getValues(), values))) {
            if (epd.getRequiredType() == PropertyType.BINARY) {
                if (data.getBinaryProperties() == null) {
                    data.setBinaryProperties(new HashMap<String, Binary[]>());
                }

                Binary[] b = new Binary[values.length];
                for (int i = 0; i < values.length; i++) {
                    b[i] = values[i] != null ? values[i].getBinary() : null;
                }
                data.getBinaryProperties().put(name, b);
            } else {
                String[] s = new String[values.length];
                for (int i = 0; i < values.length; i++) {
                    s[i] = values[i] != null ? values[i].getString() : null;
                }
                data.getProperties().put(name, s);
            }
            final ExternalPropertyImpl newProperty = new ExternalPropertyImpl(new Name(name, NodeTypeRegistry.getInstance().getNamespaces()), this, session, values);
            properties.put(name, newProperty);
            session.setNewItem(newProperty);
            session.getChangedData().put(getPath(), data);
        }

        return getProperty(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Property setProperty(String name, Value[] values, int type) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return setProperty(name, values);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Property setProperty(String name, String[] values) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        Value[] v = null;
        if (values != null) {
            v = new Value[values.length];
            for (int i = 0; i < values.length; i++) {
                v[i] = values[i] != null ? getSession().getValueFactory().createValue(values[i]) : null;
            }
        }
        return setProperty(name, v);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Property setProperty(String name, String[] values, int type) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return setProperty(name, values);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Property setProperty(String name, String value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        Value v = null;
        if (value != null) {
            v = getSession().getValueFactory().createValue(value);
        }
        return setProperty(name, v);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Property setProperty(String name, String value, int type) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return setProperty(name, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("deprecation")
    public Property setProperty(String name, InputStream value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkModify();
        if (canItemBeExtended(getPropertyDefinition(name))) {
            final Node extensionNode = getExtensionNode(true);
            if (extensionNode != null) {
                return new ExtensionProperty(extensionNode.setProperty(name, value), getPath() + "/" + name, session, this);
            }
        }
        if (!(session.getRepository().getDataSource() instanceof ExternalDataSource.Writable)) {
            throw new UnsupportedRepositoryOperationException();
        }
        if (value == null) {
            if (hasProperty(name)) {
                removeProperty(name);
            }
            return null;
        }
        Value v = null;
        Binary binary = null;
        try {
            binary = new BinaryImpl(value);
            Binary[] b = {binary};
            if (data.getBinaryProperties() == null) {
                data.setBinaryProperties(new HashMap<String, Binary[]>());
            }
            data.getBinaryProperties().put(name, b);
            v = getSession().getValueFactory().createValue(binary);
            session.registerTemporaryBinary(binary);
        } catch (IOException e) {
            throw new RepositoryException(e);
        }
        return setProperty(name, v);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Property setProperty(String name, boolean value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        Value v = getSession().getValueFactory().createValue(value);
        return setProperty(name, v);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Property setProperty(String name, double value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        Value v = getSession().getValueFactory().createValue(value);
        return setProperty(name, v);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Property setProperty(String name, long value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        Value v = getSession().getValueFactory().createValue(value);
        return setProperty(name, v);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Property setProperty(String name, Calendar value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        Value v = null;
        if (value != null) {
            v = getSession().getValueFactory().createValue(value);
        }
        return setProperty(name, v);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Property setProperty(String name, Node value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        Value v = null;
        if (value != null) {
            v = getSession().getValueFactory().createValue(value);
        }
        return setProperty(name, v);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Node getNode(String s) throws RepositoryException {
        Node n = session.getNode(getPath().endsWith("/") ? getPath() + s : getPath() + "/" + s);
        if (n != null) {
            return n;
        }
        n = getExtensionNode(false);
        if (n != null) {
            return new ExtensionNode(n.getNode(s), getPath() + "/" + s, getSession());
        }
        throw new PathNotFoundException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeIterator getNodes() throws RepositoryException {
        return getNodes(MATCH_ALL_PATTERN);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeIterator getNodes(String namePattern) throws RepositoryException {

        List<String> filteredList = new ArrayList<>();
        final boolean matchAll = MATCH_ALL_PATTERN.equals(namePattern);
        final ExternalDataSource dataSource = session.getRepository().getDataSource();

        if (ExternalDataAcl.ACL_NODE_NAME.equals(data.getName()) &&
                ExternalDataAcl.ACL_NODE_TYPE.equals(data.getType()) && data.getId().startsWith(ExternalDataAcl.ACL_NODE_NAME) &&
                dataSource instanceof ExternalDataSource.AccessControllable) {
            // get list of ace
            ExternalNodeImpl parent = (ExternalNodeImpl) getParent();
            if (parent.data.getExternalDataAcl() != null && parent.data.getExternalDataAcl().getAcl() != null && parent.data.getExternalDataAcl().getAcl().size() > 0) {
                for (ExternalDataAce ace : parent.data.getExternalDataAcl().getAcl()) {
                    String aceNodeName = ace.toString();
                    if (matchAll || ChildrenCollectorFilter.matches(aceNodeName, namePattern)) {
                        filteredList.add(aceNodeName);
                    }
                }
            }
            return new ExternalNodeIterator(filteredList);
        }

        final List<String> externalChildren = getExternalChildren();
        if (!externalChildren.isEmpty()) {
            if (!namePattern.equals("j:translation*")) {
                if (!matchAll) {
                    for (String path : externalChildren) {
                        if (ChildrenCollectorFilter.matches(path, namePattern)) {
                            filteredList.add(path);
                        }
                    }
                } else {
                    filteredList.addAll(externalChildren);
                }
            }
        }


        Set<String> languages = new HashSet<>();
        if (data.getI18nProperties() != null) {
            languages.addAll(data.getI18nProperties().keySet());
        }
        if (data.getLazyI18nProperties() != null) {
            languages.addAll(data.getLazyI18nProperties().keySet());
        }
        for (String lang : languages) {
            if (matchAll || ChildrenCollectorFilter.matches(J_TRANSLATION + lang, namePattern)) {
                filteredList.add(J_TRANSLATION + lang);
            }
        }

        // handle acl
        if (data.getExternalDataAcl() != null &&
                dataSource instanceof ExternalDataSource.AccessControllable &&
                (matchAll || ChildrenCollectorFilter.matches(ExternalDataAcl.ACL_NODE_NAME, namePattern))) {
            filteredList.add(ExternalDataAcl.ACL_NODE_NAME);
        }

        Node n = getExtensionNode(false);
        if (n != null) {
            return new ExternalNodeIterator(filteredList, matchAll ? n.getNodes() : n.getNodes(namePattern));
        }
        return new ExternalNodeIterator(filteredList);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeIterator getNodes(String[] nameGlobs) throws RepositoryException {

        final List<String> filteredList = new ArrayList<String>();
        final ExternalDataSource dataSource = session.getRepository().getDataSource();

        if (ExternalDataAcl.ACL_NODE_NAME.equals(data.getName()) &&
                ExternalDataAcl.ACL_NODE_TYPE.equals(data.getType()) && data.getId().startsWith(ExternalDataAcl.ACL_NODE_NAME) &&
                dataSource instanceof ExternalDataSource.AccessControllable) {
            // get list of ace
            ExternalNodeImpl parent = (ExternalNodeImpl) getParent();
            if (parent.data.getExternalDataAcl() != null && parent.data.getExternalDataAcl().getAcl() != null && parent.data.getExternalDataAcl().getAcl().size() > 0) {
                for (ExternalDataAce ace : parent.data.getExternalDataAcl().getAcl()) {
                    String aceNodeName = ace.toString();
                    if (ChildrenCollectorFilter.matches(aceNodeName, nameGlobs)) {
                        filteredList.add(aceNodeName);
                    }
                }
            }
            return new ExternalNodeIterator(filteredList);
        }

        for (String path : getExternalChildren()) {
            if (ChildrenCollectorFilter.matches(path, nameGlobs)) {
                filteredList.add(path);
            }
        }
        Set<String> languages = new HashSet<String>();
        if (data.getI18nProperties() != null) {
            languages.addAll(data.getI18nProperties().keySet());
        }
        if (data.getLazyI18nProperties() != null) {
            languages.addAll(data.getLazyI18nProperties().keySet());
        }
        for (String lang : languages) {
            if (ChildrenCollectorFilter.matches(J_TRANSLATION + lang, nameGlobs)) {
                filteredList.add(J_TRANSLATION + lang);
            }
        }

        // handle acl
        if (data.getExternalDataAcl() != null &&
                dataSource instanceof ExternalDataSource.AccessControllable &&
                ChildrenCollectorFilter.matches(ExternalDataAcl.ACL_NODE_NAME, nameGlobs)) {
            filteredList.add(ExternalDataAcl.ACL_NODE_NAME);
        }

        Node n = getExtensionNode(false);
        if (n != null) {
            return new ExternalNodeIterator(filteredList, n.getNodes(nameGlobs));
        }
        return new ExternalNodeIterator(filteredList);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Property getProperty(String s) throws PathNotFoundException, RepositoryException {
        Node n = getExtensionNode(false);
        if (n != null && n.hasProperty(s) && getPropertyDefinition(s) != null && canItemBeExtended(getPropertyDefinition(s))) {
            return new ExtensionProperty(n.getProperty(s), getPath() + "/" + s, session, this);
        }
        Property property = properties.get(s);
        if (property == null) {
            if (data.getLazyProperties() != null && data.getLazyProperties().contains(s)) {
                String[] values;
                if (properties.containsKey(Constants.JCR_LANGUAGE)) {
                    values = session.getI18nPropertyValues(data, properties.get(Constants.JCR_LANGUAGE).getString(), s);
                } else {
                    values = session.getPropertyValues(data, s);
                }
                data.getProperties().put(s, values);
                data.getLazyProperties().remove(s);
                ExtendedPropertyDefinition definition = getPropertyDefinition(s);
                if (definition != null && definition.getName().equals(MATCH_ALL_PATTERN) && data != null && data.getType() != null && data.getType().equals(Constants.JAHIANT_TRANSLATION)) {
                    definition = ((ExternalNodeImpl) getParent()).getPropertyDefinition(s);
                }
                ExternalPropertyImpl p;
                if (definition != null && definition.isMultiple()) {
                    Value[] jcrValues = null;
                    if (values != null) {
                        jcrValues = new Value[values.length];
                        for (int i = 0; i < values.length; i++) {
                            if (values[i] != null) {
                                jcrValues[i] = session.getValueFactory().createValue(values[i], definition.getRequiredType());
                            } else {
                                jcrValues[i] = null;
                            }
                        }
                    } 
                    p = new ExternalPropertyImpl(new Name(s, NodeTypeRegistry.getInstance().getNamespaces()), this, session, jcrValues);
                } else {
                    Value jcrValue = null;
                    if (values != null && values.length > 0) {
                        jcrValue = session.getValueFactory().createValue(values[0], definition.getRequiredType());
                    }
                    p = new ExternalPropertyImpl(new Name(s, NodeTypeRegistry.getInstance().getNamespaces()), this, session, jcrValue);
                }
                properties.put(s, p);
                return p;
            } else if (data.getLazyBinaryProperties() != null && data.getLazyBinaryProperties().contains(s)) {
                Binary[] values = session.getBinaryPropertyValues(data, s);
                data.getBinaryProperties().put(s, values);
                data.getLazyBinaryProperties().remove(s);
                ExternalPropertyImpl p;
                ExtendedPropertyDefinition definition = getPropertyDefinition(s);
                if (definition != null && definition.isMultiple()) {
                    Value[] jcrValues = null;
                    if (values != null) {
                        jcrValues = new Value[values.length];
                        for (int i = 0; i < values.length; i++) {
                            if (values[i] != null) {
                                jcrValues[i] = session.getValueFactory().createValue(values[i]);
                            } else {
                                jcrValues[i] = null;
                            }
                        }
                    } 
                    p = new ExternalPropertyImpl(new Name(s, NodeTypeRegistry.getInstance().getNamespaces()), this, session, jcrValues);
                } else {
                    Value jcrValue = null;
                    if (values != null && values.length > 0) {
                        jcrValue = session.getValueFactory().createValue(values[0]);
                    }
                    p = new ExternalPropertyImpl(new Name(s, NodeTypeRegistry.getInstance().getNamespaces()), this, session, jcrValue);
                }
                properties.put(s, p);
                return p;
            } else {
                throw new PathNotFoundException(s);
            }
        }
        return property;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PropertyIterator getProperties() throws RepositoryException {
        Node n = getExtensionNode(false);
        if (n != null) {
            return new ExternalPropertyIterator(properties, n.getProperties(), data.getLazyProperties(), data.getLazyBinaryProperties(), this);
        }
        return new ExternalPropertyIterator(properties, data.getLazyProperties(), data.getLazyBinaryProperties(), this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PropertyIterator getProperties(String namePattern) throws RepositoryException {
        final Map<String, ExternalPropertyImpl> filteredList = new HashMap<String, ExternalPropertyImpl>();
        for (Map.Entry<String, ExternalPropertyImpl> entry : properties.entrySet()) {
            if (ChildrenCollectorFilter.matches(entry.getKey(), namePattern)) {
                filteredList.put(entry.getKey(), entry.getValue());
            }
        }
        Set<String> lazyProperties = null;
        if (data.getLazyProperties() != null) {
            lazyProperties = new HashSet<String>();
            for (String propertyName : data.getLazyProperties()) {
                if (ChildrenCollectorFilter.matches(propertyName, namePattern)) {
                    lazyProperties.add(propertyName);
                }
            }
        }
        Set<String> lazyBinaryProperties = null;
        if (data.getLazyBinaryProperties() != null) {
            lazyBinaryProperties = new HashSet<String>();
            for (String propertyName : data.getLazyBinaryProperties()) {
                if (ChildrenCollectorFilter.matches(propertyName, namePattern)) {
                    lazyBinaryProperties.add(propertyName);
                }
            }
        }
        Node n = getExtensionNode(false);
        if (n != null) {
            return new ExternalPropertyIterator(filteredList, n.getProperties(namePattern), lazyProperties, lazyBinaryProperties, this);
        }
        return new ExternalPropertyIterator(filteredList, lazyProperties, lazyBinaryProperties, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Item getPrimaryItem() throws ItemNotFoundException, RepositoryException {
        throw new ItemNotFoundException("External node does not support getPrimaryItem");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUUID() throws UnsupportedRepositoryOperationException, RepositoryException {
        return getIdentifier();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getIndex() throws RepositoryException {
        return 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PropertyIterator getReferences() throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNode(String s) throws RepositoryException {
        return session.itemExists(getPath().endsWith("/") ? getPath() + s : getPath() + "/" + s);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasProperty(String relPath) throws RepositoryException {
        return properties.containsKey(relPath) ||
                (data.getLazyProperties() != null && data.getLazyProperties().contains(relPath)) ||
                (data.getLazyBinaryProperties() != null && data.getLazyBinaryProperties().contains(relPath)) ||
                (getExtensionNode(false) != null && getPropertyDefinition(relPath) != null && canItemBeExtended(getPropertyDefinition(relPath)) && getExtensionNode(false).hasProperty(relPath));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNodes() throws RepositoryException {
        return getNodes().hasNext();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasProperties() throws RepositoryException {
        return (!properties.isEmpty() ||
                (data.getLazyProperties() != null && !data.getLazyProperties().isEmpty()) ||
                (data.getLazyBinaryProperties() != null && !data.getLazyBinaryProperties().isEmpty()) ||
                getProperties().hasNext());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExtendedNodeType getPrimaryNodeType() throws RepositoryException {
        return getExtendedPrimaryNodeType();
    }

    public ExtendedNodeType getExtendedPrimaryNodeType() throws RepositoryException {
        return NodeTypeRegistry.getInstance().getNodeType(data.getType());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExtendedNodeType[] getMixinNodeTypes() throws RepositoryException {
        return getMixinNodeTypes(true);
    }

    private ExtendedNodeType[] getMixinNodeTypes(boolean withExtension) throws RepositoryException {

        List<ExtendedNodeType> nt = new ArrayList<ExtendedNodeType>();
        if (data.getMixin() != null) {
            for (String s : data.getMixin()) {
                nt.add(NodeTypeRegistry.getInstance().getNodeType(s));
            }
        }

        if(data.getExternalDataAcl() != null && data.getExternalDataAcl().getAcl().size() > 0 &&
                session.getRepository().getDataSource() instanceof ExternalDataSource.AccessControllable) {
            nt.add(NodeTypeRegistry.getInstance().getNodeType("jmix:accessControlled"));
        }

        if (withExtension) {
            Node extensionNode = getExtensionNode(false);
            if (extensionNode != null) {
                for (NodeType type : extensionNode.getMixinNodeTypes()) {
                    if (!type.isNodeType("jmix:externalProviderExtension")) {
                        nt.add(NodeTypeRegistry.getInstance().getNodeType(type.getName()));
                    }
                }
            }
        }
        return nt.toArray(new ExtendedNodeType[nt.size()]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNodeType(String nodeTypeName) throws RepositoryException {
        return isNodeType(nodeTypeName, true);
    }

    public boolean isNodeType(String nodeTypeName, boolean withExtension) throws RepositoryException {
        if (getPrimaryNodeType().isNodeType(nodeTypeName)) {
            return true;
        }
        for (NodeType nodeType : getMixinNodeTypes(withExtension)) {
            if (nodeType.isNodeType(nodeTypeName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addMixin(String mixinName) throws NoSuchNodeTypeException, VersionException, ConstraintViolationException, LockException, RepositoryException {

        if (isNodeType(mixinName) || !canManageNodeTypes()) {
            return;
        }

        if (isExtensionForbiddenMixin(mixinName) && !(data.getMixin() != null && data.getMixin().contains(mixinName))) {
            List<String> mixins = data.getMixin() == null ? new ArrayList<String>() : new ArrayList<>(data.getMixin());
            mixins.add(mixinName);
            data.setMixin(mixins);
            return;
        }

        Node extensionNode = getExtensionNode(true);
        if (extensionNode != null) {
            extensionNode.addMixin(mixinName);
            return;
        }

        throw new UnsupportedRepositoryOperationException();
    }

    protected boolean isExtensionForbiddenMixin(String mixinName) throws RepositoryException {
        return getSession().getExtensionForbiddenMixins().contains(mixinName) || getSession().getExtensionForbiddenMixins().contains("*");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeMixin(String mixinName) throws NoSuchNodeTypeException, VersionException, ConstraintViolationException, LockException, RepositoryException {

        if (!canManageNodeTypes()) {
            return;
        }

        if (isExtensionForbiddenMixin(mixinName) && data.getMixin() != null && data.getMixin().contains(mixinName)) {
            List<String> mixins = new ArrayList<>(data.getMixin());
            mixins.remove(mixinName);
            data.setMixin(mixins);
            return;
        }

        Node extensionNode = getExtensionNode(false);
        if (extensionNode != null) {
            extensionNode.removeMixin(mixinName);

            // remove child node and properties brought by the mixin
            PropertyIterator pi = getProperties();
            while (pi.hasNext()) {
                Property extensionProperty = pi.nextProperty();
                List<NodeType> nodeTypes = new ArrayList<NodeType>();
                nodeTypes.addAll(Arrays.asList(getMixinNodeTypes(true)));
                nodeTypes.add(NodeTypeRegistry.getInstance().getNodeType("jmix:externalProviderExtension"));
                boolean canSetProperty = extensionProperty.isMultiple() ? getPrimaryNodeType().canSetProperty(extensionProperty.getName(), extensionProperty.getValues()) : getPrimaryNodeType().canSetProperty(extensionProperty.getName(), extensionProperty.getValue());
                for (PropertyDefinition propertyDefinition : getPrimaryNodeType().getPropertyDefinitions()) {
                    if (propertyDefinition.getName().equals(extensionProperty.getName()) && propertyDefinition.getRequiredType() == extensionProperty.getType()) {
                        canSetProperty = true;
                        break;
                    }
                }
                if (!canSetProperty) {
                    for (NodeType mixinType : nodeTypes) {
                        if (!StringUtils.equals(mixinType.getName(), mixinName)) {
                            if (extensionProperty.isMultiple()) {
                                if (mixinType.canSetProperty(extensionProperty.getName(), extensionProperty.getValues())) {
                                    canSetProperty = true;
                                    break;
                                }
                            } else {
                                if (mixinType.canSetProperty(extensionProperty.getName(), extensionProperty.getValue())) {
                                    canSetProperty = true;
                                    break;
                                }
                            }
                        }
                    }
                    if (!canSetProperty) {
                        extensionProperty.remove();
                    }
                }
            }
            NodeIterator ni = extensionNode.getNodes();
            while (ni.hasNext()) {
                Node extensionChildNode = ni.nextNode();
                boolean canAddChildNode = getPrimaryNodeType().canAddChildNode(extensionChildNode.getName(), extensionChildNode.getPrimaryNodeType().getName());
                if (!canAddChildNode) {
                    for (NodeType mixinType : getMixinNodeTypes(true)) {
                        if (!StringUtils.equals(mixinType.getName(), mixinName)) {
                            if (mixinType.canAddChildNode(extensionChildNode.getName(), extensionChildNode.getPrimaryNodeType().getName())) {
                                canAddChildNode = true;
                                break;
                            }
                        }
                    }
                    if (!canAddChildNode) {
                        extensionChildNode.remove();
                    }
                }
            }
            return;
        }
        if (!isNodeType(mixinName)) {
            throw new NoSuchNodeTypeException("Mixin " + mixinName + " not included in node " + getPath());
        }

        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canAddMixin(String mixinName) throws NoSuchNodeTypeException, RepositoryException {
        if (isExtensionForbiddenMixin(mixinName) && canManageNodeTypes()) {
            return true;
        }
        Node extensionNode = getExtensionNode(true);
        return extensionNode != null && extensionNode.canAddMixin(mixinName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDefinition getDefinition() throws RepositoryException {
        ExternalNodeImpl parentNode;
        try {
            parentNode = (ExternalNodeImpl) getParent();
        } catch (ItemNotFoundException e) {
            return null;
        }
        ExtendedNodeType parentNodeType = parentNode.getExtendedPrimaryNodeType();
        ExtendedNodeDefinition nodeDefinition = parentNodeType.getChildNodeDefinitionsAsMap().get(getName());
        if (nodeDefinition != null) {
            return nodeDefinition;
        }
        for (Map.Entry<String, ExtendedNodeDefinition> entry : parentNodeType.getUnstructuredChildNodeDefinitions().entrySet()) {
            if (isNodeType(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Version checkin() throws VersionException, UnsupportedRepositoryOperationException, InvalidItemStateException, LockException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkout() throws UnsupportedRepositoryOperationException, LockException, RepositoryException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doneMerge(Version version) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cancelMerge(Version version) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(String s) throws NoSuchWorkspaceException, AccessDeniedException, LockException, InvalidItemStateException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeIterator merge(String s, boolean b) throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCorrespondingNodePath(String workspaceName) throws ItemNotFoundException, NoSuchWorkspaceException, AccessDeniedException, RepositoryException {
        return getPath();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCheckedOut() throws RepositoryException {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restore(String s, boolean b) throws VersionException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restore(Version version, boolean b) throws VersionException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restore(Version version, String s, boolean b) throws PathNotFoundException, ItemExistsException, VersionException, ConstraintViolationException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restoreByLabel(String s, boolean b) throws VersionException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VersionHistory getVersionHistory() throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Version getBaseVersion() throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Lock lock(boolean b, boolean b1) throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, InvalidItemStateException, RepositoryException {
        return session.getWorkspace().getLockManager().lock(getPath(), b, b1, Long.MAX_VALUE, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Lock getLock() throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, RepositoryException {
        return session.getWorkspace().getLockManager() != null ? session.getWorkspace().getLockManager().getLock(getPath()) : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unlock() throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, InvalidItemStateException, RepositoryException {
        if (session.getWorkspace().getLockManager() != null) {
            session.getWorkspace().getLockManager().unlock(getPath());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean holdsLock() throws RepositoryException {
        return session.getWorkspace().getLockManager() != null && session.getWorkspace().getLockManager().getLock(getPath()) != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isLocked() throws RepositoryException {
        return session.getWorkspace().getLockManager() != null && session.getWorkspace().getLockManager().isLocked(getPath());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Property setProperty(String name, Binary value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        Value v = null;
        if (value != null) {
            InputStream stream = value.getStream();
            try {
                return setProperty(name, stream);
            } finally {
                IOUtils.closeQuietly(stream);
            }
        }
        return setProperty(name, v);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Property setProperty(String name, BigDecimal value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        Value v = null;
        if (value != null) {
            v = getSession().getValueFactory().createValue(value);
        }
        return setProperty(name, v);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PropertyIterator getProperties(String[] nameGlobs) throws RepositoryException {
        final Map<String, ExternalPropertyImpl> filteredList = new HashMap<String, ExternalPropertyImpl>();
        for (Map.Entry<String, ExternalPropertyImpl> entry : properties.entrySet()) {
            if (ChildrenCollectorFilter.matches(entry.getKey(), nameGlobs)) {
                filteredList.put(entry.getKey(), entry.getValue());
            }
        }
        Set<String> lazyProperties = null;
        if (data.getLazyProperties() != null) {
            lazyProperties = new HashSet<String>();
            for (String propertyName : data.getLazyProperties()) {
                if (ChildrenCollectorFilter.matches(propertyName, nameGlobs)) {
                    lazyProperties.add(propertyName);
                }
            }
        }
        Set<String> lazyBinaryProperties = null;
        if (data.getLazyBinaryProperties() != null) {
            lazyBinaryProperties = new HashSet<String>();
            for (String propertyName : data.getLazyBinaryProperties()) {
                if (ChildrenCollectorFilter.matches(propertyName, nameGlobs)) {
                    lazyBinaryProperties.add(propertyName);
                }
            }
        }
        Node n = getExtensionNode(false);
        if (n != null) {
            return new ExternalPropertyIterator(filteredList, n.getProperties(nameGlobs), lazyProperties, lazyBinaryProperties, this);
        }
        return new ExternalPropertyIterator(filteredList, lazyProperties, lazyBinaryProperties, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String getIdentifier() throws RepositoryException {
        if (uuid == null) {
            if (!session.getRepository().getDataSource().isSupportsUuid() || data.getId().startsWith(ExternalSessionImpl.TRANSLATION_PREFIX)) {
                uuid = getStoreProvider().getOrCreateInternalIdentifier(data.getId());
            } else {
                uuid = data.getId();
            }
        }
        return uuid;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PropertyIterator getReferences(String name) throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PropertyIterator getWeakReferences() throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PropertyIterator getWeakReferences(String name) throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setPrimaryType(String nodeTypeName) throws NoSuchNodeTypeException, VersionException, ConstraintViolationException, LockException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeIterator getSharedSet() throws RepositoryException {
        return new ExternalNodeIterator(new ArrayList<String>());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeSharedSet() throws VersionException, LockException, ConstraintViolationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeShare() throws VersionException, LockException, ConstraintViolationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void followLifecycleTransition(String transition) throws UnsupportedRepositoryOperationException, InvalidLifecycleTransitionException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getAllowedLifecycleTransistions() throws UnsupportedRepositoryOperationException, RepositoryException {
        return new String[0];
    }

    public Node getExtensionNode(boolean create) throws RepositoryException {

        Session extensionSession = getSession().getExtensionSession();
        if (extensionSession == null) {
            return null;
        }
        List<String> extensionAllowedTypes = getSession().getExtensionAllowedTypes();
        boolean allowed = false;
        if (extensionAllowedTypes != null) {
            for (String type : extensionAllowedTypes) {
                if (isNodeType(type, false)) {
                    allowed = true;
                    break;
                }
            }
        }
        if (!allowed) {
            return null;
        }
        String path = getPath();
        boolean isRoot = path.equals("/");

        String mountPointPath = getStoreProvider().getMountPoint();
        String globalPath = mountPointPath + (isRoot ? "" : path);

        if (!extensionSession.itemExists(globalPath)) {
            if (!create) {
                return null;
            } else {
                // create extension nodes if needed
                String[] pathElements = StringUtils.split(path,"/");
                StringBuilder currentExtensionPath = new StringBuilder(mountPointPath);
                StringBuilder currentExternalPath = new StringBuilder();
                // create extension node on the mountpoint if needed
                if (!extensionSession.nodeExists(mountPointPath)) {
                    String parentPath = StringUtils.substringBeforeLast(mountPointPath, "/");
                    if (parentPath.equals("")) {
                        parentPath = "/";
                    }
                    final Node extensionParent = extensionSession.getNode(parentPath);
                    takeLockToken(extensionParent);
                    extensionParent.addMixin("jmix:hasExternalProviderExtension");
                    Node extensionNode = extensionParent.addNode(StringUtils.substringAfterLast(mountPointPath, "/"), "jnt:externalProviderExtension");
                    extensionNode.addMixin("jmix:externalProviderExtension");
                    extensionNode.setProperty("j:isExternalProviderRoot", true);
                    Node externalNode = (Node) session.getItemWithNoCheck("/");
                    extensionNode.setProperty("j:externalNodeIdentifier", externalNode.getIdentifier());
                    List<Value> values = ExtensionNode.createNodeTypeValues(session.getValueFactory(), externalNode.getPrimaryNodeType().getName());
                    extensionNode.setProperty("j:extendedType", values.toArray(new Value[values.size()]));
                }
                for (String pathElement : pathElements) {
                    currentExtensionPath.append("/").append(pathElement);
                    currentExternalPath.append("/").append(pathElement);
                    if (!extensionSession.nodeExists(currentExtensionPath.toString())) {
                        final Node extensionParent = extensionSession.getNode(StringUtils.substringBeforeLast(currentExtensionPath.toString(), "/"));
                        takeLockToken(extensionParent);
                        extensionParent.addMixin("jmix:hasExternalProviderExtension");
                        Node extensionNode = extensionParent.addNode(pathElement, "jnt:externalProviderExtension");
                        Node externalNode = (Node) session.getItemWithNoCheck(currentExternalPath.toString());
                        List<Value> values = ExtensionNode.createNodeTypeValues(session.getValueFactory(), externalNode.getPrimaryNodeType().getName());
                        extensionNode.setProperty("j:extendedType", values.toArray(new Value[values.size()]));
                        extensionNode.addMixin("jmix:externalProviderExtension");
                        extensionNode.setProperty("j:isExternalProviderRoot", false);
                        extensionNode.setProperty("j:externalNodeIdentifier", externalNode.getIdentifier());
                    }
                }
            }
        }

        Node extensionNode = extensionSession.getNode(globalPath);
        if (create && isRoot && !extensionNode.isNodeType("jmix:hasExternalProviderExtension")) {
            extensionNode.addMixin("jmix:hasExternalProviderExtension");
        }
        if (!extensionNode.isNodeType("jmix:externalProviderExtension")) {
            extensionNode.addMixin("jmix:externalProviderExtension");
        }
        return extensionNode;
    }

    public void takeLockToken(Node parentNode) throws RepositoryException {
        if (parentNode.isLocked() && parentNode.hasProperty("j:locktoken")) {
            parentNode.getSession().addLockToken(parentNode.getProperty("j:locktoken").getString());
        }
    }

    public boolean canItemBeExtended(String relPath, String primaryNodeTypeName) throws RepositoryException {
        return canItemBeExtended(getChildNodeDefinition(relPath, primaryNodeTypeName));
    }

    public boolean canItemBeExtended(ItemDefinition definition) throws RepositoryException {

        if (definition == null) {
            throw new ConstraintViolationException();
        }

        NodeType type = definition.getDeclaringNodeType();

        Map<String, List<String>> overridableProperties = getSession().getOverridableProperties();
        Map<String, List<String>> nonOverridableProperties = getSession().getNonOverridableProperties();
        for (Map.Entry<String, List<String>> entry : overridableProperties.entrySet()) {
            if ((entry.getKey().equals(MATCH_ALL_PATTERN) || type.getName().equals(entry.getKey())) &&
                    (entry.getValue().contains(MATCH_ALL_PATTERN) || entry.getValue().contains(definition.getName())) &&
                    !(nonOverridableProperties.containsKey(MATCH_ALL_PATTERN) && nonOverridableProperties.get(MATCH_ALL_PATTERN).contains(definition.getName())) &&
                    !(nonOverridableProperties.containsKey(type.getName()) && nonOverridableProperties.get(type.getName()).contains(definition.getName()))) {
                return true;
            }
        }

        if (type.isMixin()) {
            Node ext = getExtensionNode(false);
            if (ext != null) {
                for (NodeType assignedMixin : ext.getMixinNodeTypes()) {
                    if (assignedMixin.isNodeType(type.getName())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    public String toString() {
        return "external node " + data.getPath();
    }

    /**
     * Property iterator implementation
     */
    private class ExternalPropertyIterator implements PropertyIterator {

        private int pos = 0;
        private Iterator<ExternalPropertyImpl> it;
        private PropertyIterator extensionPropertiesIterator;
        private Property nextProperty = null;
        private Map<String, ExternalPropertyImpl> externalProperties;
        private Set<String> lazyProperties;
        private Iterator<String> lazyPropertiesIterator;
        private ExternalNodeImpl node;

        ExternalPropertyIterator(Map<String, ExternalPropertyImpl> externalPropertyMap, Set<String> lazyProperties,
                                 Set<String> lazyBinaryProperties, ExternalNodeImpl node) {
            this(externalPropertyMap, null, lazyProperties, lazyBinaryProperties, node);
        }

        ExternalPropertyIterator(Map<String, ExternalPropertyImpl> externalPropertyMap,
                                 PropertyIterator extensionPropertiesIterator, Set<String> lazyProperties,
                                 Set<String> lazyBinaryProperties, ExternalNodeImpl node) {
            this.extensionPropertiesIterator = extensionPropertiesIterator;
            this.externalProperties = new HashMap<String, ExternalPropertyImpl>(externalPropertyMap);
            this.lazyProperties = new HashSet<String>();
            if (lazyProperties != null) {
                this.lazyProperties.addAll(lazyProperties);
            }
            if (lazyBinaryProperties != null) {
                this.lazyProperties.addAll(lazyBinaryProperties);
            }
            this.node = node;
            fetchNext();
        }

        private void fetchNext() {
            nextProperty = null;
            if (extensionPropertiesIterator != null) {
                while (extensionPropertiesIterator.hasNext()) {
                    Property next = extensionPropertiesIterator.nextProperty();
                    try {
                        final ExtendedPropertyDefinition propertyDefinition = getPropertyDefinition(next.getName());
                        if (propertyDefinition != null && canItemBeExtended(propertyDefinition)) {
                            nextProperty = new ExtensionProperty(next, getPath() + "/" + next.getName(), node.getSession(), ExternalNodeImpl.this);
                            externalProperties.remove(next.getName());
                            lazyProperties.remove(next.getName());
                            return;
                        }
                    } catch (RepositoryException e) {
                        logger.error("Cannot get property", e);
                    }
                }
            }
            if (it == null) {
                it = externalProperties.values().iterator();
            }
            if (it.hasNext()) {
                nextProperty = it.next();
                return;
            }
            if (lazyPropertiesIterator == null && lazyProperties != null) {
                lazyPropertiesIterator = lazyProperties.iterator();
            }
            if (lazyPropertiesIterator != null && lazyPropertiesIterator.hasNext()) {
                String propertyName = lazyPropertiesIterator.next();
                try {
                    nextProperty = getProperty(propertyName);
                } catch (RepositoryException e) {
                    logger.error(e.getMessage(), e);
                    logger.error(e.getMessage(), e);
                }
            }
        }

        @Override
        public Property nextProperty() {
            if (nextProperty == null) {
                throw new NoSuchElementException();
            }
            Property next = nextProperty;
            fetchNext();
            pos++;
            return next;
        }

        @Override
        public void skip(long skipNum) {
            for (int i = 0; i < skipNum; i++) {
                nextProperty();
            }
        }

        @Override
        public long getSize() {
            return externalProperties.size() + (extensionPropertiesIterator != null ? extensionPropertiesIterator.getSize() : 0);
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
     * Node iterator implementation
     */
    private class ExternalNodeIterator implements NodeIterator {

        private int pos = 0;
        private Iterator<String> it;
        private final List<String> list;
        private NodeIterator extensionNodeIterator;
        private Node nextNode;

        public ExternalNodeIterator(List<String> list) {
            this(list, null);
        }

        public ExternalNodeIterator(List<String> list, NodeIterator extensionNodeIterator) {
            this.extensionNodeIterator = extensionNodeIterator;
            this.list = list;
            this.it = list.iterator();
            fetchNext();
        }

        private Node fetchNext() {
            nextNode = null;
            if (it.hasNext()) {
                Node next;
                do {
                    try {
                        next = getNode(it.next());
                    } catch (RepositoryException e) {
                        next = null;
                        logger.debug(e.getMessage(), e);
                    }
                } while (next == null && it.hasNext());
                nextNode = next;
                return nextNode;
            }
            if (extensionNodeIterator != null) {
                while (extensionNodeIterator.hasNext()) {
                    Node n = extensionNodeIterator.nextNode();
                    try {
                        if (!list.contains(n.getName())) {
                            String path = getPath();
                            if (!path.endsWith("/")) {
                                path += "/";
                            }
                            path += n.getName();
                            try {
                                nextNode = session.getNode(path);
                            } catch (PathNotFoundException e) {
                                logger.debug("Cannot find node " + path, e);
                            }
                            return nextNode;
                        }
                    } catch (RepositoryException e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
            return null;
        }

        @Override
        public Node nextNode() {
            if (nextNode == null) {
                throw new NoSuchElementException();
            }
            Node next = nextNode;
            fetchNext();
            pos++;
            return next;
        }

        @Override
        public void skip(long skipNum) {
            for (int i = 0; i < skipNum; i++) {
                nextNode();
            }
        }

        @Override
        public long getSize() {
            return list.size() + (extensionNodeIterator != null ? extensionNodeIterator.getSize() : 0);
        }

        @Override
        public long getPosition() {
            return pos;
        }

        @Override
        public boolean hasNext() {
            return nextNode != null;
        }

        @Override
        public Object next() {
            return nextNode();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
