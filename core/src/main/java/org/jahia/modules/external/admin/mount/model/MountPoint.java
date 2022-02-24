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
