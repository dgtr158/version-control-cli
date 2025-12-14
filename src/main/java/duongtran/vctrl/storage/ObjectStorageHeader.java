package duongtran.vctrl.storage;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * A Header for object storage.
 * Format:
 *      ObjectType contentLength\0
 * Size:
 *      sizeOf(type) + Long. BYTES + 2 (Space + Null Terminator)
 */
public class ObjectStorageHeader implements Serializable {

    private static final byte SPACE = 0X20;
    private static final byte NULL_TERMINATOR = 0X00;

    private final ObjectType type;
    private final long size;

    public ObjectStorageHeader(ObjectType type, long size) {
        this.type = type;
        this.size = size;
    }

    /**
     * Convert the object header into a byte array representation.
     * Format: objectType contentLength\0
     *
     * @return a byte array containing the object type and its content length.
     */
    public byte[] toBytes() {
        byte[] bytes = new byte[size()];
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        buf.put(type.toBytes());
        buf.put(SPACE);
        buf.putLong(size);
        buf.put(NULL_TERMINATOR);
        return bytes;
    }

    /**
     * Convert a byte array representation into the header object
     *
     * @return the header object.
     */
    public static ObjectStorageHeader fromBytes(byte[] bytes) {
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        // Type
        int start = 0, end = 0;
        while (buf.get(end) != SPACE) end++;
        byte[] typeBytes = new byte[end - start];
        buf.get(typeBytes);
        ObjectType type = ObjectType.fromString(new String(typeBytes));

        // Consume space
        buf.get();

        // Size
        long size = buf.getLong();

        // Consume Null Terminator
        buf.get();

        return new ObjectStorageHeader(type, size);
    }

    public int size() {
        return type.size() + Long.BYTES + 2;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ObjectStorageHeader that)) return false;
        return size == that.size && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, size);
    }
}
