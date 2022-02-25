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

import org.apache.commons.lang.StringUtils;
import org.jahia.modules.external.acl.ExternalDataAcl;

import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.jcr.Binary;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

/**
 * External Data. Encapsulation of data for external provider
 */

public class ExternalData {
    private String id;
    private String tmpId;
    private final String path;
    private final String name;
    private final String type;
    private boolean isNew = false;
    private List<String> mixin;
    private final Map<String,String[]> properties;
    private Map<String,Map<String,String[]>> i18nProperties;
    private Map<String,Binary[]> binaryProperties;
    private Set<String> lazyProperties;
    private Set<String> lazyBinaryProperties;
    private Map<String, Set<String>> lazyI18nProperties;
    private ExternalDataAcl externalDataAcl;

    public ExternalData(String id, String path, String type, Map<String, String[]> properties) {
        this(id, path, type, properties, false);
    }

    public ExternalData(String id, String path, String type, Map<String, String[]> properties, boolean isNew) {
        this.id = id;
        this.path = path;
        this.name = StringUtils.substringAfterLast(path, "/");
        this.type = type;
        this.properties = properties;
        this.isNew = isNew;
        if (isNew) {
            this.tmpId = id;
        }
    }

    public String getId() {
        return id;
    }

    protected String getTmpId() {
        return tmpId;
    }

    public void setId(String id) throws RepositoryException {
        if (isNew) {
            this.id = id;
        } else {
            throw new UnsupportedRepositoryOperationException();
        }
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public boolean isNew() {
        return isNew;
    }

    protected void markSaved() {
        this.isNew = false;
        this.tmpId = null;
    }

    public Map<String,String[]> getProperties() {
        return properties;
    }

    public Map<String,Map<String,String[]>> getI18nProperties() {
        return i18nProperties;
    }

    public void setI18nProperties(Map<String, Map<String, String[]>> i18nProperties) {
        this.i18nProperties = i18nProperties;
    }

    public Map<String, Binary[]> getBinaryProperties() {
        return binaryProperties;
    }

    public void setBinaryProperties(Map<String, Binary[]> binaryProperties) {
        this.binaryProperties = binaryProperties;
    }

    public List<String> getMixin() {
        return mixin;
    }

    public void setMixin(List<String> mixin) {
        this.mixin = mixin;
    }

    public Set<String> getLazyProperties() {
        return lazyProperties;
    }

    public void setLazyProperties(Set<String> lazyProperties) {
        this.lazyProperties = lazyProperties;
    }

    public Set<String> getLazyBinaryProperties() {
        return lazyBinaryProperties;
    }

    public void setLazyBinaryProperties(Set<String> lazyBinaryProperties) {
        this.lazyBinaryProperties = lazyBinaryProperties;
    }

    public Map<String, Set<String>> getLazyI18nProperties() {
        return lazyI18nProperties;
    }

    public void setLazyI18nProperties(Map<String, Set<String>> lazyI18nProperties) {
        this.lazyI18nProperties = lazyI18nProperties;
    }

    public ExternalDataAcl getExternalDataAcl() {
        return externalDataAcl;
    }

    public void setExternalDataAcl(ExternalDataAcl externalDataAcl) {
        this.externalDataAcl = externalDataAcl;
    }
}
