package duongtran.vctrl;

import duongtran.vctrl.branches.migration.Migration;
import duongtran.vctrl.branches.migration.MigrationActionType;
import duongtran.vctrl.branches.migration.MigrationChange;
import duongtran.vctrl.index.FileStat;
import duongtran.vctrl.index.UnixFileStat;
import duongtran.vctrl.index.WindowFileStat;
import duongtran.vctrl.storage.DataEntry;
import duongtran.vctrl.storage.Database;
import duongtran.vctrl.storage.ObjectType;
import duongtran.vctrl.storage.objects.Blob;
import duongtran.vctrl.utils.DirectoryNames;
import duongtran.vctrl.utils.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.WRITE;

/**
 * The Workspace class is a singleton responsible for managing the file paths
 * and directory structure within the application's root workspace.
 * It allows initializing the workspace with a given root directory and provides
 * utility methods to handle files and directories within the workspace.
 */
public class Workspace {

    private static final Logger log = LoggerFactory.getLogger(Workspace.class);

    private static Workspace instance;
    private Path rootPath;
    private Path vctrlPath;

    // Private constructor to prevent direct instantiation
    private Workspace() {
    }

    /**
     * Initializes the Workspace with the specified root path. If the root path
     * is already set, this method does nothing.
     *
     * @param path the root directory path to initialize the Workspace with
     */
    public static void initialize(String path) {
        Workspace instance = getInstance();
        if (instance.getRootPath() != null) return;
        instance.rootPath = Paths.get(path);
        instance.vctrlPath = instance.rootPath.resolve(DirectoryNames.ROOT_DIR_NAME);
    }


    /**
     * Provides a singleton instance of the Workspace class.
     * If the instance does not already exist, it initializes a new Workspace object.
     *
     * @return the singleton instance of the Workspace class
     */
    public static Workspace getInstance() {
        if (instance == null) {
            instance = new Workspace();
        }
        return instance;
    }

    /**
     * Sets the root path for the workspace. If the root path is already set, this method does nothing.
     *
     * @param rootPath the root directory path to set for the workspace
     */
    public void setRootPath(Path rootPath) {
        if (this.rootPath == null) {
            this.rootPath = rootPath;
        }
    }

    /**
     * Retrieves the root directory path of the workspace.
     *
     * @return the root directory path as a {@code Path} object
     */
    public Path getRootPath() {
        return rootPath;
    }

    /**
     * Retrieves the path of the internal `.vctrl` directory within the workspace.
     *
     * @return the path of the `.vctrl` directory as a {@code Path} object
     */
    public Path getVctrlPath() {
        return vctrlPath;
    }

    /**
     * Retrieves a list of file paths contained within the workspace's root directory.
     * This method delegates the operation to {@link #listFiles(Path)}, passing {@code null}
     * as the parameter to default to the workspace's root path.
     *
     * @return a list of absolute file paths within the workspace's root directory, excluding
     * files located in the reserved root directory. Returns an empty list if the root
     * path is unset or if an I/O error occurs.
     */
    public List<Path> listFiles() {
        return listFiles(null);
    }

    /**
     * Retrieves a list of file paths contained within the workspace, excluding any files
     * located in directories matching the reserved name for the root directory.
     * The method traverses all files and directories in the workspace's root path
     * and captures their absolute paths.
     * <p>
     * If the root path of the workspace is not set, it logs a message and returns an empty list.
     * In case of an I/O error during the operation, it logs the error message and also returns an empty list.
     *
     * @return a list of absolute file paths within the workspace, excluding files inside
     * the reserved root directory. Returns an empty list if the root path is unset
     * or if an I/O error occurs.
     */
    public List<Path> listFiles(Path path) {
        if (path == null) {
            path = rootPath;
        }
        try (Stream<Path> stream = Files.list(path)) {

            return stream
                    .filter(p -> !p.equals(rootPath))
                    .filter(p -> !p.getFileName().toString().contains(File.separator + DirectoryNames.ROOT_DIR_NAME))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            System.out.println("Error reading files: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Retrieves a list of all file paths within the workspace's root directory, excluding any file
     * paths located under the reserved `.vctrl` directory. The method traverses the directory tree
     * starting from the root path and collects all regular file paths.
     * <p>
     * If the root path of the workspace is not set, the method returns an empty list. In the event
     * of an I/O error during the traversal, it logs the error message and also returns an empty list.
     *
     * @return a list of absolute file paths from the root directory of the workspace, excluding files
     * under the `.vctrl` directory. Returns an empty list if the root path is unset or if an
     * I/O error occurs.
     */
    public List<Path> listAllFiles(Path path) {
        if (rootPath == null) {
            return new ArrayList<>();
        }

        try (Stream<Path> stream = Files.walk(rootPath)) {
            return stream
                    .filter(p -> !p.equals(rootPath))
                    .filter(Files::isRegularFile)
                    .filter(p -> !p.startsWith(vctrlPath))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            System.out.println("Error reading files: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Lists the contents of a specified directory and returns the file statistics for each entry.
     * The method filters out the base directory itself and excludes files or directories
     * that are located within the `.vctrl` directory.
     * <p>
     * If the input path is not a valid directory or if an I/O error occurs during the listing,
     * an empty list is returned.
     *
     * @param basePath the path of the directory whose contents are to be listed
     * @return a list of {@code FileStat} objects containing file information for each entry in the directory,
     * or an empty list if the path is not a valid directory or if an I/O error occurs
     */
    public List<FileStat> listDir(Path basePath) {
        if (!Files.isDirectory(basePath)) return List.of();
        try (Stream<Path> stream = Files.list(basePath)) {
            return stream
                    .filter(p -> !p.equals(basePath))
                    .filter(p -> !p.startsWith(vctrlPath))
                    .map(this::toFileStat)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            System.out.println("Error reading files: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Reads all lines from a file specified by the given {@code Path}.
     * If the provided path is {@code null}, points to a directory, or does not exist,
     * this method returns an empty list.
     * If an I/O error occurs while reading the file, an empty list is returned.
     *
     * @param path the {@code Path} to the file from which to read lines
     * @return a {@code List} of {@code String} representing all lines in the file,
     * or an empty list if the file is invalid, does not exist,
     * is a directory, or if an error occurs during reading
     */
    public List<String> getFileLines(Path path) {
        if (path == null || !Files.exists(path) || Files.isDirectory(path)) {
            return Collections.emptyList();
        }

        try {
            return Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    /**
     * Applies the specified migration by executing a sequence of actions including
     * deleting files, removing directories, creating directories, and adding or
     * modifying files as defined in the migration object.
     * <p>
     * Key ideas:
     * 1. Deletes happen first
     * 2. Empty directories are removed
     * 3. Directories needed are created
     * 4. Files are updated
     * 5. New files are created
     *
     * @param migration the migration object that contains the details of the actions
     *                  to be performed, such as directories to be removed or created,
     *                  and file changes to be applied.
     * @throws IOException              if an I/O error occurs while performing the migration actions.
     * @throws NoSuchAlgorithmException if a required cryptographic algorithm is not available.
     */
    public void applyMigration(Migration migration) throws IOException, NoSuchAlgorithmException {
        try {
            applyChangeList(migration, MigrationActionType.DELETE);
            removeDirectories(migration.getRemoveDirs());
            createDirectories(migration.getMakeDirs());
            applyChangeList(migration, MigrationActionType.MODIFIED);
            applyChangeList(migration, MigrationActionType.ADD);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    /**
     * Applies a list of changes specified in the migration object to the workspace based on the given action type.
     * This involves creating, modifying, or deleting files as dictated by the migration changes.
     *
     * @param migration the migration object containing the changes to be applied
     * @param action    the type of migration action (e.g., CREATE, UPDATE, DELETE) that determines how changes are applied
     * @throws IOException              if an I/O error occurs during file operations
     * @throws NoSuchAlgorithmException if a required cryptographic algorithm is not available
     */
    private void applyChangeList(Migration migration, MigrationActionType action) throws IOException, NoSuchAlgorithmException {

        Database database = Database.getInstance();
        for (MigrationChange change : migration.getChanges().get(action.toString())) {
            Path path = rootPath.resolve(change.getPath());
            Files.deleteIfExists(path);
            if (action == MigrationActionType.DELETE) {
                continue;
            }
            DataEntry changeEntry = change.getPair().getNewEntry();
            Blob blob = (Blob) database.loadObject(changeEntry.getObjectID(), ObjectType.BLOB);
            Files.write(
                    path,
                    blob.getContent(),
                    WRITE, CREATE_NEW
            );

            applyMode(changeEntry, path);
        }
    }

    /**
     * Applies the POSIX file permissions of a {@code DataEntry} to a specified path if the file system
     * at the given path supports POSIX file attribute view. This method retrieves the permissions from
     * the entry and sets them on the specified path.
     *
     * @param entry the {@code DataEntry} object that contains the permissions to be applied
     * @param path  the {@code Path} where the permissions will be applied
     * @throws IOException if an I/O error occurs while setting the permissions
     */
    private void applyMode(DataEntry entry, Path path) throws IOException {
        if (Files.getFileStore(path).supportsFileAttributeView("posix")) {
            Set<PosixFilePermission> perms = entry.getMode().getPosixPermissions();
            Files.setPosixFilePermissions(path, perms);
        }
    }

    /**
     * Removes the specified directories from the file system. The removal process resolves
     * each directory against the root path of the workspace and attempts to delete the directories
     * in reverse order of their sorting. If any directory cannot be deleted due to being non-existent,
     * not a directory, or not empty, it is silently ignored. In case of an unexpected I/O error,
     * a runtime exception is thrown.
     *
     * @param dirs a set of {@code Path} objects representing the directories to be removed
     */
    private void removeDirectories(Set<Path> dirs) {
        dirs.stream()
                .sorted(Comparator.reverseOrder())
                .forEach(dir -> {
                    Path path = rootPath.resolve(dir);
                    try {
                        Files.delete(path);
                    } catch (NoSuchFileException | NotDirectoryException |
                             DirectoryNotEmptyException ignored) {
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    /**
     * Creates directories for the specified set of paths. If a path already exists
     * as a regular file, it is deleted before creating the directory. If a directory
     * already exists at the path, it is left unchanged.
     *
     * @param dirs a set of {@code Path} objects representing the directories to be created
     */
    private void createDirectories(Set<Path> dirs) {
        dirs.stream()
                .sorted()
                .forEach(dir -> {
                    Path path = rootPath.resolve(dir);
                    try {
                        if (Files.exists(path) && Files.isRegularFile(path)) {
                            Files.delete(path);
                        }
                        Files.createDirectory(path);
                    } catch (FileAlreadyExistsException ignored) {
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    /**
     * Converts the given {@code Path} to a {@code FileStat} instance, based on the
     * underlying operating system. If the operating system is Windows, it creates
     * a {@code WindowFileStat} instance. Otherwise, it creates a {@code UnixFileStat}
     * instance. In case of an I/O error, an unchecked exception is thrown.
     *
     * @param path the file system path to convert to a {@code FileStat} instance
     * @return a {@code FileStat} object corresponding to the given path
     * @throws UncheckedIOException if an I/O error occurs during the operation
     */
    public FileStat toFileStat(Path path) {
        try {
            return FileUtil.isWindows()
                    ? new WindowFileStat(path)
                    : new UnixFileStat(path);
        } catch (IOException e) {
            log.error("Failed to stat file: {}", path);
            return null;
        }
    }

    /**
     * Checks if a given path exists within a specified root directory,
     * validating if the path is deeply contained within the root.
     *
     * @param path the path to check for existence within the root directory
     * @return true if the path is deeply contained within the root directory, false otherwise or in case of an IOException
     */
    public boolean exists(Path path) {
        try {
            return FileUtil.isDeeplyContained(rootPath, path);
        } catch (IOException ex) {
            return false;
        }
    }

}
