package duongtran.vctrl.diff;

import java.util.Objects;

public final class DiffOp {

    public final EditType editType;
    public final String text;

    public DiffOp(EditType editType, String text) {
        this.editType = editType;
        this.text = text;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DiffOp diffOp)) return false;
        return editType == diffOp.editType && Objects.equals(text, diffOp.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(editType, text);
    }
}
