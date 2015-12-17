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
 *     This program is free software; you can redistribute it and/or
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

import org.jahia.services.content.JCRStoreProvider;

import java.util.List;

import javax.jcr.RepositoryException;

/**
 * Shared service for external provider instances
 * Used for external content for maintaining internal to external ID mappings and provider IDs, and provide extension configuration.
 */
public interface ExternalProviderInitializerService {

    /**
     * Deletes mappings for the specified items.
     *
     * @param externalIds
     *            the list of external IDs to be removed from the mapping table
     * @param providerKey
     *            the corresponding provider key
     * @param includeDescendats
     *            if the external IDs are considered path-like (e.g. file system) and all the entries starting with those IDs should be also
     *            deleted (e.g. sub-folders)
     * @throws RepositoryException
     *             in case of a DB operation error
     */
    void delete(List<String> externalIds, String providerKey, boolean includeDescendats) throws RepositoryException;

    /**
     * Reads external identifier for the specified node via mapping table using internal UUID. If the mapping is not found <code>null</code>
     * is returned.
     *
     * @param internalId
     *            the internal UUID of the item
     * @return external identifier for the specified node via mapping table using internal UUID or <code>null</code> if the mapping cannot
     *         be found for this internal UUID
     * @throws RepositoryException
     *             in case an external identifier cannot be retrieved from the database
     */
    String getExternalIdentifier(String internalId) throws RepositoryException;

    /**
     * Reads internal UUID of the specified node via mapping table, using external ID and provider key.
     *
     * @param externalId
     *            the external ID to retrieve UUID for
     * @param providerKey
     *            the underlying provider key
     * @return an internal UUID of the specified node via mapping table or <code>null</code> if the mapping is not stored yet
     * @throws RepositoryException
     *             in case an internal identifier cannot be retrieved from the database or any other issue
     */
    String getInternalIdentifier(String externalId, String providerKey) throws RepositoryException;

    /**
     * Returns internal provider ID for the specified provider. If the provider is not registered yet, creates an ID for it and stores an
     * entry in the database.
     *
     * @param providerKey
     *            a unique provider key
     * @return the internal provider ID for the specified provider
     * @throws RepositoryException
     *             in case an ID cannot be generated for the provider
     */
    Integer getProviderId(String providerKey) throws RepositoryException;

    /**
     * Generates the internal UUID for the specified node in the mapping table, using external ID and provider key.
     *
     * @param externalId
     *            the external ID to generate UUID for
     * @param providerKey
     *            the underlying provider key
     * @param providerId
     *            the ID provider is using as a prefix for the UUIDs of nodes
     * @return an generated internal UUID
     * @throws RepositoryException
     *             in case an internal identifier cannot be stored into the database
     */
    String mapInternalIdentifier(String externalId, String providerKey, String providerId) throws RepositoryException;

    /**
     * Removes the provider entry from the DB table and also all the corresponding ID mappings.
     *
     * @param providerKey
     *            the provider key
     * @throws RepositoryException
     *             in case of a DB operation error
     */
    void removeProvider(String providerKey) throws RepositoryException;

    /**
     * Updates the external ID data for the specified entry, e.g. as a result of a move operation.
     *
     * @param oldExternalId
     *            the original external ID
     * @param newExternalId
     *            the new external ID to persist
     * @param providerKey
     *            the underlying provider key
     * @param includeDescendants
     *            if the external IDs are considered path-like (e.g. file system) and all the entries starting with those IDs should be also
     *            updated (e.g. sub-folders)
     * @throws RepositoryException
     *             in case of a DB operation failure
     */
    void updateExternalIdentifier(String oldExternalId, String newExternalId, String providerKey,
            boolean includeDescendants) throws RepositoryException;

    /**
     * @return the needed types to be override for locks.
     */
    List<String> getOverridableItemsForLocks();


    /**
     * @return the needed types to be override for ACLs.
     */
    List<String> getOverridableItemsForACLs();


    /**
     * @return the extension provider.
     */
    JCRStoreProvider getExtensionProvider();

}