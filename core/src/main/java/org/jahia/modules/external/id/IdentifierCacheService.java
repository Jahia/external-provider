package org.jahia.modules.external.id;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Bidirectional cache for external identifier mappings.
 * Provides fast lookups in both directions:
 * - External ID → Internal UUID
 * - Internal UUID → External ID
 * <p>
 * This implementation uses two synchronized Caffeine caches to ensure
 * consistent bidirectional lookups while maintaining cache eviction policies.
 *
 */
public class IdentifierCacheService {

    private static final Logger logger = LoggerFactory.getLogger(IdentifierCacheService.class);
    public static final long DEFAULT_MAX_ENTRIES = -1L; // no limit
    public static final long DEFAULT_TIME_TO_IDLE_MINUTES = 240L; // 4 hours
    public static final long DEFAULT_TIME_TO_LIVE_MINUTES = 480L; // 8 hours

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
            if (this == o)
                return true;
            if (!(o instanceof ProviderExternalId))
                return false;
            ProviderExternalId that = (ProviderExternalId) o;
            return java.util.Objects.equals(externalId, that.externalId) && java.util.Objects.equals(providerKey, that.providerKey);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(externalId, providerKey);
        }

        @Override
        public String toString() {
            return "ProviderExternalId{" + "externalId='" + externalId + '\'' + ", providerKey='" + providerKey + '\'' + '}';
        }
    }

    /**
     * Creates a new bidirectional cache with the default configuration.
     */
    public IdentifierCacheService() {
        this(DEFAULT_MAX_ENTRIES, DEFAULT_TIME_TO_IDLE_MINUTES, DEFAULT_TIME_TO_LIVE_MINUTES);
    }

    /**
     * Creates a new bidirectional cache with full custom configuration.
     *
     * @param maximumSize       maximum number of entries in each cache, or negative number for no limit
     * @param timeToIdleMinutes time-to-idle duration
     * @param timeToLiveMinutes time-to-live duration
     */
    public IdentifierCacheService(long maximumSize, long timeToIdleMinutes, long timeToLiveMinutes) {
        this.externalToInternalCache = buildCache(maximumSize, timeToIdleMinutes, timeToLiveMinutes);
        this.internalToExternalCache = buildCache(maximumSize, timeToIdleMinutes, timeToLiveMinutes);

        logger.info("Initialized BidirectionalIdentifierCache with maxSize={}, timeToIdleMinutes={} minutes, timeToLiveMinutes={} minutes",
                maximumSize, timeToIdleMinutes, timeToLiveMinutes);
    }

    private <K, V> Cache<K, V> buildCache(long maximumSize, long timeToIdleMinutes, long timeToLiveMinutes) {
        Caffeine<Object, Object> builder = Caffeine.newBuilder()
                // TTI
                .expireAfterAccess(timeToIdleMinutes, TimeUnit.MINUTES)
                // TTL
                .expireAfterWrite(timeToLiveMinutes, TimeUnit.MINUTES);
        if (maximumSize >= 0) {
            builder.maximumSize(maximumSize);
        }
        return builder.build();
    }

    /**
     * Get internal ID by external ID and provider key.
     *
     * @param externalId  the external ID
     * @param providerKey the provider key
     * @return the internal id, or null if not found in cache
     */
    public String getInternalId(String externalId, String providerKey) {
        ProviderExternalId key = new ProviderExternalId(externalId, providerKey);
        return externalToInternalCache.getIfPresent(key);
    }

    /**
     * Get external ID by internal ID.
     *
     * @param internalId the internal ID
     * @return the external identifier, or null if not found in cache
     */
    public String getExternalId(String internalId) {
        ProviderExternalId mapping = internalToExternalCache.getIfPresent(internalId);
        return mapping != null ? mapping.externalId : null;
    }

    /**
     * Store a bidirectional mapping in both caches atomically.
     * Synchronized to prevent race conditions during cache reconfiguration.
     *
     * @param externalId   the external identifier
     * @param providerKey  the provider key
     * @param internalUuid the internal UUID
     */
    public synchronized void put(String externalId, String providerKey, String internalUuid) {
        ProviderExternalId mapping = new ProviderExternalId(externalId, providerKey);

        // Store in both caches
        externalToInternalCache.put(mapping, internalUuid);
        internalToExternalCache.put(internalUuid, mapping);

        logger.debug("Cached mapping: {} <-> {}", mapping, internalUuid);
    }

    /**
     * Invalidate a mapping from both caches.
     * Synchronized to prevent race conditions during cache reconfiguration.
     * <p>
     * This method requires all three parameters to avoid dependency between the two caches
     * during invalidation, as they may be out of sync due to different eviction patterns.
     *
     * @param externalId  the external identifier
     * @param providerKey the provider key
     * @param internalId  the internal identifier
     */
    public synchronized void invalidate(String externalId, String providerKey, String internalId) {
        ProviderExternalId mapping = new ProviderExternalId(externalId, providerKey);
        externalToInternalCache.invalidate(mapping);
        internalToExternalCache.invalidate(internalId);
        logger.debug("Invalidated bidirectional mapping: {} <-> {}", mapping, internalId);
    }

    /**
     * Reconfigure the cache with new settings.
     * This atomically replaces the internal caches with new ones configured with the specified parameters.
     * All existing cache entries will be lost.
     *
     * @param maximumSize       maximum number of entries in each cache
     * @param timeToIdleMinutes time-to-idle duration in minutes
     * @param timeToLiveMinutes time-to-live duration in minutes
     */
    public synchronized void reconfigure(long maximumSize, long timeToIdleMinutes, long timeToLiveMinutes) {
        logger.info("Reconfiguring identifier cache: maxSize={}, timeToIdleMinutes={} minutes, timeToLiveMinutes={} minutes", maximumSize,
                timeToIdleMinutes, timeToLiveMinutes);

        // Build new caches with updated configuration
        Cache<ProviderExternalId, String> newForwardCache = buildCache(maximumSize, timeToIdleMinutes, timeToLiveMinutes);
        Cache<String, ProviderExternalId> newReverseCache = buildCache(maximumSize, timeToIdleMinutes, timeToLiveMinutes);

        // Atomic replacement - this ensures thread safety
        this.externalToInternalCache = newForwardCache;
        this.internalToExternalCache = newReverseCache;

        logger.info("Identifier cache reconfigured successfully");
    }

}

