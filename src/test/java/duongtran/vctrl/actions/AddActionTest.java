package duongtran.vctrl.actions;

import duongtran.vctrl.TestUtils;
import duongtran.vctrl.Workspace;
import duongtran.vctrl.index.Index;
import duongtran.vctrl.index.IndexEntry;
import duongtran.vctrl.storage.Database;
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
    Path indexPath;

    // Mock directories
    Path testFile11;
    Path testFile12;
    Path testFile21;
    Path firstDir;
    Path secondDir;


    @BeforeEach
    void setup() throws IOException {
        TestUtils.createTestWorkspace();
        workspace = Workspace.getInstance();
        database = Database.getInstance();

        // Root path
        rootPath = workspace.getRootPath();
        vctrlPath = workspace.getVctrlPath();
        indexPath = vctrlPath.resolve(DirectoryNames.INDEX);

        // Initialize test directories and files
        firstDir = rootPath.resolve("firstDir");
        secondDir = rootPath.resolve("secondDir");

        testFile11 = firstDir.resolve("file11.txt");
        testFile12 = firstDir.resolve("file12.txt");
        testFile21 = secondDir.resolve("file21.txt");

        // Create folders
        Files.createDirectories(firstDir);
        Files.createDirectories(secondDir);

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

}
