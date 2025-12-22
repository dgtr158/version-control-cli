package duongtran.vctrl.diff;

import duongtran.vctrl.utils.Utils;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Represents the result of a diff operation between two sets of files or data.
 * The result is organized as a mapping of file paths to corresponding hunks
 * that describe the differences between the old and new versions.
 * <p>
 * This class is immutable and thread-safe.
 */
public final class DiffResult {
    private final TreeMap<Path, List<Hunk>> hunkMap;

    public DiffResult(TreeMap<Path, List<Hunk>> hunkMap) {
        this.hunkMap = hunkMap;
    }

    public TreeMap<Path, List<Hunk>> getHunks() {
        return hunkMap;
    }

    public boolean isEmpty() {
        return hunkMap.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DiffResult that)) return false;

        if (this.hunkMap.size() != that.hunkMap.size()) {
            return false;
        }

        for (Map.Entry<Path, List<Hunk>> entry : this.hunkMap.entrySet()) {
            Path path = entry.getKey();
            List<Hunk> hunks1 = entry.getValue();
            List<Hunk> hunks2 = that.hunkMap.get(path);

            if (hunks2 == null) {
                return false;
            }

            if (!Utils.listsEqual(hunks1, hunks2)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(hunkMap);
    }

}
