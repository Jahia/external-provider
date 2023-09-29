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
package org.jahia.modules.external.admin.mount.model;

import java.io.Serializable;
import java.lang.String;import java.util.Collections;import java.util.HashMap;import java.util.List;import java.util.Map;

/**
 * @author kevan
 */
public class MountPointManager implements Serializable{
    private static final long serialVersionUID = 7055743449410009286L;

    Map<String, MountPointFactory> mountPointFactories = new HashMap<String, MountPointFactory>();
    List<MountPoint> mountPoints = Collections.emptyList();

    public MountPointManager(Map<String, MountPointFactory> mountPointFactories, List<MountPoint> mountPoints) {
        this.mountPointFactories = mountPointFactories;
        this.mountPoints = mountPoints;
    }

    public MountPointManager() {
    }

    public Map<String, MountPointFactory> getMountPointFactories() {
        return mountPointFactories;
    }

    public void setMountPointFactories(Map<String, MountPointFactory> mountPointFactories) {
        this.mountPointFactories = mountPointFactories;
    }

    public List<MountPoint> getMountPoints() {
        return mountPoints;
    }

    public void setMountPoints(List<MountPoint> mountPoints) {
        this.mountPoints = mountPoints;
    }
}
