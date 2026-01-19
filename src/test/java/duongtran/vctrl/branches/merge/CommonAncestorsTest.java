package duongtran.vctrl.branches.merge;

import duongtran.vctrl.TestUtils;
import duongtran.vctrl.Workspace;
import duongtran.vctrl.actions.*;
import duongtran.vctrl.references.Refs;
import duongtran.vctrl.storage.Database;
import duongtran.vctrl.storage.ObjectID;
import duongtran.vctrl.storage.ObjectType;
import duongtran.vctrl.storage.objects.Commit;
import duongtran.vctrl.utils.DirectoryNames;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class CommonAncestorsTest {

    private static final Logger log = LoggerFactory.getLogger(CheckoutActionTest.class);

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
    void testFindWithUniqueCommonAncestor() {

        AddAction addAction = new AddAction();
        CommitAction commitAction = new CommitAction();
        CheckoutAction checkoutAction = new CheckoutAction();
        BranchAction branchAction = new BranchAction();
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

            // Test the common ancestor
            CommonAncestors commonAncestor = new CommonAncestors(sixthCommit.getOid(), fifthCommit.getOid());
            Set<ObjectID> commonAncestorIds = commonAncestor.find();
            assertEquals(1, commonAncestorIds.size());
            assertTrue(commonAncestorIds.contains(thirdCommit.getOid()));

        } catch (Exception ex) {
            ex.printStackTrace();
            fail();
        }

    }


    /*
     * History with multiple paths between two commits
     *
     *        1        2        3       6        8        9
     *        o <----- o <----- o <---- o <----- o <----- o  [master]
     *                   \             /
     *                    \           /
     *                     o <----- o <----- o  [firstBranch]
     *                     4        5        7
     *
     */
    @Test
    void testFindWithMultipleCommonAncestors1() {

        AddAction addAction = new AddAction();
        CommitAction commitAction = new CommitAction();
        CheckoutAction checkoutAction = new CheckoutAction();
        BranchAction branchAction = new BranchAction();
        MergeAction mergeAction = new MergeAction();
        Refs refs = new Refs();

        String newBranchName = "firstBranch";

        try {

            // Create files and its testFile11 in the firstDir
            Files.createDirectories(firstDir);
            TestUtils.writeText(testFile11, "Test content 11");

            // Add firstDir to staging and commit
            addAction.execute(testFile11);
            Commit firstCommit = commitAction.execute("First commit");
            Thread.sleep(1100);

            // Add testFile12 and commit
            TestUtils.writeText(testFile12, "Test content 12");
            addAction.execute(testFile12);
            Commit secondCommit = commitAction.execute("Second commit");
            Thread.sleep(1100);

            // Create new branch called `firstBranch`
            branchAction.execute(newBranchName, 0);

            // Add subFirstDir and its content then commit
            Files.createDirectories(subFirstDir);
            TestUtils.writeText(testFile111, "Test content 111");
            TestUtils.writeText(testFile112, "Test content 112");
            addAction.execute(subFirstDir);
            Commit thirdCommit = commitAction.execute("Third Commit");
            Thread.sleep(1100);

            // Checkout to the new branch
            checkoutAction.execute(newBranchName, 0);

            // Create files and its testFile21 in the secondDir, then create new commit
            Files.createDirectories(secondDir);
            TestUtils.writeText(testFile21, "Test content 21");
            addAction.execute(testFile21);
            Commit fourthCommit = commitAction.execute("Fourth Commit");
            Thread.sleep(1100);

            // Create files and its testFile22 in the secondDir, then create new commit
            TestUtils.writeText(testFile22, "Test content 22");
            addAction.execute(testFile22);
            Commit fifthCommit = commitAction.execute("Fifth Commit");
            Thread.sleep(1100);

            // Checkout to master branch
            checkoutAction.execute(DirectoryNames.DEFAULT_BRANCH_NAME, 0);

            // Perform merge
            mergeAction.execute(newBranchName, 0);

            // Assertion after merged
            // HEAD must point to the master branch
            String headRawContent1 = refs.readRawHeadContent();
            assertEquals("ref: " + Path.of("refs", "heads", "master"), headRawContent1);

            // HEAD content is the sixth commit
            String headContent1 = refs.readHead();
            Commit sixthCommit = (Commit) database.loadObject(new ObjectID(headContent1), ObjectType.COMMIT);
            Thread.sleep(1100);

            // Checkout to the new branch
            checkoutAction.execute(newBranchName, 0);

            // Create files and its testFile31 in the thirdDir, then create seventh commit
            Files.createDirectories(thirdDir);
            TestUtils.writeText(testFile31, "Test content 31");
            addAction.execute(testFile31);
            Commit seventhCommit = commitAction.execute("Seventh Commit");
            Thread.sleep(1100);

            // After seventh commit: checkout to the master branch
            checkoutAction.execute(DirectoryNames.DEFAULT_BRANCH_NAME, 0);

            // Create testFile32, then create eight commit
            Files.createDirectories(thirdDir);
            TestUtils.writeText(testFile32, "Test content 32");
            addAction.execute(testFile32);
            Commit eightCommit = commitAction.execute("Eight Commit");
            Thread.sleep(1100);

            // Create fourthDir, then create ninth commit
            Files.createDirectories(fourthDir);
            TestUtils.writeText(testFile41, "Test content 41");
            TestUtils.writeText(testFile42, "Test content 42");
            addAction.execute(fourthDir);
            Commit ninthCommit = commitAction.execute("Ninth Commit");
            Thread.sleep(1100);

            // Test the common ancestor
            CommonAncestors commonAncestor = new CommonAncestors(ninthCommit.getOid(), seventhCommit.getOid());
            Set<ObjectID> commonAncestorIds = commonAncestor.find();
            assertEquals(1, commonAncestorIds.size());
            assertTrue(commonAncestorIds.contains(fifthCommit.getOid()));

        } catch (Exception ex) {
            ex.printStackTrace();
            fail();
        }

    }

    /*
     * History with many candidate common ancestors
     * Goal: merge `secondBranch` into `master` branch
     *
     *        1        2        5               9       10
     *        o <----- o <----- o <------------ o <----- o   [master]
     *                   \                     /
     *                    \                   /
     *                     o <----- o <----- o  [firstBranch]
     *                     3        6        8
     *                      \
     *                       \
     *                        o <----- o    [secondBranch]
     *                        4        7
     */
    @Test
    void testFindWithMultipleCommonAncestors2() {

        AddAction addAction = new AddAction();
        CommitAction commitAction = new CommitAction();
        CheckoutAction checkoutAction = new CheckoutAction();
        BranchAction branchAction = new BranchAction();
        MergeAction mergeAction = new MergeAction();
        Refs refs = new Refs();

        String firstBranch = "firstBranch";
        String secondBranch = "secondBranch";

        try {

            // Create files and its testFile11 in the firstDir
            Files.createDirectories(firstDir);
            TestUtils.writeText(testFile11, "Test content 11");

            // Add firstDir to staging and commit
            addAction.execute(testFile11);
            Commit firstCommit = commitAction.execute("First commit");
            Thread.sleep(1100);

            // Add testFile12 and create the second commit
            TestUtils.writeText(testFile12, "Test content 12");
            addAction.execute(testFile12);
            Commit secondCommit = commitAction.execute("Second commit");
            Thread.sleep(1100);

            // After the second commit: Create new branch called `firstBranch` from `master`, and check out
            branchAction.execute(firstBranch, 0);
            checkoutAction.execute(firstBranch, 0);

            // Add subFirstDir and its content then create third commit
            Files.createDirectories(subFirstDir);
            TestUtils.writeText(testFile111, "Test content 111");
            TestUtils.writeText(testFile112, "Test content 112");
            addAction.execute(subFirstDir);
            Commit thirdCommit = commitAction.execute("Third Commit");
            Thread.sleep(1100);

            // After the third commit: Create new branch called `secondBranch` from `firstBranch`, and checkout
            branchAction.execute(secondBranch, 0);
            checkoutAction.execute(secondBranch, 0);

            // Create files and its testFile21 in the secondDir, then create fourth commit
            Files.createDirectories(secondDir);
            TestUtils.writeText(testFile21, "Test content 21");
            addAction.execute(testFile21);
            Commit fourthCommit = commitAction.execute("Fourth Commit");
            Thread.sleep(1100);

            // After the fourth commit: Checkout to `master` and create fifth commit
            checkoutAction.execute(DirectoryNames.DEFAULT_BRANCH_NAME, 0);
            Files.createDirectories(secondDir);
            TestUtils.writeText(testFile22, "Test content 22");
            addAction.execute(testFile22);
            Commit fifthCommit = commitAction.execute("Fifth Commit");
            Thread.sleep(1100);

            // After the fifth commit: Checkout to `firstBranch` from `master`, and create sixth commit
            checkoutAction.execute(firstBranch, 0);
            Files.createDirectories(thirdDir);
            TestUtils.writeText(testFile31, "Test content 31");
            addAction.execute(testFile31);
            Commit sixthCommit = commitAction.execute("Sixth Commit");
            Thread.sleep(1100);

            // After the sixth commit: Checkout to `secondBranch` from `firstBranch`, and create seventh commit
            checkoutAction.execute(secondBranch, 0);
            Files.createDirectories(thirdDir);
            TestUtils.writeText(testFile32, "Test content 32");
            addAction.execute(testFile32);
            Commit seventhCommit = commitAction.execute("Seventh Commit");
            Thread.sleep(1100);

            // After the seventh commit: Checkout to `firstBranch` from `secondBranch`, and create eight commit
            checkoutAction.execute(firstBranch, 0);
            Files.createDirectories(fourthDir);
            TestUtils.writeText(testFile41, "Test content 41");
            addAction.execute(testFile41);
            Commit eightCommit = commitAction.execute("Eight Commit");
            Thread.sleep(1100);

            // After the eight commit: Checkout to `master` from `firstBranch`, and perform merge with `firstBranch`
            checkoutAction.execute(DirectoryNames.DEFAULT_BRANCH_NAME, 0);
            mergeAction.execute(firstBranch, 0);

            // Assertion after merged
            // HEAD must point to the master branch
            String headRawContent1 = refs.readRawHeadContent();
            assertEquals("ref: " + Path.of("refs", "heads", "master"), headRawContent1);

            // HEAD content is the ninth commit
            String headContent1 = refs.readHead();
            Commit ninthCommit = (Commit) database.loadObject(new ObjectID(headContent1), ObjectType.COMMIT);

            // After merged `firstBranch` to `master`, create the tenth commit
            Files.createDirectories(fourthDir);
            TestUtils.writeText(testFile42, "Test content 42");
            addAction.execute(testFile42);
            Commit tenthCommit = commitAction.execute("Tenth Commit");
            Thread.sleep(1100);

            // Test the common ancestor when merge `secondBranch` to `master`
            CommonAncestors commonAncestor = new CommonAncestors(tenthCommit.getOid(), seventhCommit.getOid());
            Set<ObjectID> commonAncestorIds = commonAncestor.find();
            assertEquals(1, commonAncestorIds.size());
            assertTrue(commonAncestorIds.contains(thirdCommit.getOid()));

        } catch (Exception ex) {
            ex.printStackTrace();
            fail();
        }

    }

}
