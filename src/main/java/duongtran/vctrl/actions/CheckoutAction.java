package duongtran.vctrl.actions;

import duongtran.vctrl.branches.Revision;
import duongtran.vctrl.branches.migration.Migration;
import duongtran.vctrl.branches.migration.TreeChanges;
import duongtran.vctrl.branches.migration.TreeDiff;
import duongtran.vctrl.references.Refs;
import duongtran.vctrl.storage.ObjectID;

import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * The CheckoutAction class provides functionality to perform a checkout operation in a version
 * control system. The purpose of this class is to synchronize the workspace with a specific commit
 * defined by a branch name and revision number.
 * <p>
 * This class is intended to facilitate version control workflows by handling the complexities of
 * managing branches, commits, detecting and applying differences between commit states, and updating
 * the workspace accordingly.
 */
public class CheckoutAction {

    /**
     * Executes the checkout operation to synchronize the workspace with a specified commit
     * in the targeted branch. This involves resolving the HEAD commit, determining the
     * target commit based on the specified revision, detecting differences between the two
     * commits, and applying those differences to the workspace.
     *
     * @param branchName the name of the branch to be checked out
     * @param revision   the specific revision number used to determine the target commit
     * @throws IOException              if an I/O error occurs during the execution
     * @throws NoSuchAlgorithmException if a required cryptographic algorithm is not available
     */
    public void execute(String branchName, int revision) throws IOException, NoSuchAlgorithmException {
        Refs ref = new Refs();

        // Resolve HEAD commit
        ObjectID headCommitId = new ObjectID(ref.readHeadCommitId());

        // Resolve target commit
        Revision revisionResolvers = new Revision(ref);
        ObjectID targetCommitId = new ObjectID(revisionResolvers.resolveAncestor(revision));

        // Detect changes between target commit and HEAD commit
        TreeDiff treeDiff = new TreeDiff();
        Map<Path, TreeChanges> treeDiffMap = treeDiff.detectTreeDiff(headCommitId, targetCommitId);

        // Applies changes to the workspace
        Migration migration = new Migration(headCommitId, targetCommitId, treeDiffMap);
        migration.applyChanges();
    }

}
