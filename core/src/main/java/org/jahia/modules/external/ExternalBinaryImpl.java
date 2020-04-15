/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.modules.external;

import org.apache.tika.io.IOUtils;

import javax.jcr.Binary;
import javax.jcr.RepositoryException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
* Implementation of the {@link javax.jcr.Binary} for the {@link org.jahia.modules.external.ExternalData}.
* User: loom
* Date: Aug 12, 2010
* Time: 3:21:58 PM
*
*/
public class ExternalBinaryImpl implements Binary {

    private InputStream inputStream = null;

    public ExternalBinaryImpl(InputStream inputStream) {
        if (!inputStream.markSupported()) {
            inputStream = new BufferedInputStream(inputStream);
        }
        this.inputStream = inputStream;
    }

    public InputStream getStream() throws RepositoryException {
        try {
            inputStream.reset();
        } catch (IOException e) {
            throw new RepositoryException(e);
        }
        return inputStream;
    }

    public int read(byte[] b, long position) throws IOException, RepositoryException {
        if (inputStream == null) {
            throw new IOException("Empty stream");
        }
        return inputStream.read(b, (int) position, b.length);
    }

    public long getSize() throws RepositoryException {
        return 0;
    }

    public void dispose() {
        IOUtils.closeQuietly(inputStream);
    }
}
