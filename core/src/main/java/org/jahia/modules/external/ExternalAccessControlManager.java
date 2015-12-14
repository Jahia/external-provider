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
 *     "This program is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU General Public License
 *     as published by the Free Software Foundation; either version 2
 *     of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 *     As a special exception to the terms and conditions of version 2.0 of
 *     the GPL (or any later version), you may redistribute this Program in connection
 *     with Free/Libre and Open Source Software ("FLOSS") applications as described
 *     in Jahia's FLOSS exception. You should have received a copy of the text
 *     describing the FLOSS exception, also available here:
 *     http://www.jahia.com/license"
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
 *
 */
package org.jahia.modules.external;

import org.apache.commons.lang.ArrayUtils;
import org.apache.jackrabbit.commons.iterator.AccessControlPolicyIteratorAdapter;
import org.apache.jackrabbit.core.security.JahiaLoginModule;
import org.apache.jackrabbit.core.security.JahiaPrivilegeRegistry;
import org.jahia.api.Constants;
import org.jahia.exceptions.JahiaRuntimeException;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.usermanager.JahiaUser;
import org.jahia.services.usermanager.JahiaUserManagerService;

import javax.jcr.AccessDeniedException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.jcr.security.*;
import javax.jcr.version.VersionException;
import java.util.ArrayList;
import java.util.List;

import static javax.jcr.security.Privilege.*;
import static org.jahia.api.Constants.EDIT_WORKSPACE;
import static org.jahia.api.Constants.LIVE_WORKSPACE;

/**
 * Implementation of the {@link javax.jcr.security.AccessControlManager} for the {@link org.jahia.modules.external.ExternalData}.
 *
 * @author Sergiy Shyrkov
 */
public class ExternalAccessControlManager implements AccessControlManager {

    private static final AccessControlPolicy[] POLICIES = new AccessControlPolicy[0];

    private String[] rootNodePrivileges;

    private String[] privileges;

    private boolean readOnly;

    private ExternalDataSource dataSource;

    private JahiaPrivilegeRegistry registry;

    private final ExternalSessionImpl session;

    private String rootUserName;

    public ExternalAccessControlManager(NamespaceRegistry namespaceRegistry, boolean readOnly, ExternalDataSource dataSource, ExternalSessionImpl session) {
        super();
        this.readOnly = readOnly;
        this.dataSource = dataSource;
        this.session = session;
        try {
            init(namespaceRegistry);
        } catch (RepositoryException e) {
            throw new JahiaRuntimeException(e);
        }
    }

    private void init(NamespaceRegistry namespaceRegistry) throws RepositoryException {
        registry = new JahiaPrivilegeRegistry(namespaceRegistry);
        rootUserName = JahiaUserManagerService.getInstance().getRootUserName();
        if (readOnly) {
            rootNodePrivileges = new String[] {
                    JCR_READ + "_" + EDIT_WORKSPACE, JCR_READ + "_" + LIVE_WORKSPACE
                    };
            privileges = rootNodePrivileges;
        } else {
            rootNodePrivileges = new String[] {
                    JCR_READ + "_" + EDIT_WORKSPACE, JCR_READ + "_" + LIVE_WORKSPACE,
                    JCR_WRITE + "_" + EDIT_WORKSPACE, JCR_WRITE + "_" + LIVE_WORKSPACE,
                    JCR_ADD_CHILD_NODES + "_" + EDIT_WORKSPACE, JCR_ADD_CHILD_NODES + "_" + LIVE_WORKSPACE,
                    JCR_REMOVE_CHILD_NODES + "_" + EDIT_WORKSPACE, JCR_REMOVE_CHILD_NODES + "_" + LIVE_WORKSPACE
                    };
            privileges = new String[] {
                    JCR_READ + "_" + EDIT_WORKSPACE, JCR_READ + "_" + LIVE_WORKSPACE,
                    JCR_WRITE + "_" + EDIT_WORKSPACE, JCR_WRITE + "_" + LIVE_WORKSPACE,
                    JCR_REMOVE_NODE + "_" + EDIT_WORKSPACE, JCR_REMOVE_NODE + "_" + LIVE_WORKSPACE,
                    JCR_ADD_CHILD_NODES + "_" + EDIT_WORKSPACE, JCR_ADD_CHILD_NODES + "_" + LIVE_WORKSPACE,
                    JCR_REMOVE_CHILD_NODES + "_" + EDIT_WORKSPACE, JCR_REMOVE_CHILD_NODES + "_" + LIVE_WORKSPACE
                    };
        }
    }

    public AccessControlPolicyIterator getApplicablePolicies(String absPath)
            throws PathNotFoundException, AccessDeniedException, RepositoryException {
        return AccessControlPolicyIteratorAdapter.EMPTY;
    }

    public AccessControlPolicy[] getEffectivePolicies(String absPath) throws PathNotFoundException,
            AccessDeniedException, RepositoryException {
        return POLICIES;
    }

    public AccessControlPolicy[] getPolicies(String absPath) throws PathNotFoundException,
            AccessDeniedException, RepositoryException {
        return POLICIES;
    }

    public Privilege[] getPrivileges(String absPath) throws PathNotFoundException,
            RepositoryException {
        List<Privilege> l = new ArrayList<Privilege>();
        for (String s : getPrivilegesNames(absPath)) {
            Privilege privilege = registry.getPrivilege(s, null);
            if (privilege != null) {
                l.add(privilege);
            }
        }

        return l.toArray(new Privilege[l.size()]);
    }

    private String[] getPrivilegesNames(String absPath) {
        if (dataSource instanceof ExternalDataSource.AccessControllable) {
            ExternalContentStoreProvider.setCurrentSession(session);
            try {
                return ((ExternalDataSource.AccessControllable) dataSource).getPrivilegesNames(session.getUserID(), absPath);
            } finally {
                ExternalContentStoreProvider.removeCurrentSession();
            }
        }
        return absPath.length() == 1 && "/".equals(absPath) ? rootNodePrivileges : privileges;
    }

    public Privilege[] getSupportedPrivileges(String absPath) throws PathNotFoundException,
            RepositoryException {
        return JahiaPrivilegeRegistry.getRegisteredPrivileges();
    }

    public boolean hasPrivileges(String absPath, Privilege[] privileges)
            throws PathNotFoundException, RepositoryException {
        if (privileges == null || privileges.length == 0) {
            return true;
        }
        String userID = session.getUserID();
        if (userID.startsWith(JahiaLoginModule.SYSTEM) || rootUserName.equals(userID)) {
            return true;
        }
        boolean allowed = true;
        Privilege[] granted = getPrivileges(absPath);
        for (Privilege toCheck : privileges) {
            if (toCheck != null && !ArrayUtils.contains(granted, toCheck)) {
                allowed = false;
                break;
            }
        }

        return allowed;
    }

    public Privilege privilegeFromName(String privilegeName) throws AccessControlException, RepositoryException {
        try {
            return registry.getPrivilege(privilegeName, null);
        } catch (AccessControlException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("Unknown privilege {http://www.jcp.org/jcr/1.0}")) {
                // fallback to default workspace for JCR permissions
                return registry.getPrivilege(privilegeName, Constants.EDIT_WORKSPACE);
            } else {
                throw e;
            }
        }
    }

    public void removePolicy(String absPath, AccessControlPolicy policy)
            throws PathNotFoundException, AccessControlException, AccessDeniedException,
            LockException, VersionException, RepositoryException {
        // not supported
    }

    public void setPolicy(String absPath, AccessControlPolicy policy) throws PathNotFoundException,
            AccessControlException, AccessDeniedException, LockException, VersionException,
            RepositoryException {
        // not supported
    }

}
