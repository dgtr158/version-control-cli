package duongtran.vctrl.diff;

import java.util.List;
import java.util.Objects;

public final class DiffResult {
    private final List<Hunk> hunks;

    public DiffResult(List<Hunk> hunks) {
        this.hunks = hunks;
    }

    public List<Hunk> getHunks() {
        return hunks;
    }

    public boolean isEmpty() {
        return hunks.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DiffResult that)) return false;
        return Objects.equals(hunks, that.hunks);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(hunks);
    }
}
