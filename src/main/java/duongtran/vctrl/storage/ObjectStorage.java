package duongtran.vctrl.storage;

import duongtran.vctrl.utils.Utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Represent a common object in the version control system (BLOB, TREE, COMMIT, TAG)
 */
public abstract class ObjectStorage {
    public static final String HASH_ALGORITHM = "SHA-1";
    public static final int OID_SIZE = 20;

    private String oid; // object ID

    /**
     * Retrieves the Object ID (OID) of the current object.
     *
     * @return the OID of the object as a string.
     */
    public String getOid() {
        return oid;
    }

    /**
     * Calculates the object ID using SHA-1 hash.
     *
     * @throws NoSuchAlgorithmException If SHA-1 is not available
     */
    public void calculateOid(byte[] content) throws NoSuchAlgorithmException {
        if (this.oid != null) {
            return;
        }
        MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
        this.oid = Utils.bytesToHex(digest.digest(content));
    }

    /**
     * Converts the current object into its corresponding byte array representation,
     * including a header and content. The header contains metadata such as the object
     * type and content length, while the content represents the original data of the object.
     *
     * @return a byte array representation of the complete object, including its header and content.
     */
    public byte[] toBytes() {
        // Get original content of the object
        byte[] content = getContent();

        // Header of the object
        ObjectStorageHeader header = new ObjectStorageHeader(getType(), content.length);
        byte[] headerData = header.toBytes();

        // Create object content
        byte[] bytes = new byte[headerData.length + content.length];
        System.arraycopy(headerData, 0, bytes, 0, headerData.length);
        System.arraycopy(content, 0, bytes, headerData.length, content.length);

        return bytes;
    }


    /**
     * Retrieves the content of the object as a byte array.
     *
     * @return a byte array representing the content of the object.
     */
    protected abstract byte[] getContent();

    /**
     * Retrieves the type of the object as an {@code ObjectType}.
     *
     * @return the {@code ObjectType} representing the type of the object.
     */
    public abstract ObjectType getType();

}
