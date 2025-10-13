package duongtran.example.metadata;

import duongtran.example.utils.DirectoryNames;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Workspace {
    private static Workspace instance;
    private Path rootPath;

    // Private constructor to prevent direct instantiation
    private Workspace() {}

    // Method to initialize the Workspace
    public static void initialize() {
        getInstance().rootPath = Paths.get(DirectoryNames.WORKING_DIRECTORY);
    }


    // Get a single instance of Workspace
    public static Workspace getInstance() {
        if (instance == null) {
            instance = new Workspace();
        }
        return instance;
    }

    // Set the root path only once
    public void setRootPath(Path rootPath) {
        if (this.rootPath == null) {
            this.rootPath = rootPath;
        }
    }

    public Path getRootPath() {
        return rootPath;
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
    // List all files in the workspace
    public List<Path> listFiles() {
        if (rootPath == null) {
            System.out.println("Workspace root is not set.");
            return new ArrayList<>();
        }

        try (Stream<Path> stream = Files.list(rootPath)) {

            return stream
                    .filter(path -> !path.equals(rootPath))
                    .filter(path -> !path.getFileName().toString().contains(File.separator + DirectoryNames.ROOT_DIR_NAME))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            System.out.println("Error reading files: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Read the content of the file.
     *
     * @param path the file path
     * @return byte array of the file
     * @throws IOException - if cannot read the file
     */
    public byte[] readFile(String path) throws IOException {
        File file = new File(path);
        return Files.readAllBytes(file.toPath());
    }
}
