package duongtran.vctrl.branches;

import duongtran.vctrl.TestUtils;
import duongtran.vctrl.Workspace;
import duongtran.vctrl.actions.AddAction;
import duongtran.vctrl.actions.BranchAction;
import duongtran.vctrl.actions.CheckoutAction;
import duongtran.vctrl.actions.CommitAction;
import duongtran.vctrl.references.Refs;
import duongtran.vctrl.storage.Database;
import duongtran.vctrl.storage.objects.Commit;
import duongtran.vctrl.utils.DirectoryNames;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RevListTest {

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
    Path thirdDir;
    Path fourthDir;
    Path fifthDir;

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
    Path testFile51;
    Path testFile52;

    @BeforeEach
    void setup() {
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
        fifthDir = rootPath.resolve("fifthDir");

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
        testFile51 = fourthDir.resolve("file51.txt");
        testFile52 = fourthDir.resolve("file52.txt");

    }

    @AfterEach
    void tearDown() {
        TestUtils.removeWorkspace();
        workspace = null;
    }

    @Test
    void testShowLinearCommitHistory() {

        AddAction addAction = new AddAction();
        CommitAction commitAction = new CommitAction();

        try {
            // Create files and its contents in the firstDir
            Files.createDirectories(firstDir);
            TestUtils.writeText(testFile11, "Test content 11");
            TestUtils.writeText(testFile12, "Test content 12");

            // Add firstDir to staging and commit
            addAction.execute(firstDir);
            Commit firstCommit = commitAction.execute();

            // Create files and its contents in the subFirstDir
            Files.createDirectories(subFirstDir);
            TestUtils.writeText(testFile111, "Test content 111");
            TestUtils.writeText(testFile112, "Test content 112");

            // Add subFirstDir to staging and commit
            addAction.execute(subFirstDir);
            Commit secondCommit = commitAction.execute();

            // Create files and its contents in the secondDir
            Files.createDirectories(secondDir);
            TestUtils.writeText(testFile21, "Test content 21");
            TestUtils.writeText(testFile22, "Test content 22");

            // Add the secondDir to staging and commit
            addAction.execute(secondDir);
            Commit thirdCommit = commitAction.execute();

            // Create files and its contents in the subSecondDir
            Files.createDirectories(subSecondDir);
            TestUtils.writeText(testFile211, "Test content 211");
            TestUtils.writeText(testFile212, "Test content 212");

            // Add the subSecondDir to staging and commit
            addAction.execute(subSecondDir);
            Commit fourthCommit = commitAction.execute();

            // Create files and its contents in the thirdDir
            Files.createDirectories(thirdDir);
            TestUtils.writeText(testFile31, "Test content 31");
            TestUtils.writeText(testFile32, "Test content 32");

            // Add the subSecondDir to staging and commit
            addAction.execute(thirdDir);
            Commit fifthCommit = commitAction.execute();

            // Create files and its contents in the fourthDir
            Files.createDirectories(fourthDir);
            TestUtils.writeText(testFile41, "Test content 41");
            TestUtils.writeText(testFile42, "Test content 42");

            // Add the subSecondDir to staging and commit
            addAction.execute(fourthDir);
            Commit sixthCommit = commitAction.execute();

            // Assert the commit history
            Refs refs = new Refs();
            List<String> branches = new ArrayList<>(List.of(DirectoryNames.DEFAULT_BRANCH_NAME));
            String currentBranchName = refs.getCurrentBranch();
            RevList revList = new RevList(refs, branches);
            Iterator<Commit> commitIterator = revList.iterator();

            assertTrue(commitIterator.hasNext());
            assertEquals(sixthCommit, commitIterator.next());
            assertEquals(fifthCommit, commitIterator.next());
            assertEquals(fourthCommit, commitIterator.next());
            assertEquals(thirdCommit, commitIterator.next());
            assertEquals(secondCommit, commitIterator.next());
            assertEquals(firstCommit, commitIterator.next());
            assertFalse(commitIterator.hasNext());

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }

    }

    @Test
    void testShowBranchingCommitHistory() {

        AddAction addAction = new AddAction();
        CommitAction commitAction = new CommitAction();
        CheckoutAction checkoutAction = new CheckoutAction();
        BranchAction branchAction = new BranchAction();

        try {
            // Create files and its contents in the firstDir
            Files.createDirectories(firstDir);
            TestUtils.writeText(testFile11, "Test content 11");
            TestUtils.writeText(testFile12, "Test content 12");

            // Add firstDir to staging and commit
            addAction.execute(firstDir);
            Commit firstCommit = commitAction.execute();

            // Create files and its contents in the subFirstDir
            Files.createDirectories(subFirstDir);
            TestUtils.writeText(testFile111, "Test content 111");
            TestUtils.writeText(testFile112, "Test content 112");

            // Add subFirstDir to staging and commit
            addAction.execute(subFirstDir);
            Commit secondCommit = commitAction.execute();

            String checkoutBranch = "firstBranch";
            branchAction.execute(checkoutBranch, 0);
            checkoutAction.execute(checkoutBranch, 0);

            // Create files and its contents in the secondDir
            Files.createDirectories(secondDir);
            TestUtils.writeText(testFile21, "Test content 21");
            TestUtils.writeText(testFile22, "Test content 22");

            // Add the secondDir to staging and commit
            addAction.execute(secondDir);
            Commit thirdCommit = commitAction.execute();

            // Checkout to master branch
            checkoutAction.execute(DirectoryNames.DEFAULT_BRANCH_NAME, 0);

            // Create files and its contents in the subSecondDir
            Files.createDirectories(subSecondDir);
            TestUtils.writeText(testFile211, "Test content 211");
            TestUtils.writeText(testFile212, "Test content 212");

            // Add the subSecondDir to staging and commit
            addAction.execute(subSecondDir);
            Commit fourthCommit = commitAction.execute();

            // Checkout to firstBranch
            checkoutAction.execute(checkoutBranch, 0);

            // Create files and its contents in the thirdDir
            Files.createDirectories(thirdDir);
            TestUtils.writeText(testFile31, "Test content 31");
            TestUtils.writeText(testFile32, "Test content 32");

            // Add the subSecondDir to staging and commit
            addAction.execute(thirdDir);
            Commit fifthCommit = commitAction.execute();

            // Checkout to master branch
            checkoutAction.execute(DirectoryNames.DEFAULT_BRANCH_NAME, 0);

            // Create files and its contents in the fourthDir
            Files.createDirectories(fourthDir);
            TestUtils.writeText(testFile41, "Test content 41");
            TestUtils.writeText(testFile42, "Test content 42");

            // Add the subSecondDir to staging and commit
            addAction.execute(fourthDir);
            Commit sixthCommit = commitAction.execute();

            // Checkout to firstBranch
            checkoutAction.execute(checkoutBranch, 0);

            // Create files and its contents in the fifthDir
            Files.createDirectories(fifthDir);
            TestUtils.writeText(testFile51, "Test content 51");
            TestUtils.writeText(testFile52, "Test content 52");

            // Add the subSecondDir to staging and commit
            addAction.execute(fifthDir);
            Commit seventhCommit = commitAction.execute();

            // Assert the revision list
            Refs refs = new Refs();
            String currentBranchName = refs.getCurrentBranch();
            RevList revList = new RevList(refs, new ArrayList<>(List.of(currentBranchName)));
            Iterator<Commit> commitIterator = revList.iterator();

            assertTrue(commitIterator.hasNext());
            assertEquals(seventhCommit, commitIterator.next());
            assertEquals(sixthCommit, commitIterator.next());
            assertEquals(fifthCommit, commitIterator.next());
            assertEquals(fourthCommit, commitIterator.next());
            assertEquals(thirdCommit, commitIterator.next());
            assertEquals(secondCommit, commitIterator.next());
            assertEquals(firstCommit, commitIterator.next());
            assertFalse(commitIterator.hasNext());

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }

    }




}
