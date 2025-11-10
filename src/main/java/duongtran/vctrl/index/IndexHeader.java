package duongtran.vctrl.index;


import duongtran.vctrl.common.Buffer;

public class IndexHeader {

    public static final String HEADER_SIGNATURE = "DIRC";
    public static final int HEADER_SIZE = 12;

    private final int version;
    private final int entryCount;

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

    /**
     * Convert the index header into bytes and stream it into `dstBuf`
     * @param dstBuf the target buffer
     */
    public void toBytes(Buffer dstBuf) {
        dstBuf.put((byte) 'D')
                .put((byte) 'I')
                .put((byte) 'R')
                .put((byte) 'C')
                .putInt(version)
                .putInt(entryCount);
    }

}
