package duongtran.vctrl.storage;

import duongtran.vctrl.utils.Constants;

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

    public static final byte FIXED_SIZE = Integer.BYTES + 2;

    private final ObjectType type;
    private final int contentLength;

    public ObjectStorageHeader(ObjectType type, int contentLength) {
        this.type = type;
        this.contentLength = contentLength;
    }

    /**
     * Convert the object header into a byte array representation.
     * Format: objectType contentLength\0
     *
     * @return a byte array containing the object type and its content length.
     */
    public byte[] toBytes() {
        byte[] bytes = new byte[type.getObjectHeaderSize()];
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        buf.put(type.toBytes());
        buf.put(Constants.SPACE);
        buf.putInt(contentLength);
        buf.put(Constants.NULL_TERMINATOR);
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
        while (buf.get(end) != Constants.SPACE) end++;
        byte[] typeBytes = new byte[end - start];
        buf.get(typeBytes);
        ObjectType type = ObjectType.fromString(new String(typeBytes));

        // Consume space
        buf.get();

        // Size
        int size = buf.getInt();

        // Consume Null Terminator
        buf.get();

        return new ObjectStorageHeader(type, size);
    }

    public int getContentLength() {
        return contentLength;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ObjectStorageHeader that)) return false;
        return contentLength == that.contentLength && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, contentLength);
    }
}
