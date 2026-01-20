package org.jahia.modules.external.config;

import org.jahia.modules.external.id.IdentifierCacheService;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;

import static org.jahia.modules.external.id.IdentifierCacheService.*;

public class ExternalProviderConfiguration implements ManagedService {

    private static final Logger logger = LoggerFactory.getLogger(ExternalProviderConfiguration.class);

    // Configuration keys
    private static final String KEY_MAX_ENTRIES = "cache.maxEntries";
    private static final String KEY_TIME_TO_IDLE = "cache.timeToIdleMinutes";
    private static final String KEY_TIME_TO_LIVE = "cache.timeToLiveMinutes";

    private IdentifierCacheService identifierCacheService;

    @Override
    public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
        CacheConfiguration config;
        CacheConfiguration.Builder builder = new CacheConfiguration.Builder();
        if (properties == null) {
            logger.info("No configuration found for identifier cache, using defaults");
        } else {
            // Build configuration from properties
            builder.withMaxEntries(getLongProperty(properties, KEY_MAX_ENTRIES))
                    .withTimeToIdleMinutes(getLongProperty(properties, KEY_TIME_TO_IDLE))
                    .withTimeToLiveMinutes(getLongProperty(properties, KEY_TIME_TO_LIVE));
        }

        // Reconfigure the cache service with validated configuration
        config = builder.build();
        logger.info("Identifier cache configuration updated: {}", config);
        identifierCacheService.reconfigure(config.getMaxEntries(), config.getTimeToIdleMinutes(), config.getTimeToLiveMinutes());
    }

    private long getLongProperty(Dictionary<String, ?> properties, String key) throws ConfigurationException {
        Object value = properties.get(key);
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof String) {
            String stringValue = (String) value;
            try {
                return Long.parseLong(stringValue);
            } catch (NumberFormatException e) {
                throw new ConfigurationException(key, String.format("Unable to parse value '%s'", stringValue), e);
            }
        }
        throw new ConfigurationException(key, String.format("Unexpected value '%s'", value));
    }

    /**
     * Setter for dependency injection
     */
    public void setIdentifierCacheService(IdentifierCacheService identifierCacheService) {
        this.identifierCacheService = identifierCacheService;
    }

    /**
     * Immutable configuration for the identifier cache.
     * Use the {@link Builder} to create instances.
     */
    public static class CacheConfiguration {

        private final long maxEntries;
        private final long timeToIdleMinutes;
        private final long timeToLiveMinutes;

        private CacheConfiguration(long maxEntries, long timeToIdleMinutes, long timeToLiveMinutes) {
            this.maxEntries = maxEntries;
            this.timeToIdleMinutes = timeToIdleMinutes;
            this.timeToLiveMinutes = timeToLiveMinutes;
        }

        public long getMaxEntries() {
            return maxEntries;
        }

        public long getTimeToIdleMinutes() {
            return timeToIdleMinutes;
        }

        public long getTimeToLiveMinutes() {
            return timeToLiveMinutes;
        }

        /**
         * Builder for CacheConfiguration with validation.
         */
        public static class Builder {
            private long maxEntries = DEFAULT_MAX_ENTRIES;
            private long timeToIdleMinutes = DEFAULT_TIME_TO_IDLE_MINUTES;
            private long timeToLiveMinutes = DEFAULT_TIME_TO_LIVE_MINUTES;

            public Builder withMaxEntries(long maxEntries) {
                this.maxEntries = maxEntries;
                return this;
            }

            public Builder withTimeToIdleMinutes(long timeToIdleMinutes) {
                this.timeToIdleMinutes = timeToIdleMinutes;
                return this;
            }

            public Builder withTimeToLiveMinutes(long timeToLiveMinutes) {
                this.timeToLiveMinutes = timeToLiveMinutes;
                return this;
            }

            /**
             * Build and validate the configuration.
             *
             * @return immutable CacheConfiguration instance
             * @throws ConfigurationException if the configuration parameters are invalid
             */
            public CacheConfiguration build() throws ConfigurationException {
                validate();
                return new CacheConfiguration(maxEntries, timeToIdleMinutes, timeToLiveMinutes);
            }

            private void validate() throws ConfigurationException {
                if (maxEntries <= 0) {
                    throw new ConfigurationException(KEY_MAX_ENTRIES, "maxEntries must be positive, got: " + maxEntries);
                }
                if (timeToIdleMinutes < 0) {
                    throw new ConfigurationException(KEY_TIME_TO_IDLE, "timeToIdleMinutes cannot be negative, got: " + timeToIdleMinutes);
                }
                if (timeToLiveMinutes < 0) {
                    throw new ConfigurationException(KEY_TIME_TO_LIVE, "timeToLiveMinutes cannot be negative, got: " + timeToLiveMinutes);
                }
                if (timeToIdleMinutes > timeToLiveMinutes) {
                    throw new ConfigurationException(KEY_TIME_TO_IDLE,
                            "timeToIdleMinutes cannot be greater than timeToLiveMinutes, got: " + timeToIdleMinutes + " > "
                                    + timeToLiveMinutes);
                }
            }
        }

    }
}

