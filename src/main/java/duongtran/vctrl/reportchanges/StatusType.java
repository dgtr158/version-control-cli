package duongtran.vctrl.reportchanges;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum StatusType {
    WORKSPACE_MODIFIED("workspace_modified"), // IN the workspace and IN the index, but the content is changed
    INDEX_MODIFIED("index_modified"), // IN the HEAD and IN the index, but the content is changed
    STAGED("staged"),
    UNTRACKED("untracked"), // IN the workspace but NOT IN the index
    WORKSPACE_DELETED("workspace_deleted"), // NOT IN the workspace but IN the index
    INDEX_DELETED("index_deleted"), // NOT IN the INDEX but IN the HEAD
    ADDED("added"), // IN the index but NOT IN HEAD
    ;

    private final String statusType;

    StatusType(String statusType) {
        this.statusType = statusType;
    }

    private static final Map<String, StatusType> LOOKUP =
            Arrays.stream(values())
                    .collect(Collectors.toMap(
                            t -> t.statusType,
                            t -> t
                    ));

    @Override
    public String toString() {
        return statusType;
    }

    public static StatusType fromString(String type) {
        if (type == null) {
            throw new IllegalArgumentException("Type must not be null");
        }

        StatusType result = LOOKUP.get(type);
        if (result == null) {
            throw new IllegalArgumentException("Unknown object type: " + type);
        }

        return result;
    }

}
