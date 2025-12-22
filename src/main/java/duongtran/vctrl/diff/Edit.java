package duongtran.vctrl.diff;

import java.util.Objects;

public final class Edit {
    private final EditType type;
    private final int oldLineNo; // -1 if not applicable
    private final int newLineNo; // -1 if not applicable
    private final String content;

    public Edit(EditType type, int oldLineNo, int newLineNo, String content) {
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
        if (!(o instanceof Edit edit)) return false;
        return oldLineNo == edit.oldLineNo && newLineNo == edit.newLineNo && type == edit.type && Objects.equals(content, edit.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, oldLineNo, newLineNo, content);
    }
}

