/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2016 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.modules.external.acl;

import com.google.common.base.Objects;
import org.jahia.services.content.JCRContentUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * External ace
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
