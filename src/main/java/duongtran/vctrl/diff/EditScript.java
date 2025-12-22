package duongtran.vctrl.diff;

import java.util.Objects;

/**
 * Represents an edit operation as part of a diff process. The edit operation may be
 * an insertion, deletion, or an equality. Each edit script associates with specific
 * line numbers and content from the original and/or new files.
 * <p>
 * Instances of this class are immutable and represent one discrete change or lack
 * thereof between two texts.
 */
public final class EditScript {
    private final EditType type;
    private final int oldLineNo; // -1 if not applicable
    private final int newLineNo; // -1 if not applicable
    private final String content;

    public EditScript(EditType type, int oldLineNo, int newLineNo, String content) {
        this.type = type;
        this.oldLineNo = oldLineNo;
        this.newLineNo = newLineNo;
        this.content = content;
    }

    public EditType getType() {
        return type;
    }

    public int getOldLineNo() {
        return oldLineNo;
    }

    public int getNewLineNo() {
        return newLineNo;
    }

    public String getContent() {
        return content;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof EditScript editScript)) return false;
        return oldLineNo == editScript.oldLineNo && newLineNo == editScript.newLineNo && type == editScript.type && Objects.equals(content, editScript.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, oldLineNo, newLineNo, content);
    }
}

