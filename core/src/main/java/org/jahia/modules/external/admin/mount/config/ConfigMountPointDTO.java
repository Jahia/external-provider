/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.external.admin.mount.config;

import org.jahia.modules.external.admin.mount.AbstractMountPointFactory;
import org.jahia.services.content.JCRNodeWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import java.util.Dictionary;
import java.util.List;

/**
 * @author jonathan
 */
public class ConfigMountPointDTO extends AbstractMountPointFactory {

    private static final Logger logger = LoggerFactory.getLogger(ConfigMountPointDTO.class);
    private static final String SERVICE_PID_PROPERTY = "service.pid";

    private String name;
    private String localPath;
    private String root;
    private String mountNodeType;
    private Dictionary<String, ?> dictionary;
    private List<String> keysToSave;

    public void setName(String name) {
        this.name = name;
    }

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }

    @Override public void populate(JCRNodeWrapper nodeWrapper) throws RepositoryException {
        super.populate(nodeWrapper);
        this.name = getName(nodeWrapper.getName());
        try {
            this.localPath = nodeWrapper.getProperty("mountPoint").getNode().getPath();
        } catch (PathNotFoundException e) {
            logger.warn("No local path defined for this mount point");
        }
        this.root = nodeWrapper.getPropertyAsString("j:rootPath");
    }

    @Override public String getName() {
        return name;
    }

    @Override public String getLocalPath() {
        return localPath;
    }

    public void setMountNodeType(String mountNodeType) {
        this.mountNodeType = mountNodeType;
    }

    @Override public String getMountNodeType() {
        return this.mountNodeType;
    }

    @Override public void setProperties(JCRNodeWrapper mountNode) throws RepositoryException {

        if(!mountNode.isNodeType("jmix:configPid")){
            addPidMixin(mountNode);
        }
        keysToSave.forEach(key -> {
            try {
                mountNode.setProperty(key.substring("mount.".length()).replace('_', ':'), dictionary.get(key).toString());
            } catch (RepositoryException e) {
                logger.error("Error while saving the mount point from the configuration", e);
            }
        });
    }

    public void setDictionary(Dictionary<String, ?> dictionary) {
        this.dictionary = dictionary;
    }

    public void setKeysToSave(List<String> keysToSave) {
        this.keysToSave = keysToSave;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    private void addPidMixin(JCRNodeWrapper mountNode) throws RepositoryException {
        mountNode.addMixin("jmix:configPid");
        mountNode.setProperty("configPid", dictionary.get(SERVICE_PID_PROPERTY).toString());
    }
}
