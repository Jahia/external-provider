package org.jahia.modules.external.modules.vfs;

import org.jahia.api.Constants;
import org.jahia.modules.external.ExternalData;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit testing of {@link VFSDataSource}, on the children it lists for a path
 */
public final class VFSDataSourceChildrenTest {

    private static final String CONTENT_OF_THE_FILE = "/file.txt/" + Constants.JCR_CONTENT;

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final VFSDataSource dataSource = new VFSDataSource();

    @Before
    public void serveAFolderThatHoldsAFileAndAnEmptyFolder() throws IOException {
        File root = temporaryFolder.newFolder("root");
        Files.write(new File(root, "file.txt").toPath(), "file".getBytes(StandardCharsets.UTF_8));
        assertTrue(new File(root, "empty").mkdir());
        dataSource.setRoot(root.getAbsolutePath());
    }

    @Test
    public void aFolderListsItsFileAndItsFolder() throws RepositoryException {
        assertEquals(setOf("file.txt", "empty"), new HashSet<>(dataSource.getChildren("/")));
    }

    @Test
    public void aFolderHasANodeForEachChildAndForTheContentOfEachFile() throws RepositoryException {
        assertEquals(setOf("/file.txt", CONTENT_OF_THE_FILE, "/empty"), pathsOf(dataSource.getChildrenNodes("/")));
    }

    @Test
    public void aFileListsItsContent() throws RepositoryException {
        assertEquals(Collections.singletonList(Constants.JCR_CONTENT), dataSource.getChildren("/file.txt"));
    }

    @Test
    public void aFileHasANodeForItsContent() throws RepositoryException {
        List<ExternalData> children = dataSource.getChildrenNodes("/file.txt");
        assertEquals(1, children.size());
        assertEquals(CONTENT_OF_THE_FILE, children.get(0).getPath());
        assertEquals(Constants.JAHIANT_RESOURCE, children.get(0).getType());
    }

    @Test
    public void anEmptyFolderHasNoChildren() throws RepositoryException {
        assertTrue(dataSource.getChildren("/empty").isEmpty());
        assertTrue(dataSource.getChildrenNodes("/empty").isEmpty());
    }

    @Test
    public void theContentOfAFileHasNoChildren() throws RepositoryException {
        assertTrue(dataSource.getChildren(CONTENT_OF_THE_FILE).isEmpty());
        assertTrue(dataSource.getChildrenNodes(CONTENT_OF_THE_FILE).isEmpty());
    }

    @Test
    public void aMissingPathIsNotFound() throws RepositoryException {
        try {
            dataSource.getChildren("/missing");
            fail("children were listed");
        } catch (PathNotFoundException expected) {
            // the path names nothing the data source serves
        }
        try {
            dataSource.getChildrenNodes("/missing");
            fail("children were listed");
        } catch (PathNotFoundException expected) {
            // the path names nothing the data source serves
        }
    }

    private static Set<String> setOf(String... values) {
        return new HashSet<>(Arrays.asList(values));
    }

    private static Set<String> pathsOf(List<ExternalData> nodes) {
        return nodes.stream().map(ExternalData::getPath).collect(Collectors.toSet());
    }
}
