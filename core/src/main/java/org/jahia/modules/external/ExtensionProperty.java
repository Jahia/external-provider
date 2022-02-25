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

import org.apache.commons.io.input.AutoCloseInputStream;
import org.apache.commons.lang.StringUtils;

import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.VersionException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Calendar;

/**
 * Wrapper for extension Property
 */
public class ExtensionProperty extends ExtensionItem implements Property {
    private Property property;
    private Node parentNode;

    public ExtensionProperty(Property property, String path, ExternalSessionImpl session, Node parentNode) throws RepositoryException {
        super(property, path, session);
        this.property = property;
        this.parentNode = parentNode;
    }

    @Override
    public Node getParent() throws RepositoryException {
        return parentNode;
    }

    @Override
    public void setValue(Value value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkModify();
        property.setValue(value);
    }

    @Override
    public void setValue(Value[] values) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkModify();
        property.setValue(values);
    }

    @Override
    public void setValue(String value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkModify();
        property.setValue(value);
    }

    @Override
    public void setValue(String[] values) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkModify();
        property.setValue(values);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setValue(InputStream value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkModify();
        property.setValue(value);
    }

    @Override
    public void setValue(Binary value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkModify();
        property.setValue(value);
    }

    @Override
    public void setValue(long value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkModify();
        property.setValue(value);
    }

    @Override
    public void setValue(double value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkModify();
        property.setValue(value);
    }

    @Override
    public void setValue(BigDecimal value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkModify();
        property.setValue(value);
    }

    @Override
    public void setValue(Calendar value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkModify();
        property.setValue(value);
    }

    @Override
    public void setValue(boolean value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkModify();
        property.setValue(value);
    }

    @Override
    public void setValue(Node value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkModify();
        property.setValue(value);
    }

    @Override
    public Value getValue() throws ValueFormatException, RepositoryException {
        checkRead();
        if (StringUtils.equals(getName(), "jcr:uuid")) {
            return getSession().getValueFactory().createValue(parentNode.getIdentifier());
        }  else {
            return property.getValue();
        }
    }

    @Override
    public Value[] getValues() throws ValueFormatException, RepositoryException {
        checkRead();
        return property.getValues();
    }

    @Override
    public String getString() throws ValueFormatException, RepositoryException {
        checkRead();
        return property.getValue().getString();
    }

    @Override
    public InputStream getStream() throws ValueFormatException, RepositoryException {
        checkRead();
        final Binary binary = property.getValue().getBinary();
        return new AutoCloseInputStream(binary.getStream()) {
            @Override
            public void close() throws IOException {
                super.close();
                binary.dispose();
            }
        };
    }

    @Override
    public Binary getBinary() throws ValueFormatException, RepositoryException {
        checkRead();
        return property.getValue().getBinary();
    }

    @Override
    public long getLong() throws ValueFormatException, RepositoryException {
        checkRead();
        return property.getValue().getLong();
    }

    @Override
    public double getDouble() throws ValueFormatException, RepositoryException {
        checkRead();
        return property.getValue().getDouble();
    }

    @Override
    public BigDecimal getDecimal() throws ValueFormatException, RepositoryException {
        checkRead();
        return property.getValue().getDecimal();
    }

    @Override
    public Calendar getDate() throws ValueFormatException, RepositoryException {
        checkRead();
        return property.getValue().getDate();
    }

    @Override
    public boolean getBoolean() throws ValueFormatException, RepositoryException {
        checkRead();
        return property.getValue().getBoolean();
    }

    @Override
    public Node getNode() throws ItemNotFoundException, ValueFormatException, RepositoryException {
        checkRead();
        return property.getNode();
    }

    @Override
    public Property getProperty() throws ItemNotFoundException, ValueFormatException, RepositoryException {
        checkRead();
        return property.getProperty();
    }

    @Override
    public long getLength() throws ValueFormatException, RepositoryException {
        checkRead();
        return property.getLength();
    }

    @Override
    public long[] getLengths() throws ValueFormatException, RepositoryException {
        checkRead();
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
