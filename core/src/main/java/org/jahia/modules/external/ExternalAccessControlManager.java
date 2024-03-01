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
package org.jahia.modules.external;

import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.commons.iterator.AccessControlPolicyIteratorAdapter;
import org.apache.jackrabbit.core.security.JahiaLoginModule;
import org.apache.jackrabbit.core.security.JahiaPrivilegeRegistry;
import org.jahia.api.Constants;
import org.jahia.exceptions.JahiaRuntimeException;
import org.jahia.jaas.JahiaPrincipal;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.jahia.settings.SettingsBean;
import org.jahia.utils.security.AccessManagerUtils;
import org.jahia.utils.security.PathWrapper;

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
    private static final String SYSTEM_USER = " system ";

    private Map<String, Boolean> pathPermissionCache = null;
    private Map<Object, AccessManagerUtils.CompiledAcl> compiledAcls = new HashMap<>();

    private JahiaPrivilegeRegistry registry;

    private final ExternalSessionImpl session;
    private final String workspaceName;
    private final JahiaPrincipal jahiaPrincipal;
    private final boolean aclReadOnly;
    private final boolean writable;
    private final Privilege modifyAccessControlPrivilege;
    private final Privilege writePrivilege;

    // reserved to privileges support
    private final String rootUserName;
    private final boolean supportPrivileges;
    private final ExternalDataSource dataSource;

    public ExternalAccessControlManager(NamespaceRegistry namespaceRegistry, ExternalSessionImpl session, ExternalDataSource dataSource) {

        this.session = session;
        this.workspaceName = session.getWorkspace().getName();
        this.aclReadOnly = dataSource instanceof ExternalDataSource.AccessControllable || dataSource instanceof ExternalDataSource.SupportPrivileges;
        this.writable = dataSource instanceof ExternalDataSource.Writable;
        this.supportPrivileges = dataSource instanceof ExternalDataSource.SupportPrivileges;
        this.rootUserName = JahiaUserManagerService.getInstance().getRootUserName();
        this.dataSource = dataSource;

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
        Privilege[] privileges;
        if (supportPrivileges) {
            privileges = getPrivilegesLegacy(absPath);
        } else {
            privileges = AccessManagerUtils.getPrivileges(node, jahiaPrincipal, registry);
        }
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

        // check ACLs
        Set<String> privs = new HashSet<>();
        for (Privilege privilege : privileges) {
            privs.add(privilege.getName());
        }
        String mountPoint = session.getRepository().getStoreProvider().getMountPoint();
        Session securitySession = (session.getUserID().startsWith(SYSTEM_USER)) ? session
                : JCRSessionFactory.getInstance().getCurrentSystemSession(session.getWorkspace().getName(), null, null);
        PathWrapper pathWrapper = new ExternalPathWrapperImpl(StringUtils.equals(absPath, "/") ? mountPoint : mountPoint + absPath, securitySession);
        boolean isGranted = AccessManagerUtils.isGranted(pathWrapper, privs, securitySession,
                jahiaPrincipal, workspaceName, false, pathPermissionCache, compiledAcls, registry);

        if (supportPrivileges) {
            // if the node is created in the same session, return true
            for (Item item : session.getNewItems()) {
                if (item.getPath().equals(absPath)) {
                    return true;
                }
            }
            // check privilege names
            isGranted = isGranted && hasPrivilegesLegacy(absPath, privileges);
        }

        return isGranted;
    }

    private Privilege[] getPrivilegesLegacy(String absPath) throws PathNotFoundException,
            RepositoryException {
        List<Privilege> l = new ArrayList<Privilege>();
        for (String s : getPrivilegesNamesLegacy(absPath)) {
            Privilege privilege = registry.getPrivilege(s, null);
            if (privilege != null) {
                l.add(privilege);
            }
        }

        return l.toArray(new Privilege[l.size()]);
    }

    private String[] getPrivilegesNamesLegacy(String absPath) {
        ExternalContentStoreProvider.setCurrentSession(session);
        try {
            return ((ExternalDataSource.SupportPrivileges) dataSource).getPrivilegesNames(session.getUserID(), absPath);
        } finally {
            ExternalContentStoreProvider.removeCurrentSession();
        }
    }

    private boolean hasPrivilegesLegacy(String absPath, Privilege[] privileges)
            throws PathNotFoundException, RepositoryException {

        // if no privilege set, return true
        if (privileges == null || privileges.length == 0) {
            return true;
        }

        // if root or system session return true
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
        if (!hasPrivileges(path, new Privilege[]{registry.getPrivilege(JCR_ADD_CHILD_NODES + "_" + session.getWorkspace().getName(), null)})) {
            throw new AccessDeniedException(path);
        }
    }

    //JCR_REMOVE_NODE
    public void checkRemoveNode(String path) throws RepositoryException {
        if (!hasPrivileges(path, new Privilege[]{registry.getPrivilege(JCR_REMOVE_NODE + "_" + session.getWorkspace().getName(), null)})) {
            throw new AccessDeniedException(path);
        }
    }

    public boolean canManageNodeTypes(String path) throws RepositoryException {
        return hasPrivileges(path, new Privilege[]{registry.getPrivilege(JCR_NODE_TYPE_MANAGEMENT + "_" + session.getWorkspace().getName(), null)});
    }

    private List<Privilege> getPrivilegesToFilter(Node node) {

        if (node instanceof JCRNodeWrapper) {
            node = ((JCRNodeWrapper) node).getRealNode();
        }

        List<Privilege> privilegeToFilterOut = new ArrayList<>();

        // jcr:modifyAccessControl permission when data source is AccessControllable, only on ExternalNodeImpl
        // ExtensionNodeImpl acls can be modify
        if (aclReadOnly && node instanceof ExternalNodeImpl) {
            privilegeToFilterOut.add(modifyAccessControlPrivilege);
        }

        // all write permissions in case of the data source not writable and not extendable
        if (!writable && node instanceof ExternalNodeImpl && (session.getOverridableProperties() == null || session.getOverridableProperties().size() == 0)) {
            privilegeToFilterOut.add(writePrivilege);
            privilegeToFilterOut.addAll(List.of(writePrivilege.getAggregatePrivileges()));
        }

        return privilegeToFilterOut;
    }

    private static Privilege[] filterPrivileges(Privilege[] privileges, List<Privilege> privilegesToFilterOut) {

        Set<Privilege> filteredResult = new HashSet<Privilege>();
        for (Privilege privilege : privileges) {
            if (!privilegesToFilterOut.contains(privilege)) {
                if (privilege.isAggregate() && areIntersecting(privilege.getDeclaredAggregatePrivileges(), privilegesToFilterOut)) {
                    // We de-aggregate a privilege in case any of its children are to be filtered out, since a privilege is valid only if all its children are valid.
                    filteredResult.addAll(Arrays.asList(filterPrivileges(privilege.getDeclaredAggregatePrivileges(), privilegesToFilterOut)));
                } else {
                    filteredResult.add(privilege);
                }
            }
        }

        return filteredResult.toArray(new Privilege[filteredResult.size()]);
    }

    /**
     * Test if there is an intersection between the two arrays of privileges
     * @param privileges1
     * @param privileges2
     * @return whether the two arrays contain any common element
     */
    private static boolean areIntersecting(Privilege[] privileges1, List<Privilege> privileges2) {
        for (Privilege privilege1 : privileges1) {
            if (privileges2.contains(privilege1)) {
                return true;
            }
        }
        return false;
    }
}
