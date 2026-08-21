package org.jahia.modules.external.vfs;

import org.junit.After;
import org.junit.Test;

import javax.jcr.RepositoryException;
import java.io.File;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit testing of {@link VFSDataSource}, on the roots it is given
 */
public final class VFSDataSourceTest {

    private static final String LOCAL_DIRECTORY = new File(System.getProperty("java.io.tmpdir")).getAbsolutePath();

    private final VFSDataSource dataSource = new VFSDataSource();

    @After
    public void restoreDefaults() {
        VfsRootResolver.setAllowedSchemes(null);
    }

    @Test
    public void aSupportedRootIsSet() {
        dataSource.setRoot(LOCAL_DIRECTORY);

        assertNotNull(dataSource.getRoot());
        assertEquals(LOCAL_DIRECTORY, dataSource.getRootPath());
    }

    /**
     * A lookup reads the root, the path it starts at and the manager that resolved it, so a reader must never see one
     * of them without the others.
     */
    @Test
    public void aRootAndWhatIsDerivedFromItAreSetAndClearedTogether() {
        dataSource.setRoot(LOCAL_DIRECTORY);

        assertNotNull(dataSource.getRoot());
        assertNotNull(dataSource.getRootPath());
        assertNotNull(dataSource.getManager());

        dataSource.setRoot("https://example.com/");

        assertNull(dataSource.getRoot());
        assertNull(dataSource.getRootPath());
        assertNull(dataSource.getManager());
    }

    @Test
    public void anUnsupportedRootLeavesTheDataSourceWithoutOne() {
        dataSource.setRoot("https://example.com/");

        assertNull(dataSource.getRoot());
    }

    /**
     * The repository asks a provider whether it is available by reading its root node, and reads a RepositoryException
     * as "not available". Anything else travels out of the call that mounts it.
     */
    @Test
    public void anUnsupportedRootReportsItselfThroughARepositoryException() {
        dataSource.setRoot("https://example.com/");

        try {
            dataSource.getItemByPath("/");
            fail("Expected the item lookup to report the root as unusable");
        } catch (RepositoryException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void anUnsupportedRootHasNoItems() {
        dataSource.setRoot("https://example.com/");

        assertFalse(dataSource.itemExists("/"));
    }

    /**
     * The repository asks the same instance again, so a root refused when the mount point was mounted has to become
     * usable once the schemes it names are allowed, without the mount point being mounted again.
     */
    @Test
    public void aRootRefusedByTheConfiguredSetIsTakenAgainOnceItIsAllowed() {
        VfsRootResolver.setAllowedSchemes(Collections.singletonList("sftp"));
        dataSource.setRoot(LOCAL_DIRECTORY);
        assertFalse(dataSource.itemExists("/"));

        VfsRootResolver.setAllowedSchemes(null);

        assertTrue(dataSource.itemExists("/"));
    }

    /**
     * The set a root was taken under is the set it goes on being served under, so narrowing the configuration stops a
     * mount point whose root it no longer allows, the way widening it starts one.
     */
    @Test
    public void aRootIsRefusedOnceTheConfiguredSetStopsAllowingIt() {
        dataSource.setRoot(LOCAL_DIRECTORY);
        assertTrue(dataSource.itemExists("/"));

        VfsRootResolver.setAllowedSchemes(Collections.singletonList("sftp"));

        assertFalse(dataSource.itemExists("/"));
    }

    /** Reading the set again must not cost the mount point its root when the set has not changed. */
    @Test
    public void aRootSurvivesTheSameSetBeingConfiguredAgain() {
        dataSource.setRoot(LOCAL_DIRECTORY);

        VfsRootResolver.setAllowedSchemes(Collections.singletonList("file"));

        assertTrue(dataSource.itemExists("/"));
    }

    @Test
    public void aRootSetAgainAfterAnUnsupportedOneIsUsable() {
        dataSource.setRoot("https://example.com/");
        dataSource.setRoot(LOCAL_DIRECTORY);

        assertNotNull(dataSource.getRoot());
    }
}
