package duongtran.vctrl.index;

import java.nio.file.Path;
import java.util.Objects;

public class IndexKey implements Comparable<IndexKey> {

    private final Path path;
    private final int stage;

    public IndexKey(Path path, int stage) {
        this.path = path;
        this.stage = stage;
    }

    public Path getPath() {
        return path;
    }

    public int getStage() {
        return stage;
    }

    @Override
    public int compareTo(IndexKey o) {
        int pathCompare = this.path.compareTo(o.path);
        if (pathCompare != 0) return pathCompare;
        return Integer.compare(stage, o.stage);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof IndexKey indexKey)) return false;
        return stage == indexKey.stage && Objects.equals(path, indexKey.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, stage);
    }

    @Override
    public String toString() {
        return "IndexKey{" +
                "path=" + path +
                ", stage=" + stage +
                '}';
    }
}
