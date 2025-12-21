package duongtran.vctrl.actions;

import duongtran.vctrl.TestUtils;
import duongtran.vctrl.Workspace;
import duongtran.vctrl.index.FileStat;
import duongtran.vctrl.index.Index;
import duongtran.vctrl.index.IndexEntry;
import duongtran.vctrl.reportchanges.Status;
import duongtran.vctrl.reportchanges.StatusEntry;
import duongtran.vctrl.reportchanges.StatusType;
import duongtran.vctrl.storage.Database;
import duongtran.vctrl.storage.FileMode;
import duongtran.vctrl.storage.objects.Commit;
import duongtran.vctrl.utils.DirectoryNames;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

public class StatusActionTest {

    private static final Logger log = LoggerFactory.getLogger(StatusActionTest.class);

    // Vctrl instances
    Workspace workspace;
    Database database;
    Path rootPath;
    Path vctrlPath;
    Path objectPath;

    // Mock directories
    Path firstDir;
    Path subFirstDir;
    Path secondDir;
    Path subSecondDir;

    // Mock files
    Path testFile11;
    Path testFile12;
    Path testFile111;
    Path testFile112;
    Path testFile21;
    Path testFile22;
    Path testFile211;
    Path testFile212;


    @BeforeEach
    void setup() throws IOException {
        TestUtils.createTestWorkspace();
        workspace = Workspace.getInstance();
        database = Database.getInstance();

        // Paths
        rootPath = workspace.getRootPath();
        vctrlPath = workspace.getVctrlPath();
        objectPath = vctrlPath.resolve(DirectoryNames.OBJECTS);

        // Initialize test directories and files
        firstDir = rootPath.resolve("firstDir");
        subFirstDir = firstDir.resolve("subFirstDir");
        secondDir = rootPath.resolve("secondDir");
        subSecondDir = secondDir.resolve("subSecondDir");

        testFile11 = firstDir.resolve("file11.txt");
        testFile12 = firstDir.resolve("file12.txt");
        testFile111 = subFirstDir.resolve("file111.txt");
        testFile112 = subFirstDir.resolve("file112.txt");
        testFile21 = secondDir.resolve("file21.txt");
        testFile22 = secondDir.resolve("file22.txt");
        testFile211 = subSecondDir.resolve("file211.txt");
        testFile212 = subSecondDir.resolve("file212.txt");

    }

    @AfterEach
    void tearDown() {
        TestUtils.removeWorkspace();
        workspace = null;
    }

    /*****************************
     * Workspace/index differences
     ****************************/

    /**
     * The {@code WorkspaceIndexDifferences} class is designed to test various scenarios involving
     * differences between the workspace and the index in a version control system.
     * It contains unit test methods to validate behaviors such as identifying untracked files,
     * changed contents, deleted files, and other states in the workspace compared to the last
     * staged state.
     * <p>
     * The test methods in this class check for specific conditions and report scenarios where
     * discrepancies, modifications, or absent items exist in the workspace and index.
     * <p>
     * Methods:
     * - {@code testExecuteReportUntrackedFilesWithoutIndexFile}: Validates the reporting of untracked files when the index file is missing.
     * - {@code testExecuteReportUntrackedFilesHasIndexFile}: Validates the reporting of untracked files when an index file is present.
     * - {@code testExecuteReportUntrackedDirectories}: Verifies the identification of untracked directories in the workspace.
     * - {@code testExecuteReportUntrackedEmptyDirectories}: Checks for untracked empty directories in the workspace.
     * - {@code testExecuteReportChangedContentsFromLastStaged}: Ensures changes in file contents from the last staged state are identified.
     * - {@code testExecuteReportChangedModeFromLastStaged}: Tests for changes in file mode (e.g., permissions) since the last staged state.
     * - {@code testExecuteReportChangedButKeepSizeFromLastStaged}: Verifies the detection of files with changes that maintain the same size as in the last staged state.
     * - {@code testExecuteReportChangedModifiedTimeFromLastStaged}: Validates the reporting of changes based on modified timestamps.
     * - {@code testExecuteReportDeletedFromLastStaged}: Checks the reporting of files that have been deleted from the workspace since the last staging.
     * <p>
     * This class is particularly useful in ensuring that any updates, deletions, or untracked items
     * in the workspace are correctly identified when compared to the index, reinforcing the
     * reliability of version control mechanisms.
     */
    @Nested
    class WorkspaceIndexDifferences {

        @Test
        void testExecuteReportUntrackedFilesWithoutIndexFile() {
            StatusAction statusAction = new StatusAction();

            try {

                // Create files and its contents in the firstDir
                Files.createDirectories(firstDir);
                TestUtils.writeText(testFile11, "Test content 11");
                TestUtils.writeText(testFile12, "Test content 12");

                // Execute status command
                Status actual = statusAction.execute();

                // Assert
                Status expected = new Status();
                expected.addEntry(new StatusEntry(firstDir, StatusType.UNTRACKED));
                assertEquals(expected, actual);

            } catch (Exception ex) {
                ex.printStackTrace();
                fail();
            }
        }

        @Test
        void testExecuteReportUntrackedFilesHasIndexFile() {

            StatusAction statusAction = new StatusAction();
            AddAction addAction = new AddAction();
            CommitAction commitAction = new CommitAction();

            try {

                // Create files and its contents in the firstDir
                Files.createDirectories(firstDir);
                TestUtils.writeText(testFile11, "Test content 11");
                TestUtils.writeText(testFile12, "Test content 12");

                // Add testFile11 to staging and commit
                Index index = addAction.execute(testFile11);
                Commit commit = commitAction.execute();

                // Execute status command
                Status actual = statusAction.execute();

                // Assert
                Status expected = new Status();
                expected.addEntry(new StatusEntry(testFile12, StatusType.UNTRACKED));


                assertEquals(expected, actual);

            } catch (Exception ex) {
                ex.printStackTrace();
                fail();
            }

        }


        @Test
        void testExecuteReportUntrackedDirectories() {

            StatusAction statusAction = new StatusAction();
            AddAction addAction = new AddAction();
            CommitAction commitAction = new CommitAction();

            try {

                // Create files and its contents in the firstDir
                Files.createDirectories(firstDir);
                TestUtils.writeText(testFile11, "Test content 11");
                TestUtils.writeText(testFile12, "Test content 12");

                // Create files and its contents in the subFirstDir
                Files.createDirectories(subFirstDir);
                TestUtils.writeText(testFile111, "Test content 111");
                TestUtils.writeText(testFile112, "Test content 112");

                // Create files and its content in the secondDir
                Files.createDirectories(secondDir);
                Files.createDirectories(subSecondDir);
                TestUtils.writeText(testFile211, "Test content 211");

                // Add testFile11 to staging and commit
                Index index = addAction.execute(testFile11);
                Commit commit = commitAction.execute();

                // Execute status command
                Status actual = statusAction.execute();

                // Assert
                Status expected = new Status();
                expected.addEntry(new StatusEntry(testFile12, StatusType.UNTRACKED));
                expected.addEntry(new StatusEntry(subFirstDir, StatusType.UNTRACKED));
                expected.addEntry(new StatusEntry(secondDir, StatusType.UNTRACKED));
                assertEquals(expected, actual);

            } catch (Exception ex) {
                ex.printStackTrace();
                fail();
            }

        }

        @Test
        void testExecuteReportUntrackedEmptyDirectories() {

            StatusAction statusAction = new StatusAction();
            AddAction addAction = new AddAction();
            CommitAction commitAction = new CommitAction();

            try {

                // Create files and its contents in the firstDir
                Files.createDirectories(firstDir);
                TestUtils.writeText(testFile11, "Test content 11");
                TestUtils.writeText(testFile12, "Test content 12");

                // Create files and its contents in the subFirstDir
                Files.createDirectories(subFirstDir);
                TestUtils.writeText(testFile111, "Test content 111");
                TestUtils.writeText(testFile112, "Test content 112");

                // Create files and its content in the secondDir (Empty Directory)
                Files.createDirectories(secondDir);
                Files.createDirectories(subSecondDir);

                // Add testFile11 to staging and commit
                Index index = addAction.execute(testFile11);
                Commit commit = commitAction.execute();

                // Execute status command
                Status actual = statusAction.execute();

                // Assert
                Status expected = new Status();
                expected.addEntry(new StatusEntry(testFile12, StatusType.UNTRACKED));
                expected.addEntry(new StatusEntry(subFirstDir, StatusType.UNTRACKED));
                assertEquals(expected, actual);

            } catch (Exception ex) {
                ex.printStackTrace();
                fail();
            }
        }

        @Test
        void testExecuteReportChangedContentsFromLastStaged() {

            StatusAction statusAction = new StatusAction();
            AddAction addAction = new AddAction();
            CommitAction commitAction = new CommitAction();

            try {

                // Create files and its contents in the firstDir
                Files.createDirectories(firstDir);
                TestUtils.writeText(testFile11, "Test content 11");
                TestUtils.writeText(testFile12, "Test content 12");

                // Create files and its contents in the subFirstDir
                Files.createDirectories(subFirstDir);
                TestUtils.writeText(testFile111, "Test content 111");
                TestUtils.writeText(testFile112, "Test content 112");

                // Add firstDir to staging and commit
                Index firstIndex = addAction.execute(firstDir);
                Commit firstCommit = commitAction.execute();

                // Execute status command first time
                Status firstActual = statusAction.execute();

                // Assert: After the first time, nothing changed
                Status firstExpected = new Status();
                assertEquals(firstExpected, firstActual);

                // Change contents of testFile11 and testFile112
                TestUtils.writeText(testFile11, "Test content 11 modified");
                TestUtils.writeText(testFile112, "Test content 112 modified");

                // Execute status command a second time
                Status secondActual = statusAction.execute();

                // Assert: After modifying the two files, changes will be reported
                Status secondExpected = new Status();
                secondExpected.addEntry(new StatusEntry(testFile11, StatusType.WORKSPACE_MODIFIED));
                secondExpected.addEntry(new StatusEntry(testFile112, StatusType.WORKSPACE_MODIFIED));
                assertEquals(secondExpected, secondActual);

                // Add testFile11 and testFile112 to staging and commit
                Index secondIndex = addAction.execute(firstDir);
                Commit secondCommit = commitAction.execute();

                // Execute status command a third time
                Status thirdActual = statusAction.execute();

                // Assert: After the third time, nothing changed
                Status thirdExpected = new Status();
                assertEquals(thirdExpected, thirdActual);


            } catch (Exception ex) {
                ex.printStackTrace();
                fail();
            }
        }

        @Test
        void testExecuteReportChangedModeFromLastStaged() {

            StatusAction statusAction = new StatusAction();
            AddAction addAction = new AddAction();
            CommitAction commitAction = new CommitAction();

            try {

                // Create files and its contents in the firstDir
                Files.createDirectories(firstDir);
                TestUtils.writeText(testFile11, "Test content 11");
                TestUtils.writeText(testFile12, "Test content 12");

                // Create files and its contents in the subFirstDir
                Files.createDirectories(subFirstDir);
                TestUtils.writeText(testFile111, "Test content 111");
                TestUtils.writeText(testFile112, "Test content 112");

                // Add firstDir to staging and commit
                Index firstIndex = addAction.execute(firstDir);
                Commit firstCommit = commitAction.execute();

                // Execute status command first time
                Status firstActual = statusAction.execute();

                // Assert: After the first time, nothing changed
                Status firstExpected = new Status();
                assertEquals(firstExpected, firstActual);

                // Change modes of testFile11 and testFile112
                TestUtils.changeMode(testFile11, FileMode.EXECUTABLE_FILE);
                TestUtils.changeMode(testFile112, FileMode.EXECUTABLE_FILE);

                // Execute status command a second time
                Status secondActual = statusAction.execute();

                // Assert: After modifying the two files, changes will be reported
                Status secondExpected = new Status();
                secondExpected.addEntry(new StatusEntry(testFile11, StatusType.WORKSPACE_MODIFIED));
                secondExpected.addEntry(new StatusEntry(testFile112, StatusType.WORKSPACE_MODIFIED));
                assertEquals(secondExpected, secondActual);


            } catch (Exception ex) {
                fail();
            }
        }

        @Test
        void testExecuteReportChangedButKeepSizeFromLastStaged() {

            StatusAction statusAction = new StatusAction();
            AddAction addAction = new AddAction();
            CommitAction commitAction = new CommitAction();

            try {

                // Create files and its contents in the firstDir
                Files.createDirectories(firstDir);
                TestUtils.writeText(testFile11, "Test content 11");
                TestUtils.writeText(testFile12, "Test content 12");

                // Create files and its contents in the subFirstDir
                Files.createDirectories(subFirstDir);
                TestUtils.writeText(testFile111, "Test content 111");
                TestUtils.writeText(testFile112, "Test content 112");

                // Add firstDir to staging and commit
                Index firstIndex = addAction.execute(firstDir);
                Commit firstCommit = commitAction.execute();

                // Execute status command first time
                Status firstActual = statusAction.execute();

                // Assert: After the first time, nothing changed
                Status firstExpected = new Status();
                assertEquals(firstExpected, firstActual);

                // Change contents of testFile11 and testFile112 without changing size
                TestUtils.writeText(testFile11, "Test content 91");
                TestUtils.writeText(testFile112, "Test content 912");

                // Execute status command a second time
                Status secondActual = statusAction.execute();

                // Assert: After modifying the two files, changes will be reported
                Status secondExpected = new Status();
                secondExpected.addEntry(new StatusEntry(testFile11, StatusType.WORKSPACE_MODIFIED));
                secondExpected.addEntry(new StatusEntry(testFile112, StatusType.WORKSPACE_MODIFIED));
                assertEquals(secondExpected, secondActual);


            } catch (Exception ex) {
                fail();
            }
        }

        @Test
        void testExecuteReportChangedModifiedTimeFromLastStaged() {

            StatusAction statusAction = new StatusAction();
            AddAction addAction = new AddAction();
            CommitAction commitAction = new CommitAction();

            try {

                // Create files and its contents in the firstDir
                Files.createDirectories(firstDir);
                TestUtils.writeText(testFile11, "Test content 11");
                TestUtils.writeText(testFile12, "Test content 12");

                // Create files and its contents in the subFirstDir
                Files.createDirectories(subFirstDir);
                TestUtils.writeText(testFile111, "Test content 111");
                TestUtils.writeText(testFile112, "Test content 112");

                // Add firstDir to staging and commit
                Index firstIndex = addAction.execute(firstDir);
                Commit firstCommit = commitAction.execute();

                // Get before testFile11's stat
                FileStat beforeStat = TestUtils.getFileStat(testFile11);

                // Execute status command first time
                Status firstActual = statusAction.execute();

                // Assert: After the first time, nothing changed
                Status firstExpected = new Status();
                assertEquals(firstExpected, firstActual);

                // Change the modified time of testFile11 and testFile112
                Instant time = Instant.parse("2099-01-01T00:00:00Z");
                TestUtils.setMTime(testFile11, time);
                TestUtils.setMTime(testFile112, time);


                // Execute status command a second time
                Status secondActual = statusAction.execute();

                // Assert: After changed the modified time of the two files, nothing changed
                Status secondExpected = new Status();
                assertEquals(secondExpected, secondActual);
                Index afterIndex = Index.loadFromDisk();
                assertNotNull(afterIndex);
                IndexEntry testFile11IndexEntry = afterIndex.getEntryMap().get(testFile11);
                assertNotEquals(beforeStat.getMtimeSeconds(), testFile11IndexEntry.getMtimeSeconds());

            } catch (Exception ex) {
                fail();
            }
        }

        @Test
        void testExecuteReportDeletedFromLastStaged() {

            StatusAction statusAction = new StatusAction();
            AddAction addAction = new AddAction();
            CommitAction commitAction = new CommitAction();

            try {

                // Create files and its contents in the firstDir
                Files.createDirectories(firstDir);
                TestUtils.writeText(testFile11, "Test content 11");
                TestUtils.writeText(testFile12, "Test content 12");

                // Create files and its contents in the subFirstDir
                Files.createDirectories(subFirstDir);
                TestUtils.writeText(testFile111, "Test content 111");
                TestUtils.writeText(testFile112, "Test content 112");

                // Create files and ít contents in the secondDir
                Files.createDirectories(secondDir);
                TestUtils.writeText(testFile21, "Test content 21");
                TestUtils.writeText(testFile22, "Test content 22");

                // Execute status command first time
                Status firstActual = statusAction.execute();

                // Assert: After the first time, nothing changed
                Status firstExpected = new Status();
                firstExpected.addEntry(new StatusEntry(firstDir, StatusType.UNTRACKED));
                firstExpected.addEntry(new StatusEntry(secondDir, StatusType.UNTRACKED));
                assertEquals(firstExpected, firstActual);

                // Add firstDir to staging and commit
                Index firstIndex = addAction.execute(rootPath);
                Commit firstCommit = commitAction.execute();

                // Delete testFile21
                TestUtils.deleteRecursively(testFile21);

                // Delete subFirstDir
                TestUtils.deleteRecursively(subFirstDir);

                // Execute status command a second time
                Status secondActual = statusAction.execute();

                // Assert: Report deleted files
                Status secondExpected = new Status();
                secondExpected.addEntry(new StatusEntry(testFile111, StatusType.WORKSPACE_DELETED));
                secondExpected.addEntry(new StatusEntry(testFile112, StatusType.WORKSPACE_DELETED));
                secondExpected.addEntry(new StatusEntry(testFile21, StatusType.WORKSPACE_DELETED));
                assertEquals(secondExpected, secondActual);

            } catch (Exception ex) {
                ex.printStackTrace();
                fail();
            }
        }


    }


    /*****************************
     * HEAD/index differences
     ****************************/

    @Nested
    class HeadIndexDifferences {

        @Test
        void testExecuteReportAddedFiles() {

            StatusAction statusAction = new StatusAction();
            AddAction addAction = new AddAction();
            CommitAction commitAction = new CommitAction();

            try {

                // Create files and its contents in the firstDir
                Files.createDirectories(firstDir);
                TestUtils.writeText(testFile11, "Test content 11");
                TestUtils.writeText(testFile12, "Test content 12");

                // Create files and its contents in the subFirstDir
                Files.createDirectories(subFirstDir);
                TestUtils.writeText(testFile111, "Test content 111");

                // Add all files to staging and commit
                Index index = addAction.execute(firstDir);
                Commit commit = commitAction.execute();

                // Create testFile112 in the subFirstDir, then report status
                TestUtils.writeText(testFile112, "Test content 112");

                // Add testFile112 to staging, then report status
                addAction.execute(testFile112);
                Status firstActual = statusAction.execute();

                // Assert: contains testFile112 as ADDED
                Status firstExpected = new Status();
                firstExpected.addEntry(new StatusEntry(testFile112, StatusType.ADDED));
                assertEquals(firstExpected, firstActual);

                // Commit 2nd
                commit = commitAction.execute();

                // Create files and ít contents in the secondDir
                Files.createDirectories(secondDir);
                TestUtils.writeText(testFile21, "Test content 21");
                TestUtils.writeText(testFile22, "Test content 22");

                // Add the secondDir to staging, then report status
                addAction.execute(secondDir);
                Status secondActual = statusAction.execute();

                // Assert: contains testFile21, testFile22 as ADDED
                Status secondExpected = new Status();
                secondExpected.addEntry(new StatusEntry(testFile21, StatusType.ADDED));
                secondExpected.addEntry(new StatusEntry(testFile22, StatusType.ADDED));
                assertEquals(secondExpected, secondActual);

                // Commit 3rd
                commit = commitAction.execute();

                // Report status
                Status thirdActual = statusAction.execute();

                // Assert: Nothing change
                Status thirdExpected = new Status();
                assertEquals(thirdExpected, thirdActual);

            } catch (Exception ex) {
                ex.printStackTrace();
                fail();
            }
        }

        @Test
        void testExecuteReportModifiedFiles() {

            StatusAction statusAction = new StatusAction();
            AddAction addAction = new AddAction();
            CommitAction commitAction = new CommitAction();

            try {

                // Create files and its contents in the firstDir
                Files.createDirectories(firstDir);
                TestUtils.writeText(testFile11, "Test content 11");
                TestUtils.writeText(testFile12, "Test content 12");

                // Create files and its contents in the subFirstDir
                Files.createDirectories(subFirstDir);
                TestUtils.writeText(testFile111, "Test content 111");
                TestUtils.writeText(testFile112, "Test content 112");

                // Create files and ít contents in the secondDir
                Files.createDirectories(secondDir);
                TestUtils.writeText(testFile21, "Test content 21");
                TestUtils.writeText(testFile22, "Test content 22");

                // Add and commit the first time
                addAction.execute(rootPath);
                commitAction.execute();

                // Change the file mode of testFile111 to EXECUTABLE
                TestUtils.changeMode(testFile111, FileMode.EXECUTABLE_FILE);

                // Change content of testFile21
                TestUtils.writeText(testFile21, "Test content 21 updated");

                // Add the two changes into stag and report the status
                addAction.execute(testFile111);
                addAction.execute(testFile21);
                Status firstActual = statusAction.execute();

                // Assert: contains testFile111, testFile21 as MODIFIED
                Status firstExpected = new Status();
                firstExpected.addEntry(new StatusEntry(testFile111, StatusType.INDEX_MODIFIED));
                firstExpected.addEntry(new StatusEntry(testFile21, StatusType.INDEX_MODIFIED));
                assertEquals(firstExpected, firstActual);

            } catch (Exception ex) {
                ex.printStackTrace();
                fail();
            }

        }

        @Test
        void testExecuteReportDeletedFiles() {

            StatusAction statusAction = new StatusAction();
            AddAction addAction = new AddAction();
            CommitAction commitAction = new CommitAction();

            try {

                // Create files and its contents in the firstDir
                Files.createDirectories(firstDir);
                TestUtils.writeText(testFile11, "Test content 11");
                TestUtils.writeText(testFile12, "Test content 12");

                // Create files and its contents in the subFirstDir
                Files.createDirectories(subFirstDir);
                TestUtils.writeText(testFile111, "Test content 111");
                TestUtils.writeText(testFile112, "Test content 112");

                // Create files and ít contents in the secondDir
                Files.createDirectories(secondDir);
                TestUtils.writeText(testFile21, "Test content 21");
                TestUtils.writeText(testFile22, "Test content 22");

                // Add and commit the first time
                addAction.execute(rootPath);
                commitAction.execute();

                // Remove the testFile22 and secondDir
                TestUtils.deleteRecursively(testFile112);
                TestUtils.deleteRecursively(secondDir);
                addAction.execute(testFile112);
                addAction.execute(testFile21);
                addAction.execute(testFile22);
                Status firstActual = statusAction.execute();

                // Assert: contains testFile112, testFile21, testFile22 as DELETED
                Status firstExpected = new Status();
                firstExpected.addEntry(new StatusEntry(testFile112, StatusType.INDEX_DELETED));
                firstExpected.addEntry(new StatusEntry(testFile21, StatusType.INDEX_DELETED));
                firstExpected.addEntry(new StatusEntry(testFile22, StatusType.INDEX_DELETED));
                assertEquals(firstExpected, firstActual);

            } catch (Exception ex) {
                ex.printStackTrace();
                fail();
            }

        }


    }

}
