package duongtran.vctrl.actions;

import duongtran.vctrl.index.Index;
import duongtran.vctrl.metadata.Workspace;
import duongtran.vctrl.storage.Database;
import duongtran.vctrl.storage.objects.Blob;
import duongtran.vctrl.utils.DirectoryNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

public class AddAction {

    private static final Logger log = LoggerFactory.getLogger(AddAction.class);

    private final Workspace workspace;
    private final Database database;

    public AddAction() {
        this.workspace = Workspace.getInstance();
        this.database = Database.getInstance();
    }

    public void execute(Path addPath) throws IOException {
        try {

            Index index;
            if (Files.exists(Workspace.getInstance().getRootPath().resolve(DirectoryNames.INDEX))) {
                index = Index.loadFromDisk();
            } else {
                index = new Index();
            }
            execute(addPath, index);
            index.write();

        } catch (IOException | NoSuchAlgorithmException e) {
            log.error("Failed to write index file: {}\n", e.getMessage());
            throw new IOException("Failed to add index");
        }
    }

    private void execute(Path addPath, Index index) throws IOException, NoSuchAlgorithmException {
        if (!Files.isDirectory(addPath)) {
            addFile(addPath, index);
        }
        // If addPath is a directory, list all files in the provided path
        // For each file, create a Blob object and store in DB
        for (Path path : workspace.listFiles(addPath)) {
            execute(path, index);
        }
    }

    private void addFile(Path path, Index index) throws IOException, NoSuchAlgorithmException {
        // Store files
        Blob blob = new Blob(Files.readAllBytes(path));
        String blobId = database.store(blob);

        // Create the index entries
        index.addEntry(path, blobId);
    }

}
