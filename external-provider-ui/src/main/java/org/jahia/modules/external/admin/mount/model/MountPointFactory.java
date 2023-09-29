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

/**
 * Created by kevan on 14/11/14.
 */
public class MountPointFactory implements Serializable{
    private static final long serialVersionUID = -2765662825097029693L;

    String nodeType;
    String displayableName;
    String endOfURL;

    public MountPointFactory(String nodeType, String displayableName, String endOfURL) {
        this.nodeType = nodeType;
        this.displayableName = displayableName;
        this.endOfURL = endOfURL;
    }

    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    public String getDisplayableName() {
        return displayableName;
    }

    public void setDisplayableName(String displayableName) {
        this.displayableName = displayableName;
    }

    public String getEndOfURL() {
        return endOfURL;
    }

    public void setEndOfURL(String endOfURL) {
        this.endOfURL = endOfURL;
    }
}
