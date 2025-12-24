package duongtran.vctrl.storage.objects;

import duongtran.vctrl.storage.FileMode;
import duongtran.vctrl.storage.ObjectID;
import duongtran.vctrl.utils.Constants;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * Class represent an entry of a Tree object.
 * Format:
 *      <mode><space><filename><null_terminator><objectID>
 */
public class TreeEntry {

    private final String name;
    private final ObjectID oid;
    private final FileMode mode;

    public TreeEntry(String name, ObjectID oid, FileMode mode) {
        this.name = name;
        this.oid = oid;
        this.mode = mode;
    }

    /**
     * Retrieves the file name associated with this TreeEntry instance.
     *
     * @return the name of the file as a String.
     */
    public String getFileName() {
        return name;
    }

    /**
     * Retrieves the object ID associated with this TreeEntry instance.
     *
     * @return the {@code ObjectID} of this TreeEntry.
     */
    public ObjectID getOid() {
        return oid;
    }

    /**
     * Retrieves the file mode associated with this TreeEntry instance.
     *
     * @return the {@code FileMode} representing the mode of this entry.
     */
    public FileMode getMode() {
        return mode;
    }

    /**
     * Converts the current TreeEntry object into its byte array representation.
     * This representation includes the file mode, file name, and object ID in a specific format:
     * <mode><space><filename><null_terminator><objectID>.
     *
     * @return a byte array containing the serialized representation of the TreeEntry instance.
     */
    public byte[] toBytes() {
        byte[] bytes = new byte[sizeInBytes()];
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        byte[] modeBytes = mode.toString().getBytes(StandardCharsets.UTF_8);
        byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);

        buf.put(modeBytes);
        buf.put(Constants.SPACE);
        buf.put(nameBytes);
        buf.put(Constants.NULL_TERMINATOR);
        buf.put(oid.toBytes());

        return bytes;
    }

    public boolean isTree() {
        return mode.equals(FileMode.DIRECTORY);
    }

    /**
     * Converts the provided byte array into a {@code TreeEntry} object.
     * The byte array is parsed to extract the file mode, file name, and object ID,
     * which are used to construct the {@code TreeEntry} instance.
     *
     * @param buf the byte buffer containing a serialized {@code TreeEntry} object.
     *              It must follow the format: <mode><space><filename><null_terminator><objectID>.
     * @return a new {@code TreeEntry} object reconstructed from the provided byte array.
     * @throws NoSuchAlgorithmException if the hash algorithm required for creating the {@code ObjectID}
     *                                   is not available.
     */
    public static TreeEntry fromBytes(ByteBuffer buf) throws NoSuchAlgorithmException {
        // Mode
        int start = buf.position();
        int end = start;
        while (buf.get(end) != Constants.SPACE) end++;
        byte[] modeBytes = new byte[end - start];
        buf.get(modeBytes);
        FileMode mode = FileMode.fromString(new String(modeBytes));

        // Space
        buf.get();

        // Name
        start = buf.position();
        end = start;
        while (buf.get(end) != Constants.NULL_TERMINATOR) end++;
        byte[] nameBytes = new byte[end - start];
        buf.get(nameBytes);
        String name = new String(nameBytes);

        // Null Terminator
        buf.get();

        // Object ID
        byte[] objectIDBytes = new byte[ObjectID.SIZE_IN_BYTES];
        buf.get(objectIDBytes);
        ObjectID objectID = ObjectID.fromBytes(objectIDBytes);

        return new TreeEntry(name, objectID, mode);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TreeEntry treeEntry)) return false;
        return Objects.equals(name, treeEntry.name) && Objects.equals(oid, treeEntry.oid) && mode == treeEntry.mode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, oid, mode);
    }

    /**
     * Calculates the size in bytes for the current tree entry.
     *
     * @return the size of the tree entry in bytes as an integer.
     */
    private int sizeInBytes() {
        byte[] modeBytes = mode.toString().getBytes(StandardCharsets.UTF_8);
        byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);

        return modeBytes.length             // mode
                + Byte.BYTES                // space
                + nameBytes.length          // name
                + Byte.BYTES                // null terminator
                + ObjectID.SIZE_IN_BYTES;   // object id
    }

}
