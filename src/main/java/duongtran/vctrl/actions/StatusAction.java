package duongtran.vctrl.actions;

import duongtran.vctrl.Workspace;
import duongtran.vctrl.index.FileStat;
import duongtran.vctrl.index.Index;
import duongtran.vctrl.index.IndexEntry;
import duongtran.vctrl.reportchanges.Status;
import duongtran.vctrl.reportchanges.StatusEntry;
import duongtran.vctrl.reportchanges.StatusType;
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

public class StatusAction {

    private static final Logger log = LoggerFactory.getLogger(StatusAction.class);

    private final Path rootPath;

    public StatusAction() {
        this.rootPath = Workspace.getInstance().getRootPath();

    }

    public Status execute() throws IOException, NoSuchAlgorithmException {
        Status status = new Status();

        // Load index from disk
        Index index = Index.loadFromDisk();
        if (index == null) index = new Index();

        // Scan the whole workspace and update untracked files
        scanWorkspace(status, rootPath, index);

        // Detect changes in the workspace
        detectWorkspaceChanges(status, index);

        return status;
    }

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

    private void detectWorkspaceChanges(Status status, Index index) throws IOException, NoSuchAlgorithmException {
        Map<Path, IndexEntry> entryMap = index.getEntryMap();
        Map<Path, FileStat> trackedFiles = status.getTrackedFiles();

        for (Map.Entry<Path, IndexEntry> e : entryMap.entrySet()) {
            FileStat trackedFile = trackedFiles.get(e.getKey());
            IndexEntry indexEntry = e.getValue();

            // Compare file size and file mode
            if (!indexEntry.statMatch(trackedFile)) {
                StatusEntry statusEntry = new StatusEntry(e.getKey(), StatusType.MODIFIED);
                status.addModifiedMap(statusEntry);
                status.addEntry(statusEntry);
                continue;
            }

            // Compare created time and modified time
            if (indexEntry.timeMatch(trackedFile)) {
                continue;
            }

            // Read the blob by the entry's path
            // Calculate the objectID then compare with the entry's objectID
            Blob blob = new Blob(Files.readAllBytes(Path.of(indexEntry.getPath())));
            blob.calculateOid(blob.toBytes());
            if (!Objects.equals(indexEntry.getOid(), blob.getOid().getValue())) {
                StatusEntry statusEntry = new StatusEntry(e.getKey(), StatusType.MODIFIED);
                status.addModifiedMap(statusEntry);
                status.addEntry(statusEntry);
            }

        }
    }

}
