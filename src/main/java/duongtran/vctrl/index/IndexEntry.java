package duongtran.vctrl.index;

import java.nio.file.Path;

public class IndexEntry {

    public static final int REGULAR_MODE = 0100644;
    public static final int EXECUTABLE_MODE = 0100755;
    public static final int MAX_PATH_SIZE = 0xfff;

    public static final int FIXED_SIZE_IN_BYTE = 62;

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
    }

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

    public byte[] toBytes() {
        return null;
    }

    public void toBytes(byte[] dst) {

    }

    public int getSizeInBytes() {
        int pathSize = path.getBytes(java.nio.charset.StandardCharsets.UTF_8).length + 1; // include NULL terminator
        int totalSize = FIXED_SIZE_IN_BYTE + pathSize;
        int paddingSize = (8 - (totalSize % 8)) % 8;
        return totalSize + paddingSize;
    }

}
