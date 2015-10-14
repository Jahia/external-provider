/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 * <p/>
 * Copyright (C) 2002-2015 Jahia Solutions Group SA. All rights reserved.
 * <p/>
 * THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 * 1/GPL OR 2/JSEL
 * <p/>
 * 1/ GPL
 * ======================================================================================
 * <p/>
 * IF YOU DECIDE TO CHOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 * <p/>
 * "This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 * <p/>
 * As a special exception to the terms and conditions of version 2.0 of
 * the GPL (or any later version), you may redistribute this Program in connection
 * with Free/Libre and Open Source Software ("FLOSS") applications as described
 * in Jahia's FLOSS exception. You should have received a copy of the text
 * describing the FLOSS exception, also available here:
 * http://www.jahia.com/license"
 * <p/>
 * 2/ JSEL - Commercial and Supported Versions of the program
 * ======================================================================================
 * <p/>
 * IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 * <p/>
 * Alternatively, commercial and supported versions of the program - also known as
 * Enterprise Distributions - must be used in accordance with the terms and conditions
 * contained in a separate written agreement between you and Jahia Solutions Group SA.
 * <p/>
 * If you are unsure which license is appropriate for your use,
 * please contact the sales department at sales@jahia.com.
 * <p/>
 * <p/>
 * ==========================================================================================
 * =                                   ABOUT JAHIA                                          =
 * ==========================================================================================
 * <p/>
 * Rooted in Open Source CMS, Jahia’s Digital Industrialization paradigm is about
 * streamlining Enterprise digital projects across channels to truly control
 * time-to-market and TCO, project after project.
 * Putting an end to “the Tunnel effect”, the Jahia Studio enables IT and
 * marketing teams to collaboratively and iteratively build cutting-edge
 * online business solutions.
 * These, in turn, are securely and easily deployed as modules and apps,
 * reusable across any digital projects, thanks to the Jahia Private App Store Software.
 * Each solution provided by Jahia stems from this overarching vision:
 * Digital Factory, Workspace Factory, Portal Factory and eCommerce Factory.
 * Founded in 2002 and headquartered in Geneva, Switzerland,
 * Jahia Solutions Group has its North American headquarters in Washington DC,
 * with offices in Chicago, Toronto and throughout Europe.
 * Jahia counts hundreds of global brands and governmental organizations
 * among its loyal customers, in more than 20 countries across the globe.
 * <p/>
 * For more information, please visit http://www.jahia.com
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
import java.util.Locale;

/**
 * @author kevan
 */
public class VFSMountPointFactoryHandler extends AbstractMountPointFactoryHandler<VFSMountPointFactory> implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(VFSMountPointFactoryHandler.class);

    private static final long serialVersionUID = 7189210242067838479L;

    private static final String BUNDLE = "resources.external-provider-vfs";

    private VFSMountPointFactory vfsMountPointFactory;


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
    public Boolean save(MessageContext messageContext) {
        Locale locale = LocaleContextHolder.getLocale();
        boolean validVFSPoint = validateVFS(vfsMountPointFactory);
        if(!validVFSPoint)
        {
            logger.error("Error saving mount point : " + vfsMountPointFactory.getName() + "with the root : " + vfsMountPointFactory.getRoot());
            MessageBuilder messageBuilder = new MessageBuilder().error().defaultText(Messages.get(BUNDLE, "serverSettings.vfsMountPointFactory.save.error", locale));
            messageContext.addMessage(messageBuilder.build());
            return false;
        }
        try {
            boolean available = super.save(vfsMountPointFactory);
            if (available) {
                return true;
            } else {
                logger.warn("Mount point availability problem : " + vfsMountPointFactory.getName() + "with the root : " + vfsMountPointFactory.getRoot() + "the mount point is created but unmounted");
                MessageBuilder messageBuilder = new MessageBuilder().warning().defaultText(Messages.get(BUNDLE, "serverSettings.vfsMountPointFactory.save.unavailable", locale));
                messageContext.addMessage(messageBuilder.build());
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
}
