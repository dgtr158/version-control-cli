package duongtran.vctrl.actions;

import duongtran.vctrl.branches.Revision;
import duongtran.vctrl.branches.merge.CommonAncestors;
import duongtran.vctrl.branches.migration.ConflictException;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The MergeAction class is responsible for performing merge operations in the
 * version control system. It handles the resolution of commit differences,
 * application of changes, and creation of new merge commits between branches
 * and revisions.
 */
public class MergeAction {

    private static final Logger log = LoggerFactory.getLogger(MergeAction.class);

    /**
     * Executes the merge operation for the specified branch and revision, resolving
     * commit differences, applying changes, and saving a new merge commit.
     *
     * @param branchName the name of the branch to merge into
     * @param revision the revision number to resolve the target commit
     * @throws IOException if an I/O error occurs during resolving or applying changes
     * @throws NoSuchAlgorithmException if a required cryptographic algorithm is not available
     */
    public void execute(String branchName, int revision) throws IOException, NoSuchAlgorithmException {
        Refs ref = new Refs();

        // Resolve HEAD commit
        ObjectID headCommitId = new ObjectID(ref.readHead());

        // Resolve target commit
        Revision revisionResolvers = new Revision(ref, branchName, revision);
        ObjectID mergeCommitId = new ObjectID(revisionResolvers.resolveAncestor());

        // Find the common ancestor between the two commits
        CommonAncestors commonAncestor = new CommonAncestors(headCommitId, mergeCommitId);
        Set<ObjectID> baseObjectIds = commonAncestor.find();
        ObjectID baseObjectId = baseObjectIds.iterator().next();

        // Detect differences and apply changes
        TreeDiff treeDiff = new TreeDiff();
        Map<Path, TreeDiffEntry> treeDiffMap = treeDiff.detectTreeDiff(baseObjectId, mergeCommitId);

        try {
            Migration migration = new Migration(baseObjectId, mergeCommitId, treeDiffMap);
            migration.applyChanges();
        } catch (ConflictException e) {
            throw new ConflictException(e.getMessage());
        }

        // Commit the changes
        CommitAction commitAction = new CommitAction();
        commitAction.saveCommit(new ArrayList<>(List.of(
                headCommitId, mergeCommitId
        )), "Merge commit message");

    }

}
