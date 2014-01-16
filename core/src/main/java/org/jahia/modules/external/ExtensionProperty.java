/**
 * This file is part of Jahia, next-generation open source CMS:
 * Jahia's next-generation, open source CMS stems from a widely acknowledged vision
 * of enterprise application convergence - web, search, document, social and portal -
 * unified by the simplicity of web content management.
 *
 * For more information, please visit http://www.jahia.com.
 *
 * Copyright (C) 2002-2014 Jahia Solutions Group SA. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * As a special exception to the terms and conditions of version 2.0 of
 * the GPL (or any later version), you may redistribute this Program in connection
 * with Free/Libre and Open Source Software ("FLOSS") applications as described
 * in Jahia's FLOSS exception. You should have received a copy of the text
 * describing the FLOSS exception, and it is also available here:
 * http://www.jahia.com/license
 *
 * Commercial and Supported Versions of the program (dual licensing):
 * alternatively, commercial and supported versions of the program may be used
 * in accordance with the terms and conditions contained in a separate
 * written agreement between you and Jahia Solutions Group SA.
 *
 * If you are unsure which license is appropriate for your use,
 * please contact the sales department at sales@jahia.com.
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

public class ExtensionProperty extends ExtensionItem implements Property {
    private Property property;
    private Node parentNode;

    public ExtensionProperty(Property property, String path, ExternalSessionImpl session, Node parentNode) {
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

    @SuppressWarnings("deprecation")
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
            return getSession().getValueFactory().createValue(parentNode.getIdentifier());
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
        return property.getValue().getString();
    }

    @Override
    public InputStream getStream() throws ValueFormatException, RepositoryException {
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
        return property.getValue().getBinary();
    }

    @Override
    public long getLong() throws ValueFormatException, RepositoryException {
        return property.getValue().getLong();
    }

    @Override
    public double getDouble() throws ValueFormatException, RepositoryException {
        return property.getValue().getDouble();
    }

    @Override
    public BigDecimal getDecimal() throws ValueFormatException, RepositoryException {
        return property.getValue().getDecimal();
    }

    @Override
    public Calendar getDate() throws ValueFormatException, RepositoryException {
        return property.getValue().getDate();
    }

    @Override
    public boolean getBoolean() throws ValueFormatException, RepositoryException {
        return property.getValue().getBoolean();
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
