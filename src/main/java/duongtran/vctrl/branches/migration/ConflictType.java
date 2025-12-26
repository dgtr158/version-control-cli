package duongtran.vctrl.branches.migration;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents different types of conflicts that can occur in a workspace environment,
 * typically when dealing with file or directory operations that may lead to overwriting
 * or removal of certain resources.
 */
public enum ConflictType {

    STALE_FILE("stale_file"), // File exists in the workspace but cannot be safely replaced
    STALE_DIRECTORY("stale_directory"), // Directory exists in the workspace but cannot be safely replaced
    UNTRACKED_OVERWRITTEN("untrack_overwritten"), // An untracked file would be overwritten
    UNTRACKED_REMOVED("untrack_removed"); // An untracked file or directory would be removed

    private final String type;

    ConflictType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }

    private static final Map<String, ConflictType> LOOKUP =
            Arrays.stream(values())
                    .collect(Collectors.toMap(
                            t -> t.type,
                            t -> t
                    ));

    public static ConflictType fromString(String type) {
        if (type == null) {
            throw new IllegalArgumentException("Type must not be null");
        }

        ConflictType result = LOOKUP.get(type);
        if (result == null) {
            throw new IllegalArgumentException("Unknown conflict type: " + type);
        }

        return result;
    }

}
