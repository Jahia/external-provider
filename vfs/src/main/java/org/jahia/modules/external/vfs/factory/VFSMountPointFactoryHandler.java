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
package org.jahia.modules.external.vfs.factory;

import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.jahia.modules.external.admin.mount.AbstractMountPointFactoryHandler;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.utils.i18n.Messages;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.binding.message.MessageBuilder;
import org.springframework.binding.message.MessageContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.webflow.execution.RequestContext;

import javax.jcr.RepositoryException;
import java.io.Serializable;
import java.lang.Override;
import java.util.Locale;

/**
 * @author kevan
 */
public class VFSMountPointFactoryHandler extends AbstractMountPointFactoryHandler<VFSMountPointFactory> implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(VFSMountPointFactoryHandler.class);

    private static final long serialVersionUID = 7189210242067838479L;

    private static final String BUNDLE = "resources.external-provider-vfs";

    private VFSMountPointFactory vfsMountPointFactory;

    private String stateCode;
    private String messageKey;


    public void init(RequestContext requestContext) {
        vfsMountPointFactory = new VFSMountPointFactory();
        try {
            super.init(requestContext, vfsMountPointFactory);
        } catch (RepositoryException e) {
            logger.error("Error retrieving mount point", e);
        }
        requestContext.getFlowScope().put("vfsFactory", vfsMountPointFactory);
    }

    public String getFolderList() {
        JSONObject result = new JSONObject();
        try {
            JSONArray folders = JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<JSONArray>() {
                @Override
                public JSONArray doInJCR(JCRSessionWrapper session) throws RepositoryException {
                    return getSiteFolders(session.getWorkspace());
                }
            });

            result.put("folders", folders);
        } catch (RepositoryException e) {
            logger.error("Error trying to retrieve local folders", e);
        } catch (JSONException e) {
            logger.error("Error trying to construct JSON from local folders", e);
        }

        return result.toString();
    }

    /**
     * Saves the VFS mount point.
     * @param messageContext Spring message context to hold error or warning messages
     * @return true if the save was done successfully ,false otherwise
     */
    public Boolean save(MessageContext messageContext, RequestContext requestContext) {
        stateCode = "SUCCESS";
        Locale locale = LocaleContextHolder.getLocale();
        boolean validVFSPoint = validateVFS(vfsMountPointFactory);
        if(!validVFSPoint)
        {
            logger.error("Error saving mount point : " + vfsMountPointFactory.getName() + "with the root : " + vfsMountPointFactory.getRoot());
            MessageBuilder messageBuilder = new MessageBuilder().error().defaultText(Messages.get(BUNDLE, "serverSettings.vfsMountPointFactory.save.error", locale));
            messageContext.addMessage(messageBuilder.build());
            requestContext.getConversationScope().put("adminURL", getAdminURL(requestContext));
            return false;
        }
        try {
            boolean available = super.save(vfsMountPointFactory);
            if (available) {
                stateCode = "SUCCESS";
                messageKey = "serverSettings.vfsMountPointFactory.save.success";
                requestContext.getConversationScope().put("adminURL", getAdminURL(requestContext));
                return true;
            } else {
                logger.warn("Mount point availability problem : " + vfsMountPointFactory.getName() + "with the root : " + vfsMountPointFactory.getRoot() + "the mount point is created but unmounted");
                stateCode = "WARNING";
                messageKey = "serverSettings.vfsMountPointFactory.save.unavailable";
                requestContext.getConversationScope().put("adminURL", getAdminURL(requestContext));
                return true;
            }
        } catch (RepositoryException e) {
            logger.error("Error saving mount point : " + vfsMountPointFactory.getName(), e);
            MessageBuilder messageBuilder = new MessageBuilder().error().defaultText(Messages.get(BUNDLE, "serverSettings.vfsMountPointFactory.save.error", locale));
            messageContext.addMessage(messageBuilder.build());
        }
        return false;
    }

    /**
     * Return the validation state of a vfs mount point
     * @param vfsMountPointFactory
     * @return
     */
    private boolean validateVFS(VFSMountPointFactory vfsMountPointFactory) {
        try {
            VFS.getManager().resolveFile(vfsMountPointFactory.getRoot());
        } catch (FileSystemException e) {
            logger.warn("VFS mount point " +  vfsMountPointFactory.getName() + " has validation problem "  + e.getMessage());
            return false;
        }
        return true;
    }

    @Override
    public String getAdminURL(RequestContext requestContext) {
        StringBuilder builder = new StringBuilder(super.getAdminURL(requestContext));
        if(stateCode != null && messageKey != null)
        {
            builder.append("?stateCode=").append(stateCode).append("&messageKey=").append(messageKey).append("&bundleSource=").append(BUNDLE);
        }
        return builder.toString();
    }
}
