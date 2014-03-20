/**
 * ==========================================================================================
 * =                        DIGITAL FACTORY v7.0 - Community Distribution                   =
 * ==========================================================================================
 *
 *     Rooted in Open Source CMS, Jahia's Digital Industrialization paradigm is about
 *     streamlining Enterprise digital projects across channels to truly control
 *     time-to-market and TCO, project after project.
 *     Putting an end to "the Tunnel effect", the Jahia Studio enables IT and
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
 *
 * JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION
 * ============================================
 *
 *     Copyright (C) 2002-2014 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==========================================================
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
 *     describing the FLOSS exception, and it is also available here:
 *     http://www.jahia.com/license"
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ==========================================================
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

import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of the {@link javax.jcr.Item} for the {@link org.jahia.modules.external.ExternalData}.
 * 
 * @author Thomas Draier
 */
public abstract class ExternalItemImpl implements Item {

    protected ExternalSessionImpl session;

    protected boolean isNew = false;

    public ExternalItemImpl(ExternalSessionImpl session) {
        this.session = session;
    }

    public ExternalSessionImpl getSession() {
        return session;
    }

    /**
     * Returns the underlying instance of the store provider.
     * 
     * @return the underlying instance of the store provider
     */
    protected ExternalContentStoreProvider getStoreProvider() {
        return getSession().getRepository().getStoreProvider();
    }
    
    public boolean isNode() {
        return false;  
    }

    void setNew(boolean isNew) {
        this.isNew = isNew;
    }

    public boolean isNew() {
        return isNew;
    }

    public boolean isModified() {
        return false;  
    }

    public boolean isSame(Item item) throws RepositoryException {
        return false;  
    }

    public void accept(ItemVisitor itemVisitor) throws RepositoryException {

    }

    @Override
    public Item getAncestor(int depth) throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        if (depth == 0) {
            return session.getItem("/");
        }
        Matcher matcher = Pattern.compile("(/[^/]+){" + depth + "}").matcher(getPath());
        if (matcher.find()) {
            return session.getItem(matcher.group(0));
        }
        throw new ItemNotFoundException();
    }

    @Override
    public int getDepth() throws RepositoryException {
        if (getPath().equals("/")) {
            return 0;
        }
        return getPath().split("/").length - 1;
    }

    /**
     * {@inheritDoc}
     */
    public void save() throws AccessDeniedException, ItemExistsException, ConstraintViolationException, InvalidItemStateException, ReferentialIntegrityException, VersionException, LockException, NoSuchNodeTypeException, RepositoryException {
        session.save();
    }

    /**
     * {@inheritDoc}
     */
    public void refresh(boolean b) throws InvalidItemStateException, RepositoryException {
        session.refresh(b);
    }

    /**
     * {@inheritDoc}
     */
    public void remove() throws VersionException, LockException, ConstraintViolationException, RepositoryException {

    }
}
