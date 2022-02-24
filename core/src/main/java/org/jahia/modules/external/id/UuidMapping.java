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
package org.jahia.modules.external.id;

import javax.persistence.*;

import org.hibernate.annotations.Index;

/**
 * Map that link valid uuid and (@link org.jahia.services.content.impl.external.ExternalData} id
 */
@Entity
@Table(name = "jahia_external_mapping")
public class UuidMapping {

    private String internalUuid;
    private String providerKey;
    private String externalId;

    public UuidMapping() {
    }

    @Id
    @Column(length = 36, nullable = false)
    public String getInternalUuid() {
        return internalUuid;
    }

    public void setInternalUuid(String internalUuid) {
        this.internalUuid = internalUuid;
    }

    @Column(nullable = false)
    @Index(name = "jahia_external_mapping_index1")
    public String getProviderKey() {
        return providerKey;
    }

    public void setProviderKey(String providerKey) {
        this.providerKey = providerKey;
    }

    @Lob
    @Column(nullable = false)
    public String getExternalId() {
        return externalId;
    }

    @Column()
    @Index(name = "jahia_external_mapping_index1")
    public int getExternalIdHash() {
        return externalId != null ? externalId.hashCode() : 0;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public void setExternalIdHash(int externalIdHash) {
        // do nothing
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        UuidMapping that = (UuidMapping) o;

        if (externalId != null ? !externalId.equals(that.externalId) : that.externalId != null) return false;
        if (providerKey != null ? !providerKey.equals(that.providerKey) : that.providerKey != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = providerKey != null ? providerKey.hashCode() : 0;
        result = 31 * result + (externalId != null ? externalId.hashCode() : 0);
        return result;
    }
}
