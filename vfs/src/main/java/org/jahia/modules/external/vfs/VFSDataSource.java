/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2014 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ======================================================================================
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
 *     describing the FLOSS exception, also available here:
 *     http://www.jahia.com/license"
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ======================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 *
 *
 * ==========================================================================================
 * =                                   ABOUT JAHIA                                          =
 * ==========================================================================================
 *
 *     Rooted in Open Source CMS, Jahia’s Digital Industrialization paradigm is about
 *     streamlining Enterprise digital projects across channels to truly control
 *     time-to-market and TCO, project after project.
 *     Putting an end to “the Tunnel effect”, the Jahia Studio enables IT and
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
 */
package org.jahia.modules.external.vfs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.jcr.Binary;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.Selectors;
import org.apache.commons.vfs2.VFS;
import org.apache.jackrabbit.util.ISO8601;
import org.jahia.api.Constants;
import org.jahia.modules.external.ExternalData;
import org.jahia.modules.external.ExternalDataSource;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.nodetypes.ExtendedNodeType;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * VFS Implementation of ExternalDataSource
 */
public class VFSDataSource implements ExternalDataSource, ExternalDataSource.Writable, ExternalDataSource.CanLoadChildrenInBatch {
    private static final List<String> JCR_CONTENT_LIST = Arrays.asList(Constants.JCR_CONTENT);
    private static final Set<String> SUPPORTED_NODE_TYPES = new HashSet<String>(Arrays.asList(Constants.JAHIANT_FILE, Constants.JAHIANT_FOLDER, Constants.JCR_CONTENT));
    private static final Logger logger = LoggerFactory.getLogger(VFSDataSource.class);
    private static final String JCR_CONTENT_SUFFIX = "/" + Constants.JCR_CONTENT;
    private FileObject root;
    private String rootPath;
    private FileSystemManager manager;

    /**
     * Defines the root point of the DataSource
     *
     * @param rootUri
     */
    public void setRoot(String rootUri) {
        try {
            manager = VFS.getManager();
            root = manager.resolveFile(rootUri);
            rootPath = root.getName().getPath();
        } catch (Exception e) {
            throw new RuntimeException("Cannot set root to " + rootUri, e);
        }
    }

    protected FileObject getRoot() {
        return root;
    }

    protected String getRootPath() {
        return rootPath;
    }

    protected FileSystemManager getManager() {
        return manager;
    }

    public boolean isSupportsUuid() {
        return false;
    }

    @Override
    public boolean isSupportsHierarchicalIdentifiers() {
        return true;
    }

    @Override
    public boolean itemExists(String path) {
        try {
            FileObject file = getFile(path.endsWith(JCR_CONTENT_SUFFIX) ? StringUtils.substringBeforeLast(
                    path, JCR_CONTENT_SUFFIX) : path);
            return file.exists();
        } catch (FileSystemException e) {
            logger.warn("Unable to check file existence for path " + path, e);
        }
        return false;
    }

    @Override
    public void order(String path, List<String> children) throws RepositoryException {
        // ordering is not supported in VFS
    }

    public Set<String> getSupportedNodeTypes() {
        return SUPPORTED_NODE_TYPES;
    }

    public ExternalData getItemByIdentifier(String identifier) throws ItemNotFoundException {
        if (identifier.startsWith("/")) {
            try {
                return getItemByPath(identifier);
            } catch (PathNotFoundException e) {
                throw new ItemNotFoundException(identifier, e);
            }
        }
        throw new ItemNotFoundException(identifier);
    }

    public ExternalData getItemByPath(String path) throws PathNotFoundException {
        try {
            if (path.endsWith(JCR_CONTENT_SUFFIX)) {
                FileContent content = getFile(StringUtils.substringBeforeLast(path, JCR_CONTENT_SUFFIX)).getContent();
                return getFileContent(content);
            } else {
                FileObject fileObject = getFile(path);
                if (!fileObject.exists()) {
                    throw new PathNotFoundException(path);
                }
                return getFile(fileObject);
            }

        } catch (FileSystemException e) {
            throw new PathNotFoundException("File system exception while trying to retrieve " + path, e);
        }
    }


    public FileObject getFile(String path) throws FileSystemException {
        return (path == null || path.length() == 0 || path.equals("/")) ? root : root
                .resolveFile(path.charAt(0) == '/' ? path.substring(1) : path);
    }

    public List<String> getChildren(String path) throws RepositoryException {
        try {
            if (!path.endsWith(JCR_CONTENT_SUFFIX)) {
                FileObject fileObject = getFile(path);
                if (fileObject.getType() == FileType.FILE) {
                    return JCR_CONTENT_LIST;
                } else if (fileObject.getType() == FileType.FOLDER) {
                    FileObject[] files = fileObject.getChildren();
                    if (files.length > 0) {
                        List<String> children = new LinkedList<String>();
                        for (FileObject object : files) {
                            if (getSupportedNodeTypes().contains(getDataType(object))) {
                                children.add(object.getName().getBaseName());
                            }
                        }
                        return children;
                    } else {
                        return Collections.emptyList();
                    }
                } else {
                    if (fileObject.exists()) {
                        logger.warn("Found non file or folder entry at path {}, maybe an alias. VFS file type: {}",
                                fileObject, fileObject.getType());
                    } else {
                        throw new PathNotFoundException(path);
                    }
                }
            }
        } catch (FileSystemException e) {
            logger.error("Cannot get node children", e);
        }

        return Collections.emptyList();
    }

    @Override
    public List<ExternalData> getChildrenNodes(String path) throws RepositoryException {
        try {
            if (!path.endsWith(JCR_CONTENT_SUFFIX)) {
                FileObject fileObject = getFile(path);
                if (fileObject.getType() == FileType.FILE) {
                    final FileContent content = fileObject.getContent();
                    return Collections.singletonList(getFileContent(content));
                } else if (fileObject.getType() == FileType.FOLDER) {
                    FileObject[] files = fileObject.getChildren();
                    if (files.length > 0) {
                        List<ExternalData> children = new LinkedList<ExternalData>();
                        for (FileObject object : files) {
                            if (getSupportedNodeTypes().contains(getDataType(object))) {
                                children.add(getFile(object));
                            }
                        }
                        return children;
                    } else {
                        return Collections.emptyList();
                    }
                } else {
                    if (fileObject.exists()) {
                        logger.warn("Found non file or folder entry at path {}, maybe an alias. VFS file type: {}",
                                fileObject, fileObject.getType());
                    } else {
                        throw new PathNotFoundException(path);
                    }
                }
            }
        } catch (FileSystemException e) {
            logger.error("Cannot get node children", e);
        }

        return Collections.emptyList();
    }

    @Override
    public void removeItemByPath(String path) throws RepositoryException {
        try {
            FileObject file = getFile(path);
            if (file.getType().hasChildren()) {
                file.delete(Selectors.SELECT_ALL);
            } else if (!file.delete()) {
                logger.warn("Failed to delete FileObject {}", getFile(path).toString());
            }
        } catch (FileSystemException e) {
            throw new RepositoryException(e);
        }
    }

    public void saveItem(ExternalData data) throws RepositoryException {
        try {
            ExtendedNodeType nodeType = NodeTypeRegistry.getInstance().getNodeType(data.getType());
            if (nodeType.isNodeType(Constants.NT_RESOURCE) && StringUtils.contains(data.getPath(), Constants.JCR_CONTENT)) {
                OutputStream outputStream = null;
                try {
                    final Binary[] binaries = data.getBinaryProperties().get(Constants.JCR_DATA);
                    if (binaries.length > 0) {
                        outputStream = getFile(data.getPath().substring(0, data.getPath().indexOf(JCR_CONTENT_SUFFIX))).getContent().getOutputStream();
                        for (Binary binary : binaries) {
                            InputStream stream = null;
                            try {
                                stream = binary.getStream();
                                IOUtils.copy(stream, outputStream);
                            } finally {
                                IOUtils.closeQuietly(stream);
                            }
                        }
                    }
                } catch (IOException e) {
                    throw new PathNotFoundException("I/O on file : " + data.getPath(), e);
                } catch (RepositoryException e) {
                    throw new PathNotFoundException("unable to get outputStream of : " + data.getPath(), e);
                } finally {
                    IOUtils.closeQuietly(outputStream);
                }
            } else if (nodeType.isNodeType("jnt:folder")) {
                try {
                    getFile(data.getPath()).createFolder();
                } catch (FileSystemException e) {
                    throw new PathNotFoundException(data.getPath(), e);
                }
            }
        } catch (NoSuchNodeTypeException e) {
            throw new PathNotFoundException(e);
        }
    }

    @Override
    public void move(String oldPath, String newPath) throws RepositoryException {
        if (oldPath.equals(newPath)) {
            return;
        }
        try {
            FileObject origin = getFile(oldPath);
            if(origin.isContentOpen()) {
                origin.close();
            }
            FileObject destination = getFile(newPath);
            if(destination.exists() && destination.isContentOpen()){
                destination.close();
            }
            origin.moveTo(destination);
        } catch (FileSystemException e) {
            throw new RepositoryException(oldPath, e);
        }
    }

    private ExternalData getFile(FileObject fileObject) throws FileSystemException {
        String type = getDataType(fileObject);

        Map<String, String[]> properties = new HashMap<String, String[]>();
        if (fileObject.getContent() != null) {
            long lastModifiedTime = fileObject.getContent().getLastModifiedTime();
            if (lastModifiedTime > 0) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(lastModifiedTime);
                String[] timestamp = new String[]{ISO8601.format(calendar)};
                properties.put(Constants.JCR_CREATED, timestamp);
                properties.put(Constants.JCR_LASTMODIFIED, timestamp);
            }
        }

        String path = fileObject.getName().getPath().substring(rootPath.length());
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        return new ExternalData(path, path, type, properties);
    }

    public String getDataType(FileObject fileObject) throws FileSystemException {
        return fileObject.getType() == FileType.FILE ? Constants.JAHIANT_FILE
                : Constants.JAHIANT_FOLDER;
    }

    protected ExternalData getFileContent(final FileContent content) throws FileSystemException {
        Map<String, String[]> properties = new HashMap<String, String[]>(1);

        properties.put(Constants.JCR_MIMETYPE, new String[]{getContentType(content)});

        String path = content.getFile().getName().getPath().substring(rootPath.length());
        String jcrContentPath = path + "/" + Constants.JCR_CONTENT;
        ExternalData externalData = new ExternalData(jcrContentPath, jcrContentPath, Constants.JAHIANT_RESOURCE, properties);

        Map<String, Binary[]> binaryProperties = new HashMap<String, Binary[]>(1);
        binaryProperties.put(Constants.JCR_DATA, new Binary[]{new VFSBinaryImpl(content)});
        externalData.setBinaryProperties(binaryProperties);

        return externalData;
    }

    protected String getContentType(FileContent content) throws FileSystemException {
        String s1 = content.getContentInfo().getContentType();
        if (s1 == null) {
            s1 = JCRContentUtils.getMimeType(content.getFile().getName().getBaseName());
        }
        if (s1 == null) {
            s1 = "application/octet-stream";
        }
        return s1;
    }
}
