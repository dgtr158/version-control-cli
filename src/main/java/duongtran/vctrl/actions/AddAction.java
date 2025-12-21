package duongtran.vctrl.actions;

import duongtran.vctrl.Workspace;
import duongtran.vctrl.index.Index;
import duongtran.vctrl.storage.Database;
import duongtran.vctrl.storage.objects.Blob;
import duongtran.vctrl.utils.DirectoryNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

/**
 * Represents the action of adding files or directories to an index within the
 * context of a version control system. This class provides methods to process
 * the specified paths, create necessary blob objects for files, store them
 * in a database, and update the index with entries corresponding to the added
 * files or directories.
 * <p>
 * The AddAction class interacts with a workspace to determine file paths
 * relative to a version control directory, a database to store blobs, and
 * an index to track changes made to files and directories within the workspace.
 */
public class AddAction {

    private static final Logger log = LoggerFactory.getLogger(AddAction.class);

    private final Workspace workspace;
    private final Database database;
    private final Path normalizedVctrl;

    public AddAction() {
        this.workspace = Workspace.getInstance();
        this.database = Database.getInstance();
        this.normalizedVctrl = workspace.getVctrlPath().toAbsolutePath().normalize();
    }

    /**
     * Executes the process of adding a file or directory (and its contents if applicable)
     * to the index within the context of a version control system. If the index does not
     * exist, a new one is created. The updated index is then written back to disk upon
     * successful execution.
     *
     * @param addPath the path of the file or directory to be added to the index
     * @return the updated index after processing the specified path
     */
    public Index execute(Path addPath) {
        Index index = null;
        try {

            if (Files.exists(Workspace.getInstance().getVctrlPath().resolve(DirectoryNames.INDEX))) {
                index = Index.loadFromDisk();
            } else {
                index = new Index();
            }
            execute(addPath, index);
            index.write();

        } catch (IOException | NoSuchAlgorithmException e) {
            log.error("Failed to write index file: {}\n", e.getMessage());
        }

        return index;
    }

    /**
     * Executes the process of adding a file or directory (and its contents if applicable)
     * to the index within the context of a version control system. If the specified path
     * does not exist and is already present in the index, the entry is removed. Otherwise,
     * it processes the path and updates the index accordingly.
     *
     * @param addPath the path of the file or directory to be added or removed from the index
     * @param index   the index object to be updated based on the specified path
     * @throws IOException              if the specified path does not exist or an I/O error occurs
     * @throws NoSuchAlgorithmException if the required algorithm for hashing is not available
     */
    private void execute(Path addPath, Index index) throws IOException, NoSuchAlgorithmException {
        if (!Files.exists(addPath)) {
            if (index.contains(addPath)) {
                index.removeEntry(addPath);
                return;
            }
            throw new IOException("Path does not exist: " + addPath);
        }

        // Ignore the .vctrl directory
        Path normalized = addPath.toAbsolutePath().normalize();
        if (normalized.startsWith(normalizedVctrl)) return;

        if (!Files.isDirectory(addPath)) {
            addFile(addPath, index);
            return;
        }
        // If addPath is a directory, list all files in the provided path
        // For each file, create a Blob object and store in DB
        for (Path path : workspace.listFiles(addPath)) {
            execute(path, index);
        }
    }

    /**
     * Adds the specified file to the index by storing its contents in the repository
     * and creating a corresponding entry in the index.
     *
     * @param path  the path of the file to be added
     * @param index the index object where the file entry will be registered
     * @throws IOException              if an I/O error occurs while reading the file or accessing the repository
     * @throws NoSuchAlgorithmException if the algorithm required for creating the file blob is unavailable
     */
    private void addFile(Path path, Index index) throws IOException, NoSuchAlgorithmException {
        // Store files
        Blob blob = new Blob(Files.readAllBytes(path));
        String blobId = database.store(blob).getValue();

        // Create the index entries
        index.addEntry(path, blobId);
    }

}
