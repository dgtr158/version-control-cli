package duongtran.vctrl.actions;

import duongtran.vctrl.branches.Revision;
import duongtran.vctrl.references.RefHead;
import duongtran.vctrl.references.Refs;

import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

/**
 * The BranchAction class is responsible for creating a new branch within a version control system.
 * It initializes the new branch with the last commit of the current branch and updates
 * the branch reference to point to this commit.
 */
public class BranchAction {

    /**
     * Creates a new branch, initializes it with the current branch's last commit,
     * and updates the branch reference to point to this commit.
     *
     * @param branchName the name of the new branch to be created
     * @throws IOException if an I/O error occurs during branch creation or update
     */
    public void execute(String branchName, int revision) throws IOException, NoSuchAlgorithmException {
        Refs ref = new Refs();
        RefHead refHead = ref.getRefHead();

        // Resolve target commit
        Revision revisionResolvers = new Revision(ref);
        String targetCommitId = revisionResolvers.resolveAncestor(revision);

        // Create a new branch in ref
        Path branchHead = refHead.createBranch(branchName);

        // Update the last commit id to the new branch head
        refHead.updateBranchHeadValue(branchHead, targetCommitId);

    }

}
