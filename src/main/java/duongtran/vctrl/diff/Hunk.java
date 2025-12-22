package duongtran.vctrl.diff;

import duongtran.vctrl.utils.Utils;

import java.util.List;
import java.util.Objects;

/**
 * Represents a single hunk in a diff operation, which is a contiguous block of changes
 * between the OLD file and the NEW file. Each hunk is defined by the starting line numbers
 * and lengths of the affected lines in both the OLD and NEW files. Additionally, each hunk
 * contains a list of edit scripts that specify the individual edit operations within the hunk.
 * <p>
 * Instances of this class are immutable.
 */
public final class Hunk {
    // First line number in OLD file that this hunk applies to
    private final int oldStart;
    // Number of lines from OLD file are involved
    private final int oldLength;
    // First line number in NEW file that this hunk applies to
    private final int newStart;
    // Number of lines from NEW file are involved
    private final int newLength;

    private final List<EditScript> editScripts;

    public Hunk(int oldStart, int oldLength,
                int newStart, int newLength,
                List<EditScript> editScripts) {
        this.oldStart = oldStart;
        this.oldLength = oldLength;
        this.newStart = newStart;
        this.newLength = newLength;
        this.editScripts = editScripts;
    }

    public int getOldStart() {
        return oldStart;
    }

    public int getOldLength() {
        return oldLength;
    }

    public int getNewStart() {
        return newStart;
    }

    public int getNewLength() {
        return newLength;
    }

    public List<EditScript> getEdits() {
        return editScripts;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Hunk hunk)) return false;
        if (!Utils.listsEqual(editScripts, hunk.editScripts)) return false;
        return oldStart == hunk.oldStart && oldLength == hunk.oldLength && newStart == hunk.newStart && newLength == hunk.newLength;
    }

    @Override
    public int hashCode() {
        return Objects.hash(oldStart, oldLength, newStart, newLength, editScripts);
    }
}

