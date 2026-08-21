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
package org.jahia.modules.external.vfs;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.vfs2.*;
import org.apache.jackrabbit.util.ISO8601;
import org.jahia.api.Constants;
import org.jahia.modules.external.ExternalData;
import org.jahia.modules.external.ExternalDataSource;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.nodetypes.ExtendedNodeType;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Binary;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * VFS Implementation of ExternalDataSource
 */
public class VFSDataSource implements ExternalDataSource, ExternalDataSource.Writable, ExternalDataSource.CanLoadChildrenInBatch {
    private static final List<String> JCR_CONTENT_LIST = Arrays.asList(Constants.JCR_CONTENT);
    private static final Set<String> SUPPORTED_NODE_TYPES = new HashSet<String>(Arrays.asList(Constants.JAHIANT_FILE, Constants.JAHIANT_FOLDER, Constants.JAHIANT_RESOURCE));
    private static final Logger logger = LoggerFactory.getLogger(VFSDataSource.class);
    private static final String JCR_CONTENT_SUFFIX = "/" + Constants.JCR_CONTENT;

    /**
     * How long a root that could not be taken is left alone before a lookup tries it again. A root that names a
     * location which is not answering costs a connection attempt to find out, and that attempt is made while holding
     * this instance, so a mount point on hold must not make one per lookup.
     *
     * <p>Not final: a test shortens the window rather than waiting one out.
     */
    long retryDelayNanos = TimeUnit.SECONDS.toNanos(10);
    /**
     * The root this DataSource serves, and everything derived from it, published as one value: a reader that sees the
     * root sees the path it starts at and the manager that resolved it. Read without a lock on every lookup.
     */
    private final AtomicReference<Root> root = new AtomicReference<>(Root.unset());

    /** The root this DataSource was given, kept so that it can be taken again. Guarded by this. */
    private String rootUri;

    /**
     * When the last attempt at taking the root ended, which is what the retry window is measured from. Measured from
     * the end and not the start: a location that is not answering takes longer to say so than the window itself, so a
     * window measured from the start of the attempt would already have passed by the time the attempt failed, and the
     * lookup waiting behind it would make another. Guarded by this.
     */
    private long lastAttempt;

    /**
     * Defines the root point of the DataSource. This method does not throw: a root that cannot be used leaves the
     * DataSource without one, and every lookup on it then fails instead.
     *
     * @param rootUri the root to use
     */
    public synchronized void setRoot(String rootUri) {
        this.rootUri = rootUri;
        // Read before resolving, so a set that changes while this resolves still reads as a change afterwards.
        Set<String> schemes = VfsRootResolver.getAllowedSchemes();
        try {
            root.set(Root.available(VfsRootResolver.resolveRoot(rootUri), schemes));
        } catch (Exception e) {
            // Every lookup then fails, which is what the repository reads as "this mount point is not available": it
            // records the reason, puts the mount point on hold and asks again later, and leaves the other mount points
            // alone. Throwing here would travel out of the call the repository makes for each of them in turn.
            String reason = "Cannot set root to " + rootUri + ": " + e.getMessage();
            boolean reported = root.getAndSet(Root.unavailable(reason, schemes)).reports(reason);
            if (reported) {
                logger.debug(reason);
            } else {
                logger.warn(reason);
            }
        } finally {
            lastAttempt = System.nanoTime();
        }
    }

    /**
     * The root of this DataSource, taken again when it has none or when the schemes a root may name have changed since
     * it was taken. The repository asks a mount point it put on hold whether it has become available, on the same
     * instance, so a root that could not be used when the mount point was mounted — a set of schemes not yet
     * configured, a location not yet answering — is usable from then on without the instance being restarted. A set
     * that stops naming the scheme of a root already resolved stops that root the same way, rather than serving it
     * until the instance is restarted.
     */
    private Root requireRoot() throws FileSystemException {
        Root current = root.get();
        return current.isTakenUnder(VfsRootResolver.getAllowedSchemes()) ? current : takeRootAgain();
    }

    private synchronized Root takeRootAgain() throws FileSystemException {
        Root current = root.get();
        Set<String> allowed = VfsRootResolver.getAllowedSchemes();
        if (rootUri != null && !current.isTakenUnder(allowed)
                && (!current.wasTakenUnder(allowed) || System.nanoTime() - lastAttempt >= retryDelayNanos)) {
            setRoot(rootUri);
            current = root.get();
        }
        if (current.file == null) {
            throw new VfsRootNotAllowedException(current.unavailableReason != null ? current.unavailableReason
                    : "The root of this mount point is not set");
        }
        return current;
    }

    /**
     * The path of an item of this DataSource, which is the part of its name the root's own path does not cover. A name
     * the root does not cover belongs to a root this DataSource no longer serves, and is answered as a lookup that
     * fails rather than as a string that is too short.
     */
    private String pathWithin(FileName name) throws FileSystemException {
        String rootPath = requireRoot().path;
        String path = name.getPath();
        if (!path.startsWith(rootPath)) {
            throw new VfsRootNotAllowedException(String.format("\"%s\" is not under the root \"%s\" of this mount"
                    + " point", path, rootPath));
        }
        return path.substring(rootPath.length());
    }

    /**
     * @return the root this DataSource serves, or {@code null} while it has none
     */
    // Reads one immutable value out of an atomic reference, which is what publishes it; taking the monitor that
    // setting the root takes would add no guarantee to that read, only a wait behind a resolve.
    @SuppressWarnings("java:S2886")
    protected FileObject getRoot() {
        return root.get().file;
    }

    /** @return the path the root of this DataSource starts at, or {@code null} while it has no root */
    protected String getRootPath() {
        return root.get().path;
    }

    /** @return the manager that resolved the root of this DataSource, or {@code null} while it has no root */
    protected FileSystemManager getManager() {
        return root.get().manager;
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
        } catch (VfsRootNotAllowedException e) {
            logRootUnavailable(path, e);
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
            String unescapedPath = Escaping.unescapeIllegalJcrChars(path);
            if (path.endsWith(JCR_CONTENT_SUFFIX)) {
                FileObject fileObject = getFile(StringUtils.substringBeforeLast(unescapedPath, JCR_CONTENT_SUFFIX), false);
                FileContent content = fileObject.getContent();
                if (!fileObject.exists() || fileObject.isFolder()) {
                    throw new PathNotFoundException(path);
                }
                return getFileContent(content);
            } else {
                FileObject fileObject = getFile(unescapedPath, false);
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
        return getFile(path, true);
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
                                children.add(Escaping.escapeIllegalJcrChars(object.getName().getBaseName()));
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
            logChildrenFailure(path, e);
        }

        return Collections.emptyList();
    }

    @Override
    public List<ExternalData> getChildrenNodes(String path) throws RepositoryException {
        try {
            if (!path.endsWith(JCR_CONTENT_SUFFIX)) {
                FileObject fileObject = getFile(path);
                if (fileObject.getType() == FileType.FILE && fileObject.isReadable()) {
                    final FileContent content = fileObject.getContent();
                    return Collections.singletonList(getFileContent(content));
                } else if (fileObject.getType() == FileType.FOLDER && fileObject.isReadable()) {
                    fileObject.refresh();  //in case of folder, refresh because it could be changed external
                    FileObject[] files = fileObject.getChildren();
                    if (files.length > 0) {
                        List<ExternalData> children = new LinkedList<ExternalData>();
                        for (FileObject object : files) {
                            if (getSupportedNodeTypes().contains(getDataType(object))) {
                                children.add(getFile(object));
                                if (object.getType() == FileType.FILE) {
                                    children.add(getFileContent(object.getContent()));
                                }
                            }
                        }
                        return children;
                    } else {
                        return Collections.emptyList();
                    }
                } else {
                    if (fileObject.exists()) {
                        logger.warn("Found non file or folder entry at path {}, maybe an alias or maybe it is unreadable. VFS file type: {}",
                                fileObject, fileObject.getType());
                    } else {
                        throw new PathNotFoundException(path);
                    }
                }
            }
        } catch (FileSystemException e) {
            logChildrenFailure(path, e);
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
            if (origin.isContentOpen()) {
                origin.close();
            }
            FileObject destination = getFile(newPath);
            if (destination.exists() && destination.isContentOpen()) {
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
        List<String> addedMixins = new ArrayList<>();
        final FileContent content = fileObject.getContent();
        if (content != null) {
            long lastModifiedTime = fileObject.getContent().getLastModifiedTime();
            if (lastModifiedTime > 0) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(lastModifiedTime);
                String[] timestamp = new String[]{ISO8601.format(calendar)};
                properties.put(Constants.JCR_CREATED, timestamp);
                properties.put(Constants.JCR_LASTMODIFIED, timestamp);
            }
            // Add jmix:image mixin in case of the file is a picture.
            if (content.getContentInfo() != null && content.getContentInfo().getContentType() != null
                    && fileObject.getContent().getContentInfo().getContentType().matches("image/(.*)")) {
                addedMixins.add(Constants.JAHIAMIX_IMAGE);
            }

        }

        String path = Escaping.escapeIllegalJcrChars(pathWithin(fileObject.getName()));
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        ExternalData result = new ExternalData(path, path, type, properties);
        result.setMixin(addedMixins);
        return result;
    }

    public String getDataType(FileObject fileObject) throws FileSystemException {
        return fileObject.getType() == FileType.FILE ? Constants.JAHIANT_FILE
                : Constants.JAHIANT_FOLDER;
    }

    protected ExternalData getFileContent(final FileContent content) throws FileSystemException {
        Map<String, String[]> properties = new HashMap<String, String[]>(1);

        properties.put(Constants.JCR_MIMETYPE, new String[]{getContentType(content)});

        String path = Escaping.escapeIllegalJcrChars(pathWithin(content.getFile().getName()));
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

    /**
     * A lookup on a mount point that has no root. The root reports itself when it cannot be taken, so a lookup that
     * finds none says so where a reader looks for it rather than a second time in the log.
     */
    private void logRootUnavailable(String path, VfsRootNotAllowedException e) {
        logger.debug("Cannot reach {} of this mount point: {}", path, e.getMessage());
    }

    /** Where a lookup for the children of a path failed: the root of the mount point, or the lookup itself. */
    private void logChildrenFailure(String path, FileSystemException e) {
        if (e instanceof VfsRootNotAllowedException) {
            logRootUnavailable(path, (VfsRootNotAllowedException) e);
        } else {
            logger.error("Cannot get node children", e);
        }
    }

    private FileObject getFile(String path, boolean unescapePath) throws FileSystemException {
        FileObject current = requireRoot().file;
        if (unescapePath) {
            path = Escaping.unescapeIllegalJcrChars(path);
        }
        return (path == null || path.isEmpty() || path.equals("/")) ? current : current
                .resolveFile(path.charAt(0) == '/' ? path.substring(1) : path);
    }

    /**
     * A root and what is derived from it: the path it starts at, the manager that resolved it, and the schemes it was
     * taken under. Immutable, so the whole group is published at once and a reader needs no lock to see all of it.
     */
    private static final class Root {

        private final FileObject file;
        private final String path;
        private final FileSystemManager manager;
        private final String unavailableReason;
        private final Set<String> schemes;

        private Root(FileObject file, String path, FileSystemManager manager, String unavailableReason,
                Set<String> schemes) {
            this.file = file;
            this.path = path;
            this.manager = manager;
            this.unavailableReason = unavailableReason;
            this.schemes = schemes;
        }

        private static Root available(FileObject file, Set<String> schemes) {
            return new Root(file, file.getName().getPath(), file.getFileSystem().getFileSystemManager(), null, schemes);
        }

        private static Root unavailable(String reason, Set<String> schemes) {
            return new Root(null, null, null, reason, schemes);
        }

        private static Root unset() {
            return unavailable(null, Collections.emptySet());
        }

        /** Whether this root is usable and the schemes it was taken under are still the ones a root may name. */
        private boolean isTakenUnder(Set<String> schemes) {
            return file != null && wasTakenUnder(schemes);
        }

        /** Whether the schemes this root was taken under are still the ones a root may name, usable or not. */
        private boolean wasTakenUnder(Set<String> schemes) {
            return this.schemes.equals(schemes);
        }

        /** Whether this root already answered for the given failure, so that it is reported once and not per lookup. */
        private boolean reports(String reason) {
            return file == null && reason.equals(unavailableReason);
        }
    }
}
