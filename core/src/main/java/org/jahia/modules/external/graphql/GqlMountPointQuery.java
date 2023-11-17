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
import org.jahia.modules.external.service.MountPoint;
import org.jahia.modules.external.service.MountPointService;
import org.jahia.modules.graphql.provider.dxm.osgi.annotations.GraphQLOsgiService;

import javax.inject.Inject;
import javax.jcr.RepositoryException;
import java.util.List;
import java.util.stream.Collectors;

@GraphQLName("GqlMountPointQuery")
@GraphQLDescription("Mount point queries object")
public class GqlMountPointQuery {

    @Inject
    @GraphQLOsgiService
    private MountPointService mountPointService;

    @GraphQLField
    @GraphQLDescription("Get list of mount points, or empty list if no mounts exist")
    public List<GqlMountPoint> getMountPoints() throws RepositoryException {
        return mountPointService.getMountPoints().stream()
                .map(GqlMountPoint::new)
                .collect(Collectors.toList());
    }

    @GraphQLField
    @GraphQLDescription("Get mount point with given name, or null if it doesn't exists")
    public GqlMountPoint getMountPoint(
            @GraphQLName("name") @GraphQLDescription("Name for the mount point") @GraphQLNonNull String name
    ) throws RepositoryException {
        MountPoint mp = mountPointService.getMountPoint(name);
        return mp != null ? new GqlMountPoint(mp) : null;
    }

}
