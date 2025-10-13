package duongtran.example.actions;

import duongtran.example.metadata.Workspace;
import duongtran.example.storage.Database;
import duongtran.example.utils.DirectoryNames;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;

public class InitAction {
    public static void init(String pathName) {
        if (pathName == null) {
            pathName = DirectoryNames.WORKING_DIRECTORY;
        }

        // Initialize
        // TODO: uncomment after complete storing part
//        Workspace.initialize();
//        Database.initialize();

        // Define .vctrl directory
        File rootDir = new File(pathName, DirectoryNames.ROOT_DIR_NAME);

        // Define subdirectories inside .vctrl directory
        File objectsDir = new File(rootDir, DirectoryNames.OBJECTS);
        File refsDir = new File(rootDir, DirectoryNames.REFS);

        // Create directories
        if (rootDir.mkdirs() && objectsDir.mkdirs() && refsDir.mkdirs()) {
            System.out.println(
                    MessageFormat.format("Initialized empty {0} repository in {1}"
                            , DirectoryNames.PROJECT_NAME
                            , pathName)
            );
        } else {
            System.out.println(MessageFormat.format("{0} directory is already existed", DirectoryNames.ROOT_DIR_NAME));
        }

        // Set the root path of work space
        Path rootPath = Paths.get(pathName);
        Workspace.getInstance().setRootPath(rootPath);
    }
}
