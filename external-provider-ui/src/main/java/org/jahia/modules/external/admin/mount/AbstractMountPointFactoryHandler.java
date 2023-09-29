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
package org.jahia.modules.external.admin.mount;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.jahia.services.content.*;
import org.jahia.services.content.decorator.JCRMountPointNode;
import org.jahia.services.render.RenderContext;
import org.jahia.settings.SettingsBean;
import org.jahia.utils.Patterns;
import org.jahia.utils.Url;
import org.json.JSONArray;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.webflow.execution.RequestContext;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Workspace;
import javax.jcr.query.Query;
import java.io.Serializable;
import java.util.Locale;

/**
 * @author kevan
 */
public abstract class AbstractMountPointFactoryHandler<T extends AbstractMountPointFactory> implements Serializable{
    private static final long serialVersionUID = 6394236759186947423L;
    private static final String SITES_QUERY = "select * from [jnt:virtualsite] as f where ischildnode(f,['/sites'])";
    private static final String FILES_NODENAME = "files";
    private static final String JNT_FOLDER = "jnt:folder";

    public T init(RequestContext requestContext, final T mountPointFactory) throws RepositoryException {
        if(mountPointFactory == null){
            return null;
        }

        final String mountPointIdentifier = requestContext.getRequestParameters().get("edit");
        if (StringUtils.isNotEmpty(mountPointIdentifier)) {
            return JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<T>() {
                @Override
                public T doInJCR(JCRSessionWrapper session) throws RepositoryException {
                    mountPointFactory.populate(session.getNodeByIdentifier(mountPointIdentifier));
                    return mountPointFactory;
                }
            });
        } else {
            return mountPointFactory;
        }
    }

    public Boolean save(final T mountPoint) throws RepositoryException {
        return JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<Boolean>() {
            @Override
            public Boolean doInJCR(JCRSessionWrapper session) throws RepositoryException {
                // create mount node
                JCRMountPointNode jcrMountPointNode;
                JCRNodeWrapper mounts = session.getNode("/mounts");
                if (mountPoint.isEdit()) {
                    jcrMountPointNode = (JCRMountPointNode) session.getNode(mountPoint.getInEditMountPointNodePath());

                    // rename if necessary
                    if (!jcrMountPointNode.getName().equals(mountPoint.getName() + JCRMountPointNode.MOUNT_SUFFIX)) {
                        jcrMountPointNode.rename(findAvailableNodeName(mounts, JCRContentUtils.escapeLocalNodeName(mountPoint.getName()), JCRMountPointNode.MOUNT_SUFFIX));
                    }
                } else {
                    jcrMountPointNode = (JCRMountPointNode) mounts.addNode(findAvailableNodeName(mounts, JCRContentUtils.escapeLocalNodeName(mountPoint.getName()), JCRMountPointNode.MOUNT_SUFFIX),
                            mountPoint.getMountNodeType());
                }

                jcrMountPointNode.setMountStatus(JCRMountPointNode.MountStatus.mounted);

                // local path
                if(StringUtils.isNotEmpty(mountPoint.getLocalPath())){
                    jcrMountPointNode.setProperty("mountPoint", session.getNode(mountPoint.getLocalPath()).getIdentifier());
                } else if (mountPoint.isEdit() && jcrMountPointNode.hasProperty("mountPoint")){
                    jcrMountPointNode.getProperty("mountPoint").remove();
                }

                // mount point properties
                mountPoint.setProperties(jcrMountPointNode);
                session.save();

                JCRStoreProvider provider = jcrMountPointNode.getMountProvider();
                return provider != null && provider.isAvailable();
            }
        });
    }

    public String getAdminURL(RequestContext requestContext) {
        RenderContext renderContext = getRenderContext(requestContext);
        Locale locale = LocaleContextHolder.getLocale();
        String server = Url.getServer(renderContext.getRequest());
        String context = renderContext.getURLGenerator().getContext();
        return server + context + "/cms/adminframe/default/" + locale + "/settings.manageMountPoints.html";
    }

    private RenderContext getRenderContext(RequestContext requestContext) {
        return (RenderContext) requestContext.getExternalContext().getRequestMap().get("renderContext");
    }

    protected JSONArray getSiteFolders(Workspace workspace) throws RepositoryException {
        return getSiteFolders(workspace, true, FILES_NODENAME, JNT_FOLDER);
    }

    protected JSONArray getSiteFolders(Workspace workspace, String nodeName) throws RepositoryException {
        return getSiteFolders(workspace, true, nodeName, JNT_FOLDER);
    }

    protected JSONArray getSiteFolders(Workspace workspace, String nodeName, String folderType) throws RepositoryException {
        return getSiteFolders(workspace, true, nodeName, folderType);
    }

    protected JSONArray getSiteFolders(Workspace workspace, boolean local) throws RepositoryException {
        return getSiteFolders(workspace, local, FILES_NODENAME, JNT_FOLDER);
    }

    protected JSONArray getSiteFolders(Workspace workspace, boolean local, String nodeName, String folderType) throws RepositoryException {
        JSONArray folders = new JSONArray();
        Query sitesQuery = workspace.getQueryManager().createQuery(SITES_QUERY, Query.JCR_SQL2);
        NodeIterator sites = sitesQuery.execute().getNodes();

        while (sites.hasNext()) {
            Node site = sites.nextNode();
            Node siteFiles;
            try {
                siteFiles = site.getNode(nodeName);
                folders.put(escape(siteFiles.getPath()));
            } catch (RepositoryException e) {
                // no files under the site
                continue;
            }
            StringBuilder filter = new StringBuilder("isdescendantnode(f,['").append(JCRContentUtils.sqlEncode(siteFiles.getPath())).append("'])");
            for (JCRStoreProvider provider : JCRStoreService.getInstance().getSessionFactory().getProviderList()) {
                if (!provider.isDefault() && StringUtils.startsWith(provider.getMountPoint(), siteFiles.getPath())) {
                    filter.append(" and (not isdescendantnode(f,['").append(JCRContentUtils.sqlEncode(provider.getMountPoint())).append("']))");
                }
            }


            Query siteFoldersQuery = workspace.getQueryManager().createQuery("select * from [" + folderType + "] as f where " + filter
                    , Query.JCR_SQL2);

            NodeIterator siteFolders = siteFoldersQuery.execute().getNodes();
            while (siteFolders.hasNext()) {
                Node siteFolder = siteFolders.nextNode();
                folders.put(escape(siteFolder.getPath()));
            }
        }

        return folders;
    }

    protected String escape(String path) {
        return StringUtils.isNotEmpty(path) ? StringEscapeUtils.escapeXml(path) : path;
    }

    protected String findAvailableNodeName(Node dest, String name, String suffix) {
        int i = 1;

        String basename = name;
        int dot = basename.lastIndexOf('.');
        String ext = "";
        if (dot > 0) {
            ext = basename.substring(dot);
            basename = basename.substring(0, dot);
        }
        int und = basename.lastIndexOf('-');
        if (und > -1 && Patterns.NUMBERS.matcher(basename.substring(und + 1)).matches()) {
            basename = basename.substring(0, und);
        }

        do {
            try {
                dest.getNode(name + suffix);
                String newSuffix = "-" + (i++) + ext;
                name = basename + newSuffix;
                //name has a sizelimit of 32 chars
                int maxNameSize = SettingsBean.getInstance().getMaxNameSize();
                if (name.length() > maxNameSize) {
                    name = basename.substring(0, (basename.length() <= maxNameSize ? basename.length() : maxNameSize) - newSuffix.length()) + newSuffix;
                }
            } catch (RepositoryException e) {
                break;
            }

        } while (true);

        return name + suffix;
    }
}