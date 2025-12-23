package duongtran.vctrl.actions;

import duongtran.vctrl.TestUtils;
import duongtran.vctrl.Workspace;
import duongtran.vctrl.references.Refs;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class CommitActionTest {

    private static final Logger log = LoggerFactory.getLogger(CommitActionTest.class);

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
    void testExecuteCommitFirstTime() {

        AddAction addAction;
        CommitAction commitAction;

        try {

            // Create files in the firstDir
            TestUtils.writeText(testFile11, "Test content 11");
            TestUtils.writeText(testFile12, "Test content 12");

            // Create files in the subFirstDir
            TestUtils.writeText(testFile111, "Test content 111");

            // Create a file in the secondDir
            TestUtils.writeText(testFile21, "Test content 21");

            // Add workspace's files to staging
            addAction = new AddAction();
            addAction.execute(rootPath);

            // Execute: commit
            commitAction = new CommitAction();
            Commit savedCommit = commitAction.execute();

            // Load the saved commit from the disk
            Refs refs = new Refs();
            Commit loadedCommit = Commit.loadCommit(savedCommit.getOid());
            assertEquals(savedCommit, loadedCommit);
            assertEquals(savedCommit.getOid().getValue(), refs.readHead());

        } catch (Exception ex) {
            log.error("failed: {}", ex.getMessage());
            fail();
        }

    }

}
