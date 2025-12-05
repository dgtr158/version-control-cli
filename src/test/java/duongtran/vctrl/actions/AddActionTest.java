package duongtran.vctrl.actions;

import duongtran.vctrl.TestUtils;
import duongtran.vctrl.index.Index;
import duongtran.vctrl.index.IndexEntry;
import duongtran.vctrl.index.IndexHeader;
import duongtran.vctrl.metadata.Workspace;
import duongtran.vctrl.storage.Database;
import duongtran.vctrl.utils.DirectoryNames;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class AddActionTest {

    private static final Logger log = LoggerFactory.getLogger(AddActionTest.class);

    Workspace workspace;
    Database database;


    @BeforeEach
    void setup() {
        TestUtils.createTestWorkspace();
        workspace = Workspace.getInstance();
        database = Database.getInstance();
    }

    @AfterEach
    void cleanup() {
        TestUtils.removeWorkspace();
        workspace = null;
    }


    @Test
    void testAddActionExecutesSuccessfully() {

        AddAction addAction;

        try {
            // Initialize workspace with some test files
            Path testFile1 = workspace.getRootPath().resolve("file1.txt");
            Files.writeString(testFile1, "Test content 1");
            Path testFile2 = workspace.getRootPath().resolve("file2.txt");
            Files.writeString(testFile2, "Test content 2");

            // Execute action
            addAction = new AddAction();
            addAction.execute();

            // Validate that index contains the correct entries
            Path indexPath = workspace.getRootPath().resolve(DirectoryNames.INDEX);
            assertTrue(Files.exists(indexPath));

            byte[] indexAllBytes = Files.readAllBytes(indexPath);
            ByteBuffer byteBuffer = ByteBuffer.wrap(indexAllBytes);
            Index actual = Index.fromBytes(byteBuffer);

            assertEquals(2, actual.getHeader().getEntryCount());
            assertTrue(actual.getEntryMap().containsKey(testFile1));
            assertTrue(actual.getEntryMap().containsKey(testFile2));

        } catch (Exception e) {
            log.error("Test failed: {}", e.getMessage());
            fail();
        }

    }

}
