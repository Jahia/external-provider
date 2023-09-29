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
package org.jahia.modules.external.admin.mount;

import org.apache.commons.lang.StringUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.decorator.JCRMountPointNode;

import javax.jcr.RepositoryException;
import java.io.Serializable;

/**
 * Created by kevan on 21/11/14.
 */
public abstract class AbstractMountPointFactory implements Serializable {
    private static final long serialVersionUID = 6745956005105508413L;

    private String inEditMountPointNodePath;

    protected AbstractMountPointFactory() {
    }

    public void populate(JCRNodeWrapper nodeWrapper) throws RepositoryException {
        this.inEditMountPointNodePath = nodeWrapper.getPath();
    }

    public boolean isEdit() {
        return StringUtils.isNotEmpty(inEditMountPointNodePath);
    }

    public String getName(String suffixedName) {
        return suffixedName.endsWith(JCRMountPointNode.MOUNT_SUFFIX) ?
                suffixedName.substring(0, suffixedName.length() - JCRMountPointNode.MOUNT_SUFFIX.length()) :
                suffixedName;
    }

    public abstract String getName();

    public abstract String getLocalPath();

    public abstract String getMountNodeType();

    public abstract void setProperties(JCRNodeWrapper mountNode) throws RepositoryException;

    public String getInEditMountPointNodePath() {
        return inEditMountPointNodePath;
    }

    public void setInEditMountPointNodePath(String inEditMountPointNodePath) {
        this.inEditMountPointNodePath = inEditMountPointNodePath;
    }
}
