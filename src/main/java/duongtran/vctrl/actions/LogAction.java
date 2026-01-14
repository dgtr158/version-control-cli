package duongtran.vctrl.actions;

import duongtran.vctrl.branches.RevList;
import duongtran.vctrl.cli.visitor.args.LogCommandData;
import duongtran.vctrl.references.RefHead;
import duongtran.vctrl.references.Refs;
import duongtran.vctrl.storage.ObjectID;
import duongtran.vctrl.storage.objects.Commit;
import duongtran.vctrl.utils.AnsiColor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The LogAction class provides functionality to execute a log operation,
 * which displays commit information from the repository. The display can
 * be customized through options to show one-line summaries, formatted output,
 * or default rich details including commit hashes, branch associations,
 * authorship details, and commit messages.
 */
public class LogAction {

    private final Refs refs;
    private final Map<ObjectID, List<String>> commitIdTable;
    private final BranchAction branchAction;
    private final String currentBranch;

    public LogAction() {
        this.refs = new Refs();
        this.commitIdTable = new HashMap<>();
        this.branchAction = new BranchAction();
        RefHead refHead = this.refs.getRefHead();

        // Build the commitId table
        for (String branch : this.branchAction.listBranches()) {
            ObjectID commitId = new ObjectID(refHead.getBranchHeadContent(branch));
            if (commitIdTable.containsKey(commitId)) {
                commitIdTable.get(commitId).add(branch);
            } else {
                commitIdTable.put(commitId, new ArrayList<>(List.of(branch)));
            }
        }

        // Get HEAD
        this.currentBranch = this.refs.getCurrentBranch();

    }

    /**
     * Executes the log action based on the provided command data. This method retrieves
     * the list of branches to be logged, either from the provided command arguments or
     * by listing all available branches if none are specified. It then processes each
     * commit in the retrieved revisions and displays them according to the configuration
     * specified in the command data.
     *
     * @param cmdArgs an instance of {@link LogCommandData} containing the command arguments.
     *                This includes options for formatting and the list of branches to consider.
     *                If null or if the branches list within the object is null/empty, all available
     *                branches will be processed.
     */
    public void execute(LogCommandData cmdArgs) {

        // Get log branches
        List<String> branches;
        if (cmdArgs != null && cmdArgs.revSpec.include != null && !cmdArgs.revSpec.include.isEmpty()) {
            branches = cmdArgs.revSpec.include;
        } else {
            branches = this.branchAction.listBranches();
        }

        // Get the revision list
        Refs refs = new Refs();
        RevList revList = new RevList(refs, branches);
        for (Commit commit : revList) {
            showCommit(commit, cmdArgs);
        }
    }

    /**
     * Displays the information of a given commit based on the specified command arguments.
     * Depending on the provided {@code LogCommandData}, the commit information can be shown
     * in different formats such as one-line, custom format, or the default format.
     *
     * @param commit the {@link Commit} object whose details need to be displayed; must not be null.
     * @param cmdArgs an instance of {@link LogCommandData} containing options on how to display
     *                the commit information. This includes flags for one-line display and custom
     *                formatting. If null, the default format will be used to display the commit.
     */
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
            printDefaultCommit(commit);
        }
    }

    /**
     * Displays the details of a given commit in the default format. This includes
     * the commit's hash, associated branches, author details, commit date, and
     * its message. The output is formatted and printed directly to the console.
     *
     * @param commit the {@link Commit} object containing the commit details to display; must not be null.
     */
    private void printDefaultCommit(Commit commit) {
        StringBuilder sb = new StringBuilder();
        // Commit content with the attached branch
        sb.append("commit ").append(AnsiColor.yellow(commit.getOid().getValue())).append(" ");
        sb.append(getBranchDecorator(commit));
        sb.append("\n");
        sb.append(String.format("Author: %s <%s>\n", commit.getAuthor().getName(), commit.getAuthor().getEmail()));
        sb.append(String.format("Date: %s\n", Commit.formatTime(commit.getAuthor().getTime())));
        sb.append("\n");

        // Commit messages
        for (String message : commit.getMessage().split("\n")) {
            sb.append("\t").append(message);
        }
        sb.append("\n");

        System.out.println(sb);
    }

    /**
     * Displays the commit information in a one-line format. The one-line format includes
     * the abbreviated hash of the commit, associated branch decorations, and the commit message.
     *
     * @param commit the {@link Commit} object representing the commit whose information is
     *               to be displayed; must not be null.
     */
    private void printOneLine(Commit commit) {
        StringBuilder sb = new StringBuilder();
        sb.append(AnsiColor.yellow(commit.getOid().abbreviate()));
        sb.append(" ");

        String branchDecorator = getBranchDecorator(commit);
        sb.append(branchDecorator);
        if (!branchDecorator.isEmpty()) {
            sb.append(" ");
        }

        for (String message : commit.getMessage().split("\n")) {
            sb.append(message).append(" ");
        }

        System.out.println(sb);
    }

    /**
     * Formats the output for a given {@link Commit} object based on a specified format string
     * and prints the formatted result to the standard output. The format string can contain
     * placeholders (e.g., %H, %h, %s, %an, %ad) that will be replaced with the respective
     * commit details.
     *
     * @param fmt the format string specifying how the commit details should be displayed,
     *            with placeholders for customization; must not be null.
     * @param commit the {@link Commit} object containing the details to format; must not be null.
     */
    private void printFormatted(String fmt, Commit commit) {
        String out = fmt
                .replace("%H", commit.getOid().getValue())
                .replace("%h", commit.getOid().abbreviate())
                .replace("%s", commit.getMessage())
                .replace("%an", commit.getAuthor().getName())
                .replace("%ad", Commit.formatTime(commit.getAuthor().getTime()));

        System.out.println(out);
    }

    /**
     * Retrieves a string representation of branch decorations for the given commit.
     * If the commit is associated with multiple branches, they are formatted and joined
     * into a single string, with the current branch (if present) highlighted.
     * If no branches are associated with the commit, an empty string is returned.
     *
     * @param commit the {@link Commit} object for which the branch decorations are to be retrieved; must not be null.
     * @return a formatted string representing the branches associated with the given commit.
     *         Returns an empty string if no branches are associated.
     */
    private String getBranchDecorator(Commit commit) {
        ObjectID commitId = commit.getOid();
        List<String> branches = commitIdTable.getOrDefault(commitId, List.of());

        if (branches.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("(");

        boolean firstBranch = true;

        // HEAD -> current branch
        if (branches.contains(currentBranch)) {
            sb.append(AnsiColor.cyan("HEAD"))
                    .append(AnsiColor.yellow(" -> "))
                    .append(AnsiColor.green(currentBranch));
            firstBranch = false;
        }

        /* other branches */
        for (String branch : branches) {
            if (branch.equals(currentBranch)) {
                continue;
            }
            if (!firstBranch) {
                sb.append(AnsiColor.yellow(", "));
            }
            sb.append(AnsiColor.green(branch));
            firstBranch = false;
        }

        sb.append(")");
        return sb.toString();
    }

}
