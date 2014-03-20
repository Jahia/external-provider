/**
 * ==========================================================================================
 * =                        DIGITAL FACTORY v7.0 - Community Distribution                   =
 * ==========================================================================================
 *
 *     Rooted in Open Source CMS, Jahia's Digital Industrialization paradigm is about
 *     streamlining Enterprise digital projects across channels to truly control
 *     time-to-market and TCO, project after project.
 *     Putting an end to "the Tunnel effect", the Jahia Studio enables IT and
 *     marketing teams to collaboratively and iteratively build cutting-edge
 *     online business solutions.
 *     These, in turn, are securely and easily deployed as modules and apps,
 *     reusable across any digital projects, thanks to the Jahia Private App Store Software.
 *     Each solution provided by Jahia stems from this overarching vision:
 *     Digital Factory, Workspace Factory, Portal Factory and eCommerce Factory.
 *     Founded in 2002 and headquartered in Geneva, Switzerland,
 *     Jahia Solutions Group has its North American headquarters in Washington DC,
 *     with offices in Chicago, Toronto and throughout Europe.
 *     Jahia counts hundreds of global brands and governmental organizations
 *     among its loyal customers, in more than 20 countries across the globe.
 *
 *     For more information, please visit http://www.jahia.com
 *
 * JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION
 * ============================================
 *
 *     Copyright (C) 2002-2014 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==========================================================
 *
 *     IF YOU DECIDE TO CHOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     "This program is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU General Public License
 *     as published by the Free Software Foundation; either version 2
 *     of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 *     As a special exception to the terms and conditions of version 2.0 of
 *     the GPL (or any later version), you may redistribute this Program in connection
 *     with Free/Libre and Open Source Software ("FLOSS") applications as described
 *     in Jahia's FLOSS exception. You should have received a copy of the text
 *     describing the FLOSS exception, and it is also available here:
 *     http://www.jahia.com/license"
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ==========================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
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

    private EhCacheProvider cacheProvider;
    // The ID mapping cache, where a key is a <providerKey>-<externalId-hashCode> and a value is
    // the corresponding internalId
    private Cache idCache;

    private List<String> overridableItemsForLocks;

    private JCRStoreProvider extensionProvider;


    @Override
    public void delete(List<String> externalIds, String providerKey, boolean includeDescendants)
            throws RepositoryException {
        if (externalIds.isEmpty()) {
            return;
        }
        StatelessSession session = null;
        try {
            List<Integer> hashes = new LinkedList<Integer>();
            for (String externalId : externalIds) {
                int hash = externalId.hashCode();
                hashes.add(hash);
            }
            session = hibernateSessionFactory.openStatelessSession();
            session.beginTransaction();

            // delete all
            session.createQuery(
                    "delete from UuidMapping where providerKey=:providerKey and externalIdHash in (:externalIds)")
                    .setString("providerKey", providerKey).setParameterList("externalIds", hashes).executeUpdate();

            if (includeDescendants) {
                // delete descendants
                Query selectStmt = session.createQuery("from UuidMapping where providerKey=? and externalId like ?").setString(0, providerKey);
                for (String externalId : externalIds) {
                    selectStmt.setString(1, externalId + "/%");
                    List<?> descendants = selectStmt.list();

                    for (Object mapping : descendants) {
                        UuidMapping m = (UuidMapping) mapping;
                        session.delete(m);
                        invalidateCache(m.getExternalIdHash(), providerKey);
                    }
                }
            }

            session.getTransaction().commit();

            for (String externalId : externalIds) {
                int hash = externalId.hashCode();
                invalidateCache(hash, providerKey);
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

    protected String getCacheKey(int externalIdHash, String providerKey) {
        return providerKey + "-" + externalIdHash;
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
        int hash = externalId.hashCode();
        String cacheKey = getCacheKey(hash, providerKey);
        String uuid = getIdentifierCache().get(cacheKey) != null ? (String)getIdentifierCache().get(cacheKey).getObjectValue() : null;
        if (uuid == null) {
            StatelessSession session = null;
            try {
                session = getHibernateSessionFactory().openStatelessSession();
                session.beginTransaction();
                List<?> list = session.createQuery("from UuidMapping where providerKey=? and externalIdHash=?")
                        .setString(0, providerKey).setLong(1, hash).setReadOnly(true).list();
                if (list.size() > 0) {
                    uuid = ((UuidMapping) list.get(0)).getInternalUuid();
                    getIdentifierCache().put(new Element(cacheKey, uuid, true));
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
            List<?> list = session.createQuery("from ExternalProviderID where providerKey=?").setString(0, providerKey)
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
                    throw new RepositoryException("Issue when storing external provider ID for provider " + providerId,
                            e);
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

    public void invalidateCache(int externalIdHash, String providerKey) {
        getIdentifierCache().remove(getCacheKey(externalIdHash, providerKey));
    }

    public void invalidateCache(String externalId, String providerKey) {
        invalidateCache(externalId.hashCode(), providerKey);
    }

    @Override
    public String mapInternalIdentifier(String externalId, String providerKey, String providerId)
            throws RepositoryException {
        UuidMapping uuidMapping = new UuidMapping();
        uuidMapping.setExternalId(externalId);
        uuidMapping.setProviderKey(providerKey);
        uuidMapping.setInternalUuid(providerId + "-" + StringUtils.substringAfter(UUID.randomUUID().toString(), "-"));
        org.hibernate.Session session = null;
        try {
            session = getHibernateSessionFactory().openSession();
            session.beginTransaction();
            session.save(uuidMapping);
            session.getTransaction().commit();

            // cache it
            getIdentifierCache().put(new Element(getCacheKey(externalId.hashCode(), providerKey), uuidMapping.getInternalUuid(), true));
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
            int deletedCount = session.createQuery("delete from ExternalProviderID where providerKey=?")
                    .setString(0, providerKey).executeUpdate();
            if (deletedCount > 0) {
                logger.info("Deleted external provider entry for key {}", providerKey);
                deletedCount = session.createQuery("delete from UuidMapping where providerKey=?")
                        .setString(0, providerKey).executeUpdate();
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
        this.cacheProvider = cacheProvider;
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
            List<?> list = session.createQuery("from UuidMapping where providerKey=? and externalIdHash=?")
                    .setString(0, providerKey).setLong(1, oldExternalId.hashCode()).list();
            if (list.size() > 0) {
                for (Object mapping : list) {
                    ((UuidMapping) mapping).setExternalId(newExternalId);
                    invalidate.add(oldExternalId);
                }
            }
            if (includeDescendants) {
                // update descendants
                List<?> descendants = session.createQuery("from UuidMapping where providerKey=? and externalId like ?")
                        .setString(0, providerKey).setString(1, oldExternalId + "/%").list();
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

    public void setExtensionProvider(JCRStoreProvider extensionProvider) {
        this.extensionProvider = extensionProvider;
    }

    public List<String> getOverridableItemsForLocks() {
        return overridableItemsForLocks;
    }

    public JCRStoreProvider getExtensionProvider() {
        return extensionProvider;
    }
}
