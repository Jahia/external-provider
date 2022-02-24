/*
 * Copyright (C) 2002-2022 Jahia Solutions Group SA. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
