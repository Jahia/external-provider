package org.jahia.modules.external.vfs;

import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.junit.After;
import org.junit.Test;

import javax.jcr.RepositoryException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    private final File outside = new File(LOCAL_DIRECTORY, "vfs-data-source-outside.txt");

    @After
    public void restoreDefaults() {
        VfsRootResolver.setAllowedSchemes(null);
        assertTrue("the file the test created should be removed", !outside.exists() || outside.delete());
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

    /**
     * The root of a mount point can be set again while a lookup is under way, so a name the root does not cover is
     * answered as a lookup that fails rather than as a path that is shorter than the root it is measured against.
     */
    @Test
    public void aNameTheRootDoesNotCoverIsAnsweredAsAFailedLookup() throws IOException {
        File inner = new File(LOCAL_DIRECTORY, "vfs-data-source-root/inner");
        assertTrue("the root the test needs should exist", inner.isDirectory() || inner.mkdirs());
        assertTrue("the file the test needs should exist", outside.isFile() || outside.createNewFile());
        dataSource.setRoot(inner.getAbsolutePath());

        FileContent content = VfsRootResolver.resolveRoot(outside.getAbsolutePath()).getContent();

        try {
            dataSource.getFileContent(content);
            fail("Expected the name outside the root to be refused");
        } catch (FileSystemException e) {
            assertNotNull(e.getMessage());
        }
    }

    /**
     * A mount point on hold is asked for its root by every lookup that reaches it, and finding out that a location
     * does not answer costs a connection attempt made while holding the instance. The attempts are bounded by the
     * window, whatever the lookups do, and the failure is reported once rather than once per attempt.
     */
    @Test
    public void aRootThatCannotBeTakenIsTakenAgainOncePerWindow() {
        VfsRootResolver.setAllowedSchemes(Collections.singletonList("sftp"));
        List<LogEvent> attempts = attemptsWhile(() -> {
            dataSource.setRoot(LOCAL_DIRECTORY);
            for (int lookup = 0; lookup < 5; lookup++) {
                assertFalse(dataSource.itemExists("/"));
            }
        });

        assertEquals("the attempts six lookups made: " + levelsOf(attempts), 1, attempts.size());
        assertEquals(Level.WARN, attempts.get(0).getLevel());
    }

    /** Once the window has passed the root is taken again, and the failure it answers with is already reported. */
    @Test
    public void aRootIsTakenAgainOnceTheWindowHasPassed() {
        VfsRootResolver.setAllowedSchemes(Collections.singletonList("sftp"));
        List<LogEvent> attempts = attemptsWhile(() -> {
            dataSource.setRoot(LOCAL_DIRECTORY);
            dataSource.retryDelayNanos = 0;
            assertFalse(dataSource.itemExists("/"));
        });

        assertEquals("the attempts two lookups made: " + levelsOf(attempts), 2, attempts.size());
        assertEquals(Level.WARN, attempts.get(0).getLevel());
        // the same root failing the same way again, which the mount point has already reported
        assertEquals(Level.DEBUG, attempts.get(1).getLevel());
    }

    /**
     * The attempts at taking the root that were made while a body ran. Every attempt that fails reports the root it
     * could not take, once, so those reports count the attempts; the lines a lookup writes when it finds no root are
     * not attempts and are left out.
     */
    private static List<LogEvent> attemptsWhile(Runnable body) {
        Logger dataSourceLogger = (Logger) LogManager.getLogger(VFSDataSource.class);
        Level level = dataSourceLogger.getLevel();
        CapturingAppender appender = new CapturingAppender();
        appender.start();
        dataSourceLogger.addAppender(appender);
        dataSourceLogger.setLevel(Level.DEBUG);
        try {
            body.run();
        } finally {
            dataSourceLogger.removeAppender(appender);
            dataSourceLogger.setLevel(level);
            appender.stop();
        }
        List<LogEvent> attempts = new ArrayList<>();
        for (LogEvent event : appender.events) {
            if (event.getMessage().getFormattedMessage().startsWith("Cannot set root to ")) {
                attempts.add(event);
            }
        }
        return attempts;
    }

    private static String levelsOf(List<LogEvent> events) {
        List<String> levels = new ArrayList<>();
        for (LogEvent event : events) {
            levels.add(event.getLevel() + " " + event.getMessage().getFormattedMessage());
        }
        return levels.toString();
    }

    private static final class CapturingAppender extends AbstractAppender {

        private final List<LogEvent> events = new ArrayList<>();

        private CapturingAppender() {
            super("capturing", null, null, true, Property.EMPTY_ARRAY);
        }

        @Override
        public void append(LogEvent event) {
            events.add(event.toImmutable());
        }
    }

    @Test
    public void aRootSetAgainAfterAnUnsupportedOneIsUsable() {
        dataSource.setRoot("https://example.com/");
        dataSource.setRoot(LOCAL_DIRECTORY);

        assertNotNull(dataSource.getRoot());
    }
}
