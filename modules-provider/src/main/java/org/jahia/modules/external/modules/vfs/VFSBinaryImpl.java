package org.jahia.modules.external.modules;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.Binary;
import javax.jcr.RepositoryException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JCR {@link Binary} implementation which has VFS' {@link FileContent} as an underlying source.
 * 
 * @author Sergiy Shyrkov
 */
public class VFSBinaryImpl implements Binary {
    
    private static final Logger logger = LoggerFactory.getLogger(VFSBinaryImpl.class);

    private FileContent fileContent;

    /**
     * Initializes an instance of this class with the provided VFS file.
     * 
     * @param fileContent
     *            the VFS file's content to use
     */
    public VFSBinaryImpl(FileContent fileContent) {
        super();
        this.fileContent = fileContent;
    }

    @Override
    public void dispose() {
        try {
            fileContent.close();
        } catch (FileSystemException e) {
            logger.warn(e.getMessage(), e);
        }
    }

    @Override
    public long getSize() throws RepositoryException {
        try {
            return fileContent.getSize();
        } catch (FileSystemException e) {
            throw new RepositoryException(e);
        }
    }

    @Override
    public InputStream getStream() throws RepositoryException {
        try {
            return fileContent.getInputStream();
        } catch (FileSystemException e) {
            throw new RepositoryException(e);
        }
    }

    @Override
    public int read(byte[] b, long position) throws IOException, RepositoryException {
        InputStream is = null;
        int read = 0;
        try {
            is = getStream();
            read = is.read(b, (int) position, b.length);
        } finally {
            IOUtils.closeQuietly(is);
        }
        return read;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj != null && this.getClass() == obj.getClass()) {
            VFSBinaryImpl other = (VFSBinaryImpl)obj;
            return this.fileContent == other.fileContent;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return fileContent.hashCode();
    }
}
