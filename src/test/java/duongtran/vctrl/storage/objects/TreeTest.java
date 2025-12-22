package duongtran.vctrl.storage.objects;

import duongtran.vctrl.TestUtils;
import duongtran.vctrl.Workspace;
import duongtran.vctrl.actions.AddAction;
import duongtran.vctrl.index.Index;
import duongtran.vctrl.index.IndexEntry;
import duongtran.vctrl.storage.Database;
import duongtran.vctrl.storage.FileMode;
import duongtran.vctrl.storage.ObjectID;
import duongtran.vctrl.utils.DirectoryNames;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

public class TreeTest {

    private static final Logger log = LoggerFactory.getLogger(TreeTest.class);

    // Vctrl instances
    Workspace workspace;
    Database database;
    Path rootPath;
    Path vctrlPath;
    Path objectPath;

    // Mock directories
    Path testFile11;
    Path testFile12;
    Path testFile111;
    Path testFile21;
    Path firstDir;
    Path subFirstDir;
    Path secondDir;


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

        testFile11 = firstDir.resolve("file11.txt");
        testFile12 = firstDir.resolve("file12.txt");
        testFile111 = subFirstDir.resolve("file111.txt");
        testFile21 = secondDir.resolve("file21.txt");

        // Create folders
        Files.createDirectories(firstDir);
        Files.createDirectories(subFirstDir);
        Files.createDirectories(secondDir);

    }

    @AfterEach
    void tearDown() {
        TestUtils.removeWorkspace();
        workspace = null;
    }

    @Test
    void testBuildTree() {

        AddAction addAction;

        try {

            // Create files in the firstDir
            TestUtils.writeText(testFile11, "Test content 11");
            TestUtils.writeText(testFile12, "Test content 12");

            // Create files in the subFirstDir
            TestUtils.writeText(testFile111, "Test content 111");

            // Create a file in the secondDir
            TestUtils.writeText(testFile21, "Test content 21");

            // Execute action
            addAction = new AddAction();
            addAction.execute(rootPath);

            // Load index from disk
            Index index = Index.loadFromDisk();
            Map<Path, IndexEntry> indexEntries = index.getEntryMap();

            // Specify the expected tree
            Tree expected = getExpectedTree(indexEntries);

            // Execute: build a tree from index
            Tree actual = Tree.buildTree(index.getEntryMap());

            assertEquals(expected, actual);


        } catch (Exception ex) {
            log.error("failed: {}", ex.getMessage());
            fail();
        }

    }

    @Test
    void testStoreTree() {

        AddAction addAction;

        try {

            // Create files in the firstDir
            TestUtils.writeText(testFile11, "Test content 11");
            TestUtils.writeText(testFile12, "Test content 12");

            // Create files in the subFirstDir
            TestUtils.writeText(testFile111, "Test content 111");

            // Create a file in the secondDir
            TestUtils.writeText(testFile21, "Test content 21");

            // Execute action
            addAction = new AddAction();
            addAction.execute(rootPath);

            // Load index from disk
            Index index = Index.loadFromDisk();
            Tree storedTree = Tree.buildTree(index.getEntryMap());

            // Execute: store the tree
            ObjectID storedTreeOid = Tree.store(storedTree, database);

            // Load tree from disk
            Tree loadedTree = Tree.loadTree(storedTreeOid);

            // Verify if the loaded tree is valid
            log.debug("Loaded Tree: {}", loadedTree.getType());
            TreeMap<String, TreeEntry> loadedEntries = loadedTree.getStoredEntries();
            assertEquals(2, loadedEntries.size());
            TreeEntry firstDirTreeEntry = loadedEntries.get(firstDir.getFileName().toString());
            assertNotNull(firstDirTreeEntry);
            assertEquals(firstDir.getFileName().toString(), firstDirTreeEntry.getFileName());
            assertEquals(FileMode.DIRECTORY, firstDirTreeEntry.getMode());

            TreeEntry secondDirTreeEntry = loadedEntries.get(secondDir.getFileName().toString());
            assertNotNull(secondDirTreeEntry);
            assertEquals(secondDir.getFileName().toString(), secondDirTreeEntry.getFileName());
            assertEquals(FileMode.DIRECTORY, secondDirTreeEntry.getMode());

            // Load subtree
            Tree loadedSubTree = Tree.loadTree(firstDirTreeEntry.getOid());
            TreeMap<String, TreeEntry> loadedSubTreeEntries = loadedSubTree.getStoredEntries();
            assertEquals(3, loadedSubTreeEntries.size());

            // subFirstDir
            TreeEntry firstSubDirTreeEntry = loadedSubTreeEntries.get(subFirstDir.getFileName().toString());
            assertNotNull(firstSubDirTreeEntry);
            assertEquals(subFirstDir.getFileName().toString(), firstSubDirTreeEntry.getFileName());
            assertEquals(FileMode.DIRECTORY, firstSubDirTreeEntry.getMode());

            // testFile11
            TreeEntry testFile11TreeEntry = loadedSubTreeEntries.get(testFile11.getFileName().toString());
            assertNotNull(testFile11TreeEntry);
            assertEquals(testFile11.getFileName().toString(), testFile11TreeEntry.getFileName());
            assertEquals(FileMode.REGULAR_FILE, testFile11TreeEntry.getMode());

            // testFile12
            TreeEntry testFile12TreeEntry = loadedSubTreeEntries.get(testFile12.getFileName().toString());
            assertNotNull(testFile12TreeEntry);
            assertEquals(testFile12.getFileName().toString(), testFile12TreeEntry.getFileName());
            assertEquals(FileMode.REGULAR_FILE, testFile12TreeEntry.getMode());

        } catch (Exception ex) {
            log.error("failed: {}", ex.getMessage());
            fail();
        }

    }


    private Tree getExpectedTree(Map<Path, IndexEntry> indexEntries) {

        // subFirstDir Tree
        TreeEntry file111Entry = new TreeEntry(
                testFile111.getFileName().toString()
                , new ObjectID(indexEntries.get(testFile111).getOid())
                , FileMode.REGULAR_FILE
        );
        Tree subFirstDirTree = new Tree(new ArrayList<>(List.of(file111Entry)), new ArrayList<>(), rootPath.relativize(subFirstDir));

        // firstDir Tree
        TreeEntry file12Entry = new TreeEntry(
                testFile12.getFileName().toString()
                , new ObjectID(indexEntries.get(testFile12).getOid())
                , FileMode.REGULAR_FILE
        );
        TreeEntry file11Entry = new TreeEntry(
                testFile11.getFileName().toString()
                , new ObjectID(indexEntries.get(testFile11).getOid())
                , FileMode.REGULAR_FILE
        );
        Tree firstDirTree = new Tree(
                new ArrayList<>(List.of(file11Entry, file12Entry))
                , new ArrayList<>(List.of(subFirstDirTree))
                , rootPath.relativize(firstDir)
        );

        // secondDir Tree
        TreeEntry file21Entry = new TreeEntry(
                testFile21.getFileName().toString()
                , new ObjectID(indexEntries.get(testFile21).getOid())
                , FileMode.REGULAR_FILE
        );
        Tree secondDirTree = new Tree(new ArrayList<>(List.of(file21Entry)), new ArrayList<>(), rootPath.relativize(secondDir));

        // Expected Tree
        return new Tree(
                new ArrayList<>()
                , new ArrayList<>(List.of(firstDirTree, secondDirTree))
                , Path.of("")
        );
    }

}
