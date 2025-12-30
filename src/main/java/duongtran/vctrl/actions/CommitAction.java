package duongtran.vctrl.actions;

import duongtran.vctrl.Workspace;
import duongtran.vctrl.index.Index;
import duongtran.vctrl.references.RefHead;
import duongtran.vctrl.references.Refs;
import duongtran.vctrl.storage.CommitAuthor;
import duongtran.vctrl.storage.Database;
import duongtran.vctrl.storage.ObjectID;
import duongtran.vctrl.storage.objects.Blob;
import duongtran.vctrl.storage.objects.Commit;
import duongtran.vctrl.storage.objects.Tree;
import duongtran.vctrl.utils.Constants;
import duongtran.vctrl.utils.DirectoryNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static duongtran.vctrl.utils.Constants.DEFAULT_AUTHOR;
import static duongtran.vctrl.utils.Constants.DEFAULT_EMAIL;
import static duongtran.vctrl.utils.Utils.getEnvOrDefault;

/**
 * The CommitAction class is responsible for managing the commit operation within the application.
 * It interacts with the workspace, indexes file changes, creates commit objects, and updates
 * the repository's state. This class uses internal services such as the {@link Workspace}
 * and {@link Database} to carry out these operations.
 */
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
    public Commit execute() throws IOException {
        try {
//        System.out.println("Enter the commit messages:");
//        String message = getCommitMsg();
            // TODO: get commit message from terminal
            String message = "Dummy commit message";
            return saveCommit(null, message);
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
    public Commit saveCommit(List<ObjectID> parentIds, String message) throws IOException, NoSuchAlgorithmException {

        Refs refs = new Refs();

        // 1. Build new trees from index's entries
        Index index = Index.loadFromDisk();
        if (index == null) {
            throw new IOException("The index is not exist");
        }
        Tree tree = Tree.buildTree(index.getEntryMap());

        // 2. Store the tree
        ObjectID treeObjectId = Tree.store(tree, database);

        // 3. Storing commit
        String authorName = getEnvOrDefault(Constants.ENV_AUTHOR_KEY, DEFAULT_AUTHOR);
        String authorEmail = getEnvOrDefault(Constants.ENV_EMAIL_KEY, DEFAULT_EMAIL);
        CommitAuthor author = new CommitAuthor(authorName, authorEmail, Instant.now());

        if (parentIds == null) {
            parentIds = new ArrayList<>();
            String headContent = refs.readHead();
            if (headContent != null) {
                parentIds.add(new ObjectID(headContent));
            }
        }

        Commit commit = new Commit(author, treeObjectId, message, parentIds);
        database.store(commit);

        // 4. Update HEAD
        updateHeadAfterCommit(refs, commit);

        // 5. Display the commit confirmation message
        String firstLine = getFirstLine(message);
        System.out.printf("[(root-commit) %s] %s\n", commit.getOid(), firstLine);

        return commit;

    }

    /**
     * Updates the HEAD reference and branch head values after a commit is made.
     * This method handles three scenarios:
     * 1. On the first commit, it initializes the HEAD and creates the default branch.
     * 2. When HEAD points to a branch, updates the branch's head value with the new commit's OID.
     * 3. If HEAD contains a detached OID, it updates it directly with the new OID.
     *
     * @param refs   the {@code Refs} object that manages repository references, such as HEAD
     *               and branch references
     * @param commit the {@code Commit} object containing information about the new commit,
     *               including its object ID (OID)
     * @throws IOException if there is an issue reading/writing to the file system or
     *                     repository structure during the update process
     */
    private void updateHeadAfterCommit(Refs refs, Commit commit) throws IOException {

        Path vctrlPath = Workspace.getInstance().getVctrlPath();
        RefHead refHead = refs.getRefHead();

        // If commit the first time, create a new branch with the default name
        String headContent = refs.readRawHeadContent();
        if (headContent == null) {
            // Initialize HEAD and refs/heads on first commit
            Files.createFile(vctrlPath.resolve(DirectoryNames.HEAD));
            Files.createDirectories(refHead.getReafHeadPath());

            Path defaultBranchPath = refHead.createBranch(DirectoryNames.DEFAULT_BRANCH_NAME);
            String relativeBranchRef = vctrlPath.relativize(defaultBranchPath).toString();

            Path resolvedBranchRefPath = vctrlPath.resolve(relativeBranchRef);
            refHead.updateBranchHeadValue(resolvedBranchRefPath, commit.getOid().getValue());

            refs.setHead(resolvedBranchRefPath.getFileName().toString(), commit.getOid());
            return;
        }

        // When HEAD already exists and points to a branch, update that branch's head value
        if (headContent.startsWith("ref: ")) {
            String branchRelRef = headContent.substring(5).trim();
            Path branchPath = vctrlPath.resolve(branchRelRef);
            refHead.updateBranchHeadValue(branchPath, commit.getOid().getValue());
            return;
        }

        // If HEAD contains a detached OID, keep it simple: fast-forward by writing the new OID directly.
        refs.setHead(commit.getOid().getValue());

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

    /**
     * Extracts the first line from the provided string. If the input string is null or empty,
     * an empty string is returned. Line breaks are determined using platform-independent
     * line separators.
     *
     * @param message the input string from which the first line is to be extracted;
     *                may contain multiple lines or may be null/empty
     * @return the first line of the input string, trimmed of leading and trailing whitespace;
     * if the input string is null or empty, returns an empty string
     */
    private String getFirstLine(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "";
        }

        String[] lines = message.split("\\r?\\n");
        return lines.length > 0 ? lines[0].trim() : "";
    }


}
