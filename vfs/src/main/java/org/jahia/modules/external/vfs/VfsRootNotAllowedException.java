package org.jahia.modules.external.vfs;

import org.apache.commons.vfs2.FileSystemException;

/**
 * Signals that the root path of a VFS mount point cannot be used.
 *
 * <p>A {@link FileSystemException} so that callers already handling one keep working, but the message is the plain
 * text passed in: the base class reads its message as a key into Apache VFS's own resource bundle, and a text that
 * is not a key there renders as {@code Unknown message with code "..."}.
 */
public class VfsRootNotAllowedException extends FileSystemException {

    private static final long serialVersionUID = 1L;

    private final String message;

    public VfsRootNotAllowedException(String message) {
        super(message);
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
