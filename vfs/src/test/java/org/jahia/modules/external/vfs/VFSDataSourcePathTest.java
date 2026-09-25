package org.jahia.modules.external.vfs;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.util.Text;
import org.jahia.api.Constants;
import org.jahia.modules.external.ExternalData;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.jcr.Binary;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit testing of {@link VFSDataSource}, on the paths it resolves against its root
 */
public final class VFSDataSourcePathTest {

    /** A local directory of its own per case, holding the root and its neighbours. */
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final VFSDataSource dataSource = new VFSDataSource();

    /** A file beside the root, so that the root does not hold it. */
    private File beside;

    @Before
    public void serveAFolderThatHasNeighbours() throws IOException {
        File root = temporaryFolder.newFolder("root");
        write(new File(root, "inside.txt"), "inside");
        beside = write(temporaryFolder.newFile("beside.txt"), "beside");
        write(new File(temporaryFolder.newFolder("root-namesake"), "namesake.txt"), "namesake");
        dataSource.setRoot(root.getAbsolutePath());
    }

    @Test
    public void aFileUnderTheRootIsRead() throws RepositoryException, IOException {
        assertEquals("inside", contentOf("/inside.txt"));
    }

    /**
     * A name is resolved, not filtered: one that steps out of a folder and back into the root still names a file
     * under the root.
     */
    @Test
    public void aNameThatStepsBackIntoTheRootIsRead() throws RepositoryException, IOException {
        assertEquals("inside", contentOf("/" + Text.escapeIllegalJcrChars("a/../inside.txt")));
    }

    @Test
    public void aFileBesideTheRootIsNotFoundByItsName() {
        assertNotFound("/beside.txt");
    }

    @Test
    public void aNameThatStepsOutOfTheRootIsNotFound() {
        assertNotFound("/" + Text.escapeIllegalJcrChars("a/../../beside.txt"));
    }

    /** The root is a folder, not a prefix: a folder whose name starts with the root's is not under it. */
    @Test
    public void aNameThatStepsIntoANamesakeOfTheRootIsNotFound() {
        assertNotFound("/" + Text.escapeIllegalJcrChars("a/../../root-namesake/namesake.txt"));
    }

    @Test
    public void aNameThatCarriesALocationOfItsOwnIsNotFound() {
        assertNotFound("/" + Text.escapeIllegalJcrChars(beside.toURI().toString()));
    }

    @Test
    public void aNameThatStepsOutOfTheRootDoesNotExist() {
        assertTrue(dataSource.itemExists("/inside.txt"));
        assertFalse(dataSource.itemExists("/" + Text.escapeIllegalJcrChars("a/../../beside.txt")));
    }

    @Test
    public void aNameThatStepsOutOfTheRootHasNoChildren() throws RepositoryException {
        assertEquals("inside.txt", String.join(",", dataSource.getChildren("/")));
        assertTrue(dataSource.getChildren("/" + Text.escapeIllegalJcrChars("a/../..")).isEmpty());
    }

    @Test
    public void aNameThatStepsOutOfTheRootIsNotRemoved() {
        try {
            dataSource.removeItemByPath("/" + Text.escapeIllegalJcrChars("a/../../beside.txt"));
            fail("the path was removed");
        } catch (RepositoryException expected) {
            // the path names nothing the data source serves
        }
        assertTrue(beside.exists());
    }

    private String contentOf(String path) throws RepositoryException, IOException {
        ExternalData data = dataSource.getItemByPath(path + "/" + Constants.JCR_CONTENT);
        Binary binary = data.getBinaryProperties().get(Constants.JCR_DATA)[0];
        try (InputStream stream = binary.getStream()) {
            return IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
    }

    private void assertNotFound(String path) {
        try {
            dataSource.getItemByPath(path + "/" + Constants.JCR_CONTENT);
            fail(path + " was found");
        } catch (PathNotFoundException expected) {
            // the path names nothing the data source serves
        }
    }

    private static File write(File file, String content) throws IOException {
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return file;
    }

}
