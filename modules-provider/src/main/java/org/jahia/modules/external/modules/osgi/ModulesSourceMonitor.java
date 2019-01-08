package org.jahia.modules.external.modules.osgi;

import org.apache.commons.vfs2.FileObject;

import java.io.File;

public interface ModulesSourceMonitor {
    boolean canHandleFileType(FileObject filePath);
    void handleFile(File file);
}
