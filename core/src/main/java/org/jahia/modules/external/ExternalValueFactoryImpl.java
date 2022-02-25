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
