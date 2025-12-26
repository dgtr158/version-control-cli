package duongtran.vctrl.branches.migration;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents the types of migration actions that can be performed.
 * This enumeration provides constants for adding, modifying, and deleting actions,
 * along with utility methods for working with these types.
 */
public enum MigrationActionType {

    ADD("add"),
    MODIFIED("modified"),
    DELETE("delete");

    private final String type;

    MigrationActionType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }

    private static final Map<String, MigrationActionType> LOOKUP =
            Arrays.stream(values())
                    .collect(Collectors.toMap(
                            t -> t.type,
                            t -> t
                    ));

    public static MigrationActionType fromString(String type) {
        if (type == null) {
            throw new IllegalArgumentException("Type must not be null");
        }

        MigrationActionType result = LOOKUP.get(type);
        if (result == null) {
            throw new IllegalArgumentException("Unknown migration type: " + type);
        }

        return result;
    }

}
