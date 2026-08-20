package org.jahia.modules.external.vfs;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit testing of {@link VfsRootResolver}
 */
public final class VfsRootResolverTest {

    private static final String LOCAL_DIRECTORY = new File(System.getProperty("java.io.tmpdir")).getAbsolutePath();

    @After
    public void restoreDefaults() {
        VfsRootResolver.setAllowedSchemes(null);
    }

    @Test
    public void localSchemeIsResolved() throws FileSystemException {
        FileObject root = VfsRootResolver.resolveRoot("file://" + LOCAL_DIRECTORY);

        assertEquals(VfsRootResolver.LOCAL_SCHEME, root.getName().getScheme());
        // the data source reads the manager back off the resolved root
        assertNotNull(root.getFileSystem().getFileSystemManager());
    }

    @Test
    public void pathWithoutSchemeIsResolvedAsLocal() throws FileSystemException {
        FileObject root = VfsRootResolver.resolveRoot(LOCAL_DIRECTORY);
        assertEquals(VfsRootResolver.LOCAL_SCHEME, root.getName().getScheme());
    }

    @Test
    public void remoteAndLayeredSchemesAreRefused() {
        assertSchemeRefused("http://localhost/", "http");
        assertSchemeRefused("https://example.com/", "https");
        assertSchemeRefused("ftp://127.0.0.1/", "ftp");
        assertSchemeRefused("sftp://127.0.0.1/", "sftp");
        assertSchemeRefused("smb://host/share", "smb");
        assertSchemeRefused("res:some/resource", "res");
        assertSchemeRefused("jar:file:///tmp/a.jar", "jar");
        assertSchemeRefused("zip:file:///tmp/a.zip", "zip");
        assertSchemeRefused("tar:gz:http://127.0.0.1/a.tgz", "tar");
    }

    @Test
    public void schemeIsMatchedRegardlessOfCase() {
        assertSchemeRefused("HTTPS://example.com/", "https");
    }

    @Test
    public void singleLetterPrefixIsAPathNotAScheme() throws FileSystemException {
        // a Windows drive must not read as a URI scheme; resolving it is the local provider's business
        VfsRootResolver.checkSchemeAllowed("d:/pdf-files");
    }

    @Test
    public void blankRootIsRefused() {
        assertRefused(null);
        assertRefused("");
        assertRefused("   ");
    }

    @Test
    public void defaultAllowedSchemesAreTheLocalFilesystemAlone() {
        assertEquals(Collections.singleton(VfsRootResolver.LOCAL_SCHEME), VfsRootResolver.getAllowedSchemes());
    }

    @Test
    public void configuredSchemesAreTrimmedLoweredAndReplaceTheDefault() throws FileSystemException {
        VfsRootResolver.setAllowedSchemes(Arrays.asList(" FILE ", "sftp"));

        assertEquals(new LinkedHashSet<>(Arrays.asList("file", "sftp")), VfsRootResolver.getAllowedSchemes());
        VfsRootResolver.checkSchemeAllowed("sftp://127.0.0.1/");
        VfsRootResolver.checkSchemeAllowed("file://" + LOCAL_DIRECTORY);
        // widening the set adds only what was configured
        assertSchemeRefused("https://example.com/", "https");
    }

    @Test
    public void emptyConfigurationRestoresTheLocalFilesystem() {
        VfsRootResolver.setAllowedSchemes(Collections.emptyList());

        assertEquals(Collections.singleton(VfsRootResolver.LOCAL_SCHEME), VfsRootResolver.getAllowedSchemes());
    }

    @Test
    public void aConfiguredValueThatIsNotASchemeNameRestoresTheLocalFilesystem() {
        // the set also decides which providers the resolving manager carries, so an unreadable set must not widen it
        VfsRootResolver.setAllowedSchemes(Arrays.asList("[Ljava.lang.String;@1f2a3b", "file,https", "http://x"));

        assertEquals(Collections.singleton(VfsRootResolver.LOCAL_SCHEME), VfsRootResolver.getAllowedSchemes());
        assertSchemeRefused("https://example.com/", "https");
    }

    @Test
    public void aPathWithoutASchemeIsRefusedWhenTheLocalFilesystemIsNotAllowed() {
        VfsRootResolver.setAllowedSchemes(Collections.singletonList("sftp"));

        // a plain path and a file: URI name the same place, so the allowed set answers for both alike
        assertSchemeRefused("/data/files", VfsRootResolver.LOCAL_SCHEME);
        assertSchemeRefused("file:///data/files", VfsRootResolver.LOCAL_SCHEME);
    }

    @Test
    public void aReadableSchemeSurvivesAlongsideAnUnreadableOne() {
        VfsRootResolver.setAllowedSchemes(Arrays.asList("sftp", "not a scheme"));

        assertEquals(Collections.singleton("sftp"), VfsRootResolver.getAllowedSchemes());
    }

    private static void assertSchemeRefused(String rootPath, String expectedScheme) {
        String message = assertRefused(rootPath);
        assertTrue("Expected the message to name the scheme " + expectedScheme + ", got: " + message,
                message.contains('"' + expectedScheme + '"'));
        // Apache VFS reads a FileSystemException message as a resource-bundle key; the message must survive that
        assertFalse("The message was rendered as a VFS message code: " + message,
                message.contains("Unknown message with code"));
    }

    private static String assertRefused(String rootPath) {
        try {
            VfsRootResolver.checkSchemeAllowed(rootPath);
        } catch (FileSystemException e) {
            return e.getMessage();
        }
        fail("Expected the root path to be refused: " + rootPath);
        return null;
    }
}
