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
import org.apache.commons.lang.StringUtils;
import org.hibernate.*;
import org.jahia.modules.external.ExternalProviderInitializerService;
import org.jahia.services.cache.ehcache.EhCacheProvider;
import org.jahia.services.content.JCRStoreProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * {@inheritDoc}
 */
public class ExternalProviderInitializerServiceImpl implements ExternalProviderInitializerService {

    private static final String ID_CACHE_NAME = "ExternalIdentifierMapping";

    private static final Logger logger = LoggerFactory.getLogger(ExternalProviderInitializerServiceImpl.class);

    private SessionFactory hibernateSessionFactory;

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
        StatelessSession session = null;
        try {
            session = hibernateSessionFactory.openStatelessSession();
            session.beginTransaction();

            // delete all
            // First select mapping objects by external ID hashcodes, then only delete desired ones among them.
            List<Integer> hashes = new LinkedList<Integer>();
            for (String externalId : externalIds) {
                hashes.add(externalId.hashCode());
            }
            List<?> results = session.createQuery("from UuidMapping where providerKey=:providerKey and externalIdHash in (:idHashes)")
                    .setString("providerKey", providerKey).setParameterList("idHashes", hashes).setReadOnly(true).list();
            for (Object result : results) {
                UuidMapping uuidMapping = (UuidMapping) result;
                if (externalIds.contains(uuidMapping.getExternalId())) {
                    session.delete(uuidMapping);
                }
            }

            if (includeDescendants) {
                // delete descendants
                Query selectStmt = session.createQuery("from UuidMapping where providerKey=:providerKey and externalId like :externalId").setString("providerKey", providerKey);
                for (String externalId : externalIds) {
                    selectStmt.setString("externalId", externalId + "/%");
                    List<?> descendants = selectStmt.list();

                    for (Object mapping : descendants) {
                        UuidMapping m = (UuidMapping) mapping;
                        session.delete(m);
                        invalidateCache(m.getExternalId(), providerKey);
                    }
                }
            }

            session.getTransaction().commit();

            for (String externalId : externalIds) {
                invalidateCache(externalId, providerKey);
            }
        } catch (Exception e) {
            if (session != null) {
                session.getTransaction().rollback();
            }
            throw new RepositoryException(e);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    private String getCacheKey(String externalId, String providerKey) {
        return providerKey + "-" + externalId;
    }

    @Override
    public String getExternalIdentifier(String internalId) throws RepositoryException {
        String externalId = null;
        StatelessSession session = null;
        try {
            session = getHibernateSessionFactory().openStatelessSession();
            session.beginTransaction();
            UuidMapping mapping = (UuidMapping) session.get(UuidMapping.class, internalId);
            if (mapping != null) {
                externalId = mapping.getExternalId();
            }
            session.getTransaction().commit();
        } catch (Exception e) {
            if (session != null) {
                session.getTransaction().rollback();
            }
            throw new RepositoryException(e);
        } finally {
            if (session != null) {
                session.close();
            }
        }

        return externalId;
    }

    public SessionFactory getHibernateSessionFactory() {
        return hibernateSessionFactory;
    }

    public Cache getIdentifierCache() {
        return idCache;
    }

    @Override
    public String getInternalIdentifier(String externalId, String providerKey) throws RepositoryException {

        String cacheKey = getCacheKey(externalId, providerKey);
        Cache idCache = getIdentifierCache();
        Element cacheElement = idCache.get(cacheKey);
        String uuid = cacheElement != null ? (String) cacheElement.getObjectValue() : null;

        if (uuid == null) {
            StatelessSession session = null;
            try {

                session = getHibernateSessionFactory().openStatelessSession();
                session.beginTransaction();

                // First select potentially multiple mapping objects by external ID hashcode, then find the desired one among the results.
                List<?> results = session.createQuery("from UuidMapping where providerKey=:providerKey and externalIdHash=:idHash")
                        .setString("providerKey", providerKey).setLong("idHash", externalId.hashCode()).setReadOnly(true).list();
                for (Object result : results) {
                    UuidMapping uuidMapping = (UuidMapping) result;
                    if (uuidMapping.getExternalId().equals(externalId)) {
                        uuid = uuidMapping.getInternalUuid();
                        idCache.put(new Element(cacheKey, uuid, true));
                    }
                }

                session.getTransaction().commit();
            } catch (Exception e) {
                if (session != null) {
                    session.getTransaction().rollback();
                }
                throw new RepositoryException(e);
            } finally {
                if (session != null) {
                    session.close();
                }
            }
        }

        return uuid;
    }

    @Override
    public Integer getProviderId(String providerKey) throws RepositoryException {
        ExternalProviderID providerId = null;
        SessionFactory hibernateSession = getHibernateSessionFactory();
        Session session = null;
        try {
            session = hibernateSession.openSession();
            List<?> list = session.createQuery("from ExternalProviderID where providerKey=:providerKey").setString("providerKey", providerKey)
                    .setReadOnly(true).setFlushMode(FlushMode.MANUAL).list();
            if (list.size() > 0) {
                providerId = (ExternalProviderID) list.get(0);
            } else {
                // not registered yet -> generate ID and store it
                providerId = new ExternalProviderID();
                providerId.setProviderKey(providerKey);
                try {
                    session.beginTransaction();
                    session.save(providerId);
                    session.getTransaction().commit();
                } catch (Exception e) {
                    session.getTransaction().rollback();

                    // Retry with a select, could have been rejected due to a previous concurrent insert
                    // (In cluster mode multiple jahia instances trying to insert the same providerKey for example)
                    list = session.createQuery("from ExternalProviderID where providerKey=:providerKey").setString("providerKey", providerKey)
                            .setReadOnly(true).setFlushMode(FlushMode.MANUAL).list();
                    if (list.size() > 0) {
                        providerId = (ExternalProviderID) list.get(0);
                    } else {
                        throw new RepositoryException("Issue when storing external provider ID for provider " + providerId, e);
                    }
                }
            }
        } catch (HibernateException e) {
            throw new RepositoryException("Issue when obtaining external provider ID for provider " + providerId, e);
        } finally {
            if (session != null) {
                session.close();
            }
        }

        return providerId.getId();
    }

    private void invalidateCache(String externalId, String providerKey) {
        getIdentifierCache().remove(getCacheKey(externalId, providerKey));
    }

    @Override
    public String mapInternalIdentifier(String externalId, String providerKey, String providerId)
            throws RepositoryException {
        UuidMapping uuidMapping = new UuidMapping();
        uuidMapping.setExternalId(externalId);
        uuidMapping.setProviderKey(providerKey);
        uuidMapping.setInternalUuid(providerId + "-" + StringUtils.substringAfter(UUID.randomUUID().toString(), "-"));
        org.hibernate.Session session = null;
        Thread currentThread = Thread.currentThread();
        ClassLoader previousClassLoader = currentThread.getContextClassLoader();
        currentThread.setContextClassLoader(this.getClass().getClassLoader());
        try {
            session = getHibernateSessionFactory().openSession();
            session.beginTransaction();
            session.save(uuidMapping);
            session.getTransaction().commit();

            // cache it
            getIdentifierCache().put(new Element(getCacheKey(externalId, providerKey), uuidMapping.getInternalUuid(), true));
        } catch (Exception e) {
            if (session != null) {
                session.getTransaction().rollback();
            }
            throw new RepositoryException("Error storing mapping for external node " + externalId + " [provider: "
                    + providerKey + "]", e);
        } finally {
            if (session != null) {
                session.close();
            }
            currentThread.setContextClassLoader(previousClassLoader);
        }

        return uuidMapping.getInternalUuid();
    }

    @Override
    public void removeProvider(String providerKey) throws RepositoryException {
        SessionFactory hibernateSession = getHibernateSessionFactory();
        StatelessSession session = null;
        try {
            session = hibernateSession.openStatelessSession();
            session.beginTransaction();
            int deletedCount = session.createQuery("delete from ExternalProviderID where providerKey=:providerKey")
                    .setString("providerKey", providerKey).executeUpdate();
            if (deletedCount > 0) {
                logger.info("Deleted external provider entry for key {}", providerKey);
                deletedCount = session.createQuery("delete from UuidMapping where providerKey=:providerKey")
                        .setString("providerKey", providerKey).executeUpdate();
                logger.info("Deleted {} identifier mapping entries for external provider with key {}", deletedCount,
                        providerKey);
            } else {
                logger.info("No external provider entry found for key {}", providerKey);
            }
            session.getTransaction().commit();
        } catch (Exception e) {
            if (session != null) {
                session.getTransaction().rollback();
            }
            throw new RepositoryException(
                    "Issue when removing external provider entry and identifier mappings for provider key "
                            + providerKey, e);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    public void setHibernateSessionFactory(SessionFactory hibernateSession) {
        this.hibernateSessionFactory = hibernateSession;
    }

    public void setCacheProvider(EhCacheProvider cacheProvider) {
        idCache = cacheProvider.getCacheManager().getCache(ID_CACHE_NAME);
        if (idCache == null) {
            cacheProvider.getCacheManager().addCache(ID_CACHE_NAME);
            idCache = cacheProvider.getCacheManager().getCache(ID_CACHE_NAME);
        }
    }

    @Override
    public void updateExternalIdentifier(String oldExternalId, String newExternalId, String providerKey,
                                         boolean includeDescendants) throws RepositoryException {
        Session session = null;
        try {
            List<String> invalidate = new ArrayList<String>();
            session = getHibernateSessionFactory().openSession();
            session.beginTransaction();

            // First select potentially multiple mapping objects by external ID hashcode, then update only the desired one within the results.
            List<?> results = session.createQuery("from UuidMapping where providerKey=:providerKey and externalIdHash=:idHash")
                    .setString("providerKey", providerKey).setLong("idHash", oldExternalId.hashCode()).list();
            for (Object result : results) {
                UuidMapping uuidMapping = (UuidMapping) result;
                if (uuidMapping.getExternalId().equals(oldExternalId)) {
                    uuidMapping.setExternalId(newExternalId);
                    invalidate.add(oldExternalId);
                }
            }

            if (includeDescendants) {
                // update descendants
                List<?> descendants = session.createQuery("from UuidMapping where providerKey=:providerKey and externalId like :externalId")
                        .setString("providerKey", providerKey).setString("externalId", oldExternalId + "/%").list();
                for (Object mapping : descendants) {
                    UuidMapping m = (UuidMapping) mapping;
                    m.setExternalId(newExternalId + StringUtils.substringAfter(m.getExternalId(), oldExternalId));
                    invalidate.add(m.getExternalId());
                }
            }
            session.getTransaction().commit();
            for (String id : invalidate) {
                invalidateCache(id, providerKey);
            }
        } catch (Exception e) {
            if (session != null) {
                session.getTransaction().rollback();
            }
            throw new RepositoryException(e);
        } finally {
            if (session != null) {
                session.close();
            }
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
        return overridableItemsForLocks;
    }

    @Override
    public List<String> getOverridableItemsForACLs() {
        return overridableItemsForACLs;
    }

    @Override
    public JCRStoreProvider getExtensionProvider() {
        return extensionProvider;
    }
}
