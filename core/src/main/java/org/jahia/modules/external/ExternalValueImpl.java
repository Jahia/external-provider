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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.util.ISO8601;

import javax.jcr.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.Calendar;

/**
 * Implementation of the {@link javax.jcr.Value} for the {@link org.jahia.modules.external.ExternalData}.
 * User: loom
 * Date: Aug 12, 2010
 * Time: 3:04:33 PM
 */
public class ExternalValueImpl implements Value {

    private Object value;
    private int type;

    public ExternalValueImpl(String value) {
        this(value, PropertyType.STRING);
    }

    public ExternalValueImpl(Binary value) {
        this(value, PropertyType.BINARY);
    }

    public ExternalValueImpl(long value) {
        this(value, PropertyType.LONG);
    }

    public ExternalValueImpl(double value) {
        this(value, PropertyType.DOUBLE);
    }

    public ExternalValueImpl(BigDecimal value) {
        this(value, PropertyType.DECIMAL);
    }

    public ExternalValueImpl(Calendar value) {
        this(value, PropertyType.DATE);
    }

    public ExternalValueImpl(boolean value) {
        this(value, PropertyType.BOOLEAN);
    }

    public ExternalValueImpl(Node value, boolean weakReference) throws RepositoryException {
        if (weakReference) {
            this.value = value.getIdentifier();
            this.type = PropertyType.WEAKREFERENCE;
        } else {
            this.value = value.getIdentifier();
            this.type = PropertyType.REFERENCE;
        }
    }

    public ExternalValueImpl(Object value, int type) {
        this.value = value;
        this.type = type;
    }

    public String getString() throws ValueFormatException, IllegalStateException, RepositoryException {
        if (value instanceof Calendar) {
            return ISO8601.format((Calendar) value);
        } else if (value instanceof Binary) {
            try (InputStream stream = ((Binary) value).getStream()) {
                return StringUtils.join(IOUtils.readLines(stream), '\n');
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        return value.toString();
    }

    public InputStream getStream() throws RepositoryException {
        if (value instanceof Binary) {
            return ((Binary) value).getStream();
        }
        throw new ValueFormatException();
    }

    public Binary getBinary() throws RepositoryException {
        if (value instanceof Binary) {
            return (Binary) value;
        }
        return new ExternalBinaryImpl(new ByteArrayInputStream(getString().getBytes(Charset.forName("UTF-8"))));
    }

    public long getLong() throws ValueFormatException, RepositoryException {
        if (value instanceof Long) {
            return ((Long) value);
        }
        try {
            return Long.parseLong(getString());
        } catch (NumberFormatException e) {
            throw new ValueFormatException(e);
        }
    }

    public double getDouble() throws ValueFormatException, RepositoryException {
        if (value instanceof Double) {
            return ((Double) value);
        }
        try {
            return Double.parseDouble(getString());
        } catch (NumberFormatException e) {
            throw new ValueFormatException(e);
        }
    }

    public BigDecimal getDecimal() throws ValueFormatException, RepositoryException {
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value);
        }
        return BigDecimal.valueOf(getDouble());
    }

    public Calendar getDate() throws ValueFormatException, RepositoryException {
        if (value instanceof Calendar) {
            return ((Calendar) value);
        }
        try {
            return ISO8601.parse(getString());
        } catch (RuntimeException e) {
            throw new ValueFormatException(e);
        }
    }

    public boolean getBoolean() throws ValueFormatException, RepositoryException {
        if (value instanceof Boolean) {
            return ((Boolean) value);
        }
        return Boolean.valueOf(getString());
    }

    public int getType() {
        return type;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && this.getClass() == obj.getClass()) {
            return value.equals(((ExternalValueImpl) obj).value);
        } else {
            return obj != null && obj instanceof Value && obj.equals(value);
        }
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
