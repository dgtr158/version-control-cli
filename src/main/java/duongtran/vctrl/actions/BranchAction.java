package duongtran.vctrl.actions;

import duongtran.vctrl.branches.Revision;
import duongtran.vctrl.references.RefHead;
import duongtran.vctrl.references.Refs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The BranchAction class is responsible for creating a new branch within a version control system.
 * It initializes the new branch with the last commit of the current branch and updates
 * the branch reference to point to this commit.
 */
public class BranchAction {

    private static final Logger log = LoggerFactory.getLogger(BranchAction.class);

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

        // Resolve target commit on HEAD
        Revision revisionResolvers = new Revision(ref, null);
        String targetCommitId = revisionResolvers.resolveAncestor(revision);

        // Create a new branch in ref
        Path branchHead = refHead.createBranch(branchName);

        // Update the last commit id to the new branch head
        refHead.updateBranchHeadValue(branchHead, targetCommitId);

    }

    /**
     * Retrieves the list of all branch names available in the version control system.
     * Branch names are derived from the file names present in the reference head directory.
     *
     * @return a list of strings representing the names of all branches
     */
    public List<String> listBranches() {
        Refs ref = new Refs();
        return ref.getRefHead().listAllBranches().stream().map(p -> p.getFileName().toString()).collect(Collectors.toList());
    }

    public void deleteBranch(String branchName) throws IOException, IllegalArgumentException {
        Refs ref = new Refs();
        ref.getRefHead().deleteBranch(branchName);
    }

}
