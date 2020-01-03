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
    private static Logger logger = LoggerFactory.getLogger(MountPointConfigFactory.class);

    private static final String MOUNT_PROPERTY_PREFIX = "mount.";
    private static final String NODE_NAME_PROPERTY = "mount.j_nodename";
    private static final String PRIMARY_TYPE_PROPERTY = "mount.jcr_primaryType";
    private static final String LOCAL_PATH_PROPERTY = "mount.j_path";
    private static final String ROOT_PATH_PROPERTY = "mount.j_rootPath";

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

        ConfigMountPointDTO mountPointDTO = Optional.ofNullable(filledMountPointFactory).orElseGet(ConfigMountPointDTO::new);

        try {
            mountPointDTO.setName(Optional.ofNullable(dictionary.get(NODE_NAME_PROPERTY)).map(Object::toString)
                    .orElseThrow(() -> new ConfigurationException(NODE_NAME_PROPERTY, PROPERTY_NOT_FOUND)));
            mountPointDTO.setRoot(Optional.ofNullable(dictionary.get(ROOT_PATH_PROPERTY)).map(Object::toString)
                    .orElseThrow(() -> new ConfigurationException(ROOT_PATH_PROPERTY, PROPERTY_NOT_FOUND)));
            mountPointDTO.setMountNodeType(Optional.ofNullable(dictionary.get(PRIMARY_TYPE_PROPERTY)).map(Object::toString)
                    .orElseThrow(() -> new ConfigurationException(PRIMARY_TYPE_PROPERTY, PROPERTY_NOT_FOUND)));
            mountPointDTO.setLocalPath(Optional.ofNullable(dictionary.get(LOCAL_PATH_PROPERTY)).map(Object::toString).orElse(null));
            mountPointDTO.setDictionary(dictionary);
            mountPointDTO.setKeysToSave(getPropertiesKeyToSave(dictionary));
            super.save(mountPointDTO);
        } catch (RepositoryException | ConfigurationException e) {
            logger.error("Error while saving the mount point", e);
        }
    }
}
