package duongtran.vctrl.storage;

import duongtran.vctrl.utils.Utils;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

public class ObjectID {

    public static final int SIZE_IN_BYTES = 20;

    private final String value;

    public ObjectID(String value) {
        this.value = value;
    }

    /**
     * Retrieves the string representation of the value for this ObjectID.
     *
     * @return the value associated with this ObjectID as a String.
     */
    public String getValue() {
        return this.value;
    }

    /**
     * Converts the hexadecimal string representation of this ObjectID into its byte array form
     * and writes the resulting bytes into the provided {@code ByteBuffer}.
     *
     * @param buf the {@code ByteBuffer} into which the byte array representation of this ObjectID will be written
     * @throws IllegalArgumentException if the internal hexadecimal string representation of this ObjectID is invalid
     */
    public void toBytes(ByteBuffer buf) {
        byte[] bytes = Utils.hexStringToByteArray(this.value);
        buf.put(bytes);
    }

    /**
     * Converts the hexadecimal string representation of this ObjectID into its byte array form.
     *
     * @return a byte array representing this ObjectID in its binary form, converted from its hexadecimal string value
     * @throws IllegalArgumentException if the internal hexadecimal string representation of this ObjectID is invalid
     */
    public byte[] toBytes() {
        return Utils.hexStringToByteArray(this.value);
    }

    public boolean isEmpty() {
        return value == null || value.isEmpty();
    }

    /**
     * Converts the current position and content of the provided {@code ByteBuffer}
     * into an {@code ObjectID} instance. The method reads a fixed number of bytes
     * from the buffer (defined by {@code SIZE_IN_BYTES}) and converts them into a
     * hexadecimal string representation used to create the {@code ObjectID}.
     *
     * @param buf the {@code ByteBuffer} from which the bytes for the {@code ObjectID}
     *            are read. The buffer must have at least {@code SIZE_IN_BYTES} bytes remaining.
     * @return a new {@code ObjectID} instance derived from the bytes read from the buffer.
     */
    public static ObjectID toObjectID(ByteBuffer buf) {
        byte[] bytes = new byte[SIZE_IN_BYTES];
        buf.get(bytes);
        return new ObjectID(Utils.bytesToHex(bytes));
    }

    /**
     * Converts the given byte array into an {@code ObjectID} by computing its SHA-1 hash
     * and encoding the resulting hash as a hexadecimal string.
     *
     * @param content the byte array content to be hashed and converted into an {@code ObjectID}
     * @return a new {@code ObjectID} instance representing the SHA-1 hash of the input content
     * @throws NoSuchAlgorithmException if the SHA-1 algorithm is not available
     */
    public static ObjectID toObjectID(byte[] content) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(ObjectStorage.HASH_ALGORITHM);
        return new ObjectID(Utils.bytesToHex(digest.digest(content)));
    }

    /**
     * Constructs an {@code ObjectID} instance from the given byte array by converting
     * the byte array into its hexadecimal string representation.
     *
     * @param bytes the byte array to convert into an {@code ObjectID}
     * @return an {@code ObjectID} instance created from the hexadecimal string representation
     *         of the provided byte array
     */
    public static ObjectID fromBytes(byte[] bytes) {
        return new ObjectID(Utils.bytesToHex(bytes));
    }

    /**
     * Generates a shortened or abbreviated version of the current ObjectID value.
     * Specifically, this method truncates the underlying string representation
     * of the ObjectID to the first 7 characters, typically used for compact display.
     *
     * @return a 7-character abbreviated string representation of this ObjectID
     * @throws IllegalArgumentException if the value is null or its length is less than 7
     */
    public String abbreviate() {
        if (this.value == null || this.value.length() < 7) {
            throw new IllegalArgumentException("Invalid SHA-1 value");
        }
        return this.value.substring(0, 7);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ObjectID objectID = (ObjectID) o;
        return Objects.equals(value, objectID.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
