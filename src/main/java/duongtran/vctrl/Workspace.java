package duongtran.vctrl;

import duongtran.vctrl.utils.DirectoryNames;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The Workspace class is a singleton responsible for managing the file paths
 * and directory structure within the application's root workspace.
 * It allows initializing the workspace with a given root directory and provides
 * utility methods to handle files and directories within the workspace.
 */
public class Workspace {
    private static Workspace instance;
    private Path rootPath;
    private Path vctrlPath;

    // Private constructor to prevent direct instantiation
    private Workspace() {}

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
     * Retrieves a list of file paths contained within the workspace, excluding any files
     * located in directories matching the reserved name for the root directory.
     * The method traverses all files and directories in the workspace's root path
     * and captures their absolute paths.
     *
     * If the root path of the workspace is not set, it logs a message and returns an empty list.
     * In case of an I/O error during the operation, it logs the error message and also returns an empty list.
     *
     * @return a list of absolute file paths within the workspace, excluding files inside
     *         the reserved root directory. Returns an empty list if the root path is unset
     *         or if an I/O error occurs.
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

}
