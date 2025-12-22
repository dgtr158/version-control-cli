package duongtran.vctrl.diff;

import java.util.List;
import java.util.Objects;

public final class Hunk {
    // First line number in OLD file that this hunk applies to
    private final int oldStart;
    // Number of lines from OLD file are involved
    private final int oldLength;
    // First line number in NEW file that this hunk applies to
    private final int newStart;
    // Number of lines from NEW file are involved
    private final int newLength;

    private final List<Edit> edits;

    public Hunk(int oldStart, int oldLength,
                int newStart, int newLength,
                List<Edit> edits) {
        this.oldStart = oldStart;
        this.oldLength = oldLength;
        this.newStart = newStart;
        this.newLength = newLength;
        this.edits = edits;
    }

    public int getOldStart() { return oldStart; }
    public int getOldLength() { return oldLength; }
    public int getNewStart() { return newStart; }
    public int getNewLength() { return newLength; }
    public List<Edit> getEdits() { return edits; }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Hunk hunk)) return false;
        return oldStart == hunk.oldStart && oldLength == hunk.oldLength && newStart == hunk.newStart && newLength == hunk.newLength && Objects.equals(edits, hunk.edits);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oldStart, oldLength, newStart, newLength, edits);
    }
}

