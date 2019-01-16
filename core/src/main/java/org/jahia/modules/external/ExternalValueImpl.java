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
