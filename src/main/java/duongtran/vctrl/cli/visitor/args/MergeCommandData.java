package duongtran.vctrl.cli.visitor.args;

import java.util.ArrayList;
import java.util.List;

public class MergeCommandData {

    public List<String> sources = new ArrayList<>();
    public boolean noFastForward = false;
    public boolean squash = false;
    public String message = null;

}
