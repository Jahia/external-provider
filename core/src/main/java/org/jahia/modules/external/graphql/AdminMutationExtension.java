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
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import org.jahia.modules.graphql.provider.dxm.admin.GqlAdminMutation;
import org.jahia.modules.graphql.provider.dxm.security.GraphQLRequiresPermission;

@Deprecated(since = "4.8.0", forRemoval = true)
@GraphQLTypeExtension(GqlAdminMutation.class)
@GraphQLDescription("Mutation extensions for mount point")
public class AdminMutationExtension {

    @GraphQLField
    @GraphQLName("mountPoint")
    @GraphQLDescription("Mount point mutation extension API")
    @GraphQLRequiresPermission(value = "graphqlAdminMutation")
    public static GqlMountPointMutation mountPoint() {
        return new GqlMountPointMutation();
    }
}
