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
