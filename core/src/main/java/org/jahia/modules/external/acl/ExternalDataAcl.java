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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * External acl
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
