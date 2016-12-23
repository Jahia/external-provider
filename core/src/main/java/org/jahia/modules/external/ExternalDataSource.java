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

import java.util.List;
import java.util.Set;
import javax.jcr.Binary;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

/**
 * Interface to define an external DataSource to handle (@link org.jahia.services.content.impl.external.ExternalData}
 * a DataSource is used to declare a {@link org.jahia.services.content.JCRStoreProvider}
 * This is a simple way to create a new JCR Provider
 */
public interface ExternalDataSource {

    /**
     * If implemented, the data source can avoid to fill all properties values when getting the nodes. Instead, fill the
     * lazy*Properties fields of ExternalData to tell which properties can be queried on demand, and implement
     * the get*PropertyValues for these properties.
     */
    public interface LazyProperty {
        /**
         * Get values for a lazy property
         *
         * @param path         Path of the node
         * @param propertyName Name of the property to get
         * @return values
         * @throws PathNotFoundException
         */
        String[] getPropertyValues(String path, String propertyName) throws PathNotFoundException;

        /**
         * Get values for an lazy internationalized property
         *
         * @param path         Path of the node
         * @param propertyName Name of the property to get
         * @return values
         * @throws PathNotFoundException
         */
        String[] getI18nPropertyValues(String path, String lang, String propertyName) throws PathNotFoundException;

        /**
         * Get values for a lazy binary property
         *
         * @param path         Path of the node
         * @param propertyName Name of the property to get
         * @return values
         * @throws PathNotFoundException
         */
        Binary[] getBinaryPropertyValues(String path, String propertyName) throws PathNotFoundException;
    }

    /**
     * If implemented, this interface allow and defines search feature support.
     */
    public abstract interface Searchable {
        /**
         * Execute a search query
         *
         * @param query The JCR Query, can be parsed with
         * @return List of node path
         * @throws RepositoryException
         * @see org.jahia.modules.external.query.QueryHelper
         */
        List<String> search(ExternalQuery query) throws RepositoryException;
    }

    /**
     * If implemented, this interface allow and defines writing
     */
    public interface Writable {
        /**
         * moves ExternalData from oldPath to newPath
         *
         * @param oldPath source path
         * @param newPath destination path
         * @throws PathNotFoundException
         */
        void move(String oldPath, String newPath) throws RepositoryException;

        /**
         * reorder children nodes according to the list passed as parameter
         *
         * @param path
         * @param children
         * @throws RepositoryException
         */
        void order(String path, List<String> children) throws RepositoryException;

        /**
         * Delete an item
         *
         * @param path path of the item to delete
         * @throws PathNotFoundException
         */
        void removeItemByPath(String path) throws RepositoryException;

        /**
         * saves the data
         *
         * @param data ExternalData to save
         * @throws PathNotFoundException
         */
        void saveItem(ExternalData data) throws RepositoryException;
    }

    /**
     * Implemented by a service that needs initialization/finalization logic during service life cycle.
     */
    public interface Initializable {
        /**
         * Called when the external data source provider is initializing.
         */
        void start();

        /**
         * Called when the external data source provider is stopping.
         */
        void stop();
    }

    /**
     * If implemented, allows for access control using ACL on ExternalData directly
     *
     * AccessControllable interface is not usable with SupportPrivileges
     */
    public interface AccessControllable {
    }

    /**
     * If implemented, allows for access control using getPrivilegesNames()
     * returning privilege names directly for a given path.
     */
    public interface SupportPrivileges {
        /**
         * Return the privileges a user has on a specified node
         *
         * @param username the user name
         * @param path     the node path
         * @return an array of privilege names
         */
        String[] getPrivilegesNames(String username, String path);
    }

    /**
     * If implemented, allows to get weak references
     */
    public interface Referenceable {

        /**
         * Given a node identifier, returns the paths of properties referring this node
         *
         * @param identifier   the node identifier. If the node is in this external provider, it's an external ID
         *                     (local ID in the provider). Else, it's an internal ID (global).
         * @param propertyName the property name (optional)
         * @return a list of property paths
         */
        List<String> getReferringProperties(String identifier, String propertyName);
    }

    /**
     * If implemented, allows for batch access to children data.
     */
    interface CanLoadChildrenInBatch {
        /**
         * Retrieves the children of the node represented by this ExternalDataSource, located at the specified path, as ExternalData elements.
         *
         * @param path the path from where to get children
         * @return a list of children located at the specified path, represented by ExternalData elements
         */
        List<ExternalData> getChildrenNodes(String path) throws RepositoryException;
    }

    /**
     * If implemented, allows to check availability of the provider when the "/" node is read
     */
    interface CanCheckAvailability {
        /**
         * @return true if the provider is correctly available
         * can return false or throw a RepositoryException with a custom message in case of provider unavailable
         * @throws RepositoryException
         */
        boolean isAvailable() throws RepositoryException;
    }

    /**
     * @param path path where to get children
     * @return list of paths as String
     */
    List<String> getChildren(String path) throws RepositoryException;

    /**
     * identifier is unique for an ExternalData
     *
     * @param identifier
     * @return ExternalData defined by the identifier
     * @throws ItemNotFoundException
     */
    ExternalData getItemByIdentifier(String identifier) throws ItemNotFoundException;

    /**
     * As getItemByIdentifier, get an ExternalData by its path
     *
     * @param path
     * @return ExternalData
     * @throws PathNotFoundException
     */
    ExternalData getItemByPath(String path) throws PathNotFoundException;

    /**
     * Returns a set of supported node types.
     *
     * @return a set of supported node types
     */
    Set<String> getSupportedNodeTypes();

    /**
     * Indicates if this data source has path-like hierarchical external identifiers, e.g. IDs that are using file system paths.
     *
     * @return <code>true</code> if this data source has path-like hierarchical external identifiers, e.g. IDs that are using file system
     * paths; <code>false</code> otherwise.
     */
    boolean isSupportsHierarchicalIdentifiers();

    /**
     * Indicates if the data source supports UUIDs.
     *
     * @return <code>true</code> if the data source supports UUIDs
     */
    boolean isSupportsUuid();

    /**
     * Returns <code>true</code> if an item exists at <code>path</code>; otherwise returns <code>false</code>.
     *
     * @param path item path
     * @return <code>true</code> if an item exists at <code>path</code>; otherwise returns <code>false</code>
     */
    boolean itemExists(String path);

}
