package duongtran.vctrl.actions;

import duongtran.vctrl.TestUtils;
import duongtran.vctrl.metadata.Workspace;
import duongtran.vctrl.storage.Database;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        log.info("Inside test");
    }

}
