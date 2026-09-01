---
external-provider: patch
---

Restricted the root path of a VFS mount point to the local file system.

A VFS mount point whose root path names any other scheme stops working. To check whether you are affected, read the root path of each VFS mount point: one that starts with a scheme other than `file://` is affected, and a plain path is not. To keep such a mount point working, list the schemes its root may name in the `vfsMountPoint.allowedSchemes` property of the `org.jahia.modules.external.vfs` configuration, as a comma-separated list — for example `file,sftp`. The local file system alone is the default. The property reaches the mount points already mounted, so a mount point serves again as soon as its scheme is listed, and stops when the scheme is taken off the list, without restarting the instance.

The mount point operations of the GraphQL administration API are granted by the `graphqlAdminMutation` and `graphqlAdminQuery` permissions. A permission set for one of these operations in the `org.jahia.modules.graphql.provider` configuration is not read. If you had set one of the `permission.Gql*MountPoint*` keys there, grant `graphqlAdminMutation` or `graphqlAdminQuery` to the roles that need these operations instead.
