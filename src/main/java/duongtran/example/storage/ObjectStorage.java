package duongtran.example.storage;

import duongtran.example.utils.HexUtil;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public abstract class ObjectStorage {
    private static final String HASH_ALGORITHM = "SHA-1";

    private String oid; // object ID

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
        this.oid = HexUtil.bytesToHex(digest.digest(content));
    }

    protected byte[] formatContent() {
        // Get original content of the object
        byte[] data = toBytes();

        // Header of the object
        String header = String.format("%s %d\0", getType().toString(), data.length);
        byte[] headerData = header.getBytes(StandardCharsets.ISO_8859_1);

        // Create object content
        byte[] content = new byte[headerData.length + data.length];
        System.arraycopy(headerData, 0, content, 0, headerData.length);
        System.arraycopy(data, 0, content, headerData.length, data.length);

        return content;
    }

    protected abstract byte[] toBytes();

    public abstract ObjectType getType();
}
