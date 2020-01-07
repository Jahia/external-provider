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
 *     Copyright (C) 2002-2019 Jahia Solutions Group. All rights reserved.
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
package org.jahia.modules.external.admin.mount.config;

import org.jahia.modules.external.admin.mount.AbstractMountPointFactoryHandler;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.content.decorator.JCRMountPointNode;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Short description of the class
 *
 * @author jonathansinovassin-naik
 */
public class MountPointConfigFactory extends AbstractMountPointFactoryHandler implements ManagedServiceFactory {
    private static final String PROPERTY_NOT_FOUND = "Property not found";
    public static final String MOUNT_USER = "mount.user";
    public static final String MOUNT_PASSWORD = "mount.password";
    public static final String MOUNT_URL = "mount.url";
    public static final String MOUNT_SLOW_CONNECTION = "mount.slowConnection";
    public static final String MOUNT_TYPE = "mount.type";
    public static final String MOUNT_REMOTE_PATH = "mount.remotePath";
    public static final String MOUNT_PUBLIC_USER = "mount.publicUser";
    public static final String MOUNT_REPOSITORY_ID = "mount.repositoryId";
    private static Logger logger = LoggerFactory.getLogger(MountPointConfigFactory.class);

    private static final String MOUNT_PROPERTY_PREFIX = "mount.";
    private static final String NODE_NAME_PROPERTY = "mount.j_nodename";
    private static final String PRIMARY_TYPE_PROPERTY = "mount.jcr_primaryType";
    private static final String LOCAL_PATH_PROPERTY = "mount.j_path";
    private static final String ROOT_PATH_PROPERTY = "mount.j_rootPath";

    private static final String VFS_MOUNT_POINT_NODE_TYPE = "jnt:vfsMountPoint";
    private static final String CMIS_MOUNT_POINT_NODE_TYPE = "cmis:cmisMountPoint";

    private static final String QUERY_BY_PID = "SELECT node.* FROM [jnt:mountPoint] AS node WHERE node.configPid = '%s'";

    public ConfigMountPointDTO init(final String pid) throws RepositoryException {

        return JCRTemplate.getInstance().doExecuteWithSystemSession((session -> {
            NodeIterator mountNodes = getNodeIterator(pid, session);

            ConfigMountPointDTO mountPointDTO = new ConfigMountPointDTO();
            mountNodes.forEachRemaining(node -> {
                try {
                    mountPointDTO.populate((JCRNodeWrapper) node);
                } catch (RepositoryException e) {
                    logger.error(String.format("Failed to populate the node for the pid %s", pid), e);
                }
            });
            return mountPointDTO;
        }));
    }

    @Override public String getName() {
        return "org.jahia.modules.external.mount";
    }

    @Override public void updated(String pid, Dictionary<String, ?> dictionary) {

        ConfigMountPointDTO mountPointDTO = null;
        try {
            mountPointDTO = this.init(pid);
        } catch (RepositoryException e) {
            logger.warn(String.format("Could not get the node having the property [jmix:configPid] equals to %s", pid), e);
        }
        save(dictionary, mountPointDTO);
    }

    @Override public void deleted(String pid) {
        try {
            JCRTemplate.getInstance().doExecuteWithSystemSession(session -> {
                NodeIterator mountNodes = getNodeIterator(pid, session);
                mountNodes.forEachRemaining(node -> {
                    try {
                        JCRMountPointNode mountPointNode = (JCRMountPointNode) node;
                        mountPointNode.remove();
                    } catch (RepositoryException e) {
                        logger.error(String.format("Error while populate node for the pid %s", pid), e);
                    }
                });
                session.save();
                return null;
            });
        } catch (RepositoryException e) {
            logger.error(String.format("Error while trying to delete the node with the config pid %s", pid), e);
        }
    }

    private NodeIterator getNodeIterator(String pid, JCRSessionWrapper session) throws RepositoryException {
        Query mountNodeQuery = session.getWorkspace().getQueryManager().createQuery(String.format(QUERY_BY_PID, pid), Query.JCR_SQL2);
        return mountNodeQuery.execute().getNodes();
    }

    private List<String> getPropertiesKeyToSave(Dictionary<String, ?> dictionary) {
        return Collections.list(dictionary.keys()).stream().filter(filterPropertiesByKeys()).collect(Collectors.toList());
    }

    private static Predicate<String> filterPropertiesByKeys() {
        return key -> key.contains(MOUNT_PROPERTY_PREFIX)
                && Arrays.asList(NODE_NAME_PROPERTY, PRIMARY_TYPE_PROPERTY, LOCAL_PATH_PROPERTY).indexOf(key) == -1;
    }

    private void save(Dictionary<String, ?> dictionary, ConfigMountPointDTO filledMountPointFactory) {

        try {
            ConfigMountPointDTO mountPointDTO = fillDTOWithProperties(
                    Optional.ofNullable(filledMountPointFactory).orElseGet(ConfigMountPointDTO::new), dictionary);
            super.save(mountPointDTO);
        } catch (RepositoryException | ConfigurationException e) {
            logger.error("Error while saving the mount point", e);
        }
    }

    private ConfigMountPointDTO fillDTOWithProperties(ConfigMountPointDTO mountPointDTO, Dictionary<String, ?> dictionary)
            throws ConfigurationException {
        try {
            checkMandatoryProperties(dictionary);
        } catch (ConfigurationException e) {
            logger.warn("Missing some property");
            throw e;
        }
        mountPointDTO.setName(dictionary.get(NODE_NAME_PROPERTY).toString());
        mountPointDTO.setMountNodeType(dictionary.get(PRIMARY_TYPE_PROPERTY).toString());
        mountPointDTO.setLocalPath(Optional.ofNullable(dictionary.get(LOCAL_PATH_PROPERTY)).map(Object::toString).orElse(null));
        mountPointDTO.setDictionary(dictionary);
        mountPointDTO.setKeysToSave(getPropertiesKeyToSave(dictionary));
        return mountPointDTO;
    }

    private void checkMandatoryProperties(Dictionary<String, ?> dictionary) throws ConfigurationException {

        String nodeType = Optional.ofNullable(dictionary.get(PRIMARY_TYPE_PROPERTY)).map(Object::toString)
                .orElseThrow(() -> new ConfigurationException(PRIMARY_TYPE_PROPERTY, PROPERTY_NOT_FOUND));
        Optional.ofNullable(dictionary.get(NODE_NAME_PROPERTY))
                .orElseThrow(() -> new ConfigurationException(NODE_NAME_PROPERTY, PROPERTY_NOT_FOUND));
        if (VFS_MOUNT_POINT_NODE_TYPE.equals(nodeType)) {
            Optional.ofNullable(dictionary.get(ROOT_PATH_PROPERTY))
                    .orElseThrow(() -> new ConfigurationException(ROOT_PATH_PROPERTY, PROPERTY_NOT_FOUND));

        } else if (CMIS_MOUNT_POINT_NODE_TYPE.equals(nodeType)) {
            Optional.ofNullable(dictionary.get(MOUNT_USER)).orElseThrow(() -> new ConfigurationException(MOUNT_USER, PROPERTY_NOT_FOUND));
            Optional.ofNullable(dictionary.get(MOUNT_PASSWORD))
                    .orElseThrow(() -> new ConfigurationException(MOUNT_PASSWORD, PROPERTY_NOT_FOUND));
            Optional.ofNullable(dictionary.get(MOUNT_URL)).orElseThrow(() -> new ConfigurationException(MOUNT_URL, PROPERTY_NOT_FOUND));
            Optional.ofNullable(dictionary.get(MOUNT_SLOW_CONNECTION))
                    .orElseThrow(() -> new ConfigurationException(MOUNT_SLOW_CONNECTION, PROPERTY_NOT_FOUND));
            Optional.ofNullable(dictionary.get(MOUNT_TYPE)).orElseThrow(() -> new ConfigurationException(MOUNT_TYPE, PROPERTY_NOT_FOUND));
            Optional.ofNullable(dictionary.get(MOUNT_REMOTE_PATH))
                    .orElseThrow(() -> new ConfigurationException(MOUNT_REMOTE_PATH, PROPERTY_NOT_FOUND));
            Optional.ofNullable(dictionary.get(MOUNT_PUBLIC_USER))
                    .orElseThrow(() -> new ConfigurationException(MOUNT_PUBLIC_USER, PROPERTY_NOT_FOUND));
            Optional.ofNullable(dictionary.get(MOUNT_REPOSITORY_ID))
                    .orElseThrow(() -> new ConfigurationException(MOUNT_REPOSITORY_ID, PROPERTY_NOT_FOUND));
        }
    }
}
