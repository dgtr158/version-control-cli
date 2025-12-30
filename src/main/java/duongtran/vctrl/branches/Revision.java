package duongtran.vctrl.branches;

import duongtran.vctrl.references.Refs;
import duongtran.vctrl.storage.Database;
import duongtran.vctrl.storage.ObjectID;
import duongtran.vctrl.storage.ObjectType;
import duongtran.vctrl.storage.objects.Commit;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 * The Revision class provides functionality to resolve revisions in a version control system.
 * It computes specific commits based on their relationship to the HEAD commit,
 * such as finding ancestors at a given depth.
 */
public final class Revision {

    private final Refs refs;
    private final String branchName;
    private final Database database;


    public Revision(Refs refs, String branchName) {
        this.refs = refs;
        this.branchName = branchName;
        this.database = Database.getInstance();
    }

    /**
     * Resolves a revision relative to HEAD.
     *
     * @param revision number of ancestors (0 = HEAD)
     * @return commit id
     */
    public String resolveAncestor(int revision) throws IOException, NoSuchAlgorithmException {
        ObjectID branchHeadCommit;
        if (branchName == null) {
            branchHeadCommit = new ObjectID(refs.readHead());
        } else {
            branchHeadCommit = new ObjectID(refs.getRefHead().getBranchHeadContent(branchName));
        }
        return walkAncestors(branchHeadCommit, revision);
    }


    /**
     * Traverses a series of ancestor commits starting from the given commit ID and returns
     * the commit ID of the ancestor at the specified depth.
     *
     * @param startCommitId the starting commit ID as an {@code ObjectID}, from which the
     *                      traversal of ancestor commits begins
     * @param depth         the number of ancestor commits to traverse; must be non-negative
     * @return the commit ID of the ancestor at the specified depth as a {@code String}
     * @throws IOException              if an I/O error occurs while loading objects from the database
     * @throws NoSuchAlgorithmException if the algorithm used for object storage is not supported
     * @throws IllegalArgumentException if there are not enough ancestors to reach the specified depth
     */
    private String walkAncestors(ObjectID startCommitId, int depth)
            throws IOException, NoSuchAlgorithmException {

        ObjectID currentCommitId = startCommitId;

        for (int i = 0; i < depth; i++) {
            Commit commit = (Commit) this.database.loadObject(currentCommitId, ObjectType.COMMIT);
            if (commit.getParentId() == null) {
                throw new IllegalArgumentException(
                        "Not enough ancestors for revision: " + depth
                );
            }
            currentCommitId = new ObjectID(commit.getParentId());
        }

        return currentCommitId.getValue();
    }
}

