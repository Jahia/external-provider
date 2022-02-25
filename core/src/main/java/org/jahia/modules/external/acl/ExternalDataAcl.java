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
package org.jahia.modules.external.acl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * External acl
 * This is the representation of an ACL
 * it contains
 *  - a list of ExternalAce
 *  - if it inherits the ace from its parent or not
 */
public class ExternalDataAcl {
    public static final String ACL_NODE_NAME = "j:acl";
    public static final String ACL_INHERIT_PROP_NAME = "j:inherit";
    public static final String ACL_NODE_TYPE = "jnt:acl";

    private boolean inherit;
    private HashMap<String, ExternalDataAce> acl;

    public ExternalDataAcl() {
        this(true);
    }

    public ExternalDataAcl(boolean inherit) {
        this.inherit = inherit;
        acl = new HashMap<>();
    }

    public boolean isInherit() {
        return inherit;
    }

    public void setInherit(boolean inherit) {
        this.inherit = inherit;
    }

    public ExternalDataAce getAce(String aceName) {
        return acl.get(aceName);
    }

    /**
     * Add an entry (Ace) to the ACL
     * @param aceType : type of entry, grant or deny
     * @param principal : user or group, format u:userKey or g:groupKey
     * @param roles : roles granted or denied
     */
    public void addAce(ExternalDataAce.Type aceType, String principal, Set<String> roles) {
        addAce(aceType, principal, roles, false);
    }

    public void addAce(ExternalDataAce.Type aceType, String principal, Set<String> roles, boolean aceProtected) {
        ExternalDataAce ace = new ExternalDataAce(aceType, principal, roles, aceProtected);
        acl.put(ace.toString(), ace);
    }

    public Map<String, String[]> getProperties() {
        Map<String, String[]> aclProperties = new HashMap<>();
        aclProperties.put(ExternalDataAcl.ACL_INHERIT_PROP_NAME, new String[]{String.valueOf(inherit)});
        return aclProperties;
    }

    public Collection<ExternalDataAce> getAcl() {
        return acl.values();
    }
}
