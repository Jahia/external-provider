import org.jahia.utils.DatabaseUtils

String expectedCount = "EXPECTED_COUNT" // passed to the Groovy script (string replacement)

String countQuery = "SELECT COUNT(*) FROM jahia_external_mapping"

log.info("Checking external mapping entries count...")

try (def connection = DatabaseUtils.getDatasource().getConnection();
     def statement = connection.prepareStatement(countQuery);
     def resultSet = statement.executeQuery()) {

    if (resultSet.next()) {
        int actualCount = resultSet.getInt(1)
        int expected = Integer.parseInt(expectedCount)

        if (actualCount == expected) {
            log.info("External mapping entries count matches expected: ${actualCount}")
        } else {
            throw new RuntimeException("External mapping entries count mismatch! Expected: ${expected}, Actual: ${actualCount}")
        }
    }
}
