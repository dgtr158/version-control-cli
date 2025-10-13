package duongtran.example.references;


import duongtran.example.concurrency.Lockfile;
import duongtran.example.utils.DirectoryNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class Refs {
    private static final Logger log = LoggerFactory.getLogger(Refs.class);
    private final Path path;

    public Refs() {
        this("");
    }

    public Refs(String pathName) {
        this.path = Paths.get(DirectoryNames.WORKING_DIRECTORY, DirectoryNames.ROOT_DIR_NAME, pathName);
    }

    public void updateHead(String objectId) {
        Path headPath = getHeadPath();

        try (Lockfile lockfile = new Lockfile(headPath)) {
            lockfile.acquire();
            lockfile.write(objectId + "\n");
            lockfile.commit();
        } catch (Exception e) {
            log.warn("Failed to acquire lock: {}\n Retry later", e.getMessage());
        }
    }

    public String readHead() {
        Path headPath = getHeadPath();
        if (Files.exists(headPath)) {
            try {
                return Files.readString(headPath, StandardCharsets.UTF_8);
            } catch (IOException e) {
                log.error("Failed to read HEAD: {}", e.getMessage());
                return null;
            }
        }
        return null;
    }

    private Path getHeadPath() {
        return path.resolve("HEAD");
    }
}
