---
external-provider: patch
---

Restricted the root path of a VFS mount point to the local file system. A mount point whose root names another scheme stops working; to keep it, add that scheme to `vfsMountPoint.allowedSchemes` in the `org.jahia.modules.external.vfs` configuration. A change to that set reaches the mount points already mounted, without a restart: adding a scheme puts such a mount point back to work, and removing one stops it. The mount point mutations under `admin.jahia.mountPoint` now declare the permission they require, so a `permission.GqlMountPointMutation.*` key set in `org.jahia.modules.graphql.provider.cfg` no longer applies to them.
