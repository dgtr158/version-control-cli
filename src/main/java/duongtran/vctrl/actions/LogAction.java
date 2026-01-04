package duongtran.vctrl.actions;

import duongtran.vctrl.history.CommitHistory;
import duongtran.vctrl.storage.objects.Commit;

import java.util.Iterator;

public class LogAction {

    public void execute() {

        CommitHistory commitHistory = new CommitHistory();
        Iterator<Commit> commitIterator = commitHistory.iterator();
        while (commitIterator.hasNext()) {
            Commit commit = commitIterator.next();
            showCommit(commit);
        }
    }

    private void showCommit(Commit commit) {
        System.out.println(commit.getOid().abbreviate());
    }
}
