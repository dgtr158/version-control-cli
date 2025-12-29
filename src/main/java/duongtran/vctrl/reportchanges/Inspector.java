package duongtran.vctrl.reportchanges;

import duongtran.vctrl.Workspace;
import duongtran.vctrl.index.FileStat;
import duongtran.vctrl.index.Index;
import duongtran.vctrl.index.IndexEntry;
import duongtran.vctrl.storage.DataEntry;
import duongtran.vctrl.storage.Database;
import duongtran.vctrl.storage.ObjectID;
import duongtran.vctrl.storage.objects.Blob;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;


public class Inspector {

    /**
     * Compares the given index entry with the corresponding file's status in the workspace
     * and determines their synchronization state.
     *
     * @param indexEntry the {@code IndexEntry} representing the file details stored in the index
     * @param wsFileStat the {@code FileStat} representing the file's current metadata of the file in the workspace
     * @return a {@code WorkspaceComparison} enum value indicating the comparison result:
     * {@code UNTRACKED} if the file is not in the index,
     * {@code DELETED} if the file is missing in the workspace,
     * {@code MODIFIED} if the file has been changed,
     * or {@code CLEAN} if the file is unchanged.
     * @throws IOException              if an I/O error occurs while reading the file
     * @throws NoSuchAlgorithmException if the algorithm used to compute the file's hash is not available
     */
    public WorkspaceComparison compareIndexToWorkspace(IndexEntry indexEntry, FileStat wsFileStat)
            throws IOException, NoSuchAlgorithmException {

        if (indexEntry == null) return WorkspaceComparison.UNTRACKED;
        if (wsFileStat == null) return WorkspaceComparison.DELETED;

        if (!indexEntry.statMatch(wsFileStat)) {
            return WorkspaceComparison.MODIFIED;
        }

        if (indexEntry.timeMatch(wsFileStat)) {
            return WorkspaceComparison.CLEAN;
        }

        byte[] data = Files.readAllBytes(Path.of(indexEntry.getPath()));
        Blob blob = new Blob(data);
        blob.calculateOid(blob.toBytes());

        return Objects.equals(indexEntry.getOid(), blob.getOid().getValue())
                ? WorkspaceComparison.CLEAN
                : WorkspaceComparison.MODIFIED;
    }


    /**
     * Compares the given index entry to the corresponding head entry to determine their synchronization state.
     *
     * @param indexEntry the {@code IndexEntry} representing the file details stored in the index
     * @param headEntry  the {@code DataEntry} representing the file details stored in the head
     * @return a {@code HeadComparison} enum value indicating the comparison result:
     * {@code ADDED} if the entry exists in the index but not in the head,
     * {@code MODIFIED} if the entry exists in both but differs in content or mode,
     * or {@code CLEAN} if the entry exists in both and is identical.
     */
    public HeadComparison compareIndexToHead(
            IndexEntry indexEntry,
            DataEntry headEntry
    ) {
        if (headEntry == null) {
            return HeadComparison.ADDED;
        }

        if (indexEntry.getMode() != headEntry.getMode().getIntValue()
                || !Objects.equals(new ObjectID(indexEntry.getOid()), headEntry.getObjectID())
        ) {
            return HeadComparison.MODIFIED;
        }

        return HeadComparison.CLEAN;
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
    public boolean trackableFile(FileStat fileStat, Index index) {
        Path path = fileStat.getPath();
        if (!fileStat.isDirectory()) return !index.isTracked(fileStat.getPath());

        // Considers all files and directories inside the current directory
        // If there's any trackable file in those, the current directory is trackable
        List<FileStat> allFiles = Workspace.getInstance().listDir(path);
        for (FileStat sub : allFiles) {
            if (trackableFile(sub, index)) return true;
        }

        return false;
    }
}
