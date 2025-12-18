package duongtran.vctrl.reportchanges;

import duongtran.vctrl.index.FileStat;
import duongtran.vctrl.utils.Utils;

import java.nio.file.Path;
import java.util.Objects;
import java.util.TreeMap;

public class Status {

    private final TreeMap<Path, StatusEntry> entries;

    // Paths are tracked
    private final TreeMap<Path, FileStat> trackedFiles;
    // Paths are not in the index
    private final TreeMap<Path, StatusEntry> untrackedMap;
    // Paths where the workspace content differs from what's in the index
    private final TreeMap<Path, StatusEntry> modifiedMap;

    public Status() {
        this.entries = new TreeMap<>();
        this.trackedFiles = new TreeMap<>();
        this.untrackedMap = new TreeMap<>();
        this.modifiedMap = new TreeMap<>();
    }

    public void addEntry(StatusEntry entry) {
        this.entries.put(entry.getPath(), entry);
    }

    public TreeMap<Path, StatusEntry> getEntries() {
        return entries;
    }

    public void addUntrackedMapEntry(StatusEntry entry) {
        untrackedMap.put(entry.getPath(), entry);
    }

    public TreeMap<Path, StatusEntry> getUntrackedMap() {
        return untrackedMap;
    }

    public void addTrackedFiles(FileStat fileStat) {
        this.trackedFiles.put(fileStat.getPath(), fileStat);
    }

    public TreeMap<Path, FileStat> getTrackedFiles() {
        return trackedFiles;
    }

    public void addModifiedMap(StatusEntry entry) {
        modifiedMap.put(entry.getPath(), entry);
    }

    public TreeMap<Path, StatusEntry> getModifiedMap() {
        return modifiedMap;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Status other)) return false;
        if (this.entries.size() != other.getEntries().size()) return false;

        return Utils.mapsEqual(this.entries, other.getEntries());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(entries);
    }
}
