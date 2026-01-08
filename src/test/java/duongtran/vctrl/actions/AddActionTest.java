package duongtran.vctrl.actions;

import duongtran.vctrl.TestUtils;
import duongtran.vctrl.Workspace;
import duongtran.vctrl.index.Index;
import duongtran.vctrl.index.IndexEntry;
import duongtran.vctrl.storage.Database;
import duongtran.vctrl.storage.objects.Commit;
import duongtran.vctrl.utils.DirectoryNames;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class AddActionTest {

    private static final Logger log = LoggerFactory.getLogger(AddActionTest.class);

    // Vctrl instances
    Workspace workspace;
    Database database;
    Path rootPath;
    Path vctrlPath;
    Path objectPath;
    Path indexPath;

    // Mock directories
    Path firstDir;
    Path subFirstDir;
    Path secondDir;
    Path subSecondDir;
    Path thirdDir;
    Path fourthDir;

    // Mock files
    Path testFile1;
    Path testFile11;
    Path testFile12;
    Path testFile13;
    Path testFile111;
    Path testFile112;
    Path testFile21;
    Path testFile22;
    Path testFile211;
    Path testFile212;
    Path testFile31;
    Path testFile32;
    Path testFile41;
    Path testFile42;


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
        thirdDir = rootPath.resolve("thirdDir");
        fourthDir = rootPath.resolve("fourthDir");

        testFile1 = rootPath.resolve("file1.txt");
        testFile11 = firstDir.resolve("file11.txt");
        testFile12 = firstDir.resolve("file12.txt");
        testFile13 = firstDir.resolve("file13.txt");
        testFile111 = subFirstDir.resolve("file111.txt");
        testFile112 = subFirstDir.resolve("file112.txt");
        testFile21 = secondDir.resolve("file21.txt");
        testFile22 = secondDir.resolve("file22.txt");
        testFile211 = subSecondDir.resolve("file211.txt");
        testFile212 = subSecondDir.resolve("file212.txt");
        testFile31 = thirdDir.resolve("file31.txt");
        testFile32 = thirdDir.resolve("file32.txt");
        testFile41 = fourthDir.resolve("file41.txt");
        testFile42 = fourthDir.resolve("file42.txt");



    }

    @AfterEach
    void cleanup() {
        TestUtils.removeWorkspace();
        workspace = null;
    }

    @Test
    void testAddSingleFile() {

        AddAction addAction;

        try {

            // Create folders
            Files.createDirectories(firstDir);
            Files.createDirectories(secondDir);

            // Create files in the firstDir
            TestUtils.writeText(testFile11, "Test content 11");

            // Execute action
            addAction = new AddAction();
            addAction.execute(testFile11);

            // Validate that index contains the correct entries
            Path indexPath = vctrlPath.resolve(DirectoryNames.INDEX);
            assertTrue(Files.exists(indexPath));

            // Load index from disk
            Index actual = Index.loadFromDisk();

            // Validate entries
            List<Path> expectedEntries = Collections.singletonList(
                    testFile11
            );
            assertEquals(1, actual.getHeader().getEntryCount());
            Map<Path, IndexEntry> entryMap = actual.getEntryMap();
            List<Path> actualEntries = entryMap.keySet().stream().toList();
            assertIterableEquals(expectedEntries, actualEntries);

        } catch (Exception e) {
            log.error("Test failed: {}", e.getMessage());
            fail();
        }

    }


    @Test
    void testAddActionAllFiles() {

        AddAction addAction;

        try {

            // Create folders
            Files.createDirectories(firstDir);
            Files.createDirectories(secondDir);

            // Create files in the firstDir
            TestUtils.writeText(testFile11, "Test content 11");
            TestUtils.writeText(testFile12, "Test content 12");

            // Create a file in the secondDir
            TestUtils.writeText(testFile21, "Test content 21");

            // Execute action
            addAction = new AddAction();
            addAction.execute(rootPath);

            // Validate that index contains the correct entries
            Path indexPath = vctrlPath.resolve(DirectoryNames.INDEX);
            assertTrue(Files.exists(indexPath));

            // Load index from disk
            Index actual = Index.loadFromDisk();

            // Validate entries
            List<Path> expectedEntries = Arrays.asList(
                    testFile11
                    , testFile12
                    , testFile21
            );
            assertEquals(3, actual.getHeader().getEntryCount());
            Map<Path, IndexEntry> entryMap = actual.getEntryMap();
            List<Path> actualEntries = entryMap.keySet().stream().toList();
            assertIterableEquals(expectedEntries, actualEntries);

        } catch (Exception e) {
            log.error("Test failed: {}", e.getMessage());
            fail();
        }

    }

    @Test
    void testAddActionAllDirectory() {

        AddAction addAction;
        try {

            // Create folders
            Files.createDirectories(firstDir);
            Files.createDirectories(secondDir);

            // Create files in the firstDir
            TestUtils.writeText(testFile11, "Test content 11");
            TestUtils.writeText(testFile12, "Test content 12");

            // Execute action
            addAction = new AddAction();
            addAction.execute(firstDir);

            // Load index from disk
            Index actual = Index.loadFromDisk();

            // Validate entries
            List<Path> expectedEntries = Arrays.asList(
                    testFile11
                    , testFile12
            );
            assertEquals(2, actual.getHeader().getEntryCount());
            Map<Path, IndexEntry> entryMap = actual.getEntryMap();
            List<Path> actualEntries = entryMap.keySet().stream().toList();
            assertIterableEquals(expectedEntries, actualEntries);

        } catch (Exception e) {
            log.error("Test failed: {}", e.getMessage());
            fail();
        }

    }

    @Test
    void testAddActionIncrementalChanges() {

        AddAction addAction;

        try {

            // Create folders
            Files.createDirectories(firstDir);
            Files.createDirectories(secondDir);

            // Create files in the firstDir
            TestUtils.writeText(testFile11, "Test content 11");
            TestUtils.writeText(testFile12, "Test content 12");

            // Create a file in the secondDir
            TestUtils.writeText(testFile21, "Test content 21");

            // Execute action (incremental changes)
            addAction = new AddAction();

            // Add first time
            addAction.execute(testFile11);
            addAction.execute(testFile12);
            addAction.execute(testFile21);

            // Add the second time
            addAction.execute(testFile11);
            addAction.execute(testFile12);
            addAction.execute(testFile21);

            // Add the third time
            addAction.execute(firstDir);
            addAction.execute(secondDir);

            // Load index from disk
            Index actual = Index.loadFromDisk();

            // Validate entries
            List<Path> expectedEntries = Arrays.asList(
                    testFile11
                    , testFile12
                    , testFile21
            );
            assertEquals(3, actual.getHeader().getEntryCount());
            Map<Path, IndexEntry> entryMap = actual.getEntryMap();
            List<Path> actualEntries = entryMap.keySet().stream().toList();
            assertIterableEquals(expectedEntries, actualEntries);

        } catch (Exception e) {
            log.error("Test failed: {}", e.getMessage());
            fail();
        }
    }

    @Test
    void testAddActionOverwriteExisting() {

        AddAction addAction;

        try {

            // Create folders
            Files.createDirectories(firstDir);
            Files.createDirectories(secondDir);

            // Create files in the firstDir
            TestUtils.writeText(testFile11, "Test content 11");
            TestUtils.writeText(testFile12, "Test content 12");

            // Create a file in the secondDir
            TestUtils.writeText(testFile21, "Test content 21");

            // Execute action (incremental changes)
            addAction = new AddAction();

            // Add the files
            addAction.execute(testFile11);
            addAction.execute(testFile12);
            addAction.execute(testFile21);

            // Overwrite the testFile12
            TestUtils.writeText(testFile12, "Test content 12 overwritten");
            addAction.execute(testFile12);

            // Load index from disk
            Index actual = Index.loadFromDisk();

            // Validate entries
            List<Path> expectedEntries = Arrays.asList(
                    testFile11
                    , testFile12
                    , testFile21
            );
            assertEquals(3, actual.getHeader().getEntryCount());
            Map<Path, IndexEntry> entryMap = actual.getEntryMap();
            List<Path> actualEntries = entryMap.keySet().stream().toList();
            assertIterableEquals(expectedEntries, actualEntries);

        } catch (Exception e) {
            log.error("Test failed: {}", e.getMessage());
            fail();
        }
    }

    @Test
    void testAddActionRemoveExisting() {

        AddAction addAction;

        try {

            // Create folders
            Files.createDirectories(firstDir);
            Files.createDirectories(secondDir);

            // Create files in the firstDir
            TestUtils.writeText(testFile11, "Test content 11");
            TestUtils.writeText(testFile12, "Test content 12");

            // Create a file in the secondDir
            TestUtils.writeText(testFile21, "Test content 21");

            // Execute action (incremental changes)
            addAction = new AddAction();

            // Add the files
            addAction.execute(testFile11);
            addAction.execute(testFile12);
            addAction.execute(testFile21);

            // Remove the testFile11 and all files in the secondDir
            TestUtils.deleteRecursively(testFile11);
            TestUtils.deleteRecursively(secondDir);
            addAction.execute(testFile11);
            addAction.execute(testFile21);

            // Load index from disk
            Index actual = Index.loadFromDisk();

            // Validate entries
            List<Path> expectedEntries = Arrays.asList(
                    testFile12
            );
            assertEquals(1, actual.getHeader().getEntryCount());
            Map<Path, IndexEntry> entryMap = actual.getEntryMap();
            List<Path> actualEntries = entryMap.keySet().stream().toList();
            assertIterableEquals(expectedEntries, actualEntries);

        } catch (Exception e) {
            log.error("Test failed: {}", e.getMessage());
            fail();
        }
    }

    @Test
    void testAddActionAddAllUntrackedFiles() {

        AddAction addAction = new AddAction();

        try {

            // Create files and its contents in the firstDir
            Files.createDirectories(firstDir);
            TestUtils.writeText(testFile11, "Test content 11");
            TestUtils.writeText(testFile12, "Test content 12");

            // Create files and its contents in the subFirstDir
            Files.createDirectories(subFirstDir);
            TestUtils.writeText(testFile111, "Test content 111");
            TestUtils.writeText(testFile112, "Test content 112");

            // Add all the firstDir into index
            addAction.execute(firstDir);

            // Create files and its contents in the secondDir
            Files.createDirectories(secondDir);
            TestUtils.writeText(testFile21, "Test content 21");
            TestUtils.writeText(testFile22, "Test content 22");

            // Create files and its contents in the subSecondDir
            Files.createDirectories(subSecondDir);
            TestUtils.writeText(testFile211, "Test content 211");
            TestUtils.writeText(testFile212, "Test content 212");

            // Create files and its contents in the thirdDir
            Files.createDirectories(thirdDir);
            TestUtils.writeText(testFile31, "Test content 31");
            TestUtils.writeText(testFile32, "Test content 32");

            // Create files and its contents in the fourthDir
            Files.createDirectories(fourthDir);
            TestUtils.writeText(testFile41, "Test content 41");
            TestUtils.writeText(testFile42, "Test content 42");

            // Add all untracked files
            addAction.execute();

            // Load index from disk
            Index actual = Index.loadFromDisk();

            // Validate entries
            List<Path> expectedEntries = Arrays.asList(
                    testFile11
                    , testFile12
                    , testFile111
                    , testFile112
                    , testFile41
                    , testFile42
                    , testFile21
                    , testFile22
                    , testFile211
                    , testFile212
                    , testFile31
                    , testFile32
            );
            assertEquals(12, actual.getHeader().getEntryCount());
            Map<Path, IndexEntry> entryMap = actual.getEntryMap();
            List<Path> actualEntries = entryMap.keySet().stream().toList();
            assertIterableEquals(expectedEntries, actualEntries);

        } catch (Exception e) {
            log.error("Test failed: {}", e.getMessage());
            fail();
        }
    }

}
