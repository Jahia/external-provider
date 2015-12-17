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
 *     This program is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU General Public License
 *     as published by the Free Software Foundation; either version 2
 *     of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
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
package org.jahia.modules.external.admin.mount;

import org.apache.commons.lang.StringUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.decorator.JCRMountPointNode;

import javax.jcr.RepositoryException;
import java.io.Serializable;
import java.lang.String;

/**
 * Created by kevan on 21/11/14.
 */
public abstract class AbstractMountPointFactory implements Serializable{
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
                suffixedName.substring(0, suffixedName.length() - JCRMountPointNode.MOUNT_SUFFIX.length()) : suffixedName;
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
