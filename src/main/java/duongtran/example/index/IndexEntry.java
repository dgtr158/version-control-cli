package duongtran.example.index;

import java.nio.file.Path;

public class IndexEntry {

    public static final int REGULAR_MODE = 0100644;
    public static final int EXECUTABLE_MODE = 0100755;
    public static final int MAX_PATH_SIZE = 0xfff;

    private final long ctimeSeconds;
    private final int ctimeNanos;
    private final long mtimeSeconds;
    private final int mtimeNanos;
    private final int dev;
    private final int ino;
    private final int mode;
    private final int uid;
    private final int gid;
    private final long size;
    private final String oid;
    private final int flags;
    private final String path;

    public IndexEntry(long ctimeSeconds, int ctimeNanos,
                 long mtimeSeconds, int mtimeNanos,
                 int dev, int ino, int mode,
                 int uid, int gid, long size,
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
        this.size = size;
        this.oid = oid;
        this.flags = flags;
        this.path = path;
    }

    public static IndexEntry create(Path pathname, String oid, FileStat stat) {
        String path = pathname.toString();
        int mode = stat.isExecutable() ? EXECUTABLE_MODE : REGULAR_MODE;
        int flags = Math.min(path.getBytes().length, MAX_PATH_SIZE);

        return new IndexEntry(
                stat.getCtimeSeconds(), stat.getCtimeNanos(),
                stat.getMtimeSeconds(), stat.getMtimeNanos(),
                stat.getDev(), stat.getIno(), mode,
                stat.getUid(), stat.getGid(), stat.getSize(),
                oid, flags, path
        );
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
    public long getSize() { return size; }
    public String getOid() { return oid; }
    public int getFlags() { return flags; }
    public String getPath() { return path; }
}
