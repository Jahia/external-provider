/*
 * Copyright (C) 2002-2023 Jahia Solutions Group SA. All rights reserved.
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
