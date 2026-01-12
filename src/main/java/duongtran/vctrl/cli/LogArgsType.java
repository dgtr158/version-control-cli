package duongtran.vctrl.cli;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum LogArgsType {

    ONELINE("--oneline"), PRETTY("--pretty"), FORMAT("--format");

    private final String type;

    LogArgsType(String type) {
        this.type = type;
    }

    private static final Map<String, LogArgsType> LOOKUP =
            Arrays.stream(values())
                    .collect(Collectors.toMap(
                            t -> t.type,
                            t -> t
                    ));

    @Override
    public String toString() {
        return type;
    }

    public static LogArgsType fromString(String type) {
        if (type == null) {
            throw new IllegalArgumentException("Log argument type must not be null");
        }

        LogArgsType result = LOOKUP.get(type);
        if (result == null) {
            throw new IllegalArgumentException("Unknown log argument type: " + type);
        }

        return result;
    }

}
