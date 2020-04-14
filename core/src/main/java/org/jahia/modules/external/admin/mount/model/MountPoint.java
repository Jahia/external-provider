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
package org.jahia.modules.external.admin.mount.model;

import org.apache.commons.lang.StringUtils;
import org.jahia.services.content.JCRPropertyWrapper;
import org.jahia.services.content.JCRValueWrapper;
import org.jahia.services.content.decorator.JCRMountPointNode;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.LocaleContextHolder;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.PropertyDefinition;
import java.io.Serializable;
import java.util.*;

/**
 * @author kevan
 */
public class MountPoint implements Serializable{
    private static final long serialVersionUID = 4618846382714016491L;
    private static Logger logger = LoggerFactory.getLogger(MountPoint.class);

    String name;
    String realName;
    String path;
    String displayStatusClass;
    JCRMountPointNode.MountStatus status;
    String identifier;
    String nodetype;
    Map<String,String> mountPointProperties;
    boolean showMountAction = false;
    boolean showUnmountAction = false;

    public MountPoint(JCRMountPointNode node) throws RepositoryException {
        this.realName = node.getName();
        this.name = StringUtils.removeEnd(node.getName(), JCRMountPointNode.MOUNT_SUFFIX);
        this.path = node.getTargetMountPointPath();

        // check the root node update mount point status
        try {
            node.getSession().getNode(path);
        } catch (Exception e) {
            // Do nothing
        }
        this.status = node.getMountStatus();
        this.identifier = node.getIdentifier();
        this.nodetype = node.getPrimaryNodeType().getName();
        // update mount point status

        switch (node.getMountStatus()){
            case unmounted:
                displayStatusClass = "";
                showMountAction = true;
                break;
            case mounted:
                showUnmountAction = true;
                displayStatusClass = "label-success";
                break;
            case error:
                displayStatusClass = "label-important";
                break;
            case waiting:
                displayStatusClass = "label-warning";
                break;
        }
        try {
            Locale locale = LocaleContextHolder.getLocale();
            mountPointProperties = new LinkedHashMap<>();
            JCRValueWrapper[] protectedProperties = node.hasProperty("protectedProperties") ? node.getProperty("protectedProperties").getValues() : null;
            List<String> protectedPropertiesNames = null;
            if(protectedProperties != null) {
                protectedPropertiesNames = new ArrayList<>();
                for (JCRValueWrapper jcrValueWrapper : protectedProperties) {
                    protectedPropertiesNames.add(jcrValueWrapper.getString());
                }
            }


            for (PropertyDefinition def : NodeTypeRegistry.getInstance().getNodeType(nodetype).getDeclaredPropertyDefinitions()) {
                if (node.hasProperty(def.getName())) {
                    if (protectedPropertiesNames != null && protectedPropertiesNames.contains(def.getName())) {
                        continue;
                    }
                    JCRPropertyWrapper mountPointProperty = node.getProperty(def.getName());
                    ExtendedPropertyDefinition extPropDef = (ExtendedPropertyDefinition) mountPointProperty.getDefinition();
                    if (!extPropDef.isHidden()) {
                        String key = extPropDef.getLabel(locale) + " (" + def.getName() + ")";
                        if (mountPointProperty.isMultiple()) {
                            StringBuilder sb = new StringBuilder();
                            for (Value v : mountPointProperty.getValues()) {
                                if (sb.length() > 0) {
                                    sb.append(" - ");
                                }
                                sb.append(v.getString());
                            }
                            mountPointProperties.put(key, sb.toString());
                        } else {
                            mountPointProperties.put(key, mountPointProperty.getValue().getString());
                        }
                    }
                }
            }
        } catch (NoSuchNodeTypeException e) {
            logger.warn("unable to get declared properties for " + nodetype);
        }
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public JCRMountPointNode.MountStatus getStatus() {
        return status;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setStatus(JCRMountPointNode.MountStatus status) {
        this.status = status;
    }

    public String getDisplayStatusClass() {
        return displayStatusClass;
    }

    public void setDisplayStatusClass(String displayStatusClass) {
        this.displayStatusClass = displayStatusClass;
    }

    public boolean isShowMountAction() {
        return showMountAction;
    }

    public void setShowMountAction(boolean showMountAction) {
        this.showMountAction = showMountAction;
    }

    public boolean isShowUnmountAction() {
        return showUnmountAction;
    }

    public void setShowUnmountAction(boolean showUnmountAction) {
        this.showUnmountAction = showUnmountAction;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getNodetype() {
        return nodetype;
    }

    public void setNodetype(String nodetype) {
        this.nodetype = nodetype;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public Map<String,String> getRemoteProperties() {
        return mountPointProperties;
    }
}
