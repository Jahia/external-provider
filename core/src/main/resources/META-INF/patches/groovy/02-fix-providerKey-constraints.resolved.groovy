import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.lang.StringUtils
import org.apache.jackrabbit.core.JahiaRepositoryImpl
import org.apache.jackrabbit.core.SearchManager
import org.apache.jackrabbit.core.id.NodeId
import org.jahia.osgi.BundleUtils
import org.jahia.services.content.JCRCallback
import org.jahia.services.content.JCRObservationManager
import org.jahia.services.content.JCRSessionWrapper
import org.jahia.services.content.JCRTemplate
import org.jahia.services.content.impl.jackrabbit.SpringJackrabbitRepository

import javax.jcr.Property
import javax.jcr.PropertyIterator
import javax.jcr.PropertyType
import javax.jcr.RepositoryException
import javax.sql.DataSource
import java.sql.SQLException

/**
 * Find duplicate providers in the jahia_external_provider_id table
 * @param dataSource
 * @return a map of duplicated providerKey -> [id1, id2, ...]
 */
static def findDuplicateProviders(DataSource dataSource) {
    def duplicates = [:].withDefault { [] }
    def query = """
        SELECT id, providerKey FROM jahia_external_provider_id 
        WHERE providerKey IN (
            SELECT providerKey FROM jahia_external_provider_id 
            GROUP BY providerKey HAVING COUNT(*) > 1
        )
    """

    try (def connection = dataSource.connection;
         def statement = connection.prepareStatement(query);
         def resultSet = statement.executeQuery()) {

        while (resultSet.next()) {
            def id = resultSet.getString("id")
            def providerKey = resultSet.getString("providerKey")
            duplicates[providerKey] << id
        }
    } catch (SQLException e) {
        e.printStackTrace()
    }

    return duplicates
}

/**
 * Test duplicate insert, we will try to insert two times the same provider key
 * In order to check if database is correctly configured or not
 * @param dataSource
 * @return true if duplicate insert is allowed, false otherwise
 */
static def checkIfDuplicatedInsertIsPossible(DataSource dataSource) {
    def providerKey = UUID.randomUUID().toString()
    def insertQuery = "INSERT INTO jahia_external_provider_id (providerKey) VALUES (?)"
    def deleteQuery = "DELETE FROM jahia_external_provider_id WHERE providerKey = ?"

    dataSource.connection.withCloseable { conn ->
        conn.autoCommit = false
        try {
            conn.prepareStatement(insertQuery).withCloseable { stmt ->
                stmt.setString(1, providerKey)
                stmt.executeUpdate()
                stmt.setString(1, providerKey)
                stmt.executeUpdate()
            }
            conn.commit()
            return true
        } catch (SQLException ignored) {
            conn.rollback()
            return false
        } finally {
            conn.prepareStatement(deleteQuery).withCloseable { stmt ->
                stmt.setString(1, providerKey)
                stmt.executeUpdate()
            }
        }
    }
}

/**
 * Get the count of external uuids where internalUuid is using given provider id.
 * @param dataSource The database connection source.
 * @param providerId The ID of the provider.
 * @return The count of matching external uuids.
 */
static def countMatchingInternalUuid(DataSource dataSource, String providerId) {
    def paddedId = StringUtils.leftPad(providerId, 8, "f")
    def query = """
        SELECT COUNT(*) AS count 
        FROM jahia_external_mapping 
        WHERE internalUuid LIKE ?
    """

    def count = 0
    try (def connection = dataSource.connection;
         def statement = connection.prepareStatement(query)) {
        statement.setString(1, "${paddedId}%")
        try (def resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                count = resultSet.getInt("count")
            }
        }
    }

    return count
}

/**
 * Deletes all records from the jahia_external_provider_id table where the primary key (id) matches the provided id.
 * @param dataSource The database connection source.
 * @param providerId The primary key value to match for deletion.
 */
static def deleteProviderById(DataSource dataSource, int providerId, boolean deleteExternalMappings = false) {
    def queryProvider = "DELETE FROM jahia_external_provider_id WHERE id = ?"
    def deleteMappings = "DELETE FROM jahia_external_mapping WHERE internalUuid LIKE ?"

    try (def connection = dataSource.connection;
         def statement = connection.prepareStatement(queryProvider)) {
        statement.setInt(1, providerId)
        statement.executeUpdate()
    }

    if (deleteExternalMappings) {
        try (def connection = dataSource.connection;
             def statement = connection.prepareStatement(deleteMappings)) {
            statement.setString(1, "${getPaddedProviderId(providerId.toString())}%")
            statement.executeUpdate()
        }
    }
}

/**
 * Return the external uuids that are referenced in the JCR for a given provider id.
 * @param dataSource The database connection source.
 * @param providerId The ID of the provider.
 * @return the external uuids that are referenced.
 */
static def getInternalUuidsWithWeakReferringNodes(DataSource dataSource, String providerId, SearchManager searchManager) {
    def paddedProviderId = getPaddedProviderId(providerId)
    def query = """
        SELECT internalUuid, externalId
        FROM jahia_external_mapping
        WHERE internalUuid LIKE ?
    """

    def result = [:].withDefault { [] }
    try (def connection = dataSource.connection;
         def statement = connection.prepareStatement(query)) {

        statement.setString(1, "${paddedProviderId}%") // Use parameterized query for safety

        try (def resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                def internalUuid = resultSet.getString("internalUuid")
                def externalId = resultSet.getString("externalId")

                // Call the check for weakly referring nodes
                def weakReferringNodes = searchManager.getWeaklyReferringNodes(new NodeId(internalUuid))
                if (weakReferringNodes != null && !weakReferringNodes.isEmpty()) {
                    for (NodeId weakReferringNode : weakReferringNodes) {
                        result[weakReferringNode] << [internalUuid, externalId]
                    }
                }
            }
        }
    }

    return result
}

/**
 * Fix weak references for a given JCR node UUID.
 * @param jcrReferringNodeUuid The UUID of referring the JCR node.
 * @param workspace The workspace to use.
 * @param log The logger to use.
 * return true if the weak reference was fixed, false otherwise.
 */
static def fixWeakReferences(DataSource dataSource, String providerKey, String masterId, String jcrReferringNodeUuid, String workspace, def mappingEntries, def log, def jsonMapper) {
    JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, workspace, null, new JCRCallback() {
        @Override
        Object doInJCR(JCRSessionWrapper session) throws RepositoryException {

            JCRObservationManager.setAllEventListenersDisabled(true)
            try {
                def node = null
                try {
                    node = session.getNodeByIdentifier(jcrReferringNodeUuid)
                } catch (RepositoryException ignored) {
                    // Node not found, nothing to do, just ignore
                    return true;
                }

                def changes = false;
                mappingEntries.each { mappingEntry ->
                    def internalUuid = mappingEntry[0] as String
                    def externalId = mappingEntry[1] as String

                    // check if the internalUuid is already mapped to the masterId, if not, switch the providerId
                    def masterInternalUuid = findInternalUuidByExternalId(dataSource, masterId, externalId, providerKey)
                    if (masterInternalUuid == null) {
                        masterInternalUuid = switchProviderId(dataSource, internalUuid, masterId)
                    }

                    PropertyIterator propertyIterator = node.getRealNode().getProperties();
                    while (propertyIterator.hasNext()) {
                        Property property = propertyIterator.nextProperty();
                        if (property.getType() == PropertyType.WEAKREFERENCE) {
                            if (property.isMultiple()) {
                                def values = property.getValues()
                                def newValues = new String[values.length]
                                def changesForProperty = false
                                values.eachWithIndex { entry, i ->
                                    if (entry.getString() == internalUuid) {
                                        newValues[i] = masterInternalUuid
                                        changesForProperty = true
                                    } else {
                                        newValues[i] = entry.getString()
                                    }
                                }
                                if (changesForProperty) {
                                    property.setValue(newValues)
                                    changes = true
                                    log.info("Reference fixed for node: ${node.getPath()} multi-valued property: ${property.getName()} " +
                                            "updated from ${jsonMapper.writeValueAsString(values.collect{it.getString()})} to ${jsonMapper.writeValueAsString(newValues)}")
                                }
                            } else {
                                if (internalUuid == property.getValue().getString()) {
                                    property.setValue(masterInternalUuid)
                                    changes = true
                                    log.info("Reference fixed for node: ${node.getPath()} single-valued property: ${property.getName()} " +
                                            "updated from $internalUuid to $masterInternalUuid")
                                }
                            }
                        }
                    }
                }

                if (changes) {
                    session.save()
                }
            } finally {
                JCRObservationManager.setAllEventListenersDisabled(false)
            }

            return null
        }
    })
}

/**
 * Retrieves the complete internalUuid for an entry with the given externalId.
 *
 * @param dataSource The database connection source.
 * @param providerId The provider ID used to construct the internalUuid prefix.
 * @param externalId The externalId to search for.
 * @param providerKey The providerKey to search for.
 * @return The matching internalUuid, or null if not found.
 */
static def findInternalUuidByExternalId(DataSource dataSource, String providerId, String externalId, String providerKey) {
    def uuidPrefix = getPaddedProviderId(providerId)
    def query = """
        SELECT internalUuid 
        FROM jahia_external_mapping 
        WHERE internalUuid LIKE ? 
        AND externalId = ?
        AND providerKey = ?
        LIMIT 1
    """

    dataSource.connection.withCloseable { conn ->
        conn.prepareStatement(query).withCloseable { stmt ->
            stmt.setString(1, "${uuidPrefix}%")
            stmt.setString(2, externalId)
            stmt.setString(3, providerKey)

            stmt.executeQuery().withCloseable { rs ->
                rs.next() ? rs.getString("internalUuid") : null
            }
        }
    }
}

/**
 * Updates the internalUuid to reflect a new providerId.
 *
 * @param dataSource The database connection source.
 * @param internalUuid The full existing internalUuid.
 * @param newProviderId The new provider ID to replace in internalUuid.
 * @return true the new internalUuid for this mapping.
 */
static def switchProviderId(DataSource dataSource, String internalUuid, String newProviderId) {
    def parts = internalUuid.split("-", 2) // Split only on the first "-"
    def newProviderPart = getPaddedProviderId(newProviderId) // Construct new providerId
    def newInternalUuid = newProviderPart + parts[1]

    def queryUpdate = """
        UPDATE jahia_external_mapping 
        SET internalUuid = ? 
        WHERE internalUuid = ?
    """

    dataSource.connection.withCloseable { conn ->
        conn.prepareStatement(queryUpdate).withCloseable { updateStmt ->
            updateStmt.setString(1, newInternalUuid)
            updateStmt.setString(2, internalUuid)
            return updateStmt.executeUpdate() > 0
        }
    }

    return newInternalUuid
}

/**
 * Get the provider id with padding.
 * @param providerId The provider id.
 * @return The padded provider id.
 */
static def getPaddedProviderId(String providerId) {
    return StringUtils.leftPad(providerId, 8, "f") + "-"
}

/**
 * Main method to apply the patch
 * @param log the logger already available in the groovy context
 * @return void
 */
static def applyPatch(def log) {
    log.info("External provider patching jahia_external_provider_id for unique providerKey, started...")
    def jahiaDs = BundleUtils.getOsgiService(DataSource, '(osgi.jndi.service.name=jdbc/jahia)')
    def jsonMapper = new ObjectMapper()
    def repo = (JahiaRepositoryImpl) SpringJackrabbitRepository.getInstance().getRepository()
    def defaultSearchManager = repo.getSearchManager("default")
    def liveSearchManager = repo.getSearchManager("live")

    log.info("Check if schema needs to be updated")
    if (!checkIfDuplicatedInsertIsPossible(jahiaDs)) {
        log.info("Duplicate insert not allowed, schema is already correctly configured")
        return // no need to continue
    }
    log.info("Duplicate insert allowed, schema needs to be updated !")

    log.info("Checking for existing duplicates providerKey in jahia_external_provider_id table")
    def duplicates = findDuplicateProviders(jahiaDs)
    if (!duplicates.isEmpty()) {
        log.info("Duplicated providerKey found in jahia_external_provider_id table:")
        log.info(jsonMapper.writeValueAsString(duplicates))
    } else {
        log.info("No duplicate providerKey found in jahia_external_provider_id table, schema can be safely updated")
    }

    if (!duplicates.isEmpty()) {
        duplicates.each { providerKey, ids ->
            log.info("Attempting to fix providerKey: $providerKey")
            def counts = [:].withDefault { 0 }
            def masterId = null
            ids.eachWithIndex { id, index ->
                if (index == 0) {
                    masterId = id
                }
                counts[id] = countMatchingInternalUuid(jahiaDs, id as String)
                log.info("This providerKey count: ${counts[id]} elements for id: $id")
            }
            log.info("Keep id: $masterId as master, attempting to remove the others")
            ids.each { id ->
                if (id != masterId) {
                    if (counts[id] == 0) {
                        log.info("Safely Removing id: $id, it has no external uuids")
                        deleteProviderById(jahiaDs, id as int)
                        return
                    }

                    log.info("Before removing id: $id, we need to make sure that external uuids for this id are not weakly referenced in the JCR")
                    log.info("(Careful, here the script is only capable to automatically fix indexed weak references)")
                    log.info("(indexed=no weak reference OR normal (not weak) reference properties won't be fixed, you will have to fix them manually)")
                    def defaultReferences = getInternalUuidsWithWeakReferringNodes(jahiaDs, id as String, defaultSearchManager as SearchManager)
                    def liveReferences = getInternalUuidsWithWeakReferringNodes(jahiaDs, id as String, liveSearchManager as SearchManager)
                    if (defaultReferences.isEmpty() && liveReferences.isEmpty()) {
                        log.info("Safely Removing id: $id, no referring nodes found")
                        deleteProviderById(jahiaDs, id as int, true)
                    } else {
                        log.info("Safe remove of id: $id is not yet possible, some referring nodes found")
                        defaultReferences.each { nodeId, values ->
                            fixWeakReferences(jahiaDs, providerKey as String, masterId as String, nodeId as String,
                                    "default", values, log, jsonMapper)
                        }
                        liveReferences.each { nodeId, values ->
                            fixWeakReferences(jahiaDs, providerKey as String, masterId as String, nodeId as String,
                                    "live", values, log, jsonMapper)
                        }
                        log.info("Safely Removing id: $id, referring nodes fixed")
                        deleteProviderById(jahiaDs, id as int, true)
                    }
                }
            }
        }

        log.info("Duplicate entries should be fixed, checking again for duplicates to confirm")
        duplicates = findDuplicateProviders(jahiaDs)
        if (!duplicates.isEmpty()) {
            log.info("Something went wrong, aborting schema update, duplicated entries still found in jahia_external_provider_id table:")
            log.info(jsonMapper.writeValueAsString(duplicates))
            throw new IllegalStateException("Duplicated entries still found in jahia_external_provider_id table")
        } else {
            log.info("All duplicates entries have been fixed successfully, schema can be safely updated")
        }
    }

    log.info("Patching the schema now: adding unique constraint on providerKey")
    jahiaDs.connection.withCloseable { conn ->
        conn.prepareStatement("ALTER TABLE jahia_external_provider_id ADD CONSTRAINT uq_providerKey UNIQUE (providerKey)").withCloseable { stmt ->
            stmt.executeUpdate()
        }
    }
    log.info("External provider patching jahia_external_provider_id for unique providerKey, completed")
}

applyPatch(log)
