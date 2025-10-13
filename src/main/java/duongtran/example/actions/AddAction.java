package duongtran.example.actions;

import duongtran.example.index.IndexEntry;
import duongtran.example.metadata.Workspace;
import duongtran.example.storage.Database;
import duongtran.example.index.Index;
import duongtran.example.storage.objects.Blob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.ArrayList;

public class AddAction {

    private static final Logger logger = LoggerFactory.getLogger(AddAction.class);

    private final Workspace workspace;
    private final Database database;

    public AddAction() {
        this.workspace = Workspace.getInstance();
        this.database = Database.getInstance();
    }

    public void execute() throws IOException {
        try {
            // Init index with version 2
            // Create 12-bytes header
            Index index = new Index(2);

            // list all files in the working directory
            // for each file, create a Blob object and store
            List<Path> paths = workspace.listFiles();
            for (Path path : paths) {
                if (!Files.isDirectory(path)) {
                    // Store files
                    Blob blob = new Blob(Files.readAllBytes(path));
                    String blobId = database.store(blob);

                    // Create the index entries
                    index.addEntry(path, blobId);
                }
            }



        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new IOException("Failed to add index");
        }
    }

    private void storeChangedFiles() throws IOException, NoSuchAlgorithmException {

    }

}
