package duongtran.vctrl.storage;

import duongtran.vctrl.TestUtils;
import duongtran.vctrl.Workspace;
import duongtran.vctrl.actions.AddAction;
import duongtran.vctrl.actions.CommitAction;
import duongtran.vctrl.index.Index;
import duongtran.vctrl.storage.objects.Blob;
import duongtran.vctrl.storage.objects.Commit;
import duongtran.vctrl.utils.DirectoryNames;
import duongtran.vctrl.utils.Utils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class DatabaseTest {

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

    /**
     * Tests the functionality of listing all files in the HEAD of the version control system.
     * <p>
     * This method performs the following steps:
     * 1. Creates a directory structure with predefined files and content.
     * 2. Uses the AddAction to add the files to the index and commits the changes using CommitAction.
     * 3. Invokes the method under test to retrieve all files listed in the HEAD.
     * 4. Asserts that the resulting map of files and their metadata matches the expected map.
     * <p>
     * The test verifies the correct behavior of the system when listing files in the HEAD,
     * ensuring the content and structure of the workspace are accurately reflected.
     *
     */
    @Test
    void testListAllFilesInHead() {

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
            TestUtils.writeText(testFile21, "Test content 21");
            TestUtils.writeText(testFile22, "Test content 22");

            // Create files and its content in the subSecondDir
            Files.createDirectories(subSecondDir);
            TestUtils.writeText(testFile212, "Test content 212");
            TestUtils.writeText(testFile211, "Test content 211");

            // Add all files to staging and commit
            Index index = addAction.execute(rootPath);
            Commit commit = commitAction.execute("Dummy commit message");

            // List all file in HEAD
            Map<Path, DataEntry> actualMap = Database.listFileInHead();

            // Assert
            Map<Path, DataEntry> expectedMap = new TreeMap<>();
            List<DataEntry> dataEntries = getDataEntries(rootPath);
            for (DataEntry dataEntry : dataEntries) {
                expectedMap.put(dataEntry.getPath(), dataEntry);
            }
            assertTrue(Utils.mapsEqual(expectedMap, actualMap), "Two maps are not equal");

        } catch (Exception ex) {
            ex.printStackTrace();
            fail();
        }
    }


    /**
     * Retrieves and constructs a list of {@code DataEntry} objects from the given base path.
     * Each {@code DataEntry} is created by reading file content, generating a {@code Blob} object,
     * calculating its object ID, and getting file metadata such as mode and path.
     *
     * @param basePath the base directory from which to gather file data and create {@code DataEntry} objects.
     *                 It is expected to include a valid path pointing to the workspace root directory.
     * @return a list of {@code DataEntry} objects containing file metadata and calculated object IDs.
     * @throws IOException              if an I/O error occurs during file reading.
     * @throws NoSuchAlgorithmException if the hashing algorithm (SHA-1) for object ID generation is not available.
     */
    private List<DataEntry> getDataEntries(Path basePath) throws IOException, NoSuchAlgorithmException {
        List<DataEntry> entries = new ArrayList<>();
        List<Path> allFiles = workspace.listAllFiles(basePath);
        for (Path path : allFiles) {
            byte[] bytes = Files.readAllBytes(path);
            Blob blob = new Blob(bytes);
            blob.calculateOid(blob.toBytes());
            DataEntry dataEntry = new DataEntry(TestUtils.getFileStat(path).getMode(), blob.getOid(), path);
            entries.add(dataEntry);
        }

        return entries;
    }

}
