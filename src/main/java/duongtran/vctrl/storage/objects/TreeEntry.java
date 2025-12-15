package duongtran.vctrl.storage.objects;

import duongtran.vctrl.storage.FileMode;

public class TreeEntry {
    private final String fileName;
    private final String oid;
    private final FileMode mode;

    public TreeEntry(String fileName, String oid, FileMode mode) {
        this.fileName = fileName;
        this.oid = oid;
        this.mode = mode;
    }

    public String getFileName() {
        return fileName;
    }

    public String getOid() {
        return oid;
    }
    public FileMode getMode() {
        return mode;
    }
}
