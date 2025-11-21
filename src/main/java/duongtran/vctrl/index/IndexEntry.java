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
                 String oid, int flags, String path) {
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
        this.size = getSizeInBytes();
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

        // Paths
        int start = buf.position();
        int end = start;
        while (buf.get(end) != 0) {
            end++;
        }
        byte[] pathBytes = new byte[end - start];
        buf.get(pathBytes);
        String path = new String(pathBytes, StandardCharsets.UTF_8);

        // Consume the NUll terminators
        end = buf.position();
        while (buf.hasRemaining() && buf.get(end) == 0) {
            buf.get();
            end++;
        }

        return new IndexEntry(
                ctimeSec, ctimeNanos
                ,mtimeSec, mtimeNanos
                ,dev, ino, mode, uid, gid, fileSize
                ,oid, flags, path
        );

    }

    private int getSizeInBytes() {
        int pathSize = path.getBytes(java.nio.charset.StandardCharsets.UTF_8).length + 1; // include NULL terminator
        int totalSize = FIXED_SIZE_IN_BYTE + pathSize;
        int paddingSize = (8 - (totalSize % 8)) % 8;
        return totalSize + paddingSize;
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
