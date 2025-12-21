package duongtran.vctrl.actions;

import duongtran.vctrl.Workspace;
import duongtran.vctrl.index.FileStat;
import duongtran.vctrl.index.Index;
import duongtran.vctrl.index.IndexEntry;
import duongtran.vctrl.reportchanges.Status;
import duongtran.vctrl.reportchanges.StatusEntry;
import duongtran.vctrl.reportchanges.StatusType;
import duongtran.vctrl.storage.DataEntry;
import duongtran.vctrl.storage.Database;
import duongtran.vctrl.storage.ObjectID;
import duongtran.vctrl.storage.objects.Blob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The {@code StatusAction} class provides functionality to analyze the current state
 * of the workspace, detect changes (such as untracked files, modifications, and deletions),
 * and update the index accordingly. It is responsible for traversing the workspace,
 * determining the status of files, and synchronizing the changes with the index.
 */
public class StatusAction {

    private static final Logger log = LoggerFactory.getLogger(StatusAction.class);

    private final Path rootPath;

    public StatusAction() {
        this.rootPath = Workspace.getInstance().getRootPath();
    }

    /**
     * Executes the current status action by analyzing the state of the workspace,
     * detecting changes, and updating the index accordingly. This includes scanning
     * the workspace to identify untracked files, detecting changes in tracked files,
     * and writing any updates to the index if necessary.
     *
     * @return a {@code Status} object representing the current state of the workspace,
     * including tracked files, untracked files, modified files, and deleted files.
     * @throws IOException              if an I/O error occurs while accessing the workspace or index.
     * @throws NoSuchAlgorithmException if the algorithm used for hashing file content is invalid.
     */
    public Status execute() throws IOException, NoSuchAlgorithmException {
        Status status = new Status();

        // Load index from disk
        Index index = Index.loadFromDisk();
        if (index == null) index = new Index();

        // Scan the whole workspace and update untracked files
        scanWorkspace(status, rootPath, index);

        // Detect changes in the workspace
        detectWorkspaceChanges(status, index);

        // Flush index's changes
        if (index.isChanged()) {
            index.write();
        }

        return status;
    }

    /**
     * Scans the workspace directory recursively starting from the specified base path,
     * identifies tracked and untracked files, and updates the provided {@code Status} object
     * with the results.
     *
     * @param status   the {@code Status} object to be updated with information about tracked
     *                 and untracked files in the workspace
     * @param basePath the {@code Path} representing the root directory to begin the scan
     * @param index    the {@code Index} object used to determine which files are tracked
     */
    private void scanWorkspace(Status status, Path basePath, Index index) {
        List<FileStat> allFiles = Workspace.getInstance().listDir(basePath);
        for (FileStat fileStat : allFiles) {
            Path path = fileStat.getPath();
            if (index.isTracked(path)) {
                if (fileStat.isDirectory()) scanWorkspace(status, path, index);
                else status.addTrackedFiles(fileStat);
            } else if (isTrackableFile(fileStat, index)) {
                StatusEntry statusEntry = new StatusEntry(path, StatusType.UNTRACKED);
                status.addUntrackedMapEntry(statusEntry);
                status.addEntry(statusEntry);
            }
        }
    }

    /**
     * Determines if the given file or directory is trackable. A file is considered trackable
     * if it is not already tracked in the index and isn't a directory. If the file is a directory,
     * it is trackable if any file or directory within it is trackable.
     *
     * @param fileStat the {@code FileStat} object representing the file or directory whose
     *                 trackability needs to be determined
     * @param index    the {@code Index} object used to determine whether a file or directory
     *                 is already tracked
     * @return {@code true} if the file or directory is trackable, {@code false} otherwise
     */
    private boolean isTrackableFile(FileStat fileStat, Index index) {
        Path path = fileStat.getPath();
        if (!fileStat.isDirectory()) return !index.isTracked(fileStat.getPath());

        // Considers all files and directories inside the current directory
        // If there's any trackable file in those, the current directory is trackable
        List<FileStat> allFiles = Workspace.getInstance().listDir(path);
        for (FileStat sub : allFiles) {
            if (isTrackableFile(sub, index)) return true;
        }

        return false;
    }

    /**
     * Detects changes between the workspace, index, and HEAD states to identify modifications,
     * deletions, and other discrepancies. This method updates the provided {@code Status} object
     * with the detected changes and adjusts the index if necessary.
     *
     * @param status the {@code Status} object to be updated with the detected changes, including
     *               modifications, deletions, and other file state discrepancies
     * @param index  the {@code Index} object representing the current tracked state of the repository,
     *               used for comparison against the workspace and HEAD
     * @throws IOException              if an I/O error occurs while accessing the workspace or index
     * @throws NoSuchAlgorithmException if the algorithm used for hashing file content is invalid
     */
    private void detectWorkspaceChanges(Status status, Index index) throws IOException, NoSuchAlgorithmException {
        // Index's entries
        Map<Path, IndexEntry> entryMap = index.getEntryMap();
        // Workspace's files
        Map<Path, FileStat> trackedFiles = status.getTrackedFiles();
        // Head's files
        Map<Path, DataEntry> allHeadFiles = Database.listFileInHead();

        for (Map.Entry<Path, IndexEntry> e : entryMap.entrySet()) {
            // Index/Workspace differences
            checkIndexAgainstWorkspace(e, trackedFiles, status, index);

            // Index/HEAD differences
            checkIndexAgainstHead(e, allHeadFiles, status);
        }

        // Index/HEAD differences: deleted files (files in HEAD but not in index
        for (Map.Entry<Path, DataEntry> dataEntryMap : allHeadFiles.entrySet()) {
            if (!index.contains(dataEntryMap.getKey())) {
                StatusEntry statusEntry = new StatusEntry(dataEntryMap.getKey(), StatusType.INDEX_DELETED);
                status.addIndexDeletedMapEntry(statusEntry);
                status.addEntry(statusEntry);
            }
        }
    }

    /**
     * Compares an index entry against the current state of the workspace to detect changes.
     * Updates the provided {@code Status} object with modifications or deletions detected
     * in the workspace, and adjusts the index if necessary.
     *
     * @param entryMap     a map entry containing the path and corresponding index entry to be checked
     * @param trackedFiles a map of files in the workspace tracked by their paths, used for comparison
     * @param status       the {@code Status} object to update with changes detected in the workspace,
     *                     including modified or deleted files
     * @param index        the {@code Index} object representing the current tracked state of the repository,
     *                     which may be updated during the process
     * @throws IOException              if an I/O error occurs while reading files in the workspace or index
     * @throws NoSuchAlgorithmException if the algorithm used for hashing file content is invalid
     */
    private void checkIndexAgainstWorkspace(Map.Entry<Path, IndexEntry> entryMap, Map<Path, FileStat> trackedFiles, Status status, Index index) throws IOException, NoSuchAlgorithmException {

        FileStat trackedFile = trackedFiles.get(entryMap.getKey());
        IndexEntry indexEntry = entryMap.getValue();

        // Consider a file as deleted if it's in index and not in the tracked list
        if (trackedFile == null) {
            StatusEntry statusEntry = new StatusEntry(entryMap.getKey(), StatusType.WORKSPACE_DELETED);
            status.addWorkspaceDeletedMapEntry(statusEntry);
            status.addEntry(statusEntry);
            return;
        }

        // Compare file size and file mode
        if (!indexEntry.statMatch(trackedFile)) {
            StatusEntry statusEntry = new StatusEntry(entryMap.getKey(), StatusType.WORKSPACE_MODIFIED);
            status.addModifiedMap(statusEntry);
            status.addEntry(statusEntry);
            return;
        }

        // Compare created time and modified time
        if (indexEntry.timeMatch(trackedFile)) {
            return;
        }

        // Read the blob by the entry's path
        // Calculate the objectID then compare with the entry's objectID
        Blob blob = new Blob(Files.readAllBytes(Path.of(indexEntry.getPath())));
        blob.calculateOid(blob.toBytes());
        if (Objects.equals(indexEntry.getOid(), blob.getOid().getValue())) {
            indexEntry.updateStat(trackedFile);
            index.setChanged();
        } else {
            StatusEntry statusEntry = new StatusEntry(entryMap.getKey(), StatusType.WORKSPACE_MODIFIED);
            status.addModifiedMap(statusEntry);
            status.addEntry(statusEntry);
        }

    }

    /**
     * Compares entries in the index against the state of files in the HEAD commit to detect changes.
     * Identifies files that are added or modified in the index compared to the HEAD commit
     * and updates the provided status object with the results.
     *
     * @param entryMap     a map entry containing the path and corresponding index entry to be checked
     * @param allHeadFiles a map of all files from the HEAD commit, keyed by their paths
     * @param status       the status object to update with detected changes such as added or modified files
     * @throws IOException if an I/O error occurs during the comparison process
     */
    private void checkIndexAgainstHead(Map.Entry<Path, IndexEntry> entryMap, Map<Path, DataEntry> allHeadFiles, Status status) throws IOException {
        Path indexEntryPath = entryMap.getKey();
        IndexEntry indexEntry = entryMap.getValue();
        DataEntry matchedHeadFile = allHeadFiles.get(indexEntryPath);

        // Added files
        if (matchedHeadFile == null) {
            StatusEntry statusEntry = new StatusEntry(indexEntryPath, StatusType.ADDED);
            status.addAddedMap(statusEntry);
            status.addEntry(statusEntry);
            return;
        }

        // Modified files
        if (indexEntry.getMode() != matchedHeadFile.getMode().getIntValue()
                || !Objects.equals(new ObjectID(indexEntry.getOid()), matchedHeadFile.getObjectID())) {
            StatusEntry statusEntry = new StatusEntry(indexEntryPath, StatusType.INDEX_MODIFIED);
            status.addModifiedMap(statusEntry);
            status.addEntry(statusEntry);
            return;
        }

    }

}
