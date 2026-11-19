<a href="https://www.jahia.com/">
    <img src="https://www.jahia.com/modules/jahiacom-templates/images/jahia-3x.png" alt="Jahia logo" title="Jahia" align="right" height="60" />
</a>

External Provider
======================
The External Provider module provides an API allowing integration of external system as content providers.

## Upgrade

### External Provider in Jahia patch releases

A Jahia patch release, such as 8.2.3.x or 8.2.2.x, does not upgrade the External Provider modules. An upgrade of these modules refreshes the modules that depend on them, so the instance can be unavailable during the upgrade. A new version of External Provider ships with a new minor version of Jahia, such as 8.2.4.0.

### How to upgrade External Provider

1. Upgrade `graphql-dxm-provider` to 3.7.0 or later.
2. Stop `external-provider-ui`.
3. Install the new versions of the External Provider modules during a maintenance window.
4. Start `external-provider-ui`, or install its new version.

Do not use the administration screens during the upgrade.

### Known impacts

- Before `graphql-dxm-provider` 3.7.0, an upgrade of External Provider restarts about 38 modules and rebuilds the GraphQL schema. See [graphql-core#629](https://github.com/Jahia/graphql-core/issues/629).
- With `external-provider-ui` 4.8.0 or 4.9.0, Jahia can block at startup after an upgrade or a restart. Version 4.10.0 fixes this problem. See [jahia-private#4851](https://github.com/Jahia/jahia-private/issues/4851).
- An upgrade of `external-provider-ui` can deadlock the instance, mostly on a cluster. The fix ships with Jahia 8.2.4.0. See [jahia-private#5156](https://github.com/Jahia/jahia-private/issues/5156).
- Jahia 8.2.4.0 no longer ships `external-provider-vfs`, and an upgrade keeps the installed module. See [jahia-private#3515](https://github.com/Jahia/jahia-private/issues/3515).
- In a cluster, External Provider 4.10.0 and later keep old identifiers on the other nodes after a rename or a delete. See [external-provider#274](https://github.com/Jahia/external-provider/issues/274).

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
