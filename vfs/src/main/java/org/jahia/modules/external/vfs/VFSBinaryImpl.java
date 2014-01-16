/**
 * This file is part of Jahia, next-generation open source CMS:
 * Jahia's next-generation, open source CMS stems from a widely acknowledged vision
 * of enterprise application convergence - web, search, document, social and portal -
 * unified by the simplicity of web content management.
 *
 * For more information, please visit http://www.jahia.com.
 *
 * Copyright (C) 2002-2014 Jahia Solutions Group SA. All rights reserved.
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

package org.jahia.modules.external.vfs;

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

}
