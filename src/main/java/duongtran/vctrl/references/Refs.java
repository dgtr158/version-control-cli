package duongtran.vctrl.references;


import duongtran.vctrl.Workspace;
import duongtran.vctrl.concurrency.Lockfile;
import duongtran.vctrl.storage.ObjectID;
import duongtran.vctrl.utils.DirectoryNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * The Refs class is responsible for managing the HEAD reference within a version control system.
 * It provides functionality to update and read the HEAD reference, which typically points
 * to the latest object ID in the repository.
 */
public class Refs {
    private static final Logger log = LoggerFactory.getLogger(Refs.class);

    /**
     * Updates the HEAD reference to the specified {@code ObjectID}.
     * This method acquires a lock on the HEAD reference file, writes the
     * string representation of the given {@code ObjectID} to the file,
     * and commits the change. If the lock cannot be acquired, a warning is logged.
     *
     * @param objectId the {@code ObjectID} instance representing the new value
     *                 to assign to the HEAD reference
     */
    public static void updateHead(ObjectID objectId) {
        Path headPath = Workspace.getInstance().getVctrlPath().resolve(DirectoryNames.HEAD);
        try (Lockfile lockfile = new Lockfile(headPath)) {
            lockfile.acquire();
            lockfile.write(objectId.getValue() + "\n");
            lockfile.commit();
        } catch (Exception e) {
            log.warn("Failed to acquire lock: {}\n Retry later", e.getMessage());
        }
    }

    /**
     * Reads the content of the HEAD reference file if it exists. This method checks
     * for the existence of the file at the designated {@code headPath}. If the file
     * exists, its content is read as a UTF-8 encoded string and returned. If the file
     * does not exist or an I/O error occurs while reading, the method returns {@code null}.
     *
     * @return the content of the HEAD file as a UTF-8 encoded string if the file exists and
     *         is successfully read; otherwise, returns {@code null}
     */
    public static String readHead() {
        Path headPath = Workspace.getInstance().getVctrlPath().resolve(DirectoryNames.HEAD);
        if (Files.exists(headPath)) {
            try {
                return Files.readString(headPath, StandardCharsets.UTF_8).strip();
            } catch (IOException e) {
                log.error("Failed to read HEAD: {}", e.getMessage());
                return null;
            }
        }
        return null;
    }

}
