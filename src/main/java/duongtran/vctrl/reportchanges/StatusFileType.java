package duongtran.vctrl.reportchanges;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum StatusFileType {
    ADD("added"),
    MODIFIED("modified"),
    DELETED("deleted"),
    UNTRACKED("untracked");

    private final String type;

    StatusFileType(String type) {
        this.type = type;
    }

    private static final Map<String, StatusFileType> LOOKUP =
            Arrays.stream(values())
                    .collect(Collectors.toMap(
                            t -> t.type,
                            t -> t
                    ));

    @Override
    public String toString() {
        return type;
    }

    public static StatusFileType fromString(String type) {
        if (type == null) {
            throw new IllegalArgumentException("Type must not be null");
        }

        StatusFileType result = LOOKUP.get(type);
        if (result == null) {
            throw new IllegalArgumentException("Unknown status file type: " + type);
        }

        return result;
    }


}
