package org.jahia.modules.external.vfs;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
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
        checkSchemeAllowed("d:/pdf-files");
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
        checkSchemeAllowed("sftp://127.0.0.1/");
        checkSchemeAllowed("file://" + LOCAL_DIRECTORY);
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
    public void aLayeredRootIsRefusedWhenTheSchemeUnderneathIsNotAllowed() {
        VfsRootResolver.setAllowedSchemes(Arrays.asList("file", "gz"));

        // a layer reaches whatever the layer below it names, so the set has to answer for each of them
        assertSchemeRefused("gz:http://localhost/", "http");
        assertSchemeRefused("gz:https://example.com/", "https");
        // the outermost layer the set does not name is the one reported
        assertSchemeRefused("tar:gz:http://localhost/a.tgz", "tar");
    }

    @Test
    public void aLayeredRootOverTheLocalFilesystemIsAllowedWhenBothSchemesAre() throws FileSystemException {
        VfsRootResolver.setAllowedSchemes(Arrays.asList("file", "gz"));

        checkSchemeAllowed("gz:file:///data/archive.gz");
        checkSchemeAllowed("gz:/data/archive.gz");
    }

    @Test
    public void aLayeredRootOverAPathIsRefusedWhenTheLocalFilesystemIsNotAllowed() {
        VfsRootResolver.setAllowedSchemes(Collections.singletonList("gz"));

        // the layer under gz: is a path, which names the same place as file:// and is answered for alike
        assertSchemeRefused("gz:/data/archive.gz", VfsRootResolver.LOCAL_SCHEME);
        assertSchemeRefused("gz:file:///data/archive.gz", VfsRootResolver.LOCAL_SCHEME);
        assertSchemeRefused("/data/archive.gz", VfsRootResolver.LOCAL_SCHEME);
    }

    @Test
    public void aLocalRootThatCarriesAnAuthorityIsStillLocal() throws FileSystemException {
        // file://d:/pdf-files is the form the documentation gives for a Windows drive, and it names file: alone
        checkSchemeAllowed("file://d:/pdf-files");
        checkSchemeAllowed("//data/files");
    }

    @Test
    public void aRootThatNamesAFileIsRefused() throws IOException {
        File file = new File(LOCAL_DIRECTORY, "vfs-root-resolver-file.txt");
        assertTrue("the file the test needs should exist", file.isFile() || file.createNewFile());

        try {
            VfsRootResolver.resolveRoot(file.getAbsolutePath());
            fail("Expected a root naming a file to be refused: " + file);
        } catch (FileSystemException e) {
            assertTrue("Expected the message to name the root, got: " + e.getMessage(),
                    e.getMessage().contains(file.getAbsolutePath()));
        }
    }

    @Test
    public void aRootThatDoesNotExistYetIsAccepted() throws FileSystemException {
        // a mount point may name a folder created after it, and the repository asks it again until it is there
        FileObject root = VfsRootResolver.resolveRoot(new File(LOCAL_DIRECTORY, "vfs-root-not-yet").getAbsolutePath());

        assertEquals(VfsRootResolver.LOCAL_SCHEME, root.getName().getScheme());
    }

    @Test
    public void aReadableSchemeSurvivesAlongsideAnUnreadableOne() {
        VfsRootResolver.setAllowedSchemes(Arrays.asList("sftp", "not a scheme"));

        assertEquals(Collections.singleton("sftp"), VfsRootResolver.getAllowedSchemes());
    }

    /** The check reads its set from the caller, so each case states the set in force when it calls it. */
    private static void checkSchemeAllowed(String rootPath) throws FileSystemException {
        VfsRootResolver.checkSchemeAllowed(rootPath, VfsRootResolver.getAllowedSchemes());
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
            checkSchemeAllowed(rootPath);
        } catch (FileSystemException e) {
            return e.getMessage();
        }
        fail("Expected the root path to be refused: " + rootPath);
        return null;
    }
}
