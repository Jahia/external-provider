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
