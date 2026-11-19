<a href="https://www.jahia.com/">
    <img src="https://www.jahia.com/modules/jahiacom-templates/images/jahia-3x.png" alt="Jahia logo" title="Jahia" align="right" height="60" />
</a>

External Provider
======================
The External Provider module provides an API allowing integration of external system as content providers.

## Upgrade impacts

An upgrade of External Provider can restart many modules that do not use External Provider, and it rebuilds the GraphQL schema. The cause is a package import in `graphql-dxm-provider`, not in External Provider. [graphql-core#629](https://github.com/Jahia/graphql-core/issues/629) gives the full analysis.

### Why other modules restart

When OSGi refreshes a bundle, it also restarts every bundle that imports a package from it, and every bundle that imports from those bundles. Before version 3.7.0, `graphql-dxm-provider` had a mandatory import of the `org.jahia.modules.external.modules.osgi` package from `external-provider-modules`. This import served only a development feature: the reload of the `graphql-extension.sdl` files of a module whose sources are on the disk of the server.

That import puts `graphql-dxm-provider` in the refresh chain of External Provider:

```
external-provider-vfs
  └ exports org.jahia.modules.external.vfs
       ← imported by external-provider-modules
            └ exports org.jahia.modules.external.modules.osgi
                 ← imported by graphql-dxm-provider
                      └ exports the GraphQL API
                           ← imported by about 14 modules, about 38 modules in total
```

An upgrade of External Provider refreshes `external-provider-vfs` or `external-provider-modules`. For example, `external-provider-ui` refreshes the bundles that import the `org.jahia.modules.external.admin.mount` packages, because those packages moved from `external-provider` to `external-provider-ui`.

### What you see

On a Jahia 8.2 instance, one refresh of `external-provider-vfs` 4.10.0 gives these results:

- About 38 modules stop and start again, for example `graphql-dxm-provider`, `jcontent`, `serverSettings`, `siteSettings` and `module-manager`.
- `graphql-dxm-provider` removes the GraphQL schema and builds it again.
- A screen of the administration that is open during the refresh can fail, for example with `java.lang.ClassNotFoundException: org.jahia.modules.modulemanager.flow.ModuleFile`.

### How to limit the impact

- Install `graphql-dxm-provider` 3.7.0 or later before you upgrade External Provider. Version 3.7.0 removes the import, so `graphql-dxm-provider` is no longer in the refresh chain. [graphql-core#630](https://github.com/Jahia/graphql-core/pull/630) has the change.
- Upgrade External Provider at a time of low traffic, and do not use the administration screens during the upgrade.

### Removal of External Provider VFS from the distribution

Jahia 8.2.4.0 no longer ships `external-provider-vfs`, as [jahia-private#3515](https://github.com/Jahia/jahia-private/issues/3515) states. This removal also removes the start of the refresh chain above.

- `external-provider-modules` 4.11.0 holds a private copy of the VFS classes it needs, and no longer imports `org.jahia.modules.external.vfs`. [external-provider#266](https://github.com/Jahia/external-provider/pull/266) has the change. A refresh of `external-provider-vfs` then no longer reaches `external-provider-modules`, and the Studio works without `external-provider-vfs`.
- An upgrade to Jahia 8.2.4.0 keeps `external-provider-vfs` and its mount points at their earlier version.
- 4.11.0 is the last release of `external-provider-vfs`, and the module is then archived.

If you do not use VFS mount points, delete them, then uninstall `external-provider-vfs`. Delete the mount points first, because after an uninstall each VFS mount point node stays `mounted` and serves no content. If you use VFS mount points, install `external-provider-vfs` 4.11.0 from the Jahia Maven repository.

### Blocked startup with external-provider-ui before 4.10.0

With `external-provider-ui` 4.8.0 or 4.9.0, Jahia can stop at startup after an upgrade or a restart, as [jahia-private#4851](https://github.com/Jahia/jahia-private/issues/4851) states. The log then shows `Timeout reached when waiting for final start level to be reached`, many modules do not start, and the instance is not accessible.

The cause is the refresh that `external-provider-ui` does when it starts. The packages `org.jahia.modules.external.admin.mount` and `org.jahia.modules.external.admin.mount.validator` moved from `external-provider` to `external-provider-ui`. When `external-provider-ui` starts, it refreshes each bundle that imports these packages and is not wired to it, for example `external-provider-vfs`. During the startup of Jahia, the OSGi start level thread holds a lock until all bundles start. The refresh waits for that lock, and the start level thread waits for the end of the refresh, so both threads stop.

`external-provider-ui` 4.10.0 fixes this problem: it does not do the refresh while Jahia starts, because the OSGi framework already wires all bundles at startup. [external-provider#226](https://github.com/Jahia/external-provider/pull/226) has the change. The versions after 4.10.0 also refresh only the bundles that are wired to an old exporter of the moved packages. They do no refresh during an upgrade of `external-provider-ui` itself. This change prevents the deadlock of [jahia-private#5156](https://github.com/Jahia/jahia-private/issues/5156), and [external-provider#243](https://github.com/Jahia/external-provider/pull/243) has it.

Jahia does not ship `external-provider-ui`, so an upgrade of Jahia does not upgrade it. Before you upgrade or restart Jahia, do one of these steps:

- Upgrade `external-provider-ui` to 4.10.0 or later. This version works with the other External Provider modules from version 4.2 up to version 5.0, which is excluded, so you can keep their versions.
- Stop or uninstall `external-provider-ui`.

## GraphQL API

GraphQL API endpoints are available for creating and modifying basic and VFS mount points, as well as mount and unmount and query operations. Other mount point provider modules can also extend endpoints with the same namespaces `mutation.admin.mountpoint` and `query.admin.mountpoint`. 

#### Mutations (`mutation.admin.mountpoint`)

- add

```
add
Create a simple mount point node in /mounts

Type
String
Returns the uuid of the created mount point node

Arguments
name: String!
Mount point name

mountPointRefPath: String
Target local mount point
```

- addVfs

```
addVfs
Create a VFS mount point node in /mounts

Type
String
Returns the uuid of the mount point node

Arguments
name: String!
Name for the mount point

mountPointRefPath: String
Target local mount point

rootPath: String
VFS root mount point
```

- modify 

```
Modify an existing mount point node

Type
Boolean
Return true if operation is successful

Arguments
pathOrId: String!
Mount point path or ID to modify

name: String
Rename existing mount point

mountPointRefPath: String
Change local mount point, or set to empty string to remove
```

- modifyVfs
```
modifyVfs
Modify an existing mount point node

Type
Boolean
Return true if operation is successful

Arguments
pathOrId: String!
Mount point path or ID to modify

name: String
Rename existing mount point

mountPointRefPath: String
Change target local mount point, or set to empty string to remove

rootPath: String
VFS root mount point
```

- mount

```
mount
Mount an existing mount point

Type
Boolean
Return true if operation is successful

Arguments
pathOrId: String!
Mount point path or ID to mount
```

- unmount
```
unmount
Unount an existing mount point

Type
Boolean
Return true if operation is successful

Arguments
pathOrId: String!
Mount point path or ID to mount
```

#### Queries (`query.admin.mountpoint`)

- mountPoint 

```
mountPoint
Get mount point with given name, or null if it doesn't exists

Type
GqlMountPoint
Return GqlMountPoint object

Arguments
name: String!
Name for the mount point
```

- mountPoints

```
mountPoints
Get list of mount points, or empty list if no mounts exist

Type
[GqlMountPoint]
Return list of GqlMountPoint objects
```

- Type GqlMountPoint

```
GqlMountPoint
Mount point object

Fields
mountName: String
Mount point name

mountPointRefPath: String
Mount point local reference path

mountStatus: String
Mount status

nodeType: String
Mount node type

properties: [GqlMountPointProperty]
Mount point additional properties

property(name: String!): String
Mount point property

uuid: String
Mount point node uuid
```

## Open-Source

This is an Open-Source module, you can find more details about Open-Source @ Jahia [in this repository](https://github.com/Jahia/open-source).
