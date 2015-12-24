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
package org.jahia.modules.external;

import com.google.common.collect.Lists;
import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.commons.iterator.AccessControlPolicyIteratorAdapter;
import org.apache.jackrabbit.core.security.JahiaLoginModule;
import org.apache.jackrabbit.core.security.JahiaPrivilegeRegistry;
import org.jahia.api.Constants;
import org.jahia.exceptions.JahiaRuntimeException;
import org.jahia.jaas.JahiaPrincipal;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.settings.SettingsBean;
import org.jahia.utils.security.AccessManagerUtils;

import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.security.*;
import javax.jcr.version.VersionException;
import java.util.*;

import static javax.jcr.security.Privilege.*;

/**
 * Implementation of the {@link javax.jcr.security.AccessControlManager} for the {@link org.jahia.modules.external.ExternalData}.
 *
 * @author Sergiy Shyrkov
 */
public class ExternalAccessControlManager implements AccessControlManager {

    private static final AccessControlPolicy[] POLICIES = new AccessControlPolicy[0];

    private Map<String, Boolean> pathPermissionCache = null;
    private Map<String, AccessManagerUtils.CompiledAcl> compiledAcls = new HashMap<String, AccessManagerUtils.CompiledAcl>();

    private JahiaPrivilegeRegistry registry;

    private final ExternalSessionImpl session;
    private final String workspaceName;
    private final JahiaPrincipal jahiaPrincipal;
    private final boolean aclReadOnly;
    private final boolean writable;
    private final Privilege modifyAccessControlPrivilege;
    private final Privilege writePrivilege;

    public ExternalAccessControlManager(NamespaceRegistry namespaceRegistry, ExternalSessionImpl session, ExternalDataSource dataSource) {

        this.session = session;
        this.workspaceName = session.getWorkspace().getName();
        this.aclReadOnly = dataSource instanceof ExternalDataSource.AccessControllable;
        this.writable = dataSource instanceof ExternalDataSource.Writable;

        this.pathPermissionCache = Collections.synchronizedMap(new LRUMap(SettingsBean.getInstance().getAccessManagerPathPermissionCacheMaxSize()));
        this.jahiaPrincipal = new JahiaPrincipal(session.getUserID(), session.getRealm(), session.getUserID().startsWith(JahiaLoginModule.SYSTEM), JahiaLoginModule.GUEST.equals(session.getUserID()));
        try {
            registry = new JahiaPrivilegeRegistry(namespaceRegistry);
            this.modifyAccessControlPrivilege = registry.getPrivilege("jcr:modifyAccessControl", workspaceName);
            this.writePrivilege = registry.getPrivilege("jcr:write", workspaceName);
        } catch (RepositoryException e) {
            throw new JahiaRuntimeException(e);
        }
    }

    @Override
    public AccessControlPolicyIterator getApplicablePolicies(String absPath)
            throws PathNotFoundException, AccessDeniedException, RepositoryException {
        return AccessControlPolicyIteratorAdapter.EMPTY;
    }

    @Override
    public AccessControlPolicy[] getEffectivePolicies(String absPath) throws PathNotFoundException,
            AccessDeniedException, RepositoryException {
        return POLICIES;
    }

    @Override
    public AccessControlPolicy[] getPolicies(String absPath) throws PathNotFoundException,
            AccessDeniedException, RepositoryException {
        return POLICIES;
    }

    @Override
    public Privilege[] getSupportedPrivileges(String absPath) throws PathNotFoundException,
            RepositoryException {
        return JahiaPrivilegeRegistry.getRegisteredPrivileges();
    }

    @Override
    public Privilege[] getPrivileges(final String absPath) throws PathNotFoundException,
            RepositoryException {
        JCRNodeWrapper node = JCRSessionFactory.getInstance().getCurrentSystemSession(workspaceName, null, null)
                .getNode(session.getRepository().getStoreProvider().getMountPoint() + absPath);
        Privilege[] privileges = AccessManagerUtils.getPrivileges(node, jahiaPrincipal, registry);

        // filter some privileges in some specific cases, for avoid some operation from edit engines
        List<Privilege> privilegeToFilter = getPrivilegesToFilter(node.getRealNode());
        if (privilegeToFilter.size() > 0) {
            return filterPrivileges(privileges, privilegeToFilter);
        } else {
            return privileges;
        }
    }

    @Override
    public boolean hasPrivileges(final String absPath, final Privilege[] privileges)
            throws PathNotFoundException, RepositoryException {

        if (privileges == null || privileges.length == 0) {
            // null or empty privilege array -> return true
            return true;
        } else {
            Set<String> privs = new HashSet<String>();
            for (Privilege privilege : privileges) {
                privs.add(privilege.getName());
            }
            String mountPoint = session.getRepository().getStoreProvider().getMountPoint();
            String jcrPath = StringUtils.equals(absPath,"/")? mountPoint : mountPoint + absPath;
            return AccessManagerUtils.isGranted(jcrPath, privs, JCRSessionFactory.getInstance().getCurrentSystemSession(session.getWorkspace().getName(), null, null),
                    StringUtils.substringAfterLast("/", jcrPath), jahiaPrincipal, workspaceName, false, pathPermissionCache, compiledAcls, registry);
        }
    }

    @Override
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

    @Override
    public void removePolicy(String absPath, AccessControlPolicy policy)
            throws PathNotFoundException, AccessControlException, AccessDeniedException,
            LockException, VersionException, RepositoryException {
        // not supported
    }

    @Override
    public void setPolicy(String absPath, AccessControlPolicy policy) throws PathNotFoundException,
            AccessControlException, AccessDeniedException, LockException, VersionException,
            RepositoryException {
        // not supported
    }

    public void checkRead(String path) throws RepositoryException {
        if (!hasPrivileges(path, new Privilege[]{registry.getPrivilege(JCR_READ + "_" + session.getWorkspace().getName(), null)})) {
            throw new PathNotFoundException(path);
        }
    }

    // JCR_MODIFY_PROPERTIES
    public void checkModify(String path) throws RepositoryException {
        if (!hasPrivileges(path, new Privilege[]{registry.getPrivilege(JCR_MODIFY_PROPERTIES + "_" + session.getWorkspace().getName(), null)})) {
            throw new AccessDeniedException(path);
        }
    }

    //JCR_ADD_CHILD_NODES
    public void checkAddChildNodes(String path) throws RepositoryException {
        if (!hasPrivileges(path, new Privilege[] {registry.getPrivilege(JCR_ADD_CHILD_NODES + "_" + session.getWorkspace().getName(), null)})) {
            throw new AccessDeniedException(path);
        }
    }
    //JCR_REMOVE_NODE
    public void checkRemoveNode(String path) throws RepositoryException {
        if (!hasPrivileges(path, new Privilege[] {registry.getPrivilege(JCR_REMOVE_NODE + "_" + session.getWorkspace().getName(), null)})) {
            throw new AccessDeniedException(path);
        }
    }

    public boolean canManageNodeTypes(String path) throws RepositoryException {
        return hasPrivileges(path, new Privilege[] {registry.getPrivilege(JCR_NODE_TYPE_MANAGEMENT + "_" + session.getWorkspace().getName(), null)});
    }

    private List<Privilege> getPrivilegesToFilter(Node node) {

        List<Privilege> privilegeToFilterOut = new ArrayList<>();

        // jcr:modifyAccessControl permission when data source is AccessControllable, only on ExternalNodeImpl
        // ExtensionNodeImpl acls can be modify
        if (aclReadOnly && node instanceof ExternalNodeImpl) {
            privilegeToFilterOut.add(modifyAccessControlPrivilege);
        }

        // all write permissions in case of the data source not writable and not extendable
        if (!writable && node instanceof ExternalNodeImpl && (session.getOverridableProperties() == null || session.getOverridableProperties().size() == 0)) {
            privilegeToFilterOut.add(writePrivilege);
            privilegeToFilterOut.addAll(Lists.newArrayList(writePrivilege.getAggregatePrivileges()));
        }

        return privilegeToFilterOut;
    }

    private static Privilege[] filterPrivileges(Privilege[] privileges, List<Privilege> privilegesToFilterOut) {

        Set<Privilege> filteredResult = new HashSet<Privilege>();

        for (Privilege privilege : privileges) {
            if (!privilegesToFilterOut.contains(privilege)) {
                if (privilege.isAggregate() && intersectionBetweenAggregatedAndList(privilege, privilegesToFilterOut)) {
                    filteredResult.addAll(Arrays.asList(filterPrivileges(privilege.getDeclaredAggregatePrivileges(), privilegesToFilterOut)));
                } else {
                    filteredResult.add(privilege);
                }
            }
        }

        return filteredResult.toArray(new Privilege[filteredResult.size()]);
    }

    /**
     * test if there is an intersection between the privilege aggregates and the other list
     * @param privilege the privilege that aggregate other privileges
     * @param privileges the list of privileges
     * @return true if the given privilege contains in it's aggregates at least one privilege from the list
     */
    private static boolean intersectionBetweenAggregatedAndList(Privilege privilege, List<Privilege> privileges) {
        if (privilege.isAggregate() && privilege.getAggregatePrivileges().length > 0) {
            for (Privilege subPrivilege : privilege.getAggregatePrivileges()) {
                if (privileges.contains(subPrivilege)) {
                    return true;
                }
            }
        }
        return false;
    }
}
