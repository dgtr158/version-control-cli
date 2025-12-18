package duongtran.vctrl.reportchanges;

import java.nio.file.Path;
import java.util.Objects;

public class StatusEntry {

    private final Path path;
    private final StatusType type;

    public StatusEntry(Path path, StatusType type) {
        this.path = path;
        this.type = type;
    }

    public Path getPath() {
        return path;
    }

    public StatusType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof StatusEntry that)) return false;
        return Objects.equals(path, that.path) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, type);
    }
}
