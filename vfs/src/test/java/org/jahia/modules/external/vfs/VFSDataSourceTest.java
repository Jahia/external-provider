package org.jahia.modules.external.vfs;

import org.junit.After;
import org.junit.Test;

import javax.jcr.RepositoryException;
import java.io.File;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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

    @Test
    public void anUnsupportedRootLeavesTheDataSourceWithoutOne() {
        dataSource.setRoot("https://example.com/");

        assertEquals(null, dataSource.getRoot());
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

    @Test
    public void aRootSetAgainAfterAnUnsupportedOneIsUsable() {
        dataSource.setRoot("https://example.com/");
        dataSource.setRoot(LOCAL_DIRECTORY);

        assertNotNull(dataSource.getRoot());
    }
}
