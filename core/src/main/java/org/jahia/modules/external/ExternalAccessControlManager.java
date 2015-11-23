/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2015 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ======================================================================================
 *
 *     IF YOU DECIDE TO CHOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
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
 *     ======================================================================================
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
 *
 * ==========================================================================================
 * =                                   ABOUT JAHIA                                          =
 * ==========================================================================================
 *
 *     Rooted in Open Source CMS, Jahia’s Digital Industrialization paradigm is about
 *     streamlining Enterprise digital projects across channels to truly control
 *     time-to-market and TCO, project after project.
 *     Putting an end to “the Tunnel effect”, the Jahia Studio enables IT and
 *     marketing teams to collaboratively and iteratively build cutting-edge
 *     online business solutions.
 *     These, in turn, are securely and easily deployed as modules and apps,
 *     reusable across any digital projects, thanks to the Jahia Private App Store Software.
 *     Each solution provided by Jahia stems from this overarching vision:
 *     Digital Factory, Workspace Factory, Portal Factory and eCommerce Factory.
 *     Founded in 2002 and headquartered in Geneva, Switzerland,
 *     Jahia Solutions Group has its North American headquarters in Washington DC,
 *     with offices in Chicago, Toronto and throughout Europe.
 *     Jahia counts hundreds of global brands and governmental organizations
 *     among its loyal customers, in more than 20 countries across the globe.
 *
 *     For more information, please visit http://www.jahia.com
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

    public ExternalAccessControlManager(NamespaceRegistry namespaceRegistry, ExternalSessionImpl session, ExternalDataSource dataSource) {
        super();
        this.session = session;
        this.workspaceName = session.getWorkspace().getName();
        this.aclReadOnly = dataSource instanceof ExternalDataSource.AccessControllable;
        this.writable = dataSource instanceof ExternalDataSource.Writable;

        jahiaPrincipal = new JahiaPrincipal(session.getUserID(), session.getRealm(), session.getUserID().startsWith(JahiaLoginModule.SYSTEM), JahiaLoginModule.GUEST.equals(session.getUserID()));
        try {
            init(namespaceRegistry);
        } catch (RepositoryException e) {
            throw new JahiaRuntimeException(e);
        }
    }

    private void init(NamespaceRegistry namespaceRegistry) throws RepositoryException {
        pathPermissionCache = Collections.synchronizedMap(new LRUMap(SettingsBean.getInstance().getAccessManagerPathPermissionCacheMaxSize()));
        registry = new JahiaPrivilegeRegistry(namespaceRegistry);
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

    public Privilege[] getSupportedPrivileges(String absPath) throws PathNotFoundException,
            RepositoryException {
        return JahiaPrivilegeRegistry.getRegisteredPrivileges();
    }

    public Privilege[] getPrivileges(final String absPath) throws PathNotFoundException,
            RepositoryException {
        JCRNodeWrapper node = JCRSessionFactory.getInstance().getCurrentSystemSession(workspaceName, null, null)
                .getNode(session.getRepository().getStoreProvider().getMountPoint() + absPath);
        Privilege[] privileges = AccessManagerUtils.getPrivileges(node, jahiaPrincipal, registry);

        // remove jcr:modifyAccessControl permission when data source is AccessControllable, only on ExternalNodeImpl
        // ExtensionNodeImpl acls can be modify
        boolean removeModifyAccessControl = aclReadOnly && node.getRealNode() instanceof ExternalNodeImpl;
        // remove all write permissions in case of the data source not writable and not extendable
        boolean removeAllWrite = !writable && node.getRealNode() instanceof ExternalNodeImpl &&
                (session.getOverridableProperties() == null || session.getOverridableProperties().size() == 0);
        if (removeModifyAccessControl || removeAllWrite) {
            List<Privilege> privilegeToFilterOut = new ArrayList<>();
            if(removeModifyAccessControl) {
                privilegeToFilterOut.add(registry.getPrivilege("jcr:modifyAccessControl", workspaceName));
            }
            if(removeAllWrite) {
                Privilege writePrivilege = registry.getPrivilege("jcr:write", workspaceName);
                privilegeToFilterOut.add(writePrivilege);
                privilegeToFilterOut.addAll(Lists.newArrayList(writePrivilege.getAggregatePrivileges()));
            }
            return filterPrivileges(privileges, privilegeToFilterOut);
        } else {
            return privileges;
        }
    }

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
        if (hasPrivileges(path, new Privilege[] {registry.getPrivilege(JCR_REMOVE_NODE + "_" + session.getWorkspace().getName(), null)})) {
            throw new AccessDeniedException(path);
        }
    }

    public boolean canManageNodeTypes(String path) throws RepositoryException {
        return hasPrivileges(path, new Privilege[] {registry.getPrivilege(JCR_NODE_TYPE_MANAGEMENT + "_" + session.getWorkspace().getName(), null)});
    }

    private Privilege[] filterPrivileges (Privilege[] privileges, List<Privilege> privilegesTofilter) throws RepositoryException {
        List<Privilege> privilegesList = Lists.newArrayList(privileges);
        Set<Privilege> filteredResult = new HashSet<>();

        for (Privilege privilege : privilegesList) {
            if(!privilegesTofilter.contains(privilege)) {
                if(privilege.isAggregate()) {
                    List<Privilege> aggregatedPrivileges = Lists.newArrayList(privilege.getAggregatePrivileges());
                    aggregatedPrivileges.removeAll(privilegesTofilter);
                    if(aggregatedPrivileges.size() == privilege.getAggregatePrivileges().length) {
                        filteredResult.add(privilege);
                    } else {
                        filteredResult.addAll(aggregatedPrivileges);
                    }
                } else {
                    filteredResult.add(privilege);
                }
            }
        }

        return filteredResult.toArray(new Privilege[filteredResult.size()]);
    }
}
