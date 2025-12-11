package duongtran.vctrl;

import duongtran.vctrl.storage.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class TestUtils {

    private static final Logger log = LoggerFactory.getLogger(TestUtils.class);

    public static final String TEST_ROOT_PATH = System.getProperty("user.dir");

    /**
     * Workspace structure:
     * <pwd>/workspace/
     *      firstDir/
     *          foo.txt -> "Foo in the first directory"
     *          bar.txt -> "Bar in the first directory"
     *      secondDir/
     *          bas.txt -> "Bas in the second directory"
     */
    public static void createTestWorkspace() {
        Path rootPath = Paths.get(TEST_ROOT_PATH, "workspace");


        try {
            if (Files.exists(rootPath)) {
                deleteRecursively(rootPath);
            }

            // Init workspace object
            Workspace.initialize(rootPath.toString());

            // Init Database
            Database.initialize();


        } catch (IOException e) {
            throw new RuntimeException("Failed to prepare test workspace at: " + rootPath, e);
        }

    }

    public static void removeWorkspace() {
        Path rootPath = null;
        try {
            rootPath = Paths.get(TEST_ROOT_PATH, "workspace");
            deleteRecursively(rootPath);
        } catch (Exception ex) {
            log.error("cannot remove the workspace: {}", rootPath);
        }

    }

    public static void writeText(Path file, String content) throws IOException {
        Files.writeString(file, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) return;
        try (var stream = Files.walk(path)) {
            stream.sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ex) {
                            throw new RuntimeException("Failed to delete: " + p, ex);
                        }
                    });
        }
    }

}
