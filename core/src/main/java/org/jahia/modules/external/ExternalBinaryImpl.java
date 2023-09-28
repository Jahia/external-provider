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
package org.jahia.modules.external;

import org.apache.commons.io.IOUtils;

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
