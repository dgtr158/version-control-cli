package duongtran.vctrl.actions;

import duongtran.vctrl.branches.RevList;
import duongtran.vctrl.references.Refs;
import duongtran.vctrl.storage.objects.Commit;

import java.util.ArrayList;
import java.util.List;

public class LogAction {

    public void execute(List<String> branches) {

        Refs refs = new Refs();
        if (branches == null) {
            branches = new ArrayList<>();
            String currentBranchName = refs.getCurrentBranch();
            branches.add(currentBranchName);
        }

        RevList revList = new RevList(refs, branches);
        for (Commit commit : revList) {
            showCommit(commit);
        }
    }

    private void showCommit(Commit commit) {
        System.out.println(commit.getOid().abbreviate());
    }
}
