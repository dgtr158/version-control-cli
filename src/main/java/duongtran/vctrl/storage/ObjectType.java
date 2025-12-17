package duongtran.vctrl.storage;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum ObjectType {
    BLOB("blob"),
    TREE("tree"),
    COMMIT("commit"),
    TAG("tag");

    private final String type;
    private final int objectHeaderSize;

    ObjectType(String type) {
        this.type = type;
        this.objectHeaderSize = headerSize();
    }

    /**
     * A lookup map that maps the string representation of an {@code ObjectType}
     * to its corresponding enum value for efficient reverse lookup operations.
     *
     * This map is initialized using all the values of the {@code ObjectType} enum, where
     * the string representation of the type acts as the key and the enum instance as the value.
     *
     * It is used for converting string names of object types (e.g., "blob", "tree")
     * back into their respective {@code ObjectType} enum constants.
     */
    private static final Map<String, ObjectType> LOOKUP =
            Arrays.stream(values())
                    .collect(Collectors.toMap(
                            t -> t.type,
                            t -> t
                    ));

    /**
     * @return string representation of the object type.
     */
    @Override
    public String toString() {
        return type;
    }

    /**
     * Convert the object type into a byte array representation.
     * @return byte array of the object type.
     */
    public byte[] toBytes() {
        return type.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Retrieves the size of the object header in bytes.
     *
     * @return the size of the object header.
     */
    public int getObjectHeaderSize() {
        return objectHeaderSize;
    }


    /**
     * Converts a string representation of an object type to its corresponding {@code ObjectType} enum value.
     *
     * @param type the string representation of the object type. Must not be {@code null}.
     * @return the {@code ObjectType} corresponding to the provided string.
     * @throws IllegalArgumentException if the given type is {@code null} or does not match any known {@code ObjectType}.
     */
    public static ObjectType fromString(String type) {
        if (type == null) {
            throw new IllegalArgumentException("Type must not be null");
        }

        ObjectType result = LOOKUP.get(type);
        if (result == null) {
            throw new IllegalArgumentException("Unknown object type: " + type);
        }

        return result;
    }

    /**
     * Get size in byte of the object type
     * @return size of the object type.
     */
    public int size() {
        return this.toBytes().length;
    }

    /**
     * Calculates the total size of the header for the object type.
     * The total size is the sum of the size of the object type (in bytes)
     * and the fixed size defined in the {@code ObjectStorageHeader}.
     *
     * @return the total size of the header in bytes.
     */
    private int headerSize() {
        return size() + ObjectStorageHeader.FIXED_SIZE;
    }

}
