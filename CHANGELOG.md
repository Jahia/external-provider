# external-provider Changelog

## 0.0.1

* Hardened the mount points settings screen so that mount point names and paths are displayed as they were typed.

* **external-provider-ui**: Prevented startup deadlock by skipping manual bundle refreshes during the initial OSGi start-level transition. (#226)

* Restricted the root path of a VFS mount point to the local file system.

  A VFS mount point whose root path names any other scheme stops working. To check whether you are affected, read the root path of each VFS mount point: one that starts with a scheme other than `file://` is affected, and a plain path is not. To keep such a mount point working, list the schemes its root may name in the `vfsMountPoint.allowedSchemes` property of the `org.jahia.modules.external.vfs` configuration, as a comma-separated list — for example `file,sftp`. The local file system alone is the default. Keep `file` in the list: the property also governs the source folder of a module deployed in source-folders mode, which is read from the local file system, so a list without `file` stops every such module as well as the mount points. The property reaches the mount points already mounted, so a mount point serves again as soon as its scheme is listed, and stops when the scheme is taken off the list, without restarting the instance.

  The root path of a VFS mount point names a folder. A mount point whose root path names a single file stops working, and one is refused when a mount point is created or modified. A root path naming a folder that does not exist yet is still accepted, and the mount point serves as soon as the folder is there.

* Escape the mount point name before using it in the JCR-SQL2 lookup query

* **external-provider-ui**: Fixed an issue where upgrading or restarting the module could freeze the instance by triggering unnecessary module refreshes. Refreshes now happen only when actually needed. Fixes Jahia/jahia-private#5156.

* Hardened VFS mount points so they serve only the files within the mounted folder.
