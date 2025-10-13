package duongtran.example.actions;

import duongtran.example.metadata.Workspace;
import duongtran.example.references.Refs;
import duongtran.example.storage.CommitAuthor;
import duongtran.example.storage.Database;
import duongtran.example.storage.objects.Blob;
import duongtran.example.storage.objects.Commit;
import duongtran.example.storage.objects.Entry;
import duongtran.example.storage.objects.Tree;
import duongtran.example.utils.Constants;
import duongtran.example.utils.DirectoryNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class CommitAction {

    private static final Logger logger = LoggerFactory.getLogger(CommitAction.class);

    private final Workspace workspace;
    private final Database database;

    public CommitAction() {
        this.workspace = Workspace.getInstance();
        this.database = Database.getInstance();
    }

    /**
     * Executes the commit operation for the workspace. This method commits all
     * files stored in the workspace by calling {@code storeWorkspaceFiles} and logs
     * the successfully committed files.
     * <p>
     * If an error occurs during the process, an {@link IOException} is thrown with
     * a descriptive message and the underlying exception as its cause.
     *
     * @throws IOException if there is an error during the commit process, such as
     *                     issues with reading files, hashing, or storing data.
     */
    public void execute() throws IOException {
        try {
            storeWorkspaceFiles();
            logger.info("Successfully committed files: {}", workspace.listFiles());
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new IOException("Failed to commit changes", e);
        }
    }

    /**
     * Stores all files from the workspace into the database. This method retrieves
     * the list of file paths from the workspace, reads the content of each file,
     * creates a {@link Blob} object for the file's data, and stores it in the database.
     * <p>
     * Files contained in the workspace are iterated, and only regular files
     * are processed. For each valid file, its content is read
     * as a byte array and encapsulated in a {@link Blob} object, which is then
     * stored in the database using the {@code Database.store} method.
     *
     * @throws IOException              if an error occurs while listing, reading, or storing files.
     * @throws NoSuchAlgorithmException if a required hashing algorithm is unavailable
     *                                  during the blob storage process.
     */
    private void storeWorkspaceFiles() throws IOException, NoSuchAlgorithmException {

        Tree tree = Tree.buildTree(workspace.getRootPath(), database);

        // Storing commit
        String authorName = System.getenv(Constants.ENV_AUTHOR_KEY);
        String authorEmail = System.getenv(Constants.ENV_EMAIL_KEY);
        CommitAuthor author = new CommitAuthor(authorName, authorEmail, Instant.now());

        Refs ref = new Refs();
        String parentId = ref.readHead();

        System.out.println("Enter the commit messages:");
        String message = getCommitMsg();

        Commit commit = new Commit(author, tree, message, parentId);
        database.store(commit);

        // Update HEAD
        ref.updateHead(commit.getOid());

        // Display the commit confirmation message
        String firstLine = getFirstLine(message);
        System.out.printf("[(root-commit) %s] %s\n", commit.getOid(), firstLine);

    }

    /**
     * Reads the commit message from standard input.
     *
     * @return the commit message read from standard input
     * @throws IOException if an error occurs while reading from standard input
     */
    private String getCommitMsg() throws IOException {
        StringBuilder message = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            while ((line = reader.readLine()) != null && !line.equalsIgnoreCase("end")) {
                message.append(line).append("\n");
            }
        }
        return message.toString();
    }

    private String getFirstLine(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "";
        }

        String[] lines = message.split("\\r?\\n" );
        return lines.length > 0 ? lines[0].trim() : "";
    }


}
