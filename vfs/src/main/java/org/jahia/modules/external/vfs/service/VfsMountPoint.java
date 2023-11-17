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
package org.jahia.modules.external.vfs.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.vfs2.VFS;
import org.jahia.modules.external.service.MountPoint;
import org.jahia.services.content.decorator.JCRMountPointNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;

/**
 * VFS Mount point class extension used for OSGI MountPointService operations
 */
public class VfsMountPoint extends MountPoint {

    private static final Logger logger = LoggerFactory.getLogger(VfsMountPoint.class);

    public static final String JAHIANT_VFS_MOUNTPOINT = "jnt:vfsMountPoint";
    public static final String J_ROOTPATH = "j:rootPath";

    private final String rootPath;

    public VfsMountPoint(String name, String mountPointRefPath, String rootPath) {
        super(name, mountPointRefPath);
        this.rootPath = rootPath;
    }

    @Override
    public String getMountNodeType() {
        return JAHIANT_VFS_MOUNTPOINT;
    }

    @Override
    public void setProperties(JCRMountPointNode mountNode) throws RepositoryException {
        super.setProperties(mountNode);
        if (StringUtils.isNotBlank(rootPath)) {
            mountNode.setProperty(J_ROOTPATH, rootPath);
        } else if (rootPath != null && StringUtils.isBlank(rootPath) && mountNode.hasProperty(J_ROOTPATH)) {
            mountNode.getProperty(J_ROOTPATH).remove();
        }
    }

    public boolean isValidRoot() {
        try {
            VFS.getManager().resolveFile(rootPath);
        } catch (Exception e) {
            logger.warn("Unable to resolve VFS root path '{}': {}", rootPath, e.getMessage());
            return false;
        }
        return true;
    }

    public final String getRootPath() {
        return this.rootPath;
    }
}
