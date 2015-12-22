/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2016 Jahia Solutions Group SA. All rights reserved.
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
