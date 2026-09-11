---
external-provider: patch
---

Restricted the root path of a VFS mount point to the local file system.

A VFS mount point whose root path names any other scheme stops working. To check whether you are affected, read the root path of each VFS mount point: one that starts with a scheme other than `file://` is affected, and a plain path is not. To keep such a mount point working, list the schemes its root may name in the `vfsMountPoint.allowedSchemes` property of the `org.jahia.modules.external.vfs` configuration, as a comma-separated list — for example `file,sftp`. The local file system alone is the default. The property reaches the mount points already mounted, so a mount point serves again as soon as its scheme is listed, and stops when the scheme is taken off the list, without restarting the instance.

The root path of a VFS mount point names a folder. A mount point whose root path names a single file stops working, and one is refused when a mount point is created or modified. A root path naming a folder that does not exist yet is still accepted, and the mount point serves as soon as the folder is there.
