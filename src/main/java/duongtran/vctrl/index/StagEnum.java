package duongtran.vctrl.index;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum StagEnum {

    STAGE_NORMAL(0),
    TAGE_BASE(1),
    TAGE_OURS(2),
    TAGE_THEIRS(3);

    private final int value;

    StagEnum(int value) {
        this.value = value;
    }

    public int toValue() {
        return value;
    }

    private static final Map<Integer, StagEnum> LOOKUP =
            Arrays.stream(values())
                    .collect(Collectors.toMap(
                            t -> t.value,
                            t -> t
                    ));

    public StagEnum fromValue(Integer value) {
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }

        StagEnum result = LOOKUP.get(value);
        if (result == null) {
            throw new IllegalArgumentException("Unknown stag value: " + value);
        }

        return result;
    }

}
