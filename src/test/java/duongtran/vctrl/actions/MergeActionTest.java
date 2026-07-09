package duongtran.vctrl.actions;

import duongtran.vctrl.TestUtils;
import duongtran.vctrl.Workspace;
import duongtran.vctrl.references.Refs;
import duongtran.vctrl.storage.Database;
import duongtran.vctrl.storage.ObjectID;
import duongtran.vctrl.storage.ObjectType;
import duongtran.vctrl.storage.objects.Commit;
import duongtran.vctrl.utils.DirectoryNames;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MergeActionTest {

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
    void tearDown() {
        TestUtils.removeWorkspace();
        workspace = null;
    }

    @Test
    void testMergeWithUniqueCommonAncestor() {

        AddAction addAction = new AddAction();
        CommitAction commitAction = new CommitAction();
        CheckoutAction checkoutAction = new CheckoutAction();
        BranchAction branchAction = new BranchAction();
        MergeAction mergeAction = new MergeAction();
        Refs refs = new Refs();

        try {

            // Create files and its contents in the firstDir
            Files.createDirectories(firstDir);
            TestUtils.writeText(testFile11, "Test content 11");

            // Add firstDir to staging and commit
            addAction.execute(firstDir);
            Commit firstCommit = commitAction.execute("Dummy commit message");

            // Create files and its contents in the subFirstDir
            TestUtils.writeText(testFile12, "Test content 12");
            Files.createDirectories(subFirstDir);
            TestUtils.writeText(testFile111, "Test content 111");
            TestUtils.writeText(testFile112, "Test content 112");

            // Add testFile12, subFirstDir to staging and commit
            addAction.execute(testFile12);
            addAction.execute(subFirstDir);
            Commit secondCommit = commitAction.execute("Dummy commit message");

            // Create files and its contents in the secondDir
            Files.createDirectories(secondDir);
            TestUtils.writeText(testFile21, "Test content 21");
            TestUtils.writeText(testFile22, "Test content 22");

            // Add the secondDir, testFile11, testFile112 to staging and commit
            addAction.execute(secondDir);
            addAction.execute(testFile11);
            addAction.execute(testFile112);
            Commit thirdCommit = commitAction.execute("Dummy commit message");

            // Assert the current HEAD
            String headAfterThirdCommit = refs.readHead();
            assertEquals(thirdCommit.getOid().getValue(), headAfterThirdCommit);

            // Create a new branch
            String checkoutBranchName = "firstBranch";
            branchAction.execute(checkoutBranchName, 0);

            // Assert the firstBranch's head content
            String firstBranchContent = refs.getRefHead().getBranchHeadContent(checkoutBranchName);
            assertEquals(thirdCommit.getOid().getValue(), firstBranchContent);

            // Add testFile1, and testFile13
            TestUtils.writeText(testFile1, "Test content 1");
            TestUtils.writeText(testFile13, "Test content 13");

            // Change contents of testFile11 and testFile21
            TestUtils.writeText(testFile11, "Test content 11 modified");
            TestUtils.writeText(testFile21, "Test content 21 modified");

            // Delete testFile22 and testFile112
            TestUtils.deleteRecursively(testFile22);
            TestUtils.deleteRecursively(testFile112);

            // Add changes to staging and commit
            addAction.execute(testFile1);
            addAction.execute(testFile13);
            addAction.execute(testFile11);
            addAction.execute(testFile21);
            addAction.execute(testFile22);
            addAction.execute(testFile112);
            Commit fourthCommit = commitAction.execute("Dummy commit message");

            // Check out from the fourth commit to the third commit
            checkoutAction.execute(checkoutBranchName, 0);

            // Assert the contents after checkout to another branch
            String testFile11Content = TestUtils.readFileContents(testFile11);
            String testFile21Content = TestUtils.readFileContents(testFile21);
            assertEquals("Test content 11", testFile11Content);
            assertEquals("Test content 21", testFile21Content);
            assertFalse(Files.exists(testFile1));
            assertFalse(Files.exists(testFile13));
            assertTrue(Files.exists(testFile22));
            assertTrue(Files.exists(testFile112));

            // HEAD must point to the checkout branch
            String headRawContent = refs.readRawHeadContent();
            assertEquals("ref: " + Path.of("refs", "heads", checkoutBranchName), headRawContent);
            String headContent = refs.readHead();
            assertEquals(thirdCommit.getOid().getValue(), headContent);
            assertEquals(firstBranchContent, headContent);

            // Commit after checkout, the checkout branch and head need to point to the same
            // Create files and its contents in the thirdDir
            Files.createDirectories(thirdDir);
            TestUtils.writeText(testFile31, "Test content 31");

            // Add the thirdDir to staging and commit
            addAction.execute(testFile31);
            Commit fifthCommit = commitAction.execute("Dummy commit message");

            // Assert
            String headRawContent2 = refs.readRawHeadContent();
            assertEquals("ref: " + Path.of("refs", "heads", checkoutBranchName), headRawContent2);
            String headContent2 = refs.readHead();
            assertEquals(fifthCommit.getOid().getValue(), headContent2);
            String firstBranchContent2 = refs.getRefHead().getBranchHeadContent(checkoutBranchName);
            assertEquals(fifthCommit.getOid().getValue(), firstBranchContent2);

            // master branch point to the fourth commit
            String masterBranchContentAfterCheckout = refs.getRefHead().getBranchHeadContent(DirectoryNames.DEFAULT_BRANCH_NAME);
            assertEquals(fourthCommit.getOid().getValue(), masterBranchContentAfterCheckout);

            // Check out to master branch
            checkoutAction.execute(DirectoryNames.DEFAULT_BRANCH_NAME, 0);

            // Assert the contents after checkout to the master branch
            testFile11Content = TestUtils.readFileContents(testFile11);
            testFile21Content = TestUtils.readFileContents(testFile21);
            assertEquals("Test content 11 modified", testFile11Content);
            assertEquals("Test content 21 modified", testFile21Content);
            assertTrue(Files.exists(testFile1));
            assertTrue(Files.exists(testFile13));
            assertFalse(Files.exists(testFile22));
            assertFalse(Files.exists(testFile112));
            assertFalse(Files.exists(thirdDir));
            assertFalse(Files.exists(testFile31));

            // HEAD must point to the master branch
            String headRawContent3 = refs.readRawHeadContent();
            assertEquals("ref: " + Path.of("refs", "heads", "master"), headRawContent3);
            String headContent3 = refs.readHead();
            assertEquals(fourthCommit.getOid().getValue(), headContent3);

            // Create files and its contents in the fourthDir
            Files.createDirectories(fourthDir);
            TestUtils.writeText(testFile41, "Test content 41");

            // Add and commit
            addAction.execute(fourthDir);
            Commit sixthCommit = commitAction.execute("Dummy commit message");

            // Merge firstBranch into master
            mergeAction.execute(checkoutBranchName, 0);

            // Assertion after merged
            // HEAD must point to the master branch
            String headRawContent4 = refs.readRawHeadContent();
            assertEquals("ref: " + Path.of("refs", "heads", "master"), headRawContent4);

            // HEAD content is the seventh commit
            String headContent4 = refs.readHead();
            Commit seventhCommit = (Commit) database.loadObject(new ObjectID(headContent4), ObjectType.COMMIT);

            // seventhCommit's parents are fifthCommit (current firstBranch) and sixthCommit (old master branch)
            // the sixthCommit will go first (Because it's the HEAD commit)
            List<ObjectID> seventhCommitParentIds = seventhCommit.getParentIds();
            assertEquals(2, seventhCommitParentIds.size());
            assertEquals(sixthCommit.getOid(), seventhCommitParentIds.get(0));
            assertEquals(fifthCommit.getOid(), seventhCommitParentIds.get(1));

            /* Assert workspace's files */
            // testFile1
            assertTrue(Files.exists(testFile1));
            assertEquals("Test content 1", TestUtils.readFileContents(testFile1));

            // firstDir
            assertTrue(Files.exists(firstDir));
            assertTrue(Files.exists(testFile11)); // Modified
            assertTrue(Files.exists(testFile12));
            assertTrue(Files.exists(testFile13)); // Added
            assertTrue(Files.exists(subFirstDir));
            assertTrue(Files.exists(testFile111));
            assertFalse(Files.exists(testFile112)); // Deleted
            assertEquals("Test content 11 modified", TestUtils.readFileContents(testFile11));
            assertEquals("Test content 12", TestUtils.readFileContents(testFile12));
            assertEquals("Test content 13", TestUtils.readFileContents(testFile13));
            assertEquals("Test content 111", TestUtils.readFileContents(testFile111));

            // secondDir
            assertTrue(Files.exists(secondDir));
            assertTrue(Files.exists(testFile21)); // Modified
            assertFalse(Files.exists(testFile22)); // Deleted
            assertEquals("Test content 21 modified", TestUtils.readFileContents(testFile21));

            // thirdDir
            assertTrue(Files.exists(thirdDir)); // Added
            assertTrue(Files.exists(testFile31)); // Added
            assertEquals("Test content 31", TestUtils.readFileContents(testFile31));

            // fourthDir
            assertTrue(Files.exists(fourthDir)); // Added
            assertTrue(Files.exists(testFile41)); // Added
            assertEquals("Test content 41", TestUtils.readFileContents(testFile41));


        } catch (Exception ex) {
            ex.printStackTrace();
            fail();
        }

    }

    /*
     * A null merge occurs when the requested merge commit is already reachable from the current HEAD
     * Merge `firstBranch` into `master`
     *
     *        1        2        3       6        7
     *        o <----- o <----- o <---- o <----- o  [HEAD -> master]
     *                   \             /
     *                    \           /
     *                     o <----- o   [firstBranch]
     *                     4        5
     *
     */
    @Test
    void testNullMerge() {

        AddAction addAction = new AddAction();
        CommitAction commitAction = new CommitAction();
        CheckoutAction checkoutAction = new CheckoutAction();
        BranchAction branchAction = new BranchAction();
        MergeAction mergeAction = new MergeAction();
        Refs refs = new Refs();

        String newBranch = "firstBranch";

        try {

            // Create files and its contents in the firstDir
            Files.createDirectories(firstDir);
            TestUtils.writeText(testFile11, "Test content 11");
            TestUtils.writeText(testFile12, "Test content 12");

            // Add firstDir to staging and commit
            addAction.execute(firstDir);
            Commit firstCommit = commitAction.execute("First Commit");

            // Create files and its contents in the subFirstDir
            Files.createDirectories(subFirstDir);
            TestUtils.writeText(testFile111, "Test content 111");
            TestUtils.writeText(testFile112, "Test content 112");

            // Add testFile12, subFirstDir to staging and commit
            addAction.execute(subFirstDir);
            Commit secondCommit = commitAction.execute("Second Commit");

            // Create first Branch
            branchAction.execute(newBranch, 0);

            // Create files and its contents in the secondDir
            Files.createDirectories(secondDir);
            TestUtils.writeText(testFile21, "Test content 21");
            TestUtils.writeText(testFile22, "Test content 22");

            // Add the secondDir to staging and commit
            addAction.execute(secondDir);
            Commit thirdCommit = commitAction.execute("Third commit");

            // Checkout to the `firstBranch`
            checkoutAction.execute(newBranch, 0);

            // Create files and content in the thirdDir
            Files.createDirectories(thirdDir);
            TestUtils.writeText(testFile31, "Test content 31");
            TestUtils.writeText(testFile32, "Test content 32");

            // Add thirdDir to staging and commit
            addAction.execute(thirdDir);
            commitAction.execute("Fourth Commit");

            // Create files and content in the fourthDir
            Files.createDirectories(fourthDir);
            TestUtils.writeText(testFile41, "Test content 41");
            TestUtils.writeText(testFile42, "Test content 42");

            // Checkout to `master` and perform merging `firstBranch` into `master`
            checkoutAction.execute(DirectoryNames.DEFAULT_BRANCH_NAME, 0);
            mergeAction.execute(newBranch, 0);

            // Create testFile1 content
            TestUtils.writeText(testFile1, "Test content 1");

            // Add testFile1 and create a commit
            addAction.execute(testFile1);
            Commit seventhCommit = commitAction.execute("Seventh Commit");

            // Perform null merging
            mergeAction.execute(newBranch, 0);

            // Assert: the HEAD commit is the seventh commit (no more commit is created)
            Commit afterNullMergeCommit = (Commit) database.loadObject(new ObjectID(refs.readHead()), ObjectType.COMMIT);
            assertEquals(seventhCommit, afterNullMergeCommit);

        } catch (Exception ex) {
            ex.printStackTrace();
            fail();
        }


    }

    /*
     * A null merge occurs when the requested merge commit is already reachable from the current HEAD
     * Merge `firstBranch` into `master`
     *
     *        1        2
     *        o <----- o  [HEAD -> master]
     *                   \
     *                    \
     *                     o <----- o   [firstBranch]
     *                     3        4
     *
     */
    @Test
    void testFastForwardMerge() {

        AddAction addAction = new AddAction();
        CommitAction commitAction = new CommitAction();
        CheckoutAction checkoutAction = new CheckoutAction();
        BranchAction branchAction = new BranchAction();
        MergeAction mergeAction = new MergeAction();
        Refs refs = new Refs();

        String newBranch = "firstBranch";

        try {

            // Create files and its contents in the firstDir
            Files.createDirectories(firstDir);
            TestUtils.writeText(testFile11, "Test content 11");
            TestUtils.writeText(testFile12, "Test content 12");

            // Add firstDir to staging and commit
            addAction.execute(firstDir);
            Commit firstCommit = commitAction.execute("First Commit");

            // Create files and its contents in the subFirstDir
            Files.createDirectories(subFirstDir);
            TestUtils.writeText(testFile111, "Test content 111");
            TestUtils.writeText(testFile112, "Test content 112");

            // Add testFile12, subFirstDir to staging and commit
            addAction.execute(subFirstDir);
            Commit secondCommit = commitAction.execute("Second Commit");

            // Create first Branch
            branchAction.execute(newBranch, 0);
            checkoutAction.execute(newBranch, 0);

            // Create files and its contents in the secondDir
            Files.createDirectories(secondDir);
            TestUtils.writeText(testFile21, "Test content 21");
            TestUtils.writeText(testFile22, "Test content 22");

            // Add the secondDir to staging and commit
            addAction.execute(secondDir);
            Commit thirdCommit = commitAction.execute("Third commit");

            // Create files and content in the thirdDir
            Files.createDirectories(thirdDir);
            TestUtils.writeText(testFile31, "Test content 31");
            TestUtils.writeText(testFile32, "Test content 32");

            // Add thirdDir to staging and commit
            addAction.execute(thirdDir);
            Commit fourthCommit = commitAction.execute("Fourth Commit");

            // Checkout to `master`
            checkoutAction.execute(DirectoryNames.DEFAULT_BRANCH_NAME, 0);

            // Perform fast-forward merging on
            mergeAction.execute(newBranch, 0);

            // Assert: the HEAD commit is the fourth commit (no more commit is created)
            Commit afterFastForwardMergeCommit = (Commit) database.loadObject(new ObjectID(refs.readHead()), ObjectType.COMMIT);
            assertEquals(fourthCommit, afterFastForwardMergeCommit);

            // Assert: the firstBranch's head is also the fourth commit
            ObjectID newBranchHeadCommitID = new ObjectID(refs.getRefHead().getBranchHeadContent(newBranch));
            assertEquals(fourthCommit.getOid(), newBranchHeadCommitID);

            // Assert: the master's head is also the fourth commit
            ObjectID masterHeadCommitID = new ObjectID(refs.getRefHead().getBranchHeadContent(DirectoryNames.DEFAULT_BRANCH_NAME));
            assertEquals(fourthCommit.getOid(), masterHeadCommitID);

            // Assert: the HEAD is also the fourth commit
            ObjectID headCommitID = new ObjectID(refs.readHead());
            assertEquals(fourthCommit.getOid(), headCommitID);

        } catch (Exception ex) {
            ex.printStackTrace();
            fail();
        }

    }

    /*
     * Merge `firstBranch` into `master` => conflict
     *
     *        1        2
     *        o <----- o  [HEAD -> master]
     *          \
     *           \
     *            o   [firstBranch]
     *            3
     *
     */
    @Test
    void testMergeFailed() {

        AddAction addAction = new AddAction();
        CommitAction commitAction = new CommitAction();
        CheckoutAction checkoutAction = new CheckoutAction();
        BranchAction branchAction = new BranchAction();
        MergeAction mergeAction = new MergeAction();
        Refs refs = new Refs();

        String newBranch = "firstBranch";

        try {

            // Create files and its contents in the firstDir
            Files.createDirectories(firstDir);
            TestUtils.writeText(testFile11, "Test content 11");
            TestUtils.writeText(testFile12, "Test content 12");

            // Add firstDir to staging and commit
            addAction.execute(firstDir);
            Commit firstCommit = commitAction.execute("First Commit");

            // Change the content of the testFile11 (update 1)
            TestUtils.writeText(testFile11, "Test content 11 update 1");

            // Add updated testFile11 to staging and commit
            addAction.execute(testFile11);
            Commit secondCommit = commitAction.execute("Second Commit");

            // Branch from the first commit of the master -> first branch
            branchAction.execute(newBranch, 1);
            checkoutAction.execute(newBranch, 0);

            // Change the content of the testFile11 (update 2)
            TestUtils.writeText(testFile11, "Test content 11 update 2");

            // Add updated testFile11 to staging and commit
            addAction.execute(testFile11);
            Commit thirdCommit = commitAction.execute("Third Commit");

            // Checkout back to master's head
            checkoutAction.execute(DirectoryNames.DEFAULT_BRANCH_NAME, 0);

            // Perform merge
            mergeAction.execute(newBranch, 0);

            System.out.println("processing...");

        } catch (Exception ex) {
            ex.printStackTrace();
            fail();
        }

    }


}
