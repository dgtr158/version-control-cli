package duongtran.vctrl.storage;

import java.nio.file.Path;

public class DataEntry {

    private final FileMode mode;
    private final ObjectID objectID;
    private final Path path; // relative to the root path

    public DataEntry(FileMode mode, ObjectID objectID, Path path) {
        this.mode = mode;
        this.objectID = objectID;
        this.path = path;
    }
}
