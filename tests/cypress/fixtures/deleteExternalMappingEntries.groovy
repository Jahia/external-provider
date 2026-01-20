import org.jahia.utils.DatabaseUtils

String deleteMappings = "DELETE FROM jahia_external_mapping"

log.info("Deleting all external mapping entries (jahia_external_mapping)...")


try (def connection = DatabaseUtils.getDatasource().getConnection();
     def statement = connection.prepareStatement(deleteMappings)) {
    int deletedMappingsCount = statement.executeUpdate()
    log.info("${deletedMappingsCount} external mapping entries have been deleted.")

} catch (Exception e) {
    log.error("Error while deleting external mappings entries", e)
}


