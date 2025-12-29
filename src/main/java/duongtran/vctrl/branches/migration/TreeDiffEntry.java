package duongtran.vctrl.branches.migration;

import duongtran.vctrl.storage.DataEntry;
import duongtran.vctrl.storage.objects.TreeEntry;

import java.util.Objects;

public final class TreeDiffEntry {

    private final DataEntry oldEntry;
    private final DataEntry newEntry;

    public TreeDiffEntry(DataEntry oldEntry, DataEntry newEntry) {
        this.oldEntry = oldEntry;
        this.newEntry = newEntry;
    }

    public DataEntry getOldEntry() { return oldEntry; }
    public DataEntry getNewEntry() { return newEntry; }

    public boolean isAdded()    { return oldEntry == null && newEntry != null; }
    public boolean isDeleted()  { return oldEntry != null && newEntry == null; }
    public boolean isModified() { return oldEntry != null && newEntry != null; }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TreeDiffEntry that)) return false;
        return Objects.equals(oldEntry, that.oldEntry) && Objects.equals(newEntry, that.newEntry);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oldEntry, newEntry);
    }
}
