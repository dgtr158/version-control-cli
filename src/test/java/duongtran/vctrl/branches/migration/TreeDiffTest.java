package duongtran.vctrl.branches.migration;

import duongtran.vctrl.TestUtils;
import duongtran.vctrl.Workspace;
import duongtran.vctrl.actions.AddAction;
import duongtran.vctrl.actions.CommitAction;
import duongtran.vctrl.storage.Database;
import duongtran.vctrl.storage.FileMode;
import duongtran.vctrl.storage.ObjectID;
import duongtran.vctrl.storage.objects.Blob;
import duongtran.vctrl.storage.objects.Commit;
import duongtran.vctrl.storage.objects.TreeEntry;
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

import static org.junit.jupiter.api.Assertions.*;

public class TreeDiffTest {

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

    @Test
    void testCompareTreeDiff() {

        AddAction addAction = new AddAction();
        CommitAction commitAction = new CommitAction();
        TreeDiff treeDiff = new TreeDiff();


        try {

            // Create files and its contents in the firstDir
            Files.createDirectories(firstDir);
            TestUtils.writeText(testFile11, "Test content 11");

            // Add firstDir to staging and commit
            addAction.execute(firstDir);
            Commit firstCommit = commitAction.execute();

            // Create files and its contents in the subFirstDir
            TestUtils.writeText(testFile12, "Test content 12");
            Files.createDirectories(subFirstDir);
            TestUtils.writeText(testFile111, "Test content 111");
            TestUtils.writeText(testFile112, "Test content 112");

            // Add testFile12, subFirstDir to staging and commit
            addAction.execute(testFile12);
            addAction.execute(subFirstDir);
            Commit secondCommit = commitAction.execute();
            
            // Assert Tree Diff first time: testFile12, testFile111, testFile112 were added
            Map<Path, TreeChanges> firstActualTreeDiff = treeDiff.detectTreeDiff(firstCommit.getOid(), secondCommit.getOid());
            Map<Path, TreeChanges> firstExpectedTreeDiff = buildExpectedTreeDiffNewBlob(new ArrayList<>(List.of(
                    testFile12, testFile111, testFile112
            )));
            assertTrue(Utils.mapsEqual(firstExpectedTreeDiff, firstActualTreeDiff));

            // Change contents of testFile11 and testFile112
            TestUtils.writeText(testFile11, "Test content 11 modified");
            TestUtils.writeText(testFile112, "Test content 112 modified");

            // Add testFile11, testFile112 to staging and commit
            addAction.execute(testFile11);
            addAction.execute(testFile112);
            Commit thirdCommit = commitAction.execute();

            // TODO: Assert Tree Diff first time: testFile11, testFile112, were changed

        } catch (Exception ex) {
            ex.printStackTrace();
            fail();
        }


    }

    private Map<Path, TreeChanges> buildExpectedTreeDiffNewBlob(List<Path> paths) throws IOException, NoSuchAlgorithmException {
        Map<Path, TreeChanges> added = new TreeMap<>();
        for (Path path : paths) {
            Path relativePath = rootPath.relativize(path);
            added.put(
                    relativePath
                    , new TreeChanges(null, new TreeEntry(
                            relativePath.getFileName().toString()
                            , getBlobOid(path)
                            , FileMode.REGULAR_FILE
                    )));
        }
        return added;
    }

    private ObjectID getBlobOid(Path path) throws IOException, NoSuchAlgorithmException {
        Blob blob = new Blob(Files.readAllBytes(path));
        blob.calculateOid(blob.toBytes());
        return blob.getOid();
    }

    
}
