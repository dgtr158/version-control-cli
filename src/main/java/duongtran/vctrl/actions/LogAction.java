package duongtran.vctrl.actions;

import duongtran.vctrl.branches.RevList;
import duongtran.vctrl.cli.visitor.args.LogCommandData;
import duongtran.vctrl.references.Refs;
import duongtran.vctrl.storage.objects.Commit;
import duongtran.vctrl.utils.AnsiColor;

import java.util.List;

public class LogAction {

    public void execute(LogCommandData cmdArgs) {

        // Get all branches
        BranchAction branchAction = new BranchAction();
        List<String> branches = branchAction.listBranches();

        // Get the revision list
        Refs refs = new Refs();
        RevList revList = new RevList(refs, branches);
        for (Commit commit : revList) {
            showCommit(commit, cmdArgs);
        }
    }

    private void showCommit(Commit commit, LogCommandData cmdArgs) {

        if (cmdArgs == null) {
            printDefaultCommit(commit);
            return;
        }

        if (cmdArgs.oneline) {
            printOneLine(commit);
        } else if (cmdArgs.format != null) {
            printFormatted(cmdArgs.format, commit);
        } else {
            System.out.print("fatal: unknow log command argument '%s'\n");
        }
    }

    private void printDefaultCommit(Commit commit) {
        System.out.println("commit " + AnsiColor.yellow(commit.getOid().getValue()));
        System.out.println(String.format("Author: %s <%s>", commit.getAuthor().getName(), commit.getAuthor().getEmail()));
        System.out.println(String.format("Date: %s", Commit.formatTime(commit.getAuthor().getTime())));
        System.out.println();
        for (String message : commit.getMessage().split("\n")) {
            System.out.println("    " + message);
        }
        System.out.println();
    }

    private void printOneLine(Commit commit) {
        StringBuilder sb = new StringBuilder();
        sb.append(AnsiColor.yellow(commit.getOid().abbreviate()));
        sb.append(" ");
        for (String message : commit.getMessage().split("\n")) {
            sb.append(message).append(" ");
        }

        System.out.println(sb);
    }

    private void printFormatted(String fmt, Commit commit) {
        String out = fmt
                .replace("%H", commit.getOid().getValue())
                .replace("%h", commit.getOid().abbreviate())
                .replace("%s", commit.getMessage())
                .replace("%an", commit.getAuthor().getName())
                .replace("%ad", Commit.formatTime(commit.getAuthor().getTime()));

        System.out.println(out);
    }


}
