package duongtran.vctrl.storage;

import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public enum FileMode {

    REGULAR_FILE("100644", 100644, EnumSet.of(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.GROUP_READ,
            PosixFilePermission.OTHERS_READ
    )),
    EXECUTABLE_FILE("100755", 100755, EnumSet.of(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE,
            PosixFilePermission.GROUP_READ,
            PosixFilePermission.GROUP_EXECUTE,
            PosixFilePermission.OTHERS_READ,
            PosixFilePermission.OTHERS_EXECUTE
    )),
    SYMBOLIC_LINK("120000", 120000, null),
    DIRECTORY("040000", 040000, EnumSet.of(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE,
            PosixFilePermission.GROUP_READ,
            PosixFilePermission.GROUP_EXECUTE,
            PosixFilePermission.OTHERS_READ,
            PosixFilePermission.OTHERS_EXECUTE
    )),
    GIT_LINK("160000", 160000, null);

    private final String value;
    private final int intValue;
    private final Set<PosixFilePermission> posixPermissions;

    FileMode(String value, int intValue, Set<PosixFilePermission> posixPermissions) {
        this.value = value;
        this.intValue = intValue;
        this.posixPermissions = posixPermissions;
    }

    /**
     * A lookup map that associates string representations of file modes with their corresponding
     * {@link FileMode} enum instances. The map is constructed by iterating through all available
     * enum values and mapping each file mode's string value to its associated enum constant.
     * <p>
     * This map allows for efficient retrieval of a {@link FileMode} instance based on its string
     * representation.
     */
    private static final Map<String, FileMode> LOOKUP =
            Arrays.stream(values())
                    .collect(Collectors.toMap(
                            t -> t.value,
                            t -> t
                    ));

    @Override
    public String toString() {
        return value;
    }

    public int getIntValue() {
        return intValue;
    }

    public Set<PosixFilePermission> getPosixPermissions() {
        return posixPermissions;
    }

    /**
     * Converts the string representation of the enum's file mode value to a byte array
     * encoded in UTF-8.
     *
     * @return a byte array representing the file mode value as a UTF-8 encoded string.
     */
    public byte[] toBytes() {
        return toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Retrieves the {@code FileMode} enum constant corresponding to the specified string representation
     * of a file mode. If the provided string does not match any existing values, an exception is thrown.
     *
     * @param mode the string representation of the file mode to be converted to a {@code FileMode} enum constant.
     *             Must not be {@code null}.
     * @return the {@code FileMode} enum constant corresponding to the provided string representation.
     * @throws IllegalArgumentException if {@code mode} is {@code null} or if it does not match any known file modes.
     */
    public static FileMode fromString(String mode) {
        if (mode == null) {
            throw new IllegalArgumentException("mode must not be null");
        }

        FileMode result = LOOKUP.get(mode);
        if (result == null) {
            throw new IllegalArgumentException("Unknown file mode: " + mode);
        }

        return result;
    }

}
