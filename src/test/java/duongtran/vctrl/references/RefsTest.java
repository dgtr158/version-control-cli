package duongtran.vctrl.references;

import duongtran.vctrl.TestUtils;
import duongtran.vctrl.Workspace;
import duongtran.vctrl.storage.Database;
import duongtran.vctrl.storage.ObjectID;
import duongtran.vctrl.storage.ObjectStorage;
import duongtran.vctrl.utils.DirectoryNames;
import duongtran.vctrl.utils.Utils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

import static org.junit.jupiter.api.Assertions.*;

public class RefsTest {

    // Vctrl instances
    Workspace workspace;
    Database database;
    Path rootPath;
    Path vctrlPath;
    Path refPath;
    Path headPath;
    Path refHeadPath;
    Path firstBranchPath;

    @BeforeEach
    void setup() throws IOException {
        TestUtils.createTestWorkspace();
        workspace = Workspace.getInstance();
        database = Database.getInstance();

        // Paths
        rootPath = workspace.getRootPath();
        vctrlPath = workspace.getVctrlPath();
        refPath = vctrlPath.resolve(DirectoryNames.REF_DIR_NAME);
        headPath = vctrlPath.resolve(DirectoryNames.HEAD);
        refHeadPath = refPath.resolve(DirectoryNames.REF_HEAD_DIR_NAME);
        firstBranchPath = refHeadPath.resolve("firstBranch");

        // Create the ref directory
        Files.createDirectories(refPath);

        // Create the refHeadPath directory
        Files.createDirectories(refHeadPath);

        // Create HEAD
        Files.createDirectories(headPath);

    }

    @AfterEach
    void tearDown() {
        TestUtils.removeWorkspace();
        workspace = null;
    }

    @Test
    void testGetHeadContentIsObjectID() {

        Refs refs = new Refs();

        // Create head content
        String hashContent = "Test get head content containing object id";
        byte[] contentBytes = hashContent.getBytes(StandardCharsets.UTF_8);

        try {
            // Hash the content
            MessageDigest digest = MessageDigest.getInstance(ObjectStorage.HASH_ALGORITHM);
            String hashId = Utils.bytesToHex(digest.digest(contentBytes));

            // Write the content to HEAD
            refs.setHead(hashId);

            // Read back the head content
            String readHeadContent = refs.readHead();

            // Assert: head content is objectId
            assertEquals(hashId, readHeadContent);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Test
    void testGetHeadContentIsRef() {

        Refs refs = new Refs();

        // Create head content
        String hashContent = "Test get head content containing ref";
        byte[] contentBytes = hashContent.getBytes(StandardCharsets.UTF_8);

        try {

            // Hash the content
            MessageDigest digest = MessageDigest.getInstance(ObjectStorage.HASH_ALGORITHM);
            String hashId = Utils.bytesToHex(digest.digest(contentBytes));

            // Write the content to the firstBranch
            TestUtils.writeText(firstBranchPath, hashId);

            // Write the content to HEAD
            refs.setHead("firstBranch", new ObjectID(hashId));

            // Read back the head content
            String readHeadContent = refs.readHead();

            // Assert: head content is objectId
            assertEquals(hashId, readHeadContent);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
