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
package org.jahia.modules.external.admin;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DataSourceInfo implements Serializable {
    private String clazz;
    private boolean isSupportsLazy;
    private boolean isWriteable;
    private boolean isSearchable;
    private boolean isInitializable;
    private boolean isExtendable;
    private boolean isSupportsHierarchicalIdentifiers;
    private boolean isSupportsUuid;
    private String rootNodeType;
    private Set<String> supportedTypes;
    private Map<String, Boolean> supportedQueries;
    private List<String> overridableItems;
    private List<String> nonOverridableItems;
    private List<String> extendableTypes;

    public String getClazz() {
        return clazz;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    public boolean isSupportsLazy() {
        return isSupportsLazy;
    }

    public void setSupportsLazy(boolean isSupportsLazy) {
        this.isSupportsLazy = isSupportsLazy;
    }

    public boolean isWriteable() {
        return isWriteable;
    }

    public void setWriteable(boolean isWriteable) {
        this.isWriteable = isWriteable;
    }

    public boolean isSearchable() {
        return isSearchable;
    }

    public void setSearchable(boolean isSearchable) {
        this.isSearchable = isSearchable;
    }

    public boolean isInitializable() {
        return isInitializable;
    }

    public void setInitializable(boolean isInitializable) {
        this.isInitializable = isInitializable;
    }

    public boolean isExtendable() {
        return isExtendable;
    }

    public void setExtendable(boolean isExtendable) {
        this.isExtendable = isExtendable;
    }

    public boolean isSupportsHierarchicalIdentifiers() {
        return isSupportsHierarchicalIdentifiers;
    }

    public void setSupportsHierarchicalIdentifiers(boolean isSupportsHierarchicalIdentifiers) {
        this.isSupportsHierarchicalIdentifiers = isSupportsHierarchicalIdentifiers;
    }

    public boolean isSupportsUuid() {
        return isSupportsUuid;
    }

    public void setSupportsUuid(boolean isSupportsUuid) {
        this.isSupportsUuid = isSupportsUuid;
    }

    public String getRootNodeType() {
        return rootNodeType;
    }

    public void setRootNodeType(String rootNodeType) {
        this.rootNodeType = rootNodeType;
    }

    public Set<String> getSupportedTypes() {
        return supportedTypes;
    }

    public void setSupportedTypes(Set<String> supportedTypes) {
        this.supportedTypes = supportedTypes;
    }

    public Map<String, Boolean> getSupportedQueries() {
        return supportedQueries;
    }

    public void setSupportedQueries(Map<String, Boolean> supportedQueries) {
        this.supportedQueries = supportedQueries;
    }

    public List<String> getOverridableItems() {
        return overridableItems;
    }

    public void setOverridableItems(List<String> overridableItems) {
        this.overridableItems = overridableItems;
    }

    public List<String> getNonOverridableItems() {
        return nonOverridableItems;
    }

    public void setNonOverridableItems(List<String> nonOverridableItems) {
        this.nonOverridableItems = nonOverridableItems;
    }

    public List<String> getExtendableTypes() {
        return extendableTypes;
    }

    public void setExtendableTypes(List<String> extendableTypes) {
        this.extendableTypes = extendableTypes;
    }
}
