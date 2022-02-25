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

import com.google.common.base.Objects;
import org.jahia.services.content.JCRContentUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * External ace
 * This is the representation of an Access Control Entry
 * The constructor is protected, only created by ExternalDataAcl
 * it contains,
 *  - the type : Type.Grant or Type.DENY
 *  - principal : the user
 *  - roles : a Set of String containing the roles names defined by the ACE
 *  - ace protected : not used
 */
public class ExternalDataAce {
    public static final String ACE_TYPE_PROP = "j:aceType";
    public static final String ACE_PRINCIPAL_PROP = "j:principal";
    public static final String ACE_ROLES_PROP = "j:roles";
    public static final String ACE_PROTECTED_PROP = "j:protected";
    public static final String ACE_NODE_TYPE = "jnt:ace";

    /**
     * Ace types
     */
    public enum Type {
        GRANT ("GRANT"),
        DENY ("DENY");

        private final String name;

        Type(String s) {
            name = s;
        }

        public String toString() {
            return this.name;
        }
    }

    protected ExternalDataAce(Type aceType, String principal, Set<String> roles, boolean aceProtected) {
        this.aceType = aceType;
        this.principal = principal;
        this.roles = roles;
        this.aceProtected = aceProtected;
    }

    private Type aceType;
    private String principal;
    private Set<String> roles;
    private boolean aceProtected;

    public Type getAceType() {
        return aceType;
    }

    public void setAceType(Type aceType) {
        this.aceType = aceType;
    }

    public String getPrincipal() {
        return principal;
    }

    public void setPrincipal(String principal) {
        this.principal = principal;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public boolean isAceProtected() {
        return aceProtected;
    }

    public void setAceProtected(boolean aceProtected) {
        this.aceProtected = aceProtected;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExternalDataAce that = (ExternalDataAce) o;
        return Objects.equal(aceType, that.aceType) &&
                Objects.equal(principal, that.principal) &&
                Objects.equal(roles, that.roles);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(aceType, principal, roles);
    }

    @Override
    public String toString() {
        return aceType.toString() + "_" + JCRContentUtils.replaceColon(principal).replaceAll("/", "_");
    }

    public Map<String, String[]> getProperties() {
        Map<String, String[]> properties = new HashMap<>();
        properties.put(ACE_TYPE_PROP, new String[]{aceType.toString()});
        properties.put(ACE_PRINCIPAL_PROP, new String[]{principal});
        properties.put(ACE_ROLES_PROP, roles.toArray(new String[roles.size()]));
        properties.put(ACE_PROTECTED_PROP, new String[]{String.valueOf(aceProtected)});
        return properties;
    }
}
