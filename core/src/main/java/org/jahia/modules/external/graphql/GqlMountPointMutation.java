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
package org.jahia.modules.external.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import org.apache.commons.lang3.StringUtils;
import org.jahia.modules.external.service.MountPoint;
import org.jahia.modules.external.service.MountPointService;
import org.jahia.modules.graphql.provider.dxm.DataFetchingException;
import org.jahia.modules.graphql.provider.dxm.osgi.annotations.GraphQLOsgiService;

import javax.inject.Inject;
import javax.jcr.RepositoryException;

@GraphQLName("GqlMountPointMutation")
@GraphQLDescription("Mount point mutation object")
public class GqlMountPointMutation {

    @Inject
    @GraphQLOsgiService
    private MountPointService mountPointService;

    @GraphQLField
    @GraphQLDescription("Create a mounted mount point node in /mounts")
    public String add(
            @GraphQLName("name") @GraphQLDescription("Mount point name") @GraphQLNonNull String name,
            @GraphQLName("mountPointRefPath") @GraphQLDescription("Target local mount point") String mountPointRefPath
    ) throws RepositoryException {
        MountPoint mountPoint = new MountPoint(name, mountPointRefPath);
        return mountPointService.create(mountPoint);
    }

    @GraphQLField
    @GraphQLDescription("Modify an existing mount point node")
    public boolean modify(
            @GraphQLName("pathOrId") @GraphQLDescription("Mount point path or ID to modify") @GraphQLNonNull String pathOrId,
            @GraphQLName("name") @GraphQLDescription("Rename existing mount point") String name,
            @GraphQLName("mountPointRefPath") @GraphQLDescription("Change local mount point, or set to empty string to remove") String mountPointRefPath
    ) throws RepositoryException {
        if (name != null && StringUtils.isBlank(name)) {
            throw new DataFetchingException("Specified name must not be blank");
        }
        MountPoint mountPoint = new MountPoint(name, mountPointRefPath);
        mountPoint.setPathOrId(pathOrId);
        return mountPointService.modify(mountPoint);
    }


    @GraphQLField
    @GraphQLDescription("Mount an existing mount point")
    public boolean mount(
            @GraphQLName("pathOrId") @GraphQLDescription("Mount point path or ID to mount") @GraphQLNonNull String pathOrId
    ) throws RepositoryException {
        return mountPointService.mount(pathOrId);
    }

    @GraphQLField
    @GraphQLDescription("Unmount an existing mount point")
    public boolean unmount(
            @GraphQLName("pathOrId") @GraphQLDescription("Mount point path or ID to unmount") @GraphQLNonNull String pathOrId
    ) throws RepositoryException {
        return mountPointService.unmount(pathOrId);
    }

}
