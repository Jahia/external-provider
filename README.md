<a href="https://www.jahia.com/">
    <img src="https://www.jahia.com/modules/jahiacom-templates/images/jahia-3x.png" alt="Jahia logo" title="Jahia" align="right" height="60" />
</a>

External Provider
======================
The External Provider module provides an API allowing integration of external system as content providers.

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
