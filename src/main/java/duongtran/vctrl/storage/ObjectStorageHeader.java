package duongtran.vctrl.storage;

public class ObjectStorageHeader {

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
        // TODO
        return null;
    }

    /**
     * Convert a byte array representation into the header object
     *
     * @return the header object.
     */
    public static ObjectStorageHeader fromBytes(byte[] bytes) {
        // TODO
        return null;
    }

}
