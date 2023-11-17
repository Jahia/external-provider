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

import java.util.List;
import java.util.stream.Collectors;

@GraphQLName("GqlMountPoint")
@GraphQLDescription("Mount point object")
public class GqlMountPoint {

    private final MountPoint mountPoint;

    public GqlMountPoint(MountPoint mountPoint) {
        this.mountPoint = mountPoint;
    }

    @GraphQLField
    @GraphQLDescription("Mount point node uuid")
    public String getUuid() {
        return mountPoint.getUuid();
    }
    @GraphQLField
    @GraphQLDescription("Mount point name")
    public String getMountName() {
        return mountPoint.getName();
    }

    @GraphQLField
    @GraphQLDescription("Mount status")
    public String getMountStatus() {
        return mountPoint.getMountStatus();
    }

    @GraphQLField
    @GraphQLDescription("Mount node type")
    public String getNodeType() {
        return mountPoint.getMountNodeType();
    }

    @GraphQLField
    @GraphQLDescription("Mount point local reference path")
    public String getMountPointRefPath() {
        return mountPoint.getMountPointRefPath();
    }

    @GraphQLField
    @GraphQLDescription("Mount point property")
    public String getProperty(
            @GraphQLName("name") @GraphQLDescription("The name of the property") @GraphQLNonNull String propName
    ) {
        return mountPoint.getProperty(propName);
    }

    @GraphQLField
    @GraphQLDescription("Mount point additional properties")
    public List<GqlMountPointProperty> getProperties() {
        return mountPoint.getProperties().entrySet().stream()
                .map(entry -> new GqlMountPointProperty(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

}
