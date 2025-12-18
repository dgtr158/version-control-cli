package duongtran.vctrl.reportchanges;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum StatusType {
    MODIFIED("modified"),
    STAGED("staged"),
    UNTRACKED("untracked"),
    DELETED("deleted");

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
