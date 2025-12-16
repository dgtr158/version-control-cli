package duongtran.vctrl.storage.objects;

import duongtran.vctrl.storage.FileMode;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TreeEntry treeEntry)) return false;
        return Objects.equals(fileName, treeEntry.fileName) && Objects.equals(oid, treeEntry.oid) && mode == treeEntry.mode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileName, oid, mode);
    }
}
