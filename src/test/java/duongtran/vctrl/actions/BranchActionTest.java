package duongtran.vctrl.actions;

import duongtran.vctrl.TestUtils;
import duongtran.vctrl.Workspace;
import duongtran.vctrl.references.RefHead;
import duongtran.vctrl.references.Refs;
import duongtran.vctrl.storage.Database;
import duongtran.vctrl.storage.ObjectID;
import duongtran.vctrl.storage.objects.Commit;
import duongtran.vctrl.utils.DirectoryNames;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class BranchActionTest {

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
    void testAddNewBranchToCurrentHead() {

        BranchAction branchAction = new BranchAction();
        AddAction addAction = new AddAction();
        CommitAction commitAction = new CommitAction();
        Refs ref = new Refs();
        RefHead refHead = ref.getRefHead();

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
            addAction.execute(firstDir);
            Commit firstCommit = commitAction.execute();

            // Assert first time HEAD
            String firstHeadRef = ref.readHeadRef();
            assertEquals(Path.of("refs", "heads", "master").toString(), firstHeadRef);
            ObjectID firstHeadObjectID = new ObjectID(ref.readHead());
            assertEquals(firstCommit.getOid(), firstHeadObjectID);
            ObjectID refHeadMaster = new ObjectID(refHead.getBranchHeadContent("master"));
            assertEquals(firstCommit.getOid(), refHeadMaster);

            // Create a new branch and check out to that branch
            branchAction.execute("storing-changes", 0);

            // Assert second time HEAD
            String secondHeadRef = ref.readHeadRef();
            assertEquals(Path.of("refs", "heads", "storing-changes").toString(), secondHeadRef);
            ObjectID secondHeadObjectID = new ObjectID(ref.readHead());
            assertEquals(firstCommit.getOid(), secondHeadObjectID);

            refHeadMaster = new ObjectID(refHead.getBranchHeadContent("master"));
            assertEquals(firstCommit.getOid(), refHeadMaster);
            ObjectID refHeadStoringChanges = new ObjectID(refHead.getBranchHeadContent("storing-changes"));
            assertEquals(firstCommit.getOid(), refHeadStoringChanges);

            // Change contents of testFile11 and testFile112
            TestUtils.writeText(testFile11, "Test content 11 modified");
            TestUtils.writeText(testFile112, "Test content 112 modified");

            // Add firstDir to staging and commit
            addAction.execute(testFile11);
            addAction.execute(testFile112);
            Commit secondCommit = commitAction.execute();

            // Assert third time HEAD
            String thirdHeadRef = ref.readHeadRef();
            assertEquals(Path.of("refs", "heads", "storing-changes").toString(), thirdHeadRef);
            ObjectID thirdHeadObjectID = new ObjectID(ref.readHead());
            assertEquals(secondCommit.getOid(), thirdHeadObjectID);

            refHeadMaster = new ObjectID(refHead.getBranchHeadContent("master"));
            assertEquals(firstCommit.getOid(), refHeadMaster);
            ObjectID refHeadStoringChangesSecond = new ObjectID(refHead.getBranchHeadContent("storing-changes"));
            assertEquals(secondCommit.getOid(), refHeadStoringChangesSecond);

        } catch (Exception ex) {
            ex.printStackTrace();
            fail();
        }

    }

    @Test
    void testAddNewBranchToSomeRevision() {

        BranchAction branchAction = new BranchAction();
        AddAction addAction = new AddAction();
        CommitAction commitAction = new CommitAction();
        Refs ref = new Refs();
        RefHead refHead = ref.getRefHead();

        try {

            // Create files and its contents in the firstDir
            Files.createDirectories(firstDir);
            TestUtils.writeText(testFile11, "Test content 11");
            TestUtils.writeText(testFile12, "Test content 12");

            // Add firstDir to staging and commit
            addAction.execute(firstDir);
            Commit firstCommit = commitAction.execute();

            // Assert first time HEAD
            String firstHeadRef = ref.readHeadRef();
            assertEquals(Path.of("refs", "heads", "master").toString(), firstHeadRef);
            ObjectID firstHeadObjectID = new ObjectID(ref.readHead());
            assertEquals(firstCommit.getOid(), firstHeadObjectID);
            ObjectID headCommitID1 = new ObjectID(refHead.getBranchHeadContent("master"));
            assertEquals(firstCommit.getOid(), headCommitID1);

            // Create files and its contents in the subFirstDir
            Files.createDirectories(subFirstDir);
            TestUtils.writeText(testFile111, "Test content 111");
            TestUtils.writeText(testFile112, "Test content 112");

            // Add subFirstDir to staging and commit
            addAction.execute(subFirstDir);
            Commit secondCommit = commitAction.execute();

            // Assert second time HEAD
            String secondHeadRef = ref.readHeadRef();
            assertEquals(Path.of("refs", "heads", "master").toString(), secondHeadRef);
            ObjectID secondHeadObjectID = new ObjectID(ref.readHead());
            assertEquals(secondCommit.getOid(), secondHeadObjectID);
            ObjectID headCommitID2 = new ObjectID(refHead.getBranchHeadContent("master"));
            assertEquals(secondCommit.getOid(), headCommitID2);


            // Change contents of testFile11 and testFile112
            TestUtils.writeText(testFile11, "Test content 11 modified");
            TestUtils.writeText(testFile112, "Test content 112 modified");

            // Add firstDir to staging and commit
            addAction.execute(testFile11);
            addAction.execute(testFile112);
            Commit thirdCommit = commitAction.execute();

            // Assert third time HEAD
            String thirdHeadRef = ref.readHeadRef();
            assertEquals(Path.of("refs", "heads", "master").toString(), thirdHeadRef);
            ObjectID thirdHeadObjectID = new ObjectID(ref.readHead());
            assertEquals(thirdCommit.getOid(), thirdHeadObjectID);
            ObjectID headCommitID3 = new ObjectID(refHead.getBranchHeadContent("master"));
            assertEquals(thirdCommit.getOid(), headCommitID3);

            // Create a new branch from the second-to-last commit
            branchAction.execute("second-to-last", 1);

            // Assert HEAD a fourth time
            String fourthHeadRef = ref.readHeadRef();
            assertEquals(Path.of("refs", "heads", "second-to-last").toString(), fourthHeadRef);
            ObjectID fourthHeadObjectID = new ObjectID(ref.readHead());
            assertEquals(secondCommit.getOid(), fourthHeadObjectID);

            // Create a new branch from the third-to-last commit
            branchAction.execute("third-to-last", 1);

            // Assert HEAD a fifth time
            String fifthHeadRef = ref.readHeadRef();
            assertEquals(Path.of("refs", "heads", "third-to-last").toString(), fifthHeadRef);
            ObjectID fifthHeadObjectID = new ObjectID(ref.readHead());
            assertEquals(firstCommit.getOid(), fifthHeadObjectID);

        } catch (Exception ex) {
            ex.printStackTrace();
            fail();
        }

    }


}
