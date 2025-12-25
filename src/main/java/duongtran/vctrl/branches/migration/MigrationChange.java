package duongtran.vctrl.branches.migration;

import java.nio.file.Path;

/**
 * Represents a migration change consisting of a file system path and a pair of tree changes.
 * <p>
 * This class is immutable and final. It encapsulates a `Path` that indicates the location of the change
 * and a `TreeChanges` object that describes the nature of the change by comparing the old and new state
 * of a tree entry.
 */
public final class MigrationChange {
    private final Path path;
    private final TreeChanges pair;

    public MigrationChange(Path path, TreeChanges pair) {
        this.path = path;
        this.pair = pair;
    }

    public Path getPath() {
        return path;
    }

    public TreeChanges getPair() {
        return pair;
    }
}
