/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2019 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
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
package org.jahia.modules.external.admin.mount;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.util.Text;
import org.jahia.api.Constants;
import org.jahia.modules.external.admin.mount.model.MountPoint;
import org.jahia.modules.external.admin.mount.model.MountPointFactory;
import org.jahia.modules.external.admin.mount.model.MountPointManager;
import org.jahia.services.content.*;
import org.jahia.services.content.decorator.JCRMountPointNode;
import org.jahia.services.content.nodetypes.ExtendedNodeType;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.jahia.utils.i18n.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.binding.message.MessageBuilder;
import org.springframework.binding.message.MessageContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.webflow.execution.RequestContext;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import java.io.Serializable;
import java.util.*;

/**
 * @author kevan
 */
public class MountPointsManagementFlowHandler implements Serializable {
    private static final long serialVersionUID = 1436197019769187454L;
    private static Logger logger = LoggerFactory.getLogger(MountPointsManagementFlowHandler.class);
    private static final String BUNDLE = "resources.JahiaExternalProvider";

    public enum Actions {
        MOUNT, UNMOUNT, DELETE
    }

    @Autowired
    private transient JCRStoreService jcrStoreService;

    public void init(RequestContext requestContext, MessageContext messageContext) {

        String stateCode = requestContext.getRequestParameters().get("stateCode");
        String messageKey = requestContext.getRequestParameters().get("messageKey");
        String bundleSource = requestContext.getRequestParameters().get("bundleSource");
        if(stateCode != null && messageKey != null && bundleSource!=null)
        {
            Locale locale = LocaleContextHolder.getLocale();

            MessageBuilder messageBuilder = new MessageBuilder();
            String message = Messages.get(bundleSource, messageKey, locale);
            if("ERROR".equals(stateCode) &&  message != null)
            {
                messageBuilder = messageBuilder.error().defaultText(message);
            }
            if("WARNING".equals(stateCode))
            {
                messageBuilder = messageBuilder.warning().defaultText(message);
            }
            if("SUCCESS".equals(stateCode))
            {
                messageBuilder = messageBuilder.info().defaultText(message);
            }
            messageContext.addMessage(messageBuilder.build());
        }
    }


    public MountPointManager getMountPointManagerModel() {
        try {
            return JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<MountPointManager>() {
                @Override
                public MountPointManager doInJCR(JCRSessionWrapper session) throws RepositoryException {
                    // get mount points
                    final NodeIterator nodeIterator = getMountPoints(session);
                    List<MountPoint> mountPoints = new ArrayList<>((int) nodeIterator.getSize());
                    while (nodeIterator.hasNext()) {
                        JCRMountPointNode mountPointNode = (JCRMountPointNode) nodeIterator.next();
                        mountPoints.add(new MountPoint(mountPointNode));
                    }

                    // get provider factories
                    Map<String, ProviderFactory> providerFactories = jcrStoreService.getProviderFactories();
                    Map<String, MountPointFactory> mountPointFactories = new HashMap<>();
                    for (ProviderFactory factory : providerFactories.values()) {
                        ExtendedNodeType type = NodeTypeRegistry.getInstance().getNodeType(factory.getNodeTypeName());

                        // calcul the factory URL
                        String queryString = "select * from [jmix:mountPointFactory] as factory where isdescendantnode(factory,'/modules/') and ['j:mountPointType'] = '" + type.getName() + "'";
                        Query query = session.getWorkspace().getQueryManager().createQuery(queryString, Query.JCR_SQL2);
                        QueryResult queryResult = query.execute();
                        String endOfURL = null;
                        if (queryResult.getNodes().getSize() > 0) {
                            JCRNodeWrapper factoryNode = (JCRNodeWrapper) queryResult.getNodes().next();
                            String templateName = factoryNode.getPropertyAsString("j:templateName");
                            if (StringUtils.isNotEmpty(templateName)) {
                                endOfURL = Text.escapePath(factoryNode.getPath()) + "." + templateName + ".html";
                                mountPointFactories.put(type.getName(), new MountPointFactory(type.getName(), type.getLabel(LocaleContextHolder.getLocale()), endOfURL));
                            }
                        }

                    }

                    // return model
                    return new MountPointManager(mountPointFactories, mountPoints);
                }
            });
        } catch (RepositoryException e) {
            logger.error("Error retrieving mount points", e);
            return new MountPointManager();
        }
    }

    public void doAction(final String mountPointName, Actions action, MessageContext messageContext) {
        boolean success = false;

        switch (action) {
            case MOUNT:
                success = mount(mountPointName);
                break;
            case UNMOUNT:
                success = unmount(mountPointName);
                break;
            case DELETE:
                success = delete(mountPointName);
                break;
        }

        handleMessages(messageContext, action, mountPointName, success);
    }

    private boolean mount(final String mountPointName) {
        boolean success = false;
        try {
            success = JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<Boolean>() {
                @Override
                public Boolean doInJCR(JCRSessionWrapper session) throws RepositoryException {
                    JCRMountPointNode mountPointNode = getMountPoint(session, mountPointName);
                    if (mountPointNode != null) {
                        if (mountPointNode.getMountStatus() != JCRMountPointNode.MountStatus.unmounted) {
                            logger.error("Can't mount {}, mount status of the mount point is not unmounted", mountPointName);
                            return false;
                        }
                        mountPointNode.setMountStatus(JCRMountPointNode.MountStatus.mounted);
                        session.save();
                        JCRStoreProvider mountProvider = mountPointNode.getMountProvider();
                        return mountProvider.isAvailable();
                    } else {
                        logger.error("Can't mount {}, no mount point node found", mountPointName);
                        return false;
                    }
                }
            });
        } catch (RepositoryException e) {
            logger.error("Error trying to mount " + mountPointName, e);
        }
        return success;
    }

    private boolean unmount(final String mountPointName) {
        boolean success = false;
        try {
            success = JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<Boolean>() {
                @Override
                public Boolean doInJCR(JCRSessionWrapper session) throws RepositoryException {
                    JCRMountPointNode mountPointNode = getMountPoint(session, mountPointName);
                    if (mountPointNode != null) {
                        if (mountPointNode.getMountStatus() != JCRMountPointNode.MountStatus.mounted) {
                            logger.error("Can't mount {}, current mount status of the mount point is not mounted", mountPointName);
                            return false;
                        }

                        mountPointNode.setMountStatus(JCRMountPointNode.MountStatus.unmounted);
                        session.save();
                        return mountPointNode.getMountProvider() == null;
                    } else {
                        logger.error("Can't mount {}, no mount point node found", mountPointName);
                        return false;
                    }
                }
            });
        } catch (RepositoryException e) {
            logger.error("Error trying to unmount " + mountPointName, e);
        }
        return success;
    }

    private boolean delete(final String mountPointName) {
        boolean success = false;
        try {
            success = JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<Boolean>() {
                @Override
                public Boolean doInJCR(JCRSessionWrapper session) throws RepositoryException {
                    JCRMountPointNode mountPointNode = getMountPoint(session, mountPointName);
                    if (mountPointNode != null) {
                        mountPointNode.remove();
                        session.save();
                        return getMountPoint(session, mountPointName) == null;
                    } else {
                        logger.error("Can't delete {}, no mount point node found", mountPointName);
                        return false;
                    }
                }
            });
        } catch (RepositoryException e) {
            logger.error("Error trying to delete " + mountPointName, e);
        }
        return success;
    }

    private void handleMessages(MessageContext messageContext, Actions action, String mountPoint, boolean success) {
        Locale locale = LocaleContextHolder.getLocale();
        String message = Messages.getWithArgs(BUNDLE, "serverSettings.mountPointsManagement.action." + (success ? "successMessage" : "failMessage"), locale,
                action, mountPoint);
        MessageBuilder messageBuilder = new MessageBuilder();
        if (success) {
            messageBuilder.info().defaultText(message);
        } else {
            messageBuilder.error().defaultText(message);
        }
        messageContext.addMessage(messageBuilder.build());
    }

    private JCRMountPointNode getMountPoint(JCRSessionWrapper sessionWrapper, String name) throws RepositoryException {
        Query query = sessionWrapper.getWorkspace().getQueryManager().createQuery(getMountPointQuery(name), Query.JCR_SQL2);
        QueryResult queryResult = query.execute();
        return queryResult.getNodes().getSize() > 0 ? (JCRMountPointNode) queryResult.getNodes().next() : null;
    }

    private NodeIterator getMountPoints(JCRSessionWrapper sessionWrapper) throws RepositoryException {
        Query query = sessionWrapper.getWorkspace().getQueryManager().createQuery(getMountPointQuery(null), Query.JCR_SQL2);
        return query.execute().getNodes();
    }

    private String getMountPointQuery(String name) {
        String query = "select * from [" + Constants.JAHIANT_MOUNTPOINT + "] as mount where ischildnode('/mounts')";
        if (StringUtils.isNotEmpty(name)) {
            query += (" and ['j:nodename'] = '" + name + "'");
        }
        return query;
    }

    public void setJcrStoreService(JCRStoreService jcrStoreService) {
        this.jcrStoreService = jcrStoreService;
    }
}
