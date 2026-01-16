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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Bidirectional cache for external identifier mappings.
 * Provides fast lookups in both directions:
 * - External ID → Internal UUID
 * - Internal UUID → External ID
 *
 * This implementation uses two synchronized Caffeine caches to ensure
 * consistent bidirectional lookups while maintaining cache eviction policies.
 *
 * @author Jahia Solutions Group
 */
public class IdentifierCacheService {

    private static final Logger logger = LoggerFactory.getLogger(IdentifierCacheService.class);

    // Forward cache: IdentifierMapping(providerKey, externalId) → internalUuid
    // Not final to allow reconfiguration
    private Cache<ProviderExternalId, String> externalToInternalCache;

    // Reverse cache: internalUuid → IdentifierMapping(externalId, providerKey)
    // Not final to allow reconfiguration
    private Cache<String, ProviderExternalId> internalToExternalCache;

    /**
     * Identifier mapping combining a provider key and an external ID.
     * Used as both cache key and cache value in the bidirectional cache.
     */
    private static class ProviderExternalId {
        final String externalId;
        final String providerKey;

        ProviderExternalId(String externalId, String providerKey) {
            this.externalId = externalId;
            this.providerKey = providerKey;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ProviderExternalId)) return false;
            ProviderExternalId that = (ProviderExternalId) o;
            return java.util.Objects.equals(externalId, that.externalId) &&
                   java.util.Objects.equals(providerKey, that.providerKey);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(externalId, providerKey);
        }

        @Override
        public String toString() {
            return providerKey + ":::" + externalId;
        }
    }


    /**
     * Creates a new bidirectional cache with default configuration:
     * - Maximum size: 10,000 entries
     * - TTL: 1 hour
     * - Stats recording enabled
     */
    public IdentifierCacheService() {
        this(10_000, 1, TimeUnit.HOURS);
    }

    /**
     * Creates a new bidirectional cache with custom configuration.
     *
     * @param maximumSize maximum number of entries in each cache
     * @param ttl time-to-live duration
     * @param ttlUnit time unit for TTL
     */
    public IdentifierCacheService(long maximumSize, long ttl, TimeUnit ttlUnit) {
        this(maximumSize, ttl, 0, ttlUnit);
    }

    /**
     * Creates a new bidirectional cache with full custom configuration.
     *
     * @param maximumSize maximum number of entries in each cache
     * @param timeToIdle time-to-idle duration (0 to disable)
     * @param timeToLive time-to-live duration (0 to disable)
     * @param timeUnit time unit for both idle and TTL
     */
    public IdentifierCacheService(long maximumSize, long timeToIdle, long timeToLive, TimeUnit timeUnit) {
        this.externalToInternalCache = buildCache(maximumSize, timeToIdle, timeToLive, timeUnit);
        this.internalToExternalCache = buildCache(maximumSize, timeToIdle, timeToLive, timeUnit);

        logger.info("Initialized BidirectionalIdentifierCache with maxSize={}, timeToIdle={} {}, timeToLive={} {}",
                maximumSize, timeToIdle, timeUnit, timeToLive, timeUnit);
    }

    private <K, V> Cache<K, V> buildCache(long maximumSize, long timeToIdle, long timeToLive, TimeUnit timeUnit) {
        com.github.benmanes.caffeine.cache.Caffeine<Object, Object> builder = Caffeine.newBuilder()
                .maximumSize(maximumSize)
                .recordStats();

        if (timeToIdle > 0) {
            builder.expireAfterAccess(timeToIdle, timeUnit);
        }
        if (timeToLive > 0) {
            builder.expireAfterWrite(timeToLive, timeUnit);
        }

        return builder.build();
    }

    /**
     * Get internal UUID by external ID and provider key.
     *
     * @param externalId the external identifier
     * @param providerKey the provider key
     * @return the internal UUID, or null if not found in cache
     */
    public String getInternalId(String externalId, String providerKey) {
        ProviderExternalId key = new ProviderExternalId(externalId, providerKey);
        return externalToInternalCache.getIfPresent(key);
    }

    /**
     * Get external ID by internal UUID.
     *
     * @param internalUuid the internal UUID
     * @return the external identifier, or null if not found in cache
     */
    public String getExternalId(String internalUuid) {
        ProviderExternalId mapping = internalToExternalCache.getIfPresent(internalUuid);
        return mapping != null ? mapping.externalId : null;
    }

    /**
     * Get provider key by internal UUID.
     *
     * @param internalUuid the internal UUID
     * @return the provider key, or null if not found in cache
     */
    public String getProviderKey(String internalUuid) {
        ProviderExternalId mapping = internalToExternalCache.getIfPresent(internalUuid);
        return mapping != null ? mapping.providerKey : null;
    }

    /**
     * Store a bidirectional mapping in both caches atomically.
     *
     * @param externalId the external identifier
     * @param providerKey the provider key
     * @param internalUuid the internal UUID
     */
    public void put(String externalId, String providerKey, String internalUuid) {
        ProviderExternalId mapping = new ProviderExternalId(externalId, providerKey);

        // Store in both caches
        externalToInternalCache.put(mapping, internalUuid);
        internalToExternalCache.put(internalUuid, mapping);

        if (logger.isTraceEnabled()) {
            logger.trace("Cached mapping: {} <-> {}", mapping, internalUuid);
        }
    }

    /**
     * Invalidate a mapping from both caches.
     *
     * @param externalId the external identifier
     * @param providerKey the provider key
     */
    public void invalidate(String externalId, String providerKey) {
        ProviderExternalId mapping = new ProviderExternalId(externalId, providerKey);

        // First get the internal UUID so we can remove from reverse cache
        String internalUuid = externalToInternalCache.getIfPresent(mapping);

        // Remove from both caches
        externalToInternalCache.invalidate(mapping);
        if (internalUuid != null) {
            internalToExternalCache.invalidate(internalUuid);

            if (logger.isTraceEnabled()) {
                logger.trace("Invalidated mapping: {} <-> {}", mapping, internalUuid);
            }
        }
    }

    /**
     * Invalidate a mapping by internal UUID.
     *
     * @param internalUuid the internal UUID
     */
    public void invalidateByInternalId(String internalUuid) {
        ProviderExternalId mapping = internalToExternalCache.getIfPresent(internalUuid);
        if (mapping != null) {
            externalToInternalCache.invalidate(mapping);
            internalToExternalCache.invalidate(internalUuid);

            if (logger.isTraceEnabled()) {
                logger.trace("Invalidated mapping: {} <-> {}", mapping, internalUuid);
            }
        } else {
            // Just invalidate the reverse cache if entry not found
            internalToExternalCache.invalidate(internalUuid);
        }
    }

    /**
     * Clear all entries from both caches.
     */
    public void invalidateAll() {
        externalToInternalCache.invalidateAll();
        internalToExternalCache.invalidateAll();
        logger.debug("Cleared all cache entries");
    }

    /**
     * Reconfigure the cache with new settings.
     * This atomically replaces the internal caches with new ones configured with the specified parameters.
     * All existing cache entries will be lost.
     *
     * @param maximumSize maximum number of entries in each cache
     * @param timeToIdle time-to-idle duration (0 to disable)
     * @param timeToLive time-to-live duration (0 to disable)
     * @param timeUnit time unit for both idle and TTL
     */
    public synchronized void reconfigure(long maximumSize, long timeToIdle, long timeToLive, TimeUnit timeUnit) {
        logger.info("Reconfiguring identifier cache: maxSize={}, timeToIdle={} {}, timeToLive={} {}",
                maximumSize, timeToIdle, timeUnit, timeToLive, timeUnit);

        // Log current stats before reconfiguration
        logger.info("Current cache stats before reconfiguration - Forward: {} entries (hitRate={:.2f}%), Reverse: {} entries (hitRate={:.2f}%)",
                getForwardCacheSize(),
                externalToInternalCache.stats().hitRate() * 100,
                getReverseCacheSize(),
                internalToExternalCache.stats().hitRate() * 100);

        // Build new caches with updated configuration
        Cache<ProviderExternalId, String> newForwardCache = buildCache(maximumSize, timeToIdle, timeToLive, timeUnit);
        Cache<String, ProviderExternalId> newReverseCache = buildCache(maximumSize, timeToIdle, timeToLive, timeUnit);

        // Atomic replacement - this ensures thread safety
        this.externalToInternalCache = newForwardCache;
        this.internalToExternalCache = newReverseCache;

        logger.info("Identifier cache reconfigured successfully");
    }

    /**
     * Get cache statistics for the forward cache (external → internal).
     *
     * @return cache statistics
     */
    public CacheStats getForwardCacheStats() {
        return externalToInternalCache.stats();
    }

    /**
     * Get cache statistics for the reverse cache (internal → external).
     *
     * @return cache statistics
     */
    public CacheStats getReverseCacheStats() {
        return internalToExternalCache.stats();
    }

    /**
     * Get the current size of the forward cache.
     *
     * @return number of entries in forward cache
     */
    public long getForwardCacheSize() {
        return externalToInternalCache.estimatedSize();
    }

    /**
     * Get the current size of the reverse cache.
     *
     * @return number of entries in reverse cache
     */
    public long getReverseCacheSize() {
        return internalToExternalCache.estimatedSize();
    }

    /**
     * Log cache statistics at INFO level.
     */
    public void logStats() {
        CacheStats forwardStats = getForwardCacheStats();
        CacheStats reverseStats = getReverseCacheStats();

        logger.info("Identifier Cache Stats - Forward: size={}, hitRate={:.2f}%, missRate={:.2f}%",
                    getForwardCacheSize(),
                    forwardStats.hitRate() * 100,
                    forwardStats.missRate() * 100);

        logger.info("Identifier Cache Stats - Reverse: size={}, hitRate={:.2f}%, missRate={:.2f}%",
                    getReverseCacheSize(),
                    reverseStats.hitRate() * 100,
                    reverseStats.missRate() * 100);
    }
}

