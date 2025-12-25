package duongtran.vctrl.storage.objects;


import duongtran.vctrl.storage.ObjectStorage;
import duongtran.vctrl.storage.ObjectStorageHeader;
import duongtran.vctrl.storage.ObjectType;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

/**
 * Representation of a Blob object.
 */
public class Blob extends ObjectStorage {

    private final byte[] data;

    public Blob(byte[] data) {
        this.data = data;
    }

    /**
     * Converts the current object into its byte array representation.
     * The representation includes the object header and its content.
     *
     * @return a byte array containing the serialized representation of the object.
     */
    public byte[] toBytes() {
        return super.toBytes();
    }


    /**
     * Deserializes a byte array into a {@code Blob} object.
     *
     * @param bytes the byte array to deserialize. Must not be null and should conform to the
     *              expected serialized structure of a {@code Blob}.
     * @return a {@code Blob} instance created from the provided byte array.
     */
    public static Blob fromBytes(byte[] bytes) {
        ByteBuffer buf = ByteBuffer.wrap(bytes);

        // Header
        byte[] headerBytes = new byte[ObjectType.BLOB.getObjectHeaderSize()];
        buf.get(headerBytes);
        ObjectStorageHeader header = ObjectStorageHeader.fromBytes(headerBytes);

        // Content
        byte[] contentBytes = new byte[header.getContentLength()];
        buf.get(contentBytes);

        return new Blob(contentBytes);
    }

    /**
     * Retrieves the content associated with this object.
     *
     * @return a byte array representing the content of this instance.
     */
    @Override
    public byte[] getContent() {
        return data;
    }

    /**
     * Retrieves the type of the current object represented by this instance.
     *
     * @return the {@code ObjectType} associated with this instance, specifically {@code ObjectType.BLOB}.
     */
    @Override
    public ObjectType getType() {
        return ObjectType.BLOB;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Blob blob)) return false;
        return Objects.deepEquals(data, blob.data);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }

}
