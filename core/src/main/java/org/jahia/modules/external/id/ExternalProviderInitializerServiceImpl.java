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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    public void delete(List<String> externalIds, String providerKey, boolean includeDescendants)
            throws RepositoryException {
        if (externalIds.isEmpty()) {
            return;
        }
        if (externalIds.size() > 1000) {
            throw new RepositoryException("External provider can delete only 1000 items at a time");
        }
        // delete all
        // First select mapping objects by external ID hashcodes, then only delete desired ones among them.
        List<Integer> hashes = new LinkedList<>();
        for (String externalId : externalIds) {
            hashes.add(externalId.hashCode());
        }
        List<String> invalidate = new ArrayList<>();
        String selectMappingTemplate = "SELECT internalUuid, externalId FROM jahia_external_mapping WHERE providerKey=? AND externalIdHash in (:listPlaceHolder)";
        String deleteMapping = "DELETE FROM jahia_external_mapping WHERE internalUuid=?";
        String selectMapping = selectMappingTemplate.replace(":listPlaceHolder", hashes.stream().map(integer -> "?").collect(Collectors.joining(",")));
        try (Connection connection = datasource.getConnection(); PreparedStatement statement = connection.prepareStatement(selectMapping)) {
            connection.setAutoCommit(false);
            statement.setString(1, providerKey);
            int columnIndex = 2;
            for (Integer hash : hashes) {
                statement.setInt(columnIndex++, hash);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String externalId = getExternalId(resultSet, 2);
                    if (externalIds.contains(externalId)) {
                        invalidate.add(externalId);
                        String internalId = resultSet.getString(1);
                        try (PreparedStatement deleteExternalId = connection.prepareStatement(deleteMapping)) {
                            deleteExternalId.setString(1, internalId);
                            deleteExternalId.executeUpdate();
                        }
                    }
                }
                connection.commit();
            } catch (SQLException | IOException e) {
                logger.error("Issue when deleting mapping for external provider {}", providerKey, e);
                connection.rollback();
                throw new RepositoryException(
                        "Issue when deleting mapping for external provider " + providerKey, e);
            }
        } catch (SQLException e) {
            logger.error("Issue when deleting mapping for external provider {}", providerKey, e);
            throw new RepositoryException(
                    "Issue when deleting mapping for external provider " + providerKey, e);
        }
        if (includeDescendants) {
            boolean isPostgreSQL = DatabaseUtils.getDatabaseType() == DatabaseUtils.DatabaseType.postgresql;
            String selectDescendantMapping = "SELECT internalUuid, externalId FROM jahia_external_mapping WHERE providerKey=?";
            selectDescendantMapping += isPostgreSQL ?
                    " AND convert_from(lo_get(cast(externalId as bigint)), 'UTF8') like ?" :
                    " AND externalId like ?";
            try (Connection connection = datasource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(selectDescendantMapping)) {
                connection.setAutoCommit(false);
                for (String externalId : externalIds) {
                    statement.setString(1, providerKey);
                    statement.setString(2, externalId + "/%");
                    try (ResultSet resultSet = statement.executeQuery()) {
                        while (resultSet.next()) {
                            String toBeDeletedExternalId = getExternalId(resultSet, 2);
                            invalidate.add(toBeDeletedExternalId);
                            String internalId = resultSet.getString(1);
                            try (PreparedStatement deleteExternalId = connection.prepareStatement(deleteMapping)) {
                                deleteExternalId.setString(1, internalId);
                                deleteExternalId.executeUpdate();
                            }

                        }
                        connection.commit();
                    } catch (SQLException | IOException e) {
                        logger.error("Issue when deleting mapping for external provider {}", providerKey, e);
                        connection.rollback();
                        throw new RepositoryException(
                                "Issue when deleting mapping for external provider " + providerKey, e);
                    }
                }
            } catch (SQLException e) {
                logger.error("Issue when deleting mapping for external provider {}", providerKey, e);
                throw new RepositoryException(
                        "Issue when deleting mapping for external provider " + providerKey, e);
            }
        }
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
            connection.setAutoCommit(false);
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
        String uuid = null != cacheElement ? (String) cacheElement.getObjectValue() : null;

        if (null == uuid) {
            String query = "SELECT externalId, internalUuid FROM jahia_external_mapping WHERE providerKey=? and externalIdHash=?";
            try (Connection connection = this.datasource.getConnection(); PreparedStatement statement = connection.prepareStatement(query)) {
                connection.setAutoCommit(false);
                statement.setString(1, providerKey);
                statement.setLong(2, externalId.hashCode());
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        String rowExternalId = getExternalId(resultSet, 1);
                        if (rowExternalId.equals(externalId)) {
                            uuid = resultSet.getString(2);
                        }
                        idCache.put(new Element(cacheKey, uuid, true));
                    }
                } catch (SQLException | IOException e) {
                    throw new RepositoryException("Issue when obtaining internal identifier for provider" + providerKey, e);
                }
            } catch (SQLException e) {
                throw new RepositoryException("Issue when obtaining internal identifier for provider" + providerKey, e);
            }
        }

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
        UuidMapping uuidMapping = new UuidMapping();
        uuidMapping.setExternalId(externalId);
        uuidMapping.setProviderKey(providerKey);
        uuidMapping.setInternalUuid(providerId + "-" + StringUtils.substringAfter(UUID.randomUUID().toString(), "-"));

        boolean isPostgreSQL = DatabaseUtils.getDatabaseType() == DatabaseUtils.DatabaseType.postgresql;
        String insertNewMapping = isPostgreSQL ?
                "INSERT INTO jahia_external_mapping(providerKey, externalId, externalIdHash, internalUuid) values (?,lo_from_bytea(0, ?),?,?)" :
                "INSERT INTO jahia_external_mapping(providerKey, externalId, externalIdHash, internalUuid) values (?,?,?,?)";
        try (Connection connection = datasource.getConnection();
             PreparedStatement statement = connection.prepareStatement(insertNewMapping)) {
            statement.setString(1, uuidMapping.getProviderKey());
            if (isPostgreSQL) {
                statement.setBytes(2, externalId.getBytes(StandardCharsets.UTF_8));
            } else {
                statement.setClob(2, new StringReader(externalId));
            }
            statement.setLong(3, uuidMapping.getExternalIdHash());
            statement.setString(4, uuidMapping.getInternalUuid());
            int rowAffected = statement.executeUpdate();
            if (rowAffected == 1) {
                return uuidMapping.getInternalUuid();
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
        String deleteExternalProvider = "DELETE FROM jahia_external_provider_id where providerKey=?";
        String deleteMappings = "DELETE FROM jahia_external_mapping where providerKey=?";
        try (Connection connection = datasource.getConnection(); PreparedStatement statement = connection.prepareStatement(deleteExternalProvider)) {
            statement.setString(1, providerKey);
            int deletedCount = statement.executeUpdate();
            if (deletedCount > 0) {
                logger.info("Deleted {} external provider entry for key {}", deletedCount, providerKey);
                PreparedStatement statement1 = connection.prepareStatement(deleteMappings);
                statement1.setString(1, providerKey);
                deletedCount = statement1.executeUpdate();
                logger.info("Deleted {} identifier mapping entries for external provider with key {}", deletedCount,
                        providerKey);
            } else {
                logger.info("No external provider entry found for key {}", providerKey);
            }
        } catch (SQLException e) {
            throw new RepositoryException(
                    "Issue when removing external provider entry and identifier mappings for provider key " + providerKey, e);
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
        String selectMapping = "SELECT internalUuid, externalId FROM jahia_external_mapping WHERE providerKey=? AND externalIdHash=?";
        String updateMapping = isPostgreSQL ?
                "UPDATE jahia_external_mapping SET externalId=lo_from_bytea(0, ?), externalIdHash=? WHERE internalUuid=?" :
                "UPDATE jahia_external_mapping SET externalId=?, externalIdHash=? WHERE internalUuid=?";
        try (Connection connection = datasource.getConnection();
             PreparedStatement statement = connection.prepareStatement(selectMapping)) {
            connection.setAutoCommit(false);
            statement.setString(1, providerKey);
            statement.setInt(2, oldExternalId.hashCode());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String externalId = getExternalId(resultSet, 2);
                    if (externalId.equals(oldExternalId)) {
                        invalidate.add(oldExternalId);
                        String internalId = resultSet.getString(1);
                        try (PreparedStatement updateExternalId = connection.prepareStatement(updateMapping)) {
                            if (isPostgreSQL) {
                                updateExternalId.setBytes(1, externalId.getBytes(StandardCharsets.UTF_8));
                            } else {
                                updateExternalId.setClob(1, new StringReader(newExternalId));
                            }
                            updateExternalId.setLong(2, newExternalId.hashCode());
                            updateExternalId.setString(3, internalId);
                            updateExternalId.executeUpdate();
                        }
                    }
                }
                connection.commit();
            } catch (SQLException | IOException e) {
                logger.error("Issue when updating mapping for external provider {}", providerKey, e);
                connection.rollback();
                throw new RepositoryException(
                        "Issue when updating mapping for external provider " + providerKey, e);
            }
        } catch (SQLException e) {
            logger.error("Issue when updating mapping for external provider {}", providerKey, e);
            throw new RepositoryException(
                    "Issue when updating mapping for external provider " + providerKey, e);
        }
        if (includeDescendants) {
            String selectDescendantMapping = isPostgreSQL ?
                    "SELECT internalUuid, externalId FROM jahia_external_mapping"
                            + " WHERE providerKey=? and convert_from(lo_get(cast(externalId as bigint)), 'UTF8') like ?" :
                    "SELECT internalUuid, externalId FROM jahia_external_mapping WHERE providerKey=? and externalId like ?";
            try (Connection connection = datasource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(selectDescendantMapping)) {
                connection.setAutoCommit(false);
                statement.setString(1, providerKey);
                statement.setString(2, oldExternalId + "/%");
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        String externalId = getExternalId(resultSet, 2);
                        invalidate.add(externalId);
                        String internalId = resultSet.getString(1);
                        try (PreparedStatement updateExternalId = connection.prepareStatement(updateMapping)) {
                            String updatedExternalId = newExternalId + StringUtils.substringAfter(externalId, oldExternalId);
                            if (isPostgreSQL) {
                                updateExternalId.setBytes(1, updatedExternalId.getBytes(StandardCharsets.UTF_8));
                            } else {
                                updateExternalId.setClob(1, new StringReader(updatedExternalId));
                            }

                            updateExternalId.setLong(2, updatedExternalId.hashCode());
                            updateExternalId.setString(3, internalId);
                            updateExternalId.executeUpdate();
                        }

                    }
                    connection.commit();
                } catch (SQLException | IOException e) {
                    connection.rollback();
                    logger.error("Issue when updating mapping for external provider {}", providerKey, e);
                    throw new RepositoryException(
                            "Issue when updating mapping for external provider " + providerKey, e);
                }
            } catch (SQLException e) {
                logger.error("Issue when updating mapping for external provider {}", providerKey, e);
                throw new RepositoryException(
                        "Issue when updating mapping for external provider " + providerKey, e);
            }
        }
        for (String id : invalidate) {
            this.invalidateCache(id, providerKey);
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
