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
import org.apache.jackrabbit.value.BinaryImpl;
import org.jahia.services.content.nodetypes.Name;

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
 * Implementation of the {@link javax.jcr.Property} for the {@link org.jahia.modules.external.ExternalData}.
 * User: toto
 * Date: Apr 23, 2008
 * Time: 11:46:28 AM
 */
public class ExternalPropertyImpl extends ExternalItemImpl implements Property {

    private ExternalNodeImpl node;
    private Name name;
    private Value[] values;
    private Value value;

    public ExternalPropertyImpl(Name name, ExternalNodeImpl node, ExternalSessionImpl session, Value value) throws RepositoryException {
        super(session);
        this.name = name;
        this.node = node;
        this.value = value;
    }

    public ExternalPropertyImpl(Name name, ExternalNodeImpl node, ExternalSessionImpl session, Value[] values) throws RepositoryException {
        super(session);
        this.name = name;
        this.node = node;
        this.values = values;
    }

    public ExternalPropertyImpl(Name name, ExternalNodeImpl node, ExternalSessionImpl session) throws RepositoryException {
        super(session);
        this.name = name;
        this.node = node;
    }

    public void setValue(Value value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkModify();
        this.value = value;
    }

    public void setValue(Value[] values) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkModify();
        this.values = values;
    }

    public void setValue(String s) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        if (s != null) {
            setValue(getSession().getValueFactory().createValue(s, getType()));
        } else {
            remove();
        }
    }

    public void setValue(String[] strings) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        if (strings != null) {
            Value[] v = new Value[strings.length];
            for (int i = 0; i < strings.length; i++) {
                if (strings[i] != null) {
                    v[i] = getSession().getValueFactory().createValue(strings[i], getType());
                } else {
                    v[i] = null;
                }
            }
            setValue(v);
        } else {
            remove();
        }
    }

    public void setValue(Binary[] binaries) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        if (binaries != null) {
            Value[] v = new Value[binaries.length];
            for (int i = 0; i < binaries.length; i++) {
                if (binaries[i] != null) {
                    v[i] = getSession().getValueFactory().createValue(binaries[i]);
                } else {
                    v[i] = null;
                }
            }
            setValue(v);
        } else {
            remove();
        }
    }

    public void setValue(InputStream inputStream) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        if (inputStream != null) {
            Binary b = null;
            try {
                b = new BinaryImpl(inputStream);
                setValue(getSession().getValueFactory().createValue(b));
            } catch (IOException e) {
                throw new RepositoryException(e);
            } finally {
                if (b != null) {
                    b.dispose();
                }
            }
        } else {
            remove();
        }
    }

    public void setValue(long l) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        setValue(getSession().getValueFactory().createValue(l));
    }

    public void setValue(double v) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        setValue(getSession().getValueFactory().createValue(v));
    }

    public void setValue(Calendar calendar) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        if (calendar != null) {
            setValue(getSession().getValueFactory().createValue(calendar));
        } else {
            remove();
        }
    }

    public void setValue(boolean b) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        setValue(getSession().getValueFactory().createValue(b));
    }

    public void setValue(Node node) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    public Value getValue() throws ValueFormatException, RepositoryException {
        checkRead();
        if (isMultiple()) {
            throw new ValueFormatException(getName() + " is a multi-valued property,"
                    + " so it's values can only be retrieved as an array");
        }
        return value;
    }

    public void setValue(BigDecimal value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        if (value != null) {
            setValue(getSession().getValueFactory().createValue(value));
        } else {
            remove();
        }
    }

    public Value[] getValues() throws ValueFormatException, RepositoryException {
        checkRead();
        if (!isMultiple()) {
            throw new ValueFormatException(getName() + " is a single-valued property,"
                    + " so it's value can not be retrieved as an array");
        }
        return values;
    }

    public String getString() throws ValueFormatException, RepositoryException {
        checkRead();
        if (value != null) {
            return value.getString();
        }
        return null;
    }

    public InputStream getStream() throws ValueFormatException, RepositoryException {
        checkRead();
        if (value != null) {
            final Binary binary = value.getBinary();
            return new AutoCloseInputStream(binary.getStream()) {
                @Override
                public void close() throws IOException {
                    super.close();
                    binary.dispose();
                }
            };
        }
        return null;
    }

    public long getLong() throws ValueFormatException, RepositoryException {
        checkRead();
        if (value != null) {
            return value.getLong();
        }
        return 0;
    }

    public double getDouble() throws ValueFormatException, RepositoryException {
        checkRead();
        if (value != null) {
            return value.getDouble();
        }
        return 0;
    }

    public Calendar getDate() throws ValueFormatException, RepositoryException {
        checkRead();
        if (value != null) {
            return value.getDate();
        }
        return null;
    }

    public boolean getBoolean() throws ValueFormatException, RepositoryException {
        checkRead();
        return value != null && value.getBoolean();
    }

    public Node getNode() throws ValueFormatException, RepositoryException {
        return null;
    }

    public long getLength() throws ValueFormatException, RepositoryException {
        return getLength(getValue());
    }

    protected long getLength(Value value) throws ValueFormatException, RepositoryException {
        return PropertyType.BINARY == value.getType() ? value.getBinary().getSize() : value.getString().length();
    }

    public long[] getLengths() throws ValueFormatException, RepositoryException {
        long[] lengths = new long[getValues().length];
        for (int i = 0; i < values.length; i++) {
            lengths[i] = getLength(values[i]);
        }
        return lengths;
    }

    public PropertyDefinition getDefinition() throws RepositoryException {
        return node.getPropertyDefinition(getName());
    }

    public int getType() throws RepositoryException {
        return getSafePropertyType();
    }

    public void setValue(Binary value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        if (value != null) {
            if (getBinary() != null) {
                getBinary().dispose();
            }
            setValue(getSession().getValueFactory().createValue(value));
        } else {
            remove();
        }
    }

    public Binary getBinary() throws ValueFormatException, RepositoryException {
        return value.getBinary();
    }

    public BigDecimal getDecimal() throws ValueFormatException, RepositoryException {
        return value.getDecimal();
    }

    public Property getProperty() throws ItemNotFoundException, ValueFormatException, RepositoryException {
        return this;
    }

    public boolean isMultiple() throws RepositoryException {
        if (values != null) {
            return true;
        }
        return false;
    }

    public String getPath() throws RepositoryException {
        return getParent().getPath() + "/" + name;
    }

    public String getName() throws RepositoryException {
        return name.toString();
    }

    public Node getParent() throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        return node;
    }

    @Override
    public void remove() throws VersionException, LockException, ConstraintViolationException, RepositoryException {
        ((ExternalNodeImpl) getParent()).removeProperty(getName());
    }

    private int getSafePropertyType() throws RepositoryException {
        int type = PropertyType.UNDEFINED;
        if (value != null) {
            type = value.getType();
        } else if (values != null) {
            type = values[0].getType();
        }
        if (type == PropertyType.UNDEFINED) {
            throw new ValueFormatException("No value associated with this property or type is UNDEFINED");
        }
        return type;
    }
}
