package duongtran.vctrl.index;


import java.nio.ByteBuffer;
import java.util.Objects;

public class IndexHeader {

    public static final String HEADER_SIGNATURE = "DIRC";
    public static final int HEADER_SIZE = 12;

    private final int version;
    private int entryCount;

    public IndexHeader(int version, int entryCount) {
        this.version = version;
        this.entryCount = entryCount;
    }

    public int getEntryCount() {
        return entryCount;
    }

    public int getVersion() {
        return version;
    }

    public void setEntryCount(int count) {
        this.entryCount = count;
    }

    public void incrementEntryCount() {
        this.entryCount++;
    }

    /**
     * Convert the index header into bytes and stream it into `buf`
     * @param buf the target buffer
     */
    public void toBytes(ByteBuffer buf) {
        buf.put((byte) 'D')
                .put((byte) 'I')
                .put((byte) 'R')
                .put((byte) 'C')
                .putInt(version)
                .putInt(entryCount);
    }

    public static IndexHeader fromBytes(ByteBuffer buf) {
        // Get the signature
        byte[] signatureBytes = new byte[4];
        buf.get(signatureBytes);
        String signature = new String(signatureBytes);

        if (!HEADER_SIGNATURE.contentEquals(signature)) {
            throw new IllegalArgumentException("Not the index file's header");
        }

        return new IndexHeader(buf.getInt(), buf.getInt());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        IndexHeader that = (IndexHeader) o;
        return version == that.version && entryCount == that.entryCount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, entryCount);
    }
}
