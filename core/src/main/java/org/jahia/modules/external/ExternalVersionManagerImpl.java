/**
 * This file is part of Jahia, next-generation open source CMS:
 * Jahia's next-generation, open source CMS stems from a widely acknowledged vision
 * of enterprise application convergence - web, search, document, social and portal -
 * unified by the simplicity of web content management.
 *
 * For more information, please visit http://www.jahia.com.
 *
 * Copyright (C) 2002-2014 Jahia Solutions Group SA. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * As a special exception to the terms and conditions of version 2.0 of
 * the GPL (or any later version), you may redistribute this Program in connection
 * with Free/Libre and Open Source Software ("FLOSS") applications as described
 * in Jahia's FLOSS exception. You should have received a copy of the text
 * describing the FLOSS exception, and it is also available here:
 * http://www.jahia.com/license
 *
 * Commercial and Supported Versions of the program (dual licensing):
 * alternatively, commercial and supported versions of the program may be used
 * in accordance with the terms and conditions contained in a separate
 * written agreement between you and Jahia Solutions Group SA.
 *
 * If you are unsure which license is appropriate for your use,
 * please contact the sales department at sales@jahia.com.
 */

package org.jahia.modules.external;

import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;

/**
 * Default implementation of VersionManager for External Repositories
 * User: david
 * Date: 2/26/13
 * Time: 12:09 PM
 */
public class ExternalVersionManagerImpl implements VersionManager {
    @Override
    public Version checkin(String absPath) throws VersionException, UnsupportedRepositoryOperationException, InvalidItemStateException, LockException, RepositoryException {
        return null;  
    }

    @Override
    public void checkout(String absPath) throws UnsupportedRepositoryOperationException, LockException, RepositoryException {
        
    }

    @Override
    public Version checkpoint(String absPath) throws VersionException, UnsupportedRepositoryOperationException, InvalidItemStateException, LockException, RepositoryException {
        return null;  
    }

    /**
     * {@inheritDoc}
     * @return always true because external providers is non-versionable
     */
    @Override
    public boolean isCheckedOut(String absPath) throws RepositoryException {
        return true;
    }

    @Override
    public VersionHistory getVersionHistory(String absPath) throws UnsupportedRepositoryOperationException, RepositoryException {
        return null;  
    }

    @Override
    public Version getBaseVersion(String absPath) throws UnsupportedRepositoryOperationException, RepositoryException {
        return null;  
    }

    @Override
    public void restore(Version[] versions, boolean removeExisting) throws ItemExistsException, UnsupportedRepositoryOperationException, VersionException, LockException, InvalidItemStateException, RepositoryException {
        
    }

    @Override
    public void restore(String absPath, String versionName, boolean removeExisting) throws VersionException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        
    }

    @Override
    public void restore(Version version, boolean removeExisting) throws VersionException, ItemExistsException, InvalidItemStateException, UnsupportedRepositoryOperationException, LockException, RepositoryException {
        
    }

    @Override
    public void restore(String absPath, Version version, boolean removeExisting) throws PathNotFoundException, ItemExistsException, VersionException, ConstraintViolationException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        
    }

    @Override
    public void restoreByLabel(String absPath, String versionLabel, boolean removeExisting) throws VersionException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        
    }

    @Override
    public NodeIterator merge(String absPath, String srcWorkspace, boolean bestEffort) throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
        return null;  
    }

    @Override
    public NodeIterator merge(String absPath, String srcWorkspace, boolean bestEffort, boolean isShallow) throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
        return null;  
    }

    @Override
    public void doneMerge(String absPath, Version version) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
        
    }

    @Override
    public void cancelMerge(String absPath, Version version) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
        
    }

    @Override
    public Node createConfiguration(String absPath) throws UnsupportedRepositoryOperationException, RepositoryException {
        return null;  
    }

    @Override
    public Node setActivity(Node activity) throws UnsupportedRepositoryOperationException, RepositoryException {
        return null;  
    }

    @Override
    public Node getActivity() throws UnsupportedRepositoryOperationException, RepositoryException {
        return null;  
    }

    @Override
    public Node createActivity(String title) throws UnsupportedRepositoryOperationException, RepositoryException {
        return null;  
    }

    @Override
    public void removeActivity(Node activityNode) throws UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        
    }

    @Override
    public NodeIterator merge(Node activityNode) throws VersionException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
        return null;  
    }
}
