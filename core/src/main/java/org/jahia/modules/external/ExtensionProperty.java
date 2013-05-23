package org.jahia.modules.external;

import org.apache.commons.lang.StringUtils;
import org.jahia.services.content.nodetypes.Name;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;

import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.VersionException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Calendar;

public class ExtensionProperty extends ExtensionItem implements Property {
    private Property property;
    private String uuid;

    public ExtensionProperty(Property property, String path, ExternalSessionImpl session, String uuid) {
        super(property, path, session);
        this.property = property;
        this.uuid = uuid;
    }

    @Override
    public void setValue(Value value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        property.setValue(value);
    }

    @Override
    public void setValue(Value[] values) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        property.setValue(values);
    }

    @Override
    public void setValue(String value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        property.setValue(value);
    }

    @Override
    public void setValue(String[] values) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        property.setValue(values);
    }

    @Override
    public void setValue(InputStream value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        property.setValue(value);
    }

    @Override
    public void setValue(Binary value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        property.setValue(value);
    }

    @Override
    public void setValue(long value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        property.setValue(value);
    }

    @Override
    public void setValue(double value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        property.setValue(value);
    }

    @Override
    public void setValue(BigDecimal value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        property.setValue(value);
    }

    @Override
    public void setValue(Calendar value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        property.setValue(value);
    }

    @Override
    public void setValue(boolean value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        property.setValue(value);
    }

    @Override
    public void setValue(Node value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        property.setValue(value);
    }

    @Override
    public Value getValue() throws ValueFormatException, RepositoryException {
        if (StringUtils.equals(getName(), "jcr:uuid")) {
            return getSession().getValueFactory().createValue(uuid);
        }  else {
            return property.getValue();
        }
    }

    @Override
    public Value[] getValues() throws ValueFormatException, RepositoryException {
        return property.getValues();
    }

    @Override
    public String getString() throws ValueFormatException, RepositoryException {
        return property.getString();
    }

    @Override
    public InputStream getStream() throws ValueFormatException, RepositoryException {
        return property.getStream();
    }

    @Override
    public Binary getBinary() throws ValueFormatException, RepositoryException {
        return property.getBinary();
    }

    @Override
    public long getLong() throws ValueFormatException, RepositoryException {
        return property.getLong();
    }

    @Override
    public double getDouble() throws ValueFormatException, RepositoryException {
        return property.getDouble();
    }

    @Override
    public BigDecimal getDecimal() throws ValueFormatException, RepositoryException {
        return property.getDecimal();
    }

    @Override
    public Calendar getDate() throws ValueFormatException, RepositoryException {
        return property.getDate();
    }

    @Override
    public boolean getBoolean() throws ValueFormatException, RepositoryException {
        return property.getBoolean();
    }

    @Override
    public Node getNode() throws ItemNotFoundException, ValueFormatException, RepositoryException {
        return property.getNode();
    }

    @Override
    public Property getProperty() throws ItemNotFoundException, ValueFormatException, RepositoryException {
        return property.getProperty();
    }

    @Override
    public long getLength() throws ValueFormatException, RepositoryException {
        return property.getLength();
    }

    @Override
    public long[] getLengths() throws ValueFormatException, RepositoryException {
        return property.getLengths();
    }

    @Override
    public PropertyDefinition getDefinition() throws RepositoryException {
        return property.getDefinition();
    }

    @Override
    public int getType() throws RepositoryException {
        return property.getType();
    }

    @Override
    public boolean isMultiple() throws RepositoryException {
        return property.isMultiple();
    }
}
