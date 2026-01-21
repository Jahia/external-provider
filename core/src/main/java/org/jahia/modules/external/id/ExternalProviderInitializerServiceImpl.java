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
package org.jahia.modules.external.id;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jahia.modules.external.ExternalProviderInitializerService;
import org.jahia.services.cache.ehcache.EhCacheProvider;
import org.jahia.services.content.JCRStoreProvider;
import org.jahia.utils.DatabaseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings({"java:S1141"}) // Suppress "nested try-catch" due to connection management it's necessary here
public class ExternalProviderInitializerServiceImpl implements ExternalProviderInitializerService {

    private static final String ID_CACHE_NAME = "ExternalIdentifierMapping";

    private static final Logger logger = LoggerFactory.getLogger(ExternalProviderInitializerServiceImpl.class);

    private DataSource datasource;

    // The ID mapping cache, where a key is a <providerKey>-<externalId> and a value is
    // the corresponding internalId
    private Cache idCache;

    private List<String> overridableItemsForLocks;

    private List<String> overridableItemsForACLs;

    private JCRStoreProvider extensionProvider;


    @Override
    @SuppressWarnings("java:S6912") // Suppress the "Use batch processing" warning, it's used properly
    public void delete(List<String> externalIds, String providerKey, boolean includeDescendants)
            throws RepositoryException {
        if (externalIds.isEmpty()) {
            return;
        }
        if (externalIds.size() > 1000) {
            throw new RepositoryException("External provider can delete only 1000 items at a time");
        }

        Set<String> internalIdsToDelete = new HashSet<>();
        List<String> invalidate = new ArrayList<>();
        boolean isPostgreSQL = DatabaseUtils.getDatabaseType() == DatabaseUtils.DatabaseType.postgresql;

        try (Connection connection = datasource.getConnection()) {
            connection.setAutoCommit(false);

            try {
                Set<String> externalIdsSet = new HashSet<>(externalIds); // Deduplicate input
                // PHASE 1: Collect exact matches for the provided external IDs
                collectMappings(externalIdsSet, connection, providerKey, (internalId, externalId) -> {
                    invalidate.add(externalId);
                    internalIdsToDelete.add(internalId);
                });

                // PHASE 2: Collect descendants if requested
                // This happens when the external data source is using path like external IDs
                if (includeDescendants) {
                    collectDescendants(externalIdsSet, isPostgreSQL, connection, providerKey,
                            (internalId, externalId) -> {
                                invalidate.add(externalId);
                                internalIdsToDelete.add(internalId);
                            });
                }

                // PHASE 3: Perform single batch deletion for all collected items
                if (!internalIdsToDelete.isEmpty()) {
                    performDelete(internalIdsToDelete, connection, providerKey);
                }

                connection.commit();
            } catch (SQLException | IOException e) {
                tryRollBack(connection, providerKey, e);
                throw new RepositoryException(
                        "Issue when deleting mapping for external provider " + providerKey, e);
            }
        } catch (SQLException e) {
            // Connection acquisition failed - no rollback possible
            throw new RepositoryException(
                    "Failed to acquire database connection for external provider " + providerKey, e);
        }

        // Invalidate cache for all deleted items
        for (String id : invalidate) {
            this.invalidateCache(id, providerKey);
        }
    }

    private String getCacheKey(String externalId, String providerKey) {
        return providerKey + "-" + externalId;
    }

    @Override
    public String getExternalIdentifier(String internalId) throws RepositoryException {
        String externalId = null;
        String query = "SELECT externalId FROM jahia_external_mapping WHERE internalUuid=?";
        try (Connection connection = this.datasource.getConnection(); PreparedStatement pstmt = connection.prepareStatement(query)) {
            // No transaction needed for read-only query
            pstmt.setString(1, internalId);
            try (ResultSet resultSet = pstmt.executeQuery()) {
                while (resultSet.next()) {
                    externalId = getExternalId(resultSet, 1);
                }
            } catch (SQLException | IOException e) {
                throw new RepositoryException("Issue when obtaining external provider ID for internal identfier " + internalId, e);
            }

        } catch (SQLException e) {
            throw new RepositoryException("Issue when obtaining external provider ID for internal identfier " + internalId, e);
        }

        return externalId;
    }

    private String getExternalId(ResultSet resultSet, int columnIndex) throws IOException, SQLException {
        return IOUtils.toString(resultSet.getClob(columnIndex).getCharacterStream());
    }

    @Override
    public String getInternalIdentifier(String externalId, String providerKey) throws RepositoryException {

        String cacheKey = getCacheKey(externalId, providerKey);
        Element cacheElement = idCache.get(cacheKey);

        // Check cache for existing mapping
        if (cacheElement != null) {
            return (String) cacheElement.getObjectValue();
        }

        String uuid = null;
        String query = "SELECT externalId, internalUuid FROM jahia_external_mapping WHERE providerKey=? and externalIdHash=?";
        try (Connection connection = this.datasource.getConnection(); PreparedStatement statement = connection.prepareStatement(query)) {
            // No transaction needed for read-only query
            statement.setString(1, providerKey);
            statement.setLong(2, externalId.hashCode());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String rowExternalId = getExternalId(resultSet, 1);
                    if (rowExternalId.equals(externalId)) {
                        uuid = resultSet.getString(2);
                        // Cache the found UUID
                        idCache.put(new Element(cacheKey, uuid, true));
                        break; // Found match, exit loop
                    }
                }
            } catch (SQLException | IOException e) {
                throw new RepositoryException("Issue when obtaining internal identifier for provider" + providerKey, e);
            }
        } catch (SQLException e) {
            throw new RepositoryException("Issue when obtaining internal identifier for provider" + providerKey, e);
        }

        // DO NOT cache null (not found) results - they will be created immediately
        // by the calling code in getOrCreateInternalIdentifier()
        return uuid;
    }

    @Override
    public Integer getProviderId(String providerKey) throws RepositoryException {
        // First, check if the providerKey exists in the DB
        Integer existingId = findProviderId(providerKey);
        if (existingId != null) {
            return existingId;
        }

        // Insert the new providerKey
        return insertProviderKey(providerKey);
    }

    private Integer findProviderId(String providerKey) throws RepositoryException {
        try (Connection connection = this.datasource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id FROM jahia_external_provider_id WHERE providerKey=?")) {
            statement.setString(1, providerKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error querying provider id for key: " + providerKey, e);
        }
        return null;
    }


    private Integer insertProviderKey(String providerKey) throws RepositoryException {
        boolean isOracle = DatabaseUtils.getDatabaseType().equals(DatabaseUtils.DatabaseType.oracle);
        try (Connection connection = this.datasource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     getInsertNewProviderStatement(), Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, providerKey);
            int rowAffected = statement.executeUpdate();
            if (rowAffected == 1 && !isOracle) {
                try (ResultSet resultSet = statement.getGeneratedKeys()) {
                    if (resultSet.next()) {
                        return resultSet.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            // Retry with a select, could have been rejected due to a previous concurrent insert
            // (In cluster mode multiple jahia instances trying to insert the same providerKey for example)
            Integer concurrentlyInsertedId = findProviderId(providerKey);
            if (concurrentlyInsertedId != null) {
                return concurrentlyInsertedId;
            }
            throw new RepositoryException("Error inserting provider id for key: " + providerKey, e);
        }

        if (isOracle) {
            // For Oracle, retry finding the key as it doesn't support RETURN_GENERATED_KEYS reliably
            return findProviderId(providerKey);
        }
        throw new RepositoryException("Failed to insert provider id for key: " + providerKey);
    }

    private static String getInsertNewProviderStatement() throws RepositoryException {
        boolean isUsingSequence = Stream.of(DatabaseUtils.DatabaseType.oracle, DatabaseUtils.DatabaseType.postgresql).anyMatch(databaseType -> databaseType == DatabaseUtils.getDatabaseType());
        String insertNewProvider = "INSERT INTO jahia_external_provider_id(providerKey) VALUES (?)";
        if (isUsingSequence) {
            switch (DatabaseUtils.getDatabaseType()) {
                case postgresql:
                    insertNewProvider = "INSERT INTO jahia_external_provider_id(id, providerKey) VALUES (nextval('jahia_external_provider_id_seq'), ?)";
                    break;
                case oracle:
                    insertNewProvider = "INSERT INTO jahia_external_provider_id(id, providerKey) VALUES (jahia_external_provider_id_seq.nextval, ?)";
                    break;
                default:
                    throw new RepositoryException("Unsupported database type " + DatabaseUtils.getDatabaseType());
            }
        }
        return insertNewProvider;
    }

    private void invalidateCache(String externalId, String providerKey) {
        idCache.remove(getCacheKey(externalId, providerKey));
    }

    @Override
    public String mapInternalIdentifier(String externalId, String providerKey, String providerId)
            throws RepositoryException {
        // Generate a new internal UUID
        String internalUuid = providerId + "-" + StringUtils.substringAfter(UUID.randomUUID().toString(), "-");

        boolean isPostgreSQL = DatabaseUtils.getDatabaseType() == DatabaseUtils.DatabaseType.postgresql;
        String insertNewMapping = isPostgreSQL ?
                "INSERT INTO jahia_external_mapping(providerKey, externalId, externalIdHash, internalUuid) values (?,lo_from_bytea(0, ?),?,?)" :
                "INSERT INTO jahia_external_mapping(providerKey, externalId, externalIdHash, internalUuid) values (?,?,?,?)";
        try (Connection connection = datasource.getConnection();
             PreparedStatement statement = connection.prepareStatement(insertNewMapping)) {
            statement.setString(1, providerKey);
            if (isPostgreSQL) {
                statement.setBytes(2, externalId.getBytes(StandardCharsets.UTF_8));
            } else {
                statement.setClob(2, new StringReader(externalId));
            }
            statement.setLong(3, externalId.hashCode());
            statement.setString(4, internalUuid);
            int rowAffected = statement.executeUpdate();
            if (rowAffected == 1) {
                // Cache the newly created mapping immediately
                String cacheKey = getCacheKey(externalId, providerKey);
                idCache.put(new Element(cacheKey, internalUuid, true));
                return internalUuid;
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error storing mapping for external node " + externalId + " [provider: "
                    + providerKey + "]", e);
        }
        throw new RepositoryException("Error storing mapping for external node " + externalId + " [provider: "
                + providerKey + "]");
    }

    @Override
    public void removeProvider(String providerKey) throws RepositoryException {
        String deleteExternalProvider = "DELETE FROM jahia_external_provider_id WHERE providerKey=?";
        String deleteMappings = "DELETE FROM jahia_external_mapping WHERE providerKey=?";

        try (Connection connection = datasource.getConnection()) {
            connection.setAutoCommit(false);

            try {
                int deletedProviderCount;
                int deletedMappingsCount;

                // Delete from provider table
                try (PreparedStatement statement = connection.prepareStatement(deleteExternalProvider)) {
                    statement.setString(1, providerKey);
                    deletedProviderCount = statement.executeUpdate();
                }

                // Delete from mappings table
                try (PreparedStatement statement = connection.prepareStatement(deleteMappings)) {
                    statement.setString(1, providerKey);
                    deletedMappingsCount = statement.executeUpdate();
                }

                connection.commit();

                if (deletedProviderCount > 0) {
                    logger.info("Deleted {} external provider entry for key {}", deletedProviderCount, providerKey);
                    logger.info("Deleted {} identifier mapping entries for external provider with key {}",
                            deletedMappingsCount, providerKey);
                } else {
                    logger.info("No external provider entry found for key {}", providerKey);
                }

            } catch (SQLException e) {
                tryRollBack(connection, providerKey, e);
                throw new RepositoryException(
                        "Issue when removing external provider entry and identifier mappings for provider key " + providerKey, e);
            }
        } catch (SQLException e) {
            // Connection acquisition failed - no rollback possible
            throw new RepositoryException(
                    "Failed to acquire database connection for external provider " + providerKey, e);
        }
    }

    public void setDatasource(DataSource datasource) {
        this.datasource = datasource;
    }

    public void setCacheProvider(EhCacheProvider cacheProvider) {
        idCache = cacheProvider.getCacheManager().getCache(ExternalProviderInitializerServiceImpl.ID_CACHE_NAME);
        if (null == idCache) {
            cacheProvider.getCacheManager().addCache(ExternalProviderInitializerServiceImpl.ID_CACHE_NAME);
            idCache = cacheProvider.getCacheManager().getCache(ExternalProviderInitializerServiceImpl.ID_CACHE_NAME);
        }
    }

    @Override
    public void updateExternalIdentifier(String oldExternalId, String newExternalId, String providerKey,
                                         boolean includeDescendants) throws RepositoryException {
        boolean isPostgreSQL = DatabaseUtils.getDatabaseType() == DatabaseUtils.DatabaseType.postgresql;
        List<String> invalidate = new ArrayList<>();
        // Map of internalUuid -> newExternalId for all items to update
        Map<String, String> updatesToApply = new HashMap<>();

        try (Connection connection = datasource.getConnection()) {
            connection.setAutoCommit(false);

            try {
                // PHASE 1: Collect exact match for the old external ID
                collectMappings(Collections.singleton(oldExternalId), connection, providerKey, (internalId, externalId) -> {
                    invalidate.add(externalId);
                    updatesToApply.put(internalId, newExternalId);
                });

                // PHASE 2: Collect descendants if requested
                if (includeDescendants) {
                    collectDescendants(Collections.singleton(oldExternalId), isPostgreSQL, connection, providerKey,
                            (internalId, externalId) -> {
                                invalidate.add(externalId);
                                // Replace old prefix with new prefix
                                String updatedExternalId = newExternalId + StringUtils.substringAfter(externalId, oldExternalId);
                                updatesToApply.put(internalId, updatedExternalId);
                            });
                }

                // PHASE 3: Perform batch update for all collected items
                if (!updatesToApply.isEmpty()) {
                    performUpdateExternalIdentifier(updatesToApply, isPostgreSQL, connection, providerKey);
                }

                connection.commit();
            } catch (SQLException | IOException e) {
                tryRollBack(connection, providerKey, e);
                throw new RepositoryException(
                        "Issue when updating mapping for external provider " + providerKey, e);
            }
        } catch (SQLException e) {
            // Connection acquisition failed - no rollback possible
            throw new RepositoryException(
                    "Failed to acquire database connection for external provider " + providerKey, e);
        }

        // Invalidate cache for all updated items
        for (String id : invalidate) {
            this.invalidateCache(id, providerKey);
        }
    }

    private void performUpdateExternalIdentifier(Map<String, String> updatesToApply, boolean isPostgreSQL, Connection connection, String providerKey) throws SQLException {
        String updateMapping = isPostgreSQL ?
                "UPDATE jahia_external_mapping SET externalId=lo_from_bytea(0, ?), externalIdHash=? WHERE internalUuid=?" :
                "UPDATE jahia_external_mapping SET externalId=?, externalIdHash=? WHERE internalUuid=?";
        boolean supportsBatch = connection.getMetaData().supportsBatchUpdates();

        try (PreparedStatement updateStatement = connection.prepareStatement(updateMapping)) {
            if (connection.getMetaData().supportsBatchUpdates()) {
                for (Map.Entry<String, String> entry : updatesToApply.entrySet()) {
                    String internalId = entry.getKey();
                    String updatedExternalId = entry.getValue();

                    if (isPostgreSQL) {
                        updateStatement.setBytes(1, updatedExternalId.getBytes(StandardCharsets.UTF_8));
                    } else {
                        updateStatement.setClob(1, new StringReader(updatedExternalId));
                    }
                    updateStatement.setLong(2, updatedExternalId.hashCode());
                    updateStatement.setString(3, internalId);
                    if (supportsBatch) {
                        updateStatement.addBatch();
                    } else  {
                        updateStatement.executeUpdate();
                    }
                }
                if (supportsBatch) {
                    updateStatement.executeBatch();
                    logger.debug("Batch updated {} mappings for provider {}", updatesToApply.size(), providerKey);
                } else  {
                    logger.warn("Batch updates not supported by JDBC driver, falling back to individual updates");
                }
            }
        }
    }

    private void performDelete(Set<String> internalIdsToDelete, Connection connection, String providerKey) throws SQLException {
        boolean supportsBatch = connection.getMetaData().supportsBatchUpdates();
        String deleteMapping = "DELETE FROM jahia_external_mapping WHERE internalUuid=?";
        try (PreparedStatement deleteStatement = connection.prepareStatement(deleteMapping)) {
            for (String internalId : internalIdsToDelete) {
                deleteStatement.setString(1, internalId);
                if (supportsBatch) {
                    deleteStatement.addBatch();
                } else   {
                    deleteStatement.executeUpdate();
                }
            }
            if (supportsBatch) {
                logger.debug("Batch deleted {} mappings for provider {}", internalIdsToDelete.size(), providerKey);
                deleteStatement.executeBatch();
            } else {
                logger.warn("Batch updates not supported by JDBC driver, falling back to {} individual deletes for mappings for provider {}", internalIdsToDelete.size(), providerKey);
            }
        }
    }

    private void collectMappings(Set<String> externalIdsSet, Connection connection, String providerKey, BiConsumer<String, String> processMapping) throws SQLException, IOException {
        List<Integer> hashes = new LinkedList<>();
        for (String externalId : externalIdsSet) {
            int hash = externalId.hashCode();
            if (!hashes.contains(hash)) {
                hashes.add(hash);
            }
        }

        String selectMappingTemplate = "SELECT internalUuid, externalId FROM jahia_external_mapping WHERE providerKey=? AND externalIdHash in (:listPlaceHolder)";
        String selectMapping = selectMappingTemplate.replace(":listPlaceHolder", hashes.stream().map(integer -> "?").collect(Collectors.joining(",")));

        try (PreparedStatement statement = connection.prepareStatement(selectMapping)) {
            statement.setString(1, providerKey);
            int columnIndex = 2;
            for (Integer hash : hashes) {
                statement.setInt(columnIndex++, hash);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String externalId = getExternalId(resultSet, 2);
                    if (externalIdsSet.contains(externalId)) {
                        processMapping.accept(resultSet.getString(1), externalId);
                    }
                }
            }
        }
    }

    private void collectDescendants(Set<String> externalIdsSet, boolean isPostgreSQL, Connection connection, String providerKey, BiConsumer<String, String> processDescendant) throws SQLException, IOException {
        String selectDescendantMapping = isPostgreSQL
                ? "SELECT internalUuid, externalId FROM jahia_external_mapping WHERE providerKey = ? AND convert_from(lo_get(cast(externalId as bigint)), 'UTF8') LIKE ?"
                : "SELECT internalUuid, externalId FROM jahia_external_mapping WHERE providerKey = ? AND externalId LIKE ?";

        // Reuse PreparedStatement for all external IDs
        try (PreparedStatement statement = connection.prepareStatement(selectDescendantMapping)) {
            statement.setString(1, providerKey); // Set once - same for all iterations

            for (String externalId : externalIdsSet) {
                statement.setString(2, externalId + "/%"); // Only varies per iteration

                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        // internalId/externalId
                        processDescendant.accept(resultSet.getString(1), getExternalId(resultSet, 2));
                    }
                }
            }
        }
    }

    private void tryRollBack(Connection connection, String providerKey, Throwable originalException) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            logger.error("Failed to rollback transaction for provider {}", providerKey, rollbackException);
            originalException.addSuppressed(rollbackException);
        }
    }

    public void setOverridableItemsForLocks(List<String> overridableItemsForLocks) {
        this.overridableItemsForLocks = overridableItemsForLocks;
    }

    public void setOverridableItemsForACLs(List<String> overridableItemsForACLs) {
        this.overridableItemsForACLs = overridableItemsForACLs;
    }

    public void setExtensionProvider(JCRStoreProvider extensionProvider) {
        this.extensionProvider = extensionProvider;
    }

    @Override
    public List<String> getOverridableItemsForLocks() {
        return this.overridableItemsForLocks;
    }

    @Override
    public List<String> getOverridableItemsForACLs() {
        return this.overridableItemsForACLs;
    }

    @Override
    public JCRStoreProvider getExtensionProvider() {
        return this.extensionProvider;
    }
}
