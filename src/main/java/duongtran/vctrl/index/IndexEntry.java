package duongtran.vctrl.index;

import duongtran.vctrl.utils.Utils;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class IndexEntry {

    public static final int REGULAR_MODE = 0100644;
    public static final int EXECUTABLE_MODE = 0100755;
    public static final int MAX_PATH_SIZE = 0xfff;

    public static final int FIXED_SIZE_IN_BYTE = 62;
    public static final int MIN_PATH_SIZE = 2;
    public static final int CONSUME_BYTES_BLOCK = 8;

    private int size;
    private final int ctimeSeconds;
    private final int ctimeNanos;
    private final int mtimeSeconds;
    private final int mtimeNanos;
    private final int dev;
    private final int ino;
    private final int mode;
    private final int uid;
    private final int gid;
    private final int fileSize;
    private final String oid;
    private final int flags;
    private final String path;

    public IndexEntry(int ctimeSeconds, int ctimeNanos,
                 int mtimeSeconds, int mtimeNanos,
                 int dev, int ino, int mode,
                 int uid, int gid, int fileSize,
                 String oid, int flags, String path, int size) {
        this.ctimeSeconds = ctimeSeconds;
        this.ctimeNanos = ctimeNanos;
        this.mtimeSeconds = mtimeSeconds;
        this.mtimeNanos = mtimeNanos;
        this.dev = dev;
        this.ino = ino;
        this.mode = mode;
        this.uid = uid;
        this.gid = gid;
        this.fileSize = fileSize;
        this.oid = oid;
        this.flags = flags;
        this.path = path;
        this.size = size;
    }

    public int getSize() { return size; }
    public long getCtimeSeconds() { return ctimeSeconds; }
    public int getCtimeNanos() { return ctimeNanos; }
    public long getMtimeSeconds() { return mtimeSeconds; }
    public int getMtimeNanos() { return mtimeNanos; }
    public int getDev() { return dev; }
    public int getIno() { return ino; }
    public int getMode() { return mode; }
    public int getUid() { return uid; }
    public int getGid() { return gid; }
    public long getFileSize() { return fileSize; }
    public String getOid() { return oid; }
    public int getFlags() { return flags; }
    public String getPath() { return path; }


    /**
     * Convert the Index Entry into byte buffer.
     * Index Entry size in bytes is multiple of 8.
     *
     * @param buf The byte buffer to write into
     * @throws IllegalArgumentException If hashed object id is not in 20 bytes
     */
    public void toBytes(ByteBuffer buf) throws IllegalArgumentException {
        buf.putInt(this.ctimeSeconds);
        buf.putInt(this.ctimeNanos);
        buf.putInt(this.mtimeSeconds);
        buf.putInt(this.mtimeNanos);
        buf.putInt(this.dev);
        buf.putInt(this.ino);
        buf.putInt(this.mode);
        buf.putInt(this.uid);
        buf.putInt(this.gid);
        buf.putInt(this.fileSize);

        // Entry's ObjectID
        byte[] oidBytes = Utils.hexStringToByteArray(this.oid);
        if (oidBytes.length != 20) {
            throw new IllegalArgumentException("Index entry's oid is not 20 bytes in size");
        }
        buf.put(oidBytes);

        // Flags
        buf.putShort((short) (this.flags & 0xFFFF));

        // Path
        byte[] pathBytes = this.path.getBytes(StandardCharsets.UTF_8);
        buf.put(pathBytes);
        buf.put((byte) 0); // NULL terminator

        // Padding
        int totalSize = FIXED_SIZE_IN_BYTE + pathBytes.length + 1;
        int paddingSize = (8 - (totalSize % 8)) % 8;
        for (int i = 0; i < paddingSize; i++) {
            buf.put((byte) 0);
        }

    }

    /**
     * Create an index entry from byte buffer.
     *
     * @param buf The byte buffer to read from
     * @return an index entry object
     */
    public static IndexEntry fromBytes(ByteBuffer buf) {

        int ctimeSec = buf.getInt();
        int ctimeNanos = buf.getInt();
        int mtimeSec = buf.getInt();
        int mtimeNanos = buf.getInt();
        int dev = buf.getInt();
        int ino = buf.getInt();
        int mode = buf.getInt();
        int uid = buf.getInt();
        int gid = buf.getInt();
        int fileSize = buf.getInt();

        // Object ID
        byte[] oidBytes = new byte[20];
        buf.get(oidBytes);
        String oid = Utils.bytesToHex(oidBytes);

        // Flags
        int flags = buf.getShort() & 0xFFFF;

        // Path
        byte[] pathBytes = parseIndexPath(buf);
        String path = new String(pathBytes, StandardCharsets.UTF_8);

        // Entry size
        int size = FIXED_SIZE_IN_BYTE + pathBytes.length;

        return new IndexEntry(
                ctimeSec, ctimeNanos
                ,mtimeSec, mtimeNanos
                ,dev, ino, mode, uid, gid, fileSize
                ,oid, flags, path, size
        );

    }

    /**S
     * Calculate the size in bytes of the index entry.
     *
     * @return the size in bytes of the index entry.
     */
    public static int computeEntrySize(String path) {
        int pathSize = path.getBytes(java.nio.charset.StandardCharsets.UTF_8).length + 1; // include NULL terminator
        int totalSize = FIXED_SIZE_IN_BYTE + pathSize;
        int paddingSize = (CONSUME_BYTES_BLOCK - (totalSize % CONSUME_BYTES_BLOCK)) % CONSUME_BYTES_BLOCK;
        return totalSize + paddingSize;
    }

    private static byte[] parseIndexPath(ByteBuffer buf) {
        byte[] pathBytes = consume(buf, MIN_PATH_SIZE);
        while (pathBytes.length > 0 && pathBytes[pathBytes.length - 1] != 0x00) {
            byte[] block = consume(buf, CONSUME_BYTES_BLOCK);
            pathBytes = concat(pathBytes, block);
        }
        return pathBytes;
    }

    private static byte[] consume(ByteBuffer buf, int size) {
        byte[] out = new byte[size];
        buf.get(out);
        return out;
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        IndexEntry that = (IndexEntry) o;
        return ctimeSeconds == that.ctimeSeconds
                && ctimeNanos == that.ctimeNanos
                && mtimeSeconds == that.mtimeSeconds
                && mtimeNanos == that.mtimeNanos
                && dev == that.dev
                && ino == that.ino
                && mode == that.mode
                && uid == that.uid
                && gid == that.gid
                && fileSize == that.fileSize
                && flags == that.flags
                && Objects.equals(oid, that.oid)
                && Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ctimeSeconds, ctimeNanos, mtimeSeconds, mtimeNanos, dev, ino, mode, uid, gid, fileSize, oid, flags, path);
    }

}
