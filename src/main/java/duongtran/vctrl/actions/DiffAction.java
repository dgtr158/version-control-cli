package duongtran.vctrl.actions;

import duongtran.vctrl.Workspace;
import duongtran.vctrl.diff.*;
import duongtran.vctrl.index.Index;
import duongtran.vctrl.index.IndexEntry;
import duongtran.vctrl.reportchanges.Status;
import duongtran.vctrl.reportchanges.StatusEntry;
import duongtran.vctrl.reportchanges.StatusType;
import duongtran.vctrl.storage.DataEntry;
import duongtran.vctrl.storage.Database;
import duongtran.vctrl.storage.ObjectID;

import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * This class is responsible for performing diff operations to identify differences
 * between different states of the repository, specifically targeting differences
 * between the index and the workspace, or between the index and the HEAD.
 * <p>
 * The class leverages the repository state, such as the index, workspace, and HEAD,
 * to compute a detailed set of differences (referred to as hunks) for each file.
 * <p>
 * The results of the diff operation are represented using {@code DiffResult} objects,
 * which encapsulate per-file diff details.
 */
public class DiffAction {

    // Keep up to 2 unchanged lines before, and up to 2 unchanged lines after
    public static final int DIFF_CONTEXT = 2;

    private final Workspace workspace;
    private final Database database;
    private final StatusAction statusAction;

    public DiffAction() {
        this.workspace = Workspace.getInstance();
        this.database = Database.getInstance();
        this.statusAction = new StatusAction();
    }

    /**
     * Executes a diff operation to detect differences between the index and the workspace,
     * or between the index and the HEAD, based on the provided cache parameter. The method
     * relies on the current workspace status and the loaded index to generate a {@code DiffResult}.
     *
     * @param isCached a boolean indicating whether to perform a diff between the index and HEAD
     *                 (if {@code true}), or between the index and the workspace (if {@code false}).
     * @return a {@code DiffResult} containing the differences detected between the compared entities.
     * @throws IOException              if an I/O error occurs while accessing the index or workspace.
     * @throws NoSuchAlgorithmException if the algorithm used for hashing file content during
     *                                  comparison is invalid.
     */
    public DiffResult execute(boolean isCached) throws IOException, NoSuchAlgorithmException {
        // Get the workspace's statuses
        Status status = statusAction.execute();

        // Load the index
        Index index = Index.loadFromDisk();
        if (index == null) {
            index = new Index();
        }

        if (isCached) {
            return spotIndexHeadDifferences(index, status);
        }
        return spotIndexWorkspaceDifferences(index, status);

    }

    /**
     * Identifies the differences between the index and HEAD by comparing the files
     * tracked in the index with the corresponding files in the HEAD. The method generates
     * a {@code DiffResult} that contains a mapping of file paths to their respective
     * differences (hunks).
     *
     * @param index  the {@code Index} representing the tracked file states.
     * @param status the {@code Status} containing information about the modified files in the index.
     * @return a {@code DiffResult} object containing the differences (hunks) between the files
     * in the index and the corresponding files in the HEAD.
     * @throws IOException              if an I/O error occurs while accessing file content during processing.
     * @throws NoSuchAlgorithmException if the hashing algorithm used for blob comparisons is invalid.
     */
    private DiffResult spotIndexHeadDifferences(Index index, Status status) throws IOException, NoSuchAlgorithmException {
        NavigableMap<Path, StatusEntry> indexModifiedMap = status.get(StatusType.INDEX_MODIFIED);
        Map<Path, IndexEntry> indexEntryMap = index.getEntryMap();
        Map<Path, DataEntry> headFiles = Database.listFileInHead();

        TreeMap<Path, List<Hunk>> hunkMap = new TreeMap<>();
        for (Map.Entry<Path, StatusEntry> indexModifiedMapEntry : indexModifiedMap.entrySet()) {
            Path path = indexModifiedMapEntry.getKey();
            // Read the index's entry lines
            IndexEntry indexEntry = indexEntryMap.get(path);
            if (indexEntry == null) continue;
            List<String> indexEntryLines = database.getBlobLines(new ObjectID(indexEntry.getOid()));

            // Read the head files lines
            DataEntry headFileData = headFiles.get(path);
            List<String> headFileDataLines = database.getBlobLines(headFileData.getObjectID());

            // Spot the differences
            List<EditScript> editScriptScripts = MyersDiff.diff(headFileDataLines, indexEntryLines);
            List<Hunk> hunks = HunkBuilder.build(editScriptScripts, DIFF_CONTEXT);
            hunkMap.put(path, hunks);

        }
        return new DiffResult(hunkMap);
    }

    /**
     * Identifies the differences between the index and the workspace by analyzing the modified files
     * in the workspace and comparing them against the corresponding entries in the index. The method
     * generates a {@code DiffResult} object containing a mapping of file paths to their differences (hunks).
     *
     * @param index  the {@code Index} representing the tracked file states.
     * @param status the {@code Status} containing the information about the modified files in the workspace.
     * @return a {@code DiffResult} object containing the differences (hunks) identified between the
     * workspace files and their corresponding entries in the index.
     */
    private DiffResult spotIndexWorkspaceDifferences(Index index, Status status) {
        NavigableMap<Path, StatusEntry> workspaceModifiedMap = status.get(StatusType.WORKSPACE_MODIFIED);
        Map<Path, IndexEntry> indexEntryMap = index.getEntryMap();

        TreeMap<Path, List<Hunk>> hunkMap = new TreeMap<>();
        for (Map.Entry<Path, StatusEntry> modifiedMapEntry : workspaceModifiedMap.entrySet()) {
            Path path = modifiedMapEntry.getKey();
            // Read the index's entry lines
            IndexEntry indexEntry = indexEntryMap.get(path);
            if (indexEntry == null) continue;
            List<String> indexEntryLines = database.getBlobLines(new ObjectID(indexEntry.getOid()));

            // Read the workspace lines
            List<String> workspaceFileLines = workspace.getFileLines(path);

            // Spot the differences
            List<EditScript> editScript = MyersDiff.diff(indexEntryLines, workspaceFileLines);
            List<Hunk> hunks = HunkBuilder.build(editScript, DIFF_CONTEXT);
            hunkMap.put(path, hunks);

        }
        return new DiffResult(hunkMap);
    }

}
