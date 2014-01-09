/**
 * This file is part of Jahia, next-generation open source CMS:
 * Jahia's next-generation, open source CMS stems from a widely acknowledged vision
 * of enterprise application convergence - web, search, document, social and portal -
 * unified by the simplicity of web content management.
 *
 * For more information, please visit http://www.jahia.com.
 *
 * Copyright (C) 2002-2013 Jahia Solutions Group SA. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * As a special exception to the terms and conditions of version 2.0 of
 * the GPL (or any later version), you may redistribute this Program in connection
 * with Free/Libre and Open Source Software ("FLOSS") applications as described
 * in Jahia's FLOSS exception. You should have received a copy of the text
 * describing the FLOSS exception, and it is also available here:
 * http://www.jahia.com/license
 *
 * Commercial and Supported Versions of the program (dual licensing):
 * alternatively, commercial and supported versions of the program may be used
 * in accordance with the terms and conditions contained in a separate
 * written agreement between you and Jahia Solutions Group SA.
 *
 * If you are unsure which license is appropriate for your use,
 * please contact the sales department at sales@jahia.com.
 */

package org.jahia.modules.external.modules;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jahia.api.Constants;
import org.jahia.data.templates.JahiaTemplatesPackage;
import org.jahia.services.content.*;
import org.jahia.services.templates.JahiaTemplateManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ModulesImportExportHelper {

    private static final Logger logger = LoggerFactory.getLogger(ModulesImportExportHelper.class);

    private JahiaTemplateManagerService templateService;

    private final Object syncObject = new Object();

    public void setTemplateService(JahiaTemplateManagerService templateService) {
        this.templateService = templateService;
    }

    public void regenerateImportFiles(final Set<String> modules) throws RepositoryException {
        synchronized (syncObject) {
            JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<Object>() {
                @Override
                public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                    for (String module : modules) {
                        JahiaTemplatesPackage pack = templateService.getTemplatePackageById(module);
                        if (pack != null) {
                            File sources = templateService.getSources(pack, session);
                            if (sources != null) {
                                templateService.regenerateImportFile(module, sources, session);
                            }
                        }
                    }
                    return null;
                }
            });
        }
    }

    public void updateImportFileNodes(final List<File> files, final String importFilesRootFolder, final String filesNodePath) {
        synchronized (syncObject) {
            try {
                JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<Object>() {
                    @Override
                    public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                        JCRNodeWrapper filesNode = session.getNode(filesNodePath);
                        for (File file : files) {
                            if (!file.exists()) {
                                String relPath = StringUtils.removeStart(file.getPath(), importFilesRootFolder);
                                if (relPath.endsWith(file.getName() + File.separator + file.getName())) {
                                    relPath = StringUtils.substringBeforeLast(relPath, File.separator);
                                }
                                if (filesNode.hasNode(relPath)) {
                                    JCRNodeWrapper node = filesNode.getNode(relPath);
                                    boolean removeEmptyFolder = !node.hasNodes() && !node.equals(filesNode);
                                    node.remove();
                                    while (removeEmptyFolder) {
                                        JCRNodeWrapper parent = node.getParent();
                                        removeEmptyFolder = !node.hasNodes() && !node.equals(filesNode);
                                        node.remove();
                                        node = parent;
                                    }
                                }
                            } else if (file.isFile()) {
                                JCRNodeWrapper node = filesNode;
                                String[] pathSegments = StringUtils.removeStart(file.getPath(), importFilesRootFolder).split(File.separator);
                                int endIndex;
                                if (pathSegments.length >= 2 && pathSegments[pathSegments.length - 1].equals(pathSegments[pathSegments.length - 2])) {
                                    endIndex = pathSegments.length - 2;
                                } else {
                                    endIndex = pathSegments.length - 1;
                                }

                                for (int i = 0; i < endIndex; i++) {
                                    String pathSegment = pathSegments[i];
                                    if (node.hasNode(pathSegment)) {
                                        node = node.getNode(pathSegment);
                                    } else {
                                        node = node.addNode(pathSegment, Constants.JAHIANT_FOLDER);
                                    }
                                }

                                InputStream is = null;
                                try {
                                    is = new BufferedInputStream(new FileInputStream(file));
                                    node.uploadFile(file.getName(), is, JCRContentUtils.getMimeType(file.getName()));
                                } catch (FileNotFoundException e) {
                                    logger.error("Failed to upload import file", e);
                                } finally {
                                    IOUtils.closeQuietly(is);
                                }
                            }
                        }
                        session.save();
                        return null;
                    }
                });
            } catch (RepositoryException e) {
                logger.error("Failed to update import files", e);
            }
        }
    }
}
