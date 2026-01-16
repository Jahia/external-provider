/*
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2026 Jahia Solutions Group. All rights reserved.
 *
 *     This file is part of a Jahia's Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance with the terms
 *     contained in the Jahia Solutions Group Terms &amp; Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     For questions regarding licensing, support, production usage...
 *     please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *
 * ==========================================================================================
 */
package org.jahia.modules.external.id;

import java.io.Serializable;
import java.util.Objects;

// TODO find better name
public class CacheKey implements Serializable {
    private final String externalId;
    private final String providerKey;

    public CacheKey(String externalId, String providerKey) {
        this.externalId = externalId;
        this.providerKey = providerKey;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getProviderKey() {
        return providerKey;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        CacheKey cacheKey = (CacheKey) o;
        return Objects.equals(externalId, cacheKey.externalId) && Objects.equals(providerKey, cacheKey.providerKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(externalId, providerKey);
    }
}
