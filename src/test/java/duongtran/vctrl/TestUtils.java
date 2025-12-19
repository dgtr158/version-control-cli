package duongtran.vctrl;

import duongtran.vctrl.index.FileStat;
import duongtran.vctrl.index.UnixFileStat;
import duongtran.vctrl.index.WindowFileStat;
import duongtran.vctrl.storage.Database;
import duongtran.vctrl.storage.FileMode;
import duongtran.vctrl.utils.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Instant;
import java.util.Set;

public class TestUtils {

    private static final Logger log = LoggerFactory.getLogger(TestUtils.class);

    public static final String TEST_ROOT_PATH = System.getProperty("user.dir");

    /**
     * Workspace structure:
     * <pwd>/workspace/
     * firstDir/
     * foo.txt -> "Foo in the first directory"
     * bar.txt -> "Bar in the first directory"
     * secondDir/
     * bas.txt -> "Bas in the second directory"
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

    /**
     * Writes the specified text content to a file at the given {@code Path}.
     * If the file does not exist, it will be created. If the file already exists,
     * its existing content will be overwritten.
     * <p>
     * This method uses UTF-8 encoding and truncates the file before writing
     * the new content.
     *
     * @param file    the {@code Path} of the file where the text content should be written.
     *                Must not be {@code null}.
     * @param content the {@code String} content to write into the file.
     *                Must not be {@code null}.
     * @throws IOException if an I/O error occurs while writing to the file.
     */
    public static void writeText(Path file, String content) throws IOException {
        Files.writeString(file, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Deletes a directory or file at the specified {@code Path} recursively.
     * If the specified path is a directory, its contents will also be deleted,
     * ensuring the directory is removed entirely. Files are deleted as they
     * are encountered.
     * <p>
     * This method first traverses the directory tree in reverse order (from leaves
     * to root) to safely delete all files and directories without leaving any
     * residual files or subdirectories. If the path does not exist, the method
     * exits without performing any action.
     *
     * @param path the {@code Path} to the file or directory to delete. Must not
     *             be {@code null}. If the path points to a directory, all its
     *             contents will be deleted recursively.
     * @throws IOException      if an I/O error occurs during the deletion process.
     * @throws RuntimeException if an error occurs while attempting to delete a
     *                          specific file or directory.
     */
    public static void deleteRecursively(Path path) throws IOException {
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

    /**
     * Changes the mode of the given file or directory to the specified {@link FileMode}.
     * The method ensures that the provided {@code Path} matches the given {@code FileMode},
     * and if applicable, modifies the execution permissions based on the target file mode.
     * <p>
     * This method is compatible with both Unix and non-Unix systems. On Unix systems,
     * it explicitly sets the POSIX file permissions, while on other systems it relies
     * on Java's file API to manage executability.
     *
     * @param path the {@code Path} to the file or directory whose mode needs to be changed;
     *             must not be {@code null}.
     * @param mode the {@link FileMode} to which the file or directory should be set;
     *             must not be {@code null}.
     * @throws IOException              if an I/O error occurs while modifying file permissions.
     * @throws IllegalArgumentException if the specified path does not match
     *                                  the expected type for the given mode.
     */
    public static void changeMode(Path path, FileMode mode) throws IOException {
        if (mode == FileMode.GIT_LINK) {
            return;
        }

        if (mode == FileMode.DIRECTORY) {
            if (!Files.isDirectory(path)) {
                throw new IllegalArgumentException("Not a directory: " + path);
            }
            return;
        }

        if (mode == FileMode.SYMBOLIC_LINK) {
            if (!Files.isSymbolicLink(path)) {
                throw new IllegalArgumentException("Not a symlink: " + path);
            }
            return;
        }

        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("Not a regular file: " + path);
        }

        boolean isExecutable = (mode == FileMode.EXECUTABLE_FILE);
        if (FileUtil.isUnix()) {
            Set<PosixFilePermission> perms =
                    Files.getPosixFilePermissions(path);

            if (isExecutable) {
                perms.add(PosixFilePermission.OWNER_EXECUTE);
                perms.add(PosixFilePermission.GROUP_EXECUTE);
                perms.add(PosixFilePermission.OTHERS_EXECUTE);
            } else {
                perms.remove(PosixFilePermission.OWNER_EXECUTE);
                perms.remove(PosixFilePermission.GROUP_EXECUTE);
                perms.remove(PosixFilePermission.OTHERS_EXECUTE);
            }

            Files.setPosixFilePermissions(path, perms);
        } else {
            path.toFile().setExecutable(isExecutable);
        }
    }

    /**
     * Sets the last modified time of the specified file or directory to the given {@code Instant}.
     *
     * @param path    the {@code Path} of the file or directory whose last modified time is to be updated.
     *                Must not be {@code null}.
     * @param instant the {@code Instant} representing the new last modified time.
     *                Must not be {@code null}.
     * @throws IOException if an I/O error occurs while setting the last modified time.
     */
    public static void setMTime(Path path, Instant instant) throws IOException {
        Files.setLastModifiedTime(path, FileTime.from(instant));
    }

    /**
     * Returns the file status information for the specified file or directory at the given {@code Path}.
     * Depending on the operating system, the method returns an instance of {@code WindowFileStat} or {@code UnixFileStat}.
     *
     * @param path the {@code Path} of the file or directory whose status information is to be retrieved.
     *             Must not be {@code null}.
     * @return a {@code FileStat} instance containing metadata about the specified file or directory.
     * @throws IOException if an I/O error occurs while retrieving the file or directory status.
     */
    public static FileStat getFileStat(Path path) throws IOException {
        return FileUtil.isWindows() ? new WindowFileStat(path) : new UnixFileStat(path);
    }

}
