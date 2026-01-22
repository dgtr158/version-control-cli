package duongtran.vctrl.actions;

import duongtran.vctrl.branches.merge.MergeInputs;
import duongtran.vctrl.branches.merge.MergeResolve;
import duongtran.vctrl.branches.migration.Migration;
import duongtran.vctrl.branches.migration.TreeDiff;
import duongtran.vctrl.branches.migration.TreeDiffEntry;
import duongtran.vctrl.references.Refs;
import duongtran.vctrl.storage.ObjectID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

/**
 * The MergeAction class is responsible for performing merge operations in the
 * version control system. It handles the resolution of commit differences,
 * application of changes, and creation of new merge commits between branches
 * and revisions.
 */
public class MergeAction {

    private static final Logger log = LoggerFactory.getLogger(MergeAction.class);

    private final Refs refs;

    public MergeAction() {
        this.refs = new Refs();
    }

    /**
     * Executes the merge operation for the specified branch and revision, resolving
     * commit differences, applying changes, and saving a new merge commit.
     *
     * @param branchName the name of the branch to merge into
     * @param revision   the revision number to resolve the target commit
     * @throws IOException              if an I/O error occurs during resolving or applying changes
     * @throws NoSuchAlgorithmException if a required cryptographic algorithm is not available
     */
    public void execute(String branchName, int revision) throws IOException, NoSuchAlgorithmException {

        // Build inputs
        MergeInputs inputs =
                new MergeInputs(this.refs, branchName, revision);

        // Null Merge
        if (inputs.isAlreadyMerged()) {
            handleNullMerged();
            return;
        }

        // Fast-forward merge
        if (inputs.isFastForward()) {
            handleFastForwardMerged(inputs);
            return;
        }

        // Load index and resolve merge
        MergeResolve resolve = new MergeResolve(inputs);
        resolve.execute();

        // Write merge commit
        CommitAction commitAction = new CommitAction();
        commitAction.saveCommit(
                List.of(
                        inputs.getHead()
                        , inputs.getOther()
                ),
                "Merge branch " + branchName
        );

    }

    private void handleNullMerged() {
        System.out.println("Already up to date.");
    }

    private void handleFastForwardMerged(MergeInputs inputs) {
        ObjectID headCommitID = inputs.getHead();
        ObjectID otherCommitID = inputs.getOther();

        System.out.printf("Updating #{ %s }..#{ %s }\n", headCommitID.abbreviate(), otherCommitID.abbreviate());
        System.out.println("Fast-forward");

        TreeDiff treeDiff = new TreeDiff();
        try {
            Map<Path, TreeDiffEntry> diff =
                    treeDiff.detectTreeDiff(
                            inputs.getOther(),
                            inputs.getHead()
                    );
            Migration migration = new Migration(
                    inputs.getOther(),
                    inputs.getHead(),
                    diff
            );
            migration.applyChanges();

            // Update HEAD
            String currentBranch = this.refs.getCurrentBranch();
            this.refs.getRefHead().updateBranchHeadValue(this.refs.getRefHead().getReafHeadPath().resolve(currentBranch), otherCommitID.getValue());

        } catch (Exception e) {
            log.error("Error when performing fast-forward merge: {}", e.getMessage());
        }

    }

}
