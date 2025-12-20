package duongtran.vctrl.storage;

import java.nio.file.Path;
import java.util.Objects;

public class DataEntry {

    private final FileMode mode;
    private final ObjectID objectID;
    private final Path path; // relative to the root path

    public DataEntry(FileMode mode, ObjectID objectID, Path path) {
        this.mode = mode;
        this.objectID = objectID;
        this.path = path;
    }

    public FileMode getMode() {
        return mode;
    }

    public ObjectID getObjectID() {
        return objectID;
    }

    public Path getPath() {
        return path;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DataEntry dataEntry)) return false;
        return mode == dataEntry.mode && Objects.equals(objectID, dataEntry.objectID) && Objects.equals(path, dataEntry.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, objectID, path);
    }

}
