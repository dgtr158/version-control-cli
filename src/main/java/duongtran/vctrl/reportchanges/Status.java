package duongtran.vctrl.reportchanges;

import duongtran.vctrl.index.FileStat;
import duongtran.vctrl.utils.Utils;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

/**
 * The Status class tracks the state of files in a workspace. This includes
 * categorization of files into tracked, untracked, modified, and deleted states.
 * It operates by storing file status information in separate maps based on the file's state.
 */
public class Status {

    private final EnumMap<StatusType, NavigableMap<Path, StatusEntry>> entries;

    // Paths are tracked
    private final TreeMap<Path, FileStat> trackedFiles;


    public Status() {
        this.entries = new EnumMap<>(StatusType.class);
        for (StatusType type: StatusType.values()) {
            entries.put(type, new TreeMap<>());
        }
        this.trackedFiles = new TreeMap<>();
    }

    public void add(StatusEntry entry) {
        entries.get(entry.getType()).put(entry.getPath(), entry);
    }

    public NavigableMap<Path, StatusEntry> get(StatusType type) {
        return entries.get(type);
    }

    public boolean isEmpty(StatusType type) {
        return entries.get(type).isEmpty();
    }

    public boolean isEmpty() {
        for (StatusType type : StatusType.values()) {
            if (!isEmpty(type)) return false;
        }

        return true;
    }

    public EnumMap<StatusType, NavigableMap<Path, StatusEntry>> getAll() {
        return entries;
    }

    public void addTrackedFiles(FileStat fileStat) {
        this.trackedFiles.put(fileStat.getPath(), fileStat);
    }

    public TreeMap<Path, FileStat> getTrackedFiles() {
        return trackedFiles;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Status other)) return false;

        EnumMap<StatusType, NavigableMap<Path, StatusEntry>> otherEntryMap = other.getAll();
        if (this.entries.size() != otherEntryMap.size()) return false;

        for (StatusType type: StatusType.values()) {
            if (!Utils.mapsEqual(this.entries.get(type), otherEntryMap.get(type))) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(entries);
    }

}
