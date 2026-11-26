# external-provider Changelog

## 4.12.0

Version 4.11.0 was never published, so this section also lists its changes.

### New Features

* Changed Jahia External Provider Modules so the Studio works without Jahia External Provider VFS installed.

### Bug Fixes

* Fixed lookups by identifier of external content that failed on other cluster nodes after a rename or a move.

  On a cluster that runs 4.10.0, such a lookup failed for up to 8 hours. The `org.jahia.modules.external.cfg` file that 4.10.0 added is no longer read.

* Hardened the mount points settings screen so that mount point names and paths are displayed as they were typed.

* Restricted the root path of a VFS mount point to the local file system.

  A VFS mount point whose root path names any other scheme stops working. To check whether you are affected, read the root path of each VFS mount point: one that starts with a scheme other than `file://` is affected, and a plain path is not. To keep such a mount point working, list the schemes its root may name in the `vfsMountPoint.allowedSchemes` property of the `org.jahia.modules.external.vfs` configuration, as a comma-separated list — for example `file,ftp`. The local file system alone is the default. The property reaches the mount points already mounted, so a mount point serves again as soon as its scheme is listed, and stops when the scheme is taken off the list, without restarting the instance.

  The root path of a VFS mount point names a folder. A mount point whose root path names a single file stops working, and one is refused when a mount point is created or modified. An `http` or `https` URL names a single file, so a mount point with such a root stops working even when its scheme is listed. A root path naming a folder that does not exist yet is still accepted, and the mount point serves as soon as the folder is there.

* Hardened the external providers settings screen so that mount point paths with certain special characters are displayed correctly.

* Escape the mount point name before using it in the JCR-SQL2 lookup query

* **external-provider-ui**: Fixed an issue where upgrading or restarting the module could freeze the instance by triggering unnecessary module refreshes. Refreshes now happen only when actually needed. Fixes Jahia/jahia-private#5156.

* Added maven.jahia.com to the Maven repositories external-provider uses to download its dependencies.

* Hardened VFS mount points so they serve only the files within the mounted folder.
