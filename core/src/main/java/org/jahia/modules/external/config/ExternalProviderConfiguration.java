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
package org.jahia.modules.external.config;

import org.jahia.modules.external.id.IdentifierCacheService;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.concurrent.TimeUnit;

/**
 * OSGi configuration for the bidirectional identifier cache.
 *
 * Configuration file: org.jahia.modules.external.cfg
 *
 * Properties:
 * - cache.maxEntries (default: 10000)
 * - cache.timeToIdleMinutes (default: 60)
 * - cache.timeToLiveMinutes (default: 120)
 */
public class ExternalProviderConfiguration implements ManagedService {

    private static final Logger logger = LoggerFactory.getLogger(ExternalProviderConfiguration.class);

    // Default values
    private static final long DEFAULT_MAX_ENTRIES = 10_000L;
    private static final long DEFAULT_TIME_TO_IDLE_MINUTES = 60L;
    private static final long DEFAULT_TIME_TO_LIVE_MINUTES = 120L;

    // Configuration keys
    private static final String KEY_MAX_ENTRIES = "cache.maxEntries";
    private static final String KEY_TIME_TO_IDLE = "cache.timeToIdleMinutes";
    private static final String KEY_TIME_TO_LIVE = "cache.timeToLiveMinutes";

    // Current configuration values
    private long maxEntries = DEFAULT_MAX_ENTRIES;
    private long timeToIdleMinutes = DEFAULT_TIME_TO_IDLE_MINUTES;
    private long timeToLiveMinutes = DEFAULT_TIME_TO_LIVE_MINUTES;

    // Reference to the cache service
    private IdentifierCacheService identifierCache;

    @Override
    public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
        if (properties == null) {
            logger.info("No configuration found for identifier cache, using defaults");
            resetToDefaults();
            return;
        }

        try {
            // Read configuration
            maxEntries = getLongProperty(properties, KEY_MAX_ENTRIES, DEFAULT_MAX_ENTRIES);
            timeToIdleMinutes = getLongProperty(properties, KEY_TIME_TO_IDLE, DEFAULT_TIME_TO_IDLE_MINUTES);
            timeToLiveMinutes = getLongProperty(properties, KEY_TIME_TO_LIVE, DEFAULT_TIME_TO_LIVE_MINUTES);

            // Validate configuration
            validateConfiguration();

            logger.info("Identifier cache configuration updated: maxEntries={}, timeToIdleMinutes={}, timeToLiveMinutes={}",
                    maxEntries, timeToIdleMinutes, timeToLiveMinutes);

            // Reconfigure the cache service with new settings
            if (identifierCache != null) {
                identifierCache.reconfigure(maxEntries, timeToIdleMinutes, timeToLiveMinutes, TimeUnit.MINUTES);
            }

        } catch (Exception e) {
            logger.error("Error updating identifier cache configuration", e);
            throw new ConfigurationException("cache", "Invalid cache configuration: " + e.getMessage(), e);
        }
    }

    private void validateConfiguration() throws ConfigurationException {
        if (maxEntries <= 0) {
            throw new ConfigurationException(KEY_MAX_ENTRIES, "maxEntries must be positive, got: " + maxEntries);
        }
        if (timeToIdleMinutes < 0) {
            throw new ConfigurationException(KEY_TIME_TO_IDLE, "timeToIdleMinutes cannot be negative, got: " + timeToIdleMinutes);
        }
        if (timeToLiveMinutes < 0) {
            throw new ConfigurationException(KEY_TIME_TO_LIVE, "timeToLiveMinutes cannot be negative, got: " + timeToLiveMinutes);
        }
        if (timeToIdleMinutes > 0 && timeToLiveMinutes > 0 && timeToIdleMinutes > timeToLiveMinutes) {
            logger.warn("timeToIdleMinutes ({}) is greater than timeToLiveMinutes ({}). This may cause unexpected behavior.",
                    timeToIdleMinutes, timeToLiveMinutes);
        }
    }

    private long getLongProperty(Dictionary<String, ?> properties, String key, long defaultValue) {
        Object value = properties.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                logger.warn("Invalid value for {}: '{}', using default: {}", key, value, defaultValue);
                return defaultValue;
            }
        }
        logger.warn("Unexpected type for {}: {}, using default: {}", key, value.getClass(), defaultValue);
        return defaultValue;
    }

    private void resetToDefaults() {
        maxEntries = DEFAULT_MAX_ENTRIES;
        timeToIdleMinutes = DEFAULT_TIME_TO_IDLE_MINUTES;
        timeToLiveMinutes = DEFAULT_TIME_TO_LIVE_MINUTES;
    }

    // Getters for configuration values
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
     * Setter for dependency injection - receives the cache service.
     */
    public void setIdentifierCache(IdentifierCacheService identifierCache) {
        this.identifierCache = identifierCache;
    }
}

