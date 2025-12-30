package duongtran.vctrl.references;

import duongtran.vctrl.Workspace;
import duongtran.vctrl.concurrency.Lockfile;
import duongtran.vctrl.utils.DirectoryNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * The RefHead class is responsible for managing branch references in a version
 * control system. It handles the creation, update, and retrieval of branch-specific
 * HEAD reference files stored within the reference head directory.
 */
public class RefHead {

    private static final Logger log = LoggerFactory.getLogger(RefHead.class);

    private final Path reafHeadPath;

    public RefHead(Path refPath) {
        reafHeadPath = refPath.resolve(DirectoryNames.REF_HEAD_DIR_NAME);
    }

    public Path getReafHeadPath() {
        return reafHeadPath;
    }

    /**
     * Creates a new branch with the given branch name by resolving its path relative
     * to the reference head directory. The method ensures that a branch with the
     * specified name does not already exist and updates the HEAD reference to point
     * to the newly created branch path.
     *
     * @param branchName the name of the branch to be created
     * @return the {@code Path} of the newly created branch
     * @throws IllegalArgumentException if a branch with the given name already exists
     */
    public Path createBranch(String branchName) {

        Path branchPath = this.reafHeadPath.resolve(branchName);
        if (Files.isRegularFile(branchPath)) {
            throw new IllegalArgumentException(
                    "A branch named '" + branchName + "' already exists."
            );
        }

        // Create a new branch ref
        try {
            Path branchHeadPath = reafHeadPath.resolve(branchName);
            Files.createFile(branchHeadPath);
            return branchHeadPath;
        } catch (IOException ex) {
            log.error("Failed to create branch: {}", ex.getMessage());
            return null;
        }

    }

    /**
     * Updates the content of the branch's HEAD reference file with the specified object ID.
     * If the file does not exist, it will be created. The method ensures that the file is
     * properly locked during the update to prevent concurrent access issues and commits
     * the update atomically.
     *
     * @param path     the path to the branch's HEAD reference file
     * @param objectID the object ID to be written into the branch's HEAD reference file
     * @throws IOException if an I/O error occurs during file creation, locking, writing, or committing
     */
    public void updateBranchHeadValue(Path path, String objectID) throws IOException {
        try {
            if (!Files.exists(path)) {
                Files.createFile(path);
            }

            Lockfile lockfile = new Lockfile(path);
            lockfile.acquire();
            lockfile.write(objectID + "\n");
            lockfile.commit();
        } catch (Exception e) {
            log.warn("Failed to acquire lock: {}\n Retry later", e.getMessage());
        }
    }

    /**
     * Retrieves the content of the HEAD reference file for a specific branch if it exists.
     * The method checks for the existence of the branch's HEAD reference file in the
     * reference head directory. If the file is found, it reads its content using UTF-8
     * encoding, strips any leading or trailing whitespace, and returns the resulting string.
     * If the file does not exist or an I/O error occurs during reading, the method returns
     * {@code null}.
     *
     * @param branchName the name of the branch whose HEAD reference content is to be retrieved
     * @return the content of the branch's HEAD reference file as a string, or {@code null} if
     * the file does not exist or an error occurs during reading
     */
    public String getBranchHeadContent(String branchName) {
        Path branchHeadPath = reafHeadPath.resolve(branchName);
        if (Files.exists(branchHeadPath)) {
            try {
                return Files.readString(branchHeadPath, StandardCharsets.UTF_8).strip();
            } catch (IOException e) {
                log.error("Failed to read HEAD: {}", e.getMessage());
                return null;
            }
        }
        return null;
    }

    /**
     * Lists all branch files under the reference head path of the workspace.
     *
     * @return a list of {@code Path} objects representing all branches available
     * in the reference head directory
     */
    public List<Path> listAllBranches() {
        return Workspace.getInstance().listFiles(reafHeadPath);
    }

    public void deleteBranch(String branchName) throws IOException, IllegalArgumentException {
        // The branch does not exist
        Path branchPath = reafHeadPath.resolve(branchName);
        if (!Files.exists(branchPath)) {
            throw new IllegalArgumentException("The branch [" + branchName + "] does not exist");
        }

        // Do not delete it if the branch is in the current HEAD
        Refs ref = new Refs();
        String headContent = ref.readRawHeadContent();
        if (headContent != null && headContent.startsWith("ref: ")) {
            String currentHeadBranch = Path.of(headContent.substring(5).trim()).getFileName().toString();
            if (branchName.equals(currentHeadBranch)) {
                throw new IllegalArgumentException("The branch [" + branchName + "] is in current HEAD");
            }
        }

        // Perform delete
        Files.delete(branchPath);
    }


}
