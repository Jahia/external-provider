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
package org.jahia.modules.external.service;

import org.apache.commons.lang3.StringUtils;
import org.jahia.api.Constants;
import org.jahia.services.content.*;
import org.jahia.services.content.decorator.JCRMountPointNode;
import org.jahia.services.content.decorator.JCRMountPointNode.MountStatus;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import java.util.ArrayList;
import java.util.List;

@Component(service = MountPointService.class, immediate = true)
public class MountPointServiceImpl implements MountPointService {

    private static final Logger logger = LoggerFactory.getLogger(MountPointServiceImpl.class);

    /**
     * Creates a mount point node based on available node name in /mounts and set mount point node as mounted.
     *
     * @param mountPoint The mount point to set with a given name, mount node type, and (optional) local path
     * @return
     * @throws RepositoryException
     */
    public String create(MountPoint mountPoint) throws RepositoryException {
        return JCRTemplate.getInstance().doExecuteWithSystemSession(session -> {
            JCRNodeWrapper mounts = session.getNode("/mounts");
            String jcrMountName = mountPoint.getAvailableNodeName(mounts);
            logger.debug("Creating mount point {} in /mounts", jcrMountName);
            JCRMountPointNode jcrMountPointNode = (JCRMountPointNode) mounts.addNode(jcrMountName, mountPoint.getMountNodeType());

            jcrMountPointNode.setMountStatus(JCRMountPointNode.MountStatus.mounted);
            setMountPointRefPath(mountPoint, session, jcrMountPointNode);

            // set additional mount point node properties
            mountPoint.setProperties(jcrMountPointNode);
            session.save();

            return jcrMountPointNode.getIdentifier();
        });
    }

    public boolean modify(MountPoint mountPoint) throws RepositoryException {
        return JCRTemplate.getInstance().doExecuteWithSystemSession(session -> {
            String pathOrId = mountPoint.getPathOrId();
            JCRMountPointNode jcrMountPointNode = getMountPointNode(session, pathOrId);

            // rename
            String newName = mountPoint.getName();
            if (StringUtils.isNotBlank(newName)) {
                JCRNodeWrapper mounts = session.getNode("/mounts");
                String jcrMountName = mountPoint.getAvailableNodeName(mounts);
                logger.debug("Renaming mount point {} to '{}'", pathOrId, jcrMountName);
                jcrMountPointNode.rename(jcrMountName);
            }

            setMountPointRefPath(mountPoint, session, jcrMountPointNode);

            // set additional mount point node properties
            mountPoint.setProperties(jcrMountPointNode);
            session.save();

            return true;
        });
    }

    /*
     * Set mount point ref path as 'mountPoint' reference property
     * If ref path is a blank string, then remove mountPoint property if property exists
     */
    protected void setMountPointRefPath(MountPoint mountPoint, JCRSessionWrapper session,
            JCRMountPointNode jcrMountPointNode) throws RepositoryException {

        String mountPointRefPath = mountPoint.getMountPointRefPath();
        if (StringUtils.isNotBlank(mountPointRefPath)) {
            String mountPointRefId = session.getNode(mountPointRefPath).getIdentifier();
            jcrMountPointNode.setProperty("mountPoint", mountPointRefId);
        } else if (mountPointRefPath != null && StringUtils.isBlank(mountPointRefPath) && jcrMountPointNode.hasProperty("mountPoint")) {
            jcrMountPointNode.getProperty("mountPoint").remove();
        }
    }

    public boolean mount(String pathOrId) throws RepositoryException {
        return setMountStatus(pathOrId, MountStatus.mounted);
    }

    public boolean unmount(String pathOrId) throws RepositoryException {
        return setMountStatus(pathOrId, MountStatus.unmounted);
    }

    private boolean setMountStatus(String pathOrId, MountStatus mountStatus) throws RepositoryException {
        return JCRTemplate.getInstance().doExecuteWithSystemSession(session -> {
            JCRMountPointNode jcrMountPointNode = getMountPointNode(session, pathOrId);
            logger.debug("Setting mount status of mount point node {} to {}", pathOrId, mountStatus);
            if (jcrMountPointNode.getMountStatus() == mountStatus) {
               logger.warn("Mount status for node {} is already {}", pathOrId, mountStatus);
            } else {
               jcrMountPointNode.setMountStatus(mountStatus);
               session.save();
            }

            // return if mount status corresponds to the mount status being set
            return (isMounted(jcrMountPointNode) == (mountStatus == MountStatus.mounted));
        });
    }

    public boolean isMounted(JCRMountPointNode mountPointNode) {
        try {
            // make an exception for jnt:mountPoint as it does not return a mount provider
            boolean baseMounted = mountPointNode.getPrimaryNodeType().isNodeType(Constants.JAHIANT_MOUNTPOINT)
                    && mountPointNode.getMountStatus() == MountStatus.mounted;
            JCRStoreProvider mountProvider = mountPointNode.getMountProvider();
            return baseMounted || (mountProvider != null && mountProvider.isAvailable());
        } catch (RepositoryException ignored) {}

        return false;
    }

    public JCRMountPointNode getMountPointNode(JCRSessionWrapper session, String pathOrId) throws RepositoryException {
        if (pathOrId != null) {
            JCRNodeWrapper nodeWrapper = ('/' == pathOrId.charAt(0)) ?
                    session.getNode(JCRContentUtils.escapeNodePath(pathOrId)) :
                    session.getNodeByIdentifier(pathOrId);
            if (nodeWrapper instanceof JCRMountPointNode) {
                return (JCRMountPointNode) nodeWrapper;
            }
        }
        throw new RepositoryException("No mount point node found for given path or ID: " + pathOrId);
    }

    public MountPoint getMountPoint(String name) throws RepositoryException {
        String mountName = JCRContentUtils.escapeNodePath(name) + JCRMountPointNode.MOUNT_SUFFIX;
        return JCRTemplate.getInstance().doExecuteWithSystemSession(session -> {
            Query query = session.getWorkspace().getQueryManager().createQuery(getMountPointQuery(mountName), Query.JCR_SQL2);
            QueryResult queryResult = query.execute();
            long querySize = queryResult.getNodes().getSize();
            JCRMountPointNode n = querySize > 0 ? (JCRMountPointNode) queryResult.getNodes().next() : null;
            return n == null ? null : new MountPoint(n);
        });
    }

    public List<MountPoint> getMountPoints() throws RepositoryException {
        return JCRTemplate.getInstance().doExecuteWithSystemSession(session -> {
            Query query = session.getWorkspace().getQueryManager().createQuery(getMountPointQuery(null), Query.JCR_SQL2);
            NodeIterator iter = query.execute().getNodes();
            List<MountPoint> mountPoints = new ArrayList<>((int) iter.getSize());
            while (iter.hasNext()) {
                JCRMountPointNode mountPointNode = (JCRMountPointNode) iter.next();
                mountPoints.add(new MountPoint(mountPointNode));
            }
            return mountPoints;
        });
    }

    private String getMountPointQuery(String name) {
        String query = "select * from [" + Constants.JAHIANT_MOUNTPOINT + "] as mount where ischildnode('/mounts')";
        if (StringUtils.isNotEmpty(name)) {
            query += " and ['j:nodename'] = '" + name + "'";
        }
        logger.debug("Query: {}", query);
        return query;
    }

}
