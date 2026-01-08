package duongtran.vctrl.actions;

import duongtran.vctrl.TestUtils;
import duongtran.vctrl.Workspace;
import duongtran.vctrl.branches.migration.ConflictException;
import duongtran.vctrl.index.Index;
import duongtran.vctrl.references.Refs;
import duongtran.vctrl.storage.Database;
import duongtran.vctrl.storage.objects.Blob;
import duongtran.vctrl.storage.objects.Commit;
import duongtran.vctrl.utils.DirectoryNames;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CheckoutActionTest {

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

    }

    @AfterEach
    void tearDown() {
        TestUtils.removeWorkspace();
        workspace = null;
    }

    @Test
    void testCheckoutSameBranch() {

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

            // Assert the contents of testFile11 and testFile112
            String testFile11Content = TestUtils.readFileContents(testFile11);
            String testFile21Content = TestUtils.readFileContents(testFile21);
            assertEquals("Test content 11", testFile11Content);
            assertEquals("Test content 21", testFile21Content);
            assertFalse(Files.exists(testFile1));
            assertFalse(Files.exists(testFile13));
            assertTrue(Files.exists(testFile22));
            assertTrue(Files.exists(testFile112));

            // The index must reflect the current workspace
            Index actualIndex = Index.loadFromDisk();
            Index expectedIndex = new Index();
            updateIndexEntries(expectedIndex, new ArrayList<>(List.of(
                    testFile11, testFile12, testFile21, testFile22, testFile111, testFile112
            )));
            assertEquals(expectedIndex, actualIndex);

            // HEAD must point to the checkout branch
            String headRawContent = refs.readRawHeadContent();
            assertEquals("ref: " + Path.of("refs", "heads", checkoutBranchName), headRawContent);
            String headContent = refs.readHead();
            assertEquals(thirdCommit.getOid().getValue(), headContent);
            assertEquals(firstBranchContent, headContent);

            // Commit after checkout, the checkout branch and head need to point to the same
            // Create files and its contents in the secondDir
            Files.createDirectories(thirdDir);
            TestUtils.writeText(testFile31, "Test content 31");
            TestUtils.writeText(testFile31, "Test content 32");

            // Add the thirdDir to staging and commit
            addAction.execute(thirdDir);
            Commit fifthCommit = commitAction.execute("Dummy commit message");
            String headRawContent2 = refs.readRawHeadContent();
            assertEquals("ref: " + Path.of("refs", "heads", checkoutBranchName), headRawContent2);
            String headContent2 = refs.readHead();
            assertEquals(fifthCommit.getOid().getValue(), headContent2);
            String firstBranchContent2 = refs.getRefHead().getBranchHeadContent(checkoutBranchName);
            assertEquals(fifthCommit.getOid().getValue(), firstBranchContent2);

            // master branch point to the fourth commit
            String masterBranchContentAfterCheckout = refs.getRefHead().getBranchHeadContent(DirectoryNames.DEFAULT_BRANCH_NAME);
            assertEquals(fourthCommit.getOid().getValue(), masterBranchContentAfterCheckout);

        } catch (Exception ex) {
            ex.printStackTrace();
            fail();
        }
    }

    @Test
    void testCheckoutHasConflict() throws IOException, NoSuchAlgorithmException {

        AddAction addAction = new AddAction();
        CommitAction commitAction = new CommitAction();
        CheckoutAction checkoutAction = new CheckoutAction();
        Index indexBeforeCheckout = null;

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

            // Add testFile1, and testFile13
            TestUtils.writeText(testFile1, "Test content 1");
            TestUtils.writeText(testFile13, "Test content 13");

            // Change contents of testFile11 and testFile21
            TestUtils.writeText(testFile11, "Test content 11 modified first time");
            TestUtils.writeText(testFile21, "Test content 21 modified first time");

            // Delete testFile22 and testFile112
            TestUtils.deleteRecursively(testFile22);
            TestUtils.deleteRecursively(testFile112);

            // Add changes to staging and commit
            addAction.execute(testFile1);
            addAction.execute(testFile13);
            addAction.execute(testFile11);
            addAction.execute(testFile21);
            addAction.execute(testFile22);
            indexBeforeCheckout = addAction.execute(testFile112);
            Commit fourthCommit = commitAction.execute("Dummy commit message");

            // Change contents of testFile11 and testFile21
            TestUtils.writeText(testFile11, "Test content 11 modified second time");

            // Check out from the fourth commit to the third commit
            checkoutAction.execute(DirectoryNames.DEFAULT_BRANCH_NAME, 1);

            // After executed checkout, need to go to the exception block
            fail();

        } catch (ConflictException ex) {
            log.debug("Conflict Error messages: {}", ex.getMessage());
            assertFalse(ex.getMessage().isEmpty());
        } catch (Exception ex) {
            ex.printStackTrace();
            fail();
        } finally {
            // Assert the contents
            String testFile11Content = TestUtils.readFileContents(testFile11);
            String testFile21Content = TestUtils.readFileContents(testFile21);
            assertEquals("Test content 11 modified second time", testFile11Content);
            assertEquals("Test content 21 modified first time", testFile21Content);
            assertTrue(Files.exists(testFile1));
            assertTrue(Files.exists(testFile13));
            assertFalse(Files.exists(testFile22));
            assertFalse(Files.exists(testFile112));

            // The index must reflect the current workspace
            Index actualIndex = Index.loadFromDisk();
            assertEquals(indexBeforeCheckout, actualIndex);

        }
    }

    private void updateIndexEntries(Index index, List<Path> paths) {
        try {
            for (Path path : paths) {
                byte[] fileBytes = Files.readAllBytes(path);
                Blob blob = new Blob(fileBytes);
                blob.calculateOid(blob.toBytes());
                index.addEntry(path, blob.getOid().getValue());
            }
            byte[] bytes = new byte[index.getSizeInBytes()];
            ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
            // Trigger to compute index's checksum
            index.toBytes(buf);
        } catch (Exception ex) {
            log.error("Cannot update index: {}", ex.getMessage());
        }


    }

}
