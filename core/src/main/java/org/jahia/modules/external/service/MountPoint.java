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
 *     Copyright (C) 2002-2023 Jahia Solutions Group. All rights reserved.
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
package org.jahia.modules.external.service;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.stream.Streams;
import org.jahia.api.Constants;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRPropertyWrapper;
import org.jahia.services.content.JCRValueWrapper;
import org.jahia.services.content.decorator.JCRMountPointNode;
import org.jahia.services.content.decorator.JCRMountPointNode.*;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.jahia.settings.SettingsBean;
import org.jahia.utils.Patterns;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.*;
import javax.jcr.nodetype.PropertyDefinition;
import java.util.*;
import java.util.stream.Collectors;

public class MountPoint {

    private static final Logger logger = LoggerFactory.getLogger(MountPoint.class);

    private String pathOrId = null;

    private String name;

    private String uuid;

    private String mountPointRefPath;

    private MountStatus mountStatus;

    private String primaryNodeType = Constants.JAHIANT_MOUNTPOINT;

    private Map<String, String> mountProperties;

    public MountPoint(String name, String mountPointRefPath) {
        this.name = name;
        this.mountPointRefPath = mountPointRefPath;
    }

    public MountPoint(JCRMountPointNode node) {
        this.name = StringUtils.removeEnd(node.getName(), JCRMountPointNode.MOUNT_SUFFIX);
        this.mountPointRefPath = node.getTargetMountPointPath();
        this.pathOrId = node.getPath();

        // check the root node update mount point status
        try { node.getSession().getNode(pathOrId);
        } catch (RepositoryException ignore) {}

        try {
            this.uuid = node.getIdentifier();
            this.mountStatus = node.getMountStatus();
            this.primaryNodeType = node.getPrimaryNodeType().getName();
            this.mountProperties = extractProperties(node, primaryNodeType);
        } catch (RepositoryException e) {
            logger.warn("Encountered error when extracting properties from node {}: {}", pathOrId, e.getMessage());
        }
    }

    private Map<String, String> extractProperties(JCRMountPointNode node, String nodeType) {
        Map<String, String> props = new LinkedHashMap<>();
        try {
            // collect protected properties
            JCRValueWrapper[] protectedProperties = node.hasProperty("protectedProperties") ?
                    node.getProperty("protectedProperties").getValues() : null;
            Set<String> protectedProps = new HashSet<>();
            if(protectedProperties != null) {
                protectedProps = Streams.stream(Arrays.stream(protectedProperties))
                        .map(Value::getString)
                        .collect(Collectors.toSet());
            }

            for (PropertyDefinition def : NodeTypeRegistry.getInstance().getNodeType(nodeType).getDeclaredPropertyDefinitions()) {
                String propName = def.getName();
                if (node.hasProperty(propName) & !protectedProps.contains(propName)) {
                    JCRPropertyWrapper jcrProp = node.getProperty(propName);
                    if (!((ExtendedPropertyDefinition) jcrProp.getDefinition()).isHidden()) {
                        String propValue = jcrProp.isMultiple() ?
                                Streams.stream(Arrays.stream(jcrProp.getValues())).map(Value::getString).collect(Collectors.joining("-")) :
                                jcrProp.getValue().getString();
                        props.put(propName, propValue);
                    }
                }
            }
        } catch (RepositoryException e) {
            logger.warn("Unable to extract properties for node {}: {}", node.getPath(), e.getMessage());
        }
        return props;
    }

    public String getPathOrId() {
        return this.pathOrId;
    }

    public void setPathOrId(String pathOrId) {
        this.pathOrId = pathOrId;
    }

    public String getUuid() {
        return this.uuid;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public String getMountStatus() {
        return this.mountStatus.toString();
    }

    public void setMountPointRefPath(String mountPointRefPath) {
        this.mountPointRefPath = mountPointRefPath;
    }

    public String getMountPointRefPath() {
        return this.mountPointRefPath;
    }

    public String getMountNodeType() {
        return this.primaryNodeType;
    }

    public static String getSuffix() {
        return JCRMountPointNode.MOUNT_SUFFIX;
    }

    public void setProperties(JCRMountPointNode mountNode) throws RepositoryException {
    }

    public String getProperty(String propName) {
        return this.mountProperties.get(propName);
    }

    public Map<String, String> getProperties() {
        return this.mountProperties;
    }

    public String getAvailableNodeName(Node dest) {
        int i = 1;

        String jcrName = JCRContentUtils.escapeLocalNodeName(getName());
        String basename = jcrName;
        int dot = basename.lastIndexOf('.');
        String ext = "";
        if (dot > 0) {
            ext = basename.substring(dot);
            basename = basename.substring(0, dot);
        }
        int und = basename.lastIndexOf('-');
        if (und > -1 && Patterns.NUMBERS.matcher(basename.substring(und + 1)).matches()) {
            basename = basename.substring(0, und);
        }

        do {
            try {
                dest.getNode(jcrName + getSuffix());
                String newSuffix = "-" + (i++) + ext;
                jcrName = basename + newSuffix;
                //name has a sizelimit of 32 chars
                int maxNameSize = SettingsBean.getInstance().getMaxNameSize();
                if (jcrName.length() > maxNameSize) {
                    jcrName = basename.substring(0, (basename.length() <= maxNameSize ? basename.length() : maxNameSize) - newSuffix.length()) + newSuffix;
                }
            } catch (RepositoryException e) {
                break;
            }

        } while (true);

        return jcrName + getSuffix();
    }

}
