---
external-provider: patch
---

Restricted the root path of a VFS mount point to the local file system. A mount point whose root names another scheme stops working after the upgrade; to keep it, add that scheme to `vfsMountPoint.allowedSchemes` in the `org.jahia.modules.external.vfs` configuration.
