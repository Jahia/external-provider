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
package org.jahia.modules.external.vfs.graphql;

import graphql.annotations.annotationTypes.*;
import org.apache.commons.lang3.StringUtils;
import org.jahia.modules.external.graphql.GqlMountPointMutation;
import org.jahia.modules.external.service.MountPointService;
import org.jahia.modules.external.vfs.service.VfsMountPoint;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.osgi.BundleUtils;

import javax.jcr.RepositoryException;

@GraphQLTypeExtension(GqlMountPointMutation.class)
public class GqlVfsMountPointMutation {

    private final MountPointService mountPointService;

    public GqlVfsMountPointMutation(GqlMountPointMutation m) {
        // Unable to inject OSGI service; get through utils
        this.mountPointService = BundleUtils.getOsgiService(MountPointService.class, null);
    }

    @GraphQLField
    @GraphQLDescription("Create a mounted VFS mount point node in /mounts")
    public String addVfs(
            @GraphQLName("name") @GraphQLDescription("Name for the mount point") @GraphQLNonNull String name,
            @GraphQLName("mountPointRefPath") @GraphQLDescription("Target local mount point") String mountPointRefPath,
            @GraphQLName("rootPath") @GraphQLDescription("VFS root mount point") String rootPath
    ) {
        if (StringUtils.isBlank(name)) {
            throw new DataFetchingException("Specified name must not be blank");
        }

        VfsMountPoint mountPoint = new VfsMountPoint(name, mountPointRefPath, rootPath);
        if (!mountPoint.isValidRoot()) {
            throw new DataFetchingException("Unable to validate root path " + rootPath
                    + " or root path is invalid. Please check logs for details");
        }

        try {
            return mountPointService.create(mountPoint);
        } catch (RepositoryException e) {
            throw new DataFetchingException(e.getMessage());
        }
    }

    @GraphQLField
    @GraphQLDescription("Modify an existing mount point node")
    public boolean modifyVfs(
            @GraphQLName("pathOrId") @GraphQLDescription("Mount point path or ID to modify") @GraphQLNonNull String pathOrId,
            @GraphQLName("name") @GraphQLDescription("Rename existing mount point") String name,
            @GraphQLName("mountPointRefPath") @GraphQLDescription("Change target local mount point, or set to empty string to remove") String mountPointRefPath,
            @GraphQLName("rootPath") @GraphQLDescription("VFS root mount point") String rootPath
    ) {
        if (name != null && StringUtils.isBlank(name)) {
            throw new DataFetchingException("Specified name must not be blank");
        }

        VfsMountPoint mountPoint = new VfsMountPoint(name, mountPointRefPath, rootPath);
        mountPoint.setPathOrId(pathOrId);
        if (mountPoint.getRootPath() != null && !mountPoint.isValidRoot()) {
            throw new DataFetchingException("Unable to validate root path " + rootPath
                    + " or root path is invalid. Please check logs for details");
        }

        try {
            return mountPointService.modify(mountPoint);
        } catch (RepositoryException e) {
            throw new DataFetchingException(e.getMessage());
        }
    }

}
