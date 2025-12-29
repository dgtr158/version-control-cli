package duongtran.vctrl.references;


import duongtran.vctrl.Workspace;
import duongtran.vctrl.concurrency.Lockfile;
import duongtran.vctrl.storage.ObjectID;
import duongtran.vctrl.utils.DirectoryNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The Refs class is responsible for managing the HEAD reference within a version control system.
 * It provides functionality to update and read the HEAD reference, which typically points
 * to the latest object ID in the repository.
 */
public class Refs {
    private static final Logger log = LoggerFactory.getLogger(Refs.class);

    private Path refsPath;
    private RefHead refHead;

    public Refs() {
        try {
            Workspace workspace = Workspace.getInstance();
            Path vctrlPath = workspace.getVctrlPath();
            refsPath = vctrlPath.resolve(DirectoryNames.REF_DIR_NAME);
            refHead = new RefHead(vctrlPath.resolve(DirectoryNames.REF_DIR_NAME));

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public Path getRefsPath() {
        return refsPath;
    }

    public RefHead getRefHead() {
        return refHead;
    }

    /**
     * Updates the content of the HEAD reference file. This method locks the HEAD file,
     * writes the provided content to it, and then commits the changes. If the lock cannot
     * be acquired, a warning is logged indicating the failure.
     *
     * @param content the new content to write into the HEAD reference file. This is typically
     *                a reference or pointer to the current branch or commit.
     */
    public void updateHeadRef(String content) {
        Path headPath = Workspace.getInstance().getVctrlPath().resolve(DirectoryNames.HEAD);
        try (Lockfile lockfile = new Lockfile(headPath)) {
            lockfile.acquire();
            lockfile.write(content + "\n");
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
     * is successfully read; otherwise, returns {@code null}
     */
    public String readHeadCommitId() {
        Path headPath = Workspace.getInstance().getVctrlPath().resolve(DirectoryNames.HEAD);
        if (Files.exists(headPath)) {
            try {
                String relativeRefPath = Files.readString(headPath, StandardCharsets.UTF_8).strip();
                Path branchHeadPath = Workspace.getInstance().getVctrlPath().resolve(relativeRefPath);
                return Files.readString(branchHeadPath, StandardCharsets.UTF_8).strip();
            } catch (IOException e) {
                log.error("Failed to read HEAD: {}", e.getMessage());
                return null;
            }
        }
        return null;
    }

    /**
     * Reads the content of the HEAD reference file if it exists.
     * This method determines the HEAD file's path based on the workspace's version control directory.
     * If the file exists, it attempts to read its content as a UTF-8 encoded string.
     * If an I/O error occurs during reading or the file does not exist, the method returns null.
     *
     * @return the content of the HEAD file as a UTF-8 encoded string if the file exists and is successfully read;
     * otherwise, returns null
     */
    public String readHeadRef() {
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

    /**
     * Sets the HEAD reference of the repository. This method updates the HEAD to either
     * point to a branch (symbolic reference) or directly to a commit (using the provided ObjectID).
     *
     * @param branchName the name of the branch to set as the HEAD reference. If the branch exists,
     *                   the HEAD will be updated as a symbolic reference to this branch.
     * @param objectID   the commit identifier (ObjectID) used to update the HEAD if the branch does not exist.
     *                   The HEAD will directly reference this specific commit.
     */
    public void setHead(String branchName, ObjectID objectID) {
        Path branchPath = refHead.getReafHeadPath().resolve(branchName);
        // If the branch exists, update the HEAD as a sym ref to that branch
        if (Files.exists(branchPath)) {
            Path vctrlPath = Workspace.getInstance().getVctrlPath();
            String headContent = String.format("ref: %s", vctrlPath.relativize(branchPath));
            this.updateHeadRef(headContent);
        } else { // Update the HEAD with the objectID
            this.updateHeadRef(objectID.getValue());
        }
    }

}
