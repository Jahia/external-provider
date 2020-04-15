/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group SA. All rights reserved.
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

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.UUID;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;

import org.apache.jackrabbit.util.ISO8601;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the {@link javax.jcr.ValueFactory} for the {@link org.jahia.modules.external.ExternalData}.
 * User: loom
 * Date: Aug 12, 2010
 * Time: 3:03:58 PM
 *
 */
public class ExternalValueFactoryImpl implements ValueFactory {
    private static final Logger logger = LoggerFactory.getLogger(ExternalValueFactoryImpl.class);

    private ExternalSessionImpl session;

    public ExternalValueFactoryImpl(ExternalSessionImpl session) {
        this.session = session;
    }

    public ExternalValueImpl createValue(String value) {
        return new ExternalValueImpl(value);
    }

    public ExternalValueImpl createValue(String value, int type) throws ValueFormatException {
        if (type == PropertyType.UNDEFINED) {
            type = PropertyType.STRING;
        }

        switch (type) {
            case PropertyType.BINARY :
                throw new ValueFormatException("Not allowed to convert string ["+value+"] to binary value");
            case PropertyType.BOOLEAN :
                return createValue(Boolean.parseBoolean(value));
            case PropertyType.DATE :
                return createValue(ISO8601.parse(value));
            case PropertyType.DECIMAL :
                return createValue(new BigDecimal(value));
            case PropertyType.DOUBLE :
                return createValue(Double.parseDouble(value));
            case PropertyType.LONG :
                return createValue(Long.parseLong(value));
            case PropertyType.REFERENCE :
            case PropertyType.WEAKREFERENCE :
                try {
                    if (!session.getRepository().getDataSource().isSupportsUuid()) {
                        try {
                            UUID.fromString(value);
                        } catch (IllegalArgumentException e) {
                            String internalId = session.getRepository().getStoreProvider().getOrCreateInternalIdentifier(value);
                            return new ExternalValueImpl(internalId, type);
                        }
                    }
                } catch (RepositoryException e) {
                    logger.error(e.getMessage(), e);
                }
                return new ExternalValueImpl(value, type);
            case PropertyType.STRING :
            case PropertyType.NAME :
            case PropertyType.PATH :
            case PropertyType.URI :
                return new ExternalValueImpl(value, type);
        }
        throw new ValueFormatException("Unsupported value type " + type);
    }

    public ExternalValueImpl createValue(long value) {
        return new ExternalValueImpl(value);
    }

    public ExternalValueImpl createValue(double value) {
        return new ExternalValueImpl(value);
    }

    public ExternalValueImpl createValue(BigDecimal value) {
        return new ExternalValueImpl(value);
    }

    public ExternalValueImpl createValue(boolean value) {
        return new ExternalValueImpl(value);
    }

    public ExternalValueImpl createValue(Calendar value) {
        return new ExternalValueImpl(value);
    }

    public ExternalValueImpl createValue(InputStream value) {
        return new ExternalValueImpl(new ExternalBinaryImpl(value));
    }

    public ExternalValueImpl createValue(Binary value) {
        return new ExternalValueImpl(value);
    }

    public ExternalValueImpl createValue(Node value) throws RepositoryException {
        return new ExternalValueImpl(value, false);
    }

    public ExternalValueImpl createValue(Node value, boolean weak) throws RepositoryException {
        return new ExternalValueImpl(value, weak);
    }

    public Binary createBinary(InputStream stream) throws RepositoryException {
        return new ExternalBinaryImpl(stream);
    }

}
