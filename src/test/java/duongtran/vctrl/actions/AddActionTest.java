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
        log.info(workspace.toString());
    }

    @AfterEach
    void cleanup() {
        TestUtils.removeWorkspace();
        workspace = null;
    }


    @Test
    void testSerializeDeserialize() {

        AddAction addAction;

        try {
            // Execute action
            addAction = new AddAction();
            addAction.execute();

            // Validate
            // Load index file, Convert it into ByteBuffer
            Path indexPath = Workspace.getInstance().getRootPath().resolve(DirectoryNames.INDEX);
            byte[] indexAllBytes = Files.readAllBytes(indexPath);
            ByteBuffer byteBuffer = ByteBuffer.wrap(indexAllBytes);
            Index actual = Index.fromBytes(byteBuffer);

            // Validate header
            IndexHeader header = actual.getHeader();
            assertEquals(3, header.getEntryCount());
            assertEquals(Index.VERSION, header.getVersion());

            // Validate entries
            Map<Path, IndexEntry> entryMap = actual.getEntryMap();


        } catch (Exception e) {
            log.error("Failed to execute add action: {}", e.getMessage());
        }


    }

}
