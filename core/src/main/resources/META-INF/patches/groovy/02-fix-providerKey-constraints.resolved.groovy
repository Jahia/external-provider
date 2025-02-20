import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.lang.StringUtils
import org.jahia.osgi.BundleUtils
import org.jahia.services.content.JCRCallback
import org.jahia.services.content.JCRNodeWrapper
import org.jahia.services.content.JCRObservationManager
import org.jahia.services.content.JCRSessionWrapper
import org.jahia.services.content.JCRTemplate
import org.jahia.services.content.nodetypes.ExtendedNodeType
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition
import org.jahia.services.content.nodetypes.NodeTypeRegistry

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
        ORDER BY id
    """

    dataSource.connection.withCloseable { conn ->
        conn.prepareStatement(query).withCloseable { stat ->
            stat.executeQuery().withCloseable { rs ->
                while (rs.next()) {
                    def id = rs.getString("id")
                    def providerKey = rs.getString("providerKey")
                    duplicates[providerKey] << id
                }
            }
        }
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
static def deleteProviderById(DataSource dataSource, int providerId) {
    def deleteProvider = "DELETE FROM jahia_external_provider_id WHERE id = ?"
    def deleteMappings = "DELETE FROM jahia_external_mapping WHERE internalUuid LIKE ?"

    try (def connection = dataSource.connection;
         def statement = connection.prepareStatement(deleteProvider)) {
        statement.setInt(1, providerId)
        statement.executeUpdate()
    }

    try (def connection = dataSource.connection;
         def statement = connection.prepareStatement(deleteMappings)) {
        statement.setString(1, "${getPaddedProviderId(providerId.toString())}%")
        statement.executeUpdate()
    }
}


/**
 * Will fix reference properties targeting a duplicated providerId
 * Will execute given queries, and for each node result, check for references that would required to be fixed.
 * @param dataSource the data source
 * @param id the duplicated provider id
 * @param masterId the master provider id
 * @param providerKey the provider key
 * @param queries the queries
 * @param workspace the workspace
 * @param log the logger
 * @return void
 */
static def fixReferences(DataSource dataSource, String id, String masterId, String providerKey, def queries,
                         String workspace, def log, Map<String, String> fixedMappings) {
    log.info("Fixing references in workspace: $workspace, for provider: $id")
    def paddedId = getPaddedProviderId(id)
    JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, workspace, null, new JCRCallback<Object>() {
        @Override
        Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
            JCRObservationManager.setAllEventListenersDisabled(true)
            try {
                queries.each { query ->
                    def changes = false
                    def result = session.getWorkspace().getQueryManager().createQuery(query, "JCR-SQL2").execute().getNodes()
                    while (result.hasNext()) {
                        def resultNode = result.next() as JCRNodeWrapper

                        // Catch i18n refs on i18ns translation sub nodes.
                        def nodes = [resultNode.getRealNode()]
                        def i18ns = resultNode.getI18Ns()
                        while (i18ns.hasNext()) {
                            nodes.add(i18ns.nextNode())
                        }

                        nodes.each { node ->
                            PropertyIterator propertyIterator = node.getProperties();
                            while (propertyIterator.hasNext()) {
                                Property property = propertyIterator.nextProperty();
                                if (property.getType() == PropertyType.WEAKREFERENCE || property.getType() == PropertyType.REFERENCE) {
                                    // handle multiple refs
                                    if (property.isMultiple()) {
                                        def values = property.getValues()
                                        def newValues = new String[values.length]
                                        def changesForProperty = false
                                        values.eachWithIndex { entry, i ->
                                            if (entry.getString() != null && entry.getString().startsWith(paddedId)) {
                                                log.info("Ref to fix: ${node.getPath()} -> ${property.getName()}")
                                                String fixedRef = fixInternalUUID(dataSource, entry.getString(), masterId, providerKey, fixedMappings)
                                                if (fixedRef != null) {
                                                    newValues[i] = fixedRef
                                                    log.info(" - fixed from: ${entry.getString()} -> ${fixedRef}")
                                                    changesForProperty = true
                                                } else {
                                                    newValues[i] = entry.getString()
                                                }
                                            } else {
                                                newValues[i] = entry.getString()
                                            }
                                        }
                                        if (changesForProperty) {
                                            property.setValue(newValues)
                                            changes = true
                                        }
                                    } else {
                                        // handle single ref
                                        def value = property.getValue()
                                        if (value != null && value.getString() != null && value.getString().startsWith(paddedId)) {
                                            log.info("Ref to fix: ${node.getPath()} -> ${property.getName()}")
                                            String fixedRef = fixInternalUUID(dataSource, value.getString(), masterId, providerKey, fixedMappings)
                                            if (fixedRef != null) {
                                                property.setValue(fixedRef)
                                                log.info(" - fixed from: ${value.getString()} to: ${fixedRef}")
                                                changes = true
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (changes) {
                        session.save()
                    }
                }
            } finally {
                JCRObservationManager.setAllEventListenersDisabled(false)
            }
            return null
        }
    })
    log.info("Done fixing references in workspace: $workspace, for provider: $id")
}

/**
 * Will fix a given internalUUID by returning a valid internalUUID from master provider.
 * 2 cases:
 * - if master already have the mapping -> reuse it
 * - if master doesn't contains the mapping -> switch internalUUID to be fix to the master provider.
 * @param dataSource the data source
 * @param internalUuid the internal UUID to be fix and replace by master
 * @param masterId the master provider id
 * @param providerKey the provider key
 * @return the master internalUUID to be used in references, or null if internal UUID to fix not found in DB
 */
static def fixInternalUUID(DataSource dataSource, String internalUuid, String masterId, String providerKey, Map<String, String> fixedMappings) {
    if (fixedMappings.containsKey(internalUuid)){
        return fixedMappings.get(internalUuid)
    }

    // first find the externalId
    def externalId = null
    dataSource.connection.withCloseable { conn ->
        conn.prepareStatement("SELECT externalId FROM jahia_external_mapping WHERE internalUuid = ?").withCloseable { stmt ->
            stmt.setString(1, internalUuid)

            stmt.executeQuery().withCloseable { rs ->
                externalId = rs.next() ? rs.getString("externalId") : null
            }
        }
    }

    if (externalId == null) {
        // Ignore the ref, the mapping does not exists in DB
        return null
    }

    def masterInternalUuid = findInternalUuidByExternalId(dataSource, masterId, externalId, providerKey)
    if (masterInternalUuid == null) {
        masterInternalUuid = switchProviderId(dataSource, internalUuid, masterId)
    }

    fixedMappings.put(internalUuid, masterInternalUuid)
    return masterInternalUuid
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
                return rs.next() ? rs.getString("internalUuid") : null
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
 * Scan the node types for reference properties
 * @return a map of indexed no node types and indexed property names
 */
static def scanNodeTypesForReferenceProperties() {
    // couple of node types that can be safely ignored to avoid unnecessary scans
    def ignoredNodeTypes = [
            "jnt:task", "jnt:dashboardDoc", "jnt:listProjects", "jnt:workflowTask",
            "jnt:createTaskForm", "jnt:tagCloud", "jnt:pageFormCreation", "jnt:listSites",
            "jnt:createWebProject", "jnt:portletReference", "jnt:simpleSearchForm",
            "jnt:customSearchForm", "jnt:customSearchForm"
    ]

    def nodeTypeRegistry = NodeTypeRegistry.getInstance()
    def iterator = nodeTypeRegistry.getAllNodeTypes(null)

    def indexedNoNodeTypes = [] as Set
    def indexedPropertyName = [] as Set

    iterator.each { ExtendedNodeType nodeType ->
        if (nodeType.name in ignoredNodeTypes || !nodeType.isNodeType("jnt:content")) {
            return
        }
        nodeType.declaredPropertyDefinitions.each { propertyDefinition ->
            if (propertyDefinition.requiredType in [PropertyType.WEAKREFERENCE, PropertyType.REFERENCE]) {
                if (propertyDefinition.index == ExtendedPropertyDefinition.INDEXED_NO) {
                    indexedNoNodeTypes << nodeType.name
                } else {
                    indexedPropertyName << propertyDefinition.name
                }
            }
        }
    }

    return [indexedNoNodeTypes: indexedNoNodeTypes, indexedPropertyName: indexedPropertyName]
}

/**
 * Build a query to find all nodes that have a ref property starting with the given id
 * @param id the provider id
 * @param indexedPropertyNames the property names
 * @return the JCR SQL2 query statement.
 */
static def buildQueryForIndexedPropertyNames(String id, indexedPropertyNames) {
    def paddedId = getPaddedProviderId(id)
    def queryParts = indexedPropertyNames.collect { entry -> "([$entry] like '$paddedId%')" }
    def query = "SELECT * FROM [jnt:content] where " + queryParts.join(" OR ")
    return query
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
        log.info("Scanning NodeTypes for reference properties")
        def scannedNodeTypes = scanNodeTypesForReferenceProperties()
        log.info "Scanned: Indexed=no node types: ${scannedNodeTypes.indexedNoNodeTypes}"
        log.info "Scanned: Indexed property names: ${scannedNodeTypes.indexedPropertyName}"
        def indexedNoNodeTypesQueries = scannedNodeTypes.indexedNoNodeTypes.collect { nodeType -> "SELECT * FROM [$nodeType]" }

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
                    if (counts[id] == 0 || (scannedNodeTypes.indexedNoNodeTypes.isEmpty() && scannedNodeTypes.indexedPropertyName.isEmpty())) {
                        log.info("Safely Removing id: $id, it has no external uuids OR no reference properties definition registered")
                        deleteProviderById(jahiaDs, id as int)
                        return
                    }

                    log.info("Before removing id: $id, we need to make sure that external uuids are not referenced in the JCR")
                    def queries = []
                    if (!scannedNodeTypes.indexedPropertyName.isEmpty()) {
                        queries.add(buildQueryForIndexedPropertyNames(id as String, scannedNodeTypes.indexedPropertyName))
                    }
                    queries.addAll(indexedNoNodeTypesQueries)

                    Map<String, String> fixedMappings = new HashMap<>()
                    fixReferences(jahiaDs, id as String, masterId as String, providerKey as String, queries, "default", log, fixedMappings)
                    fixReferences(jahiaDs, id as String, masterId as String, providerKey as String, queries, "live", log, fixedMappings)
                    deleteProviderById(jahiaDs, id as int)
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
