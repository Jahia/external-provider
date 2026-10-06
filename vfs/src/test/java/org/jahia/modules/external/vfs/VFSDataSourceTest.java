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

import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.provider.local.LocalFileName;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit testing of {@link VFSDataSource}, on the roots it is given
 */
public final class VFSDataSourceTest {

    /** A local directory of its own per case, so that a case which needs a file in it leaves nothing behind. */
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final VFSDataSource dataSource = new VFSDataSource();

    private String localDirectory;

    @Before
    public void useATemporaryDirectory() {
        localDirectory = temporaryFolder.getRoot().getAbsolutePath();
    }

    @After
    public void restoreDefaults() {
        VfsRootResolver.setAllowedSchemes(null);
    }

    @Test
    public void aSupportedRootIsSet() {
        dataSource.setRoot(localDirectory);

        assertNotNull(dataSource.getRoot());
        assertEquals(new File(localDirectory), folderTheRootNames());
    }

    /**
     * The folder the root of the DataSource names. Rebuilt from the name of the root the way
     * {@code ModulesDataSource.getRealRoot()} does, because a local file name keeps the root of the file system apart
     * from the path: on a platform that has drives the path alone is not the absolute path of the folder, so reading
     * it as one would answer for this machine rather than for the code.
     */
    private File folderTheRootNames() {
        FileName name = dataSource.getRoot().getName();
        return new File(((LocalFileName) name).getRootFile(), dataSource.getRootPath());
    }

    /**
     * A lookup reads the root, the path it starts at and the manager that resolved it, so a reader must never see one
     * of them without the others.
     */
    @Test
    public void aRootAndWhatIsDerivedFromItAreSetAndClearedTogether() {
        dataSource.setRoot(localDirectory);

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
        dataSource.setRoot(localDirectory);
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
        dataSource.setRoot(localDirectory);
        assertTrue(dataSource.itemExists("/"));

        VfsRootResolver.setAllowedSchemes(Collections.singletonList("sftp"));

        assertFalse(dataSource.itemExists("/"));
    }

    /** Reading the set again must not cost the mount point its root when the set has not changed. */
    @Test
    public void aRootSurvivesTheSameSetBeingConfiguredAgain() {
        dataSource.setRoot(localDirectory);

        VfsRootResolver.setAllowedSchemes(Collections.singletonList("file"));

        assertTrue(dataSource.itemExists("/"));
    }

    /**
     * The package the resolver lives in is exported, so a root may be held by a bundle other than the one that
     * releases the manager, and a released manager answers nothing. A root taken with one is taken again, against a
     * manager built afresh, rather than answered through the one that was closed under it.
     */
    @Test
    public void aRootIsTakenAgainOnceTheManagerItWasTakenWithIsReleased() {
        dataSource.setRoot(localDirectory);
        FileSystemManager released = dataSource.getManager();

        VfsRootResolver.close();

        assertTrue(dataSource.itemExists("/"));
        assertNotSame(released, dataSource.getManager());
    }

    /**
     * A mount point that has lost its root has no children to answer with, and no empty folder either. A session that
     * already resolved the node reads the failure, the way a lookup by path answers the same condition, rather than
     * an existing folder that has nothing in it.
     */
    @Test
    public void aLostRootIsNotAnsweredAsAnEmptyFolder() throws RepositoryException {
        dataSource.setRoot(localDirectory);
        VfsRootResolver.setAllowedSchemes(Collections.singletonList("sftp"));

        try {
            dataSource.getChildren("/");
            fail("Expected the children lookup to report the root as unusable");
        } catch (PathNotFoundException e) {
            assertNotNull(e.getMessage());
        }
        try {
            dataSource.getChildrenNodes("/");
            fail("Expected the children lookup to report the root as unusable");
        } catch (PathNotFoundException e) {
            assertNotNull(e.getMessage());
        }
    }

    /**
     * The root of a mount point can be set again while a lookup is under way, so a name the root does not cover is
     * answered as a lookup that fails rather than as a path that is shorter than the root it is measured against.
     */
    @Test
    public void aNameTheRootDoesNotCoverIsAnsweredAsAFailedLookup() throws IOException {
        File inner = temporaryFolder.newFolder("vfs-data-source-root", "inner");
        File outside = temporaryFolder.newFile("vfs-data-source-outside.txt");
        dataSource.setRoot(inner.getAbsolutePath());

        // resolved through the manager the data source is using, because the resolver answers for a root and this
        // names a file
        FileContent content = dataSource.getManager().resolveFile(outside.getAbsolutePath()).getContent();

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
            dataSource.setRoot(localDirectory);
            for (int lookup = 0; lookup < 5; lookup++) {
                assertFalse(dataSource.itemExists("/"));
            }
        });

        assertEquals("the attempts six lookups made: " + levelsOf(attempts), 1, attempts.size());
        assertEquals(Level.WARN, attempts.get(0).getLevel());
    }

    /**
     * Taking a root costs a connection to whatever it names, and a location that is not answering takes as long as it
     * takes. A lookup made while an attempt is under way reports the mount point as it stands rather than waiting for
     * that connection, because every request thread that touches the mount point would otherwise wait with it.
     */
    @Test(timeout = 60000)
    public void aLookupDoesNotWaitBehindAnAttemptAtTakingTheRoot() throws InterruptedException {
        CountDownLatch taking = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicBoolean slowly = new AtomicBoolean();
        VFSDataSource takesItsRootSlowly = new VFSDataSource() {
            @Override
            public synchronized void setRoot(String rootUri) {
                if (slowly.get()) {
                    taking.countDown();
                    awaitQuietly(release);
                }
                super.setRoot(rootUri);
            }

            @Override
            long retryDelayNanos() {
                return 0;
            }
        };
        VfsRootResolver.setAllowedSchemes(Collections.singletonList("sftp"));
        takesItsRootSlowly.setRoot(localDirectory);
        slowly.set(true);

        Thread attempt = new Thread(() -> takesItsRootSlowly.itemExists("/"));
        attempt.setDaemon(true);
        attempt.start();
        assertTrue("the attempt should have started", taking.await(10, TimeUnit.SECONDS));

        // The attempt holds until it is released below, so a lookup that waited for it is a lookup that took the
        // whole of HELD_SECONDS. Timed rather than merely answered: waiting for the attempt answers too, late.
        long start = System.nanoTime();
        assertFalse(takesItsRootSlowly.itemExists("/"));
        long waited = System.nanoTime() - start;

        release.countDown();
        attempt.join();
        assertTrue("a lookup made while the root was being taken waited " + TimeUnit.NANOSECONDS.toMillis(waited)
                + "ms for it", waited < TimeUnit.SECONDS.toNanos(HELD_SECONDS / 2));
    }

    /** How long the attempt in the case above is held for, which is what a lookup must not wait. */
    private static final long HELD_SECONDS = 20;

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await(HELD_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Once the window has passed the root is taken again, and the failure it answers with is already reported. */
    @Test
    public void aRootIsTakenAgainOnceTheWindowHasPassed() {
        VfsRootResolver.setAllowedSchemes(Collections.singletonList("sftp"));
        VFSDataSource takenAgainAtOnce = new VFSDataSource() {
            @Override
            long retryDelayNanos() {
                return 0;
            }
        };
        List<LogEvent> attempts = attemptsWhile(() -> {
            takenAgainAtOnce.setRoot(localDirectory);
            assertFalse(takenAgainAtOnce.itemExists("/"));
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
        dataSource.setRoot(localDirectory);

        assertNotNull(dataSource.getRoot());
    }
}
