package duongtran.vctrl.merge;

import duongtran.vctrl.storage.Database;
import duongtran.vctrl.storage.ObjectID;
import duongtran.vctrl.storage.ObjectType;
import duongtran.vctrl.storage.objects.Commit;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * The CommonAncestors class is responsible for finding the first common ancestor
 * commit between two commits in a repository. It processes commits using a priority
 * queue and uses flags to determine the reachability of commits from both
 * initial input commits.
 * <p>
 * This class employs traversal of commit history and maintains flags associated
 * with each commit to identify common ancestors efficiently.
 */
public class CommonAncestors {

    private final Database database;

    private final Map<ObjectID, EnumSet<Flag>> flags = new HashMap<>();
    private final PriorityQueue<Commit> queue;

    private enum Flag {
        PARENT1, PARENT2
    }

    private static final EnumSet<Flag> BOTH =
            EnumSet.of(Flag.PARENT1, Flag.PARENT2);

    public CommonAncestors(ObjectID firstCommitId, ObjectID secondCommitId) throws IOException, NoSuchAlgorithmException {
        this.database = Database.getInstance();

        this.queue = new PriorityQueue<>(
                Comparator.comparingLong((Commit c) -> c.getAuthor().getTime().getEpochSecond()).reversed()
        );

        Commit firstCommit = (Commit) database.loadObject(firstCommitId, ObjectType.COMMIT);
        Commit secondCommit = (Commit) database.loadObject(secondCommitId, ObjectType.COMMIT);

        mark(firstCommit.getOid(), Flag.PARENT1);
        mark(secondCommit.getOid(), Flag.PARENT2);

        queue.add(firstCommit);
        queue.add(secondCommit);
    }

    /**
     * Finds the common ancestor commit between two commit objects in the repository.
     * The method processes commits in a priority queue, analyzing their flags
     * to determine if they are reachable by both input commits. If such a common
     * ancestor is found, its ObjectID is returned.
     * <p>
     * The search operates on commit flags, which indicate whether a commit is
     * reachable from one or both of the initial input commits. The algorithm
     * continues until the queue is empty or a common ancestor is located.
     *
     * @return the {@code ObjectID} of the first common ancestor commit, or {@code null}
     * if no common ancestor is found.
     * @throws IOException              if an error occurs while accessing the repository.
     * @throws NoSuchAlgorithmException if the required cryptographic algorithm is unavailable.
     */
    public ObjectID find() throws IOException, NoSuchAlgorithmException {
        while (!queue.isEmpty()) {
            Commit commit = queue.poll();
            EnumSet<Flag> f = flags.get(commit.getOid());

            if (f.equals(BOTH)) {
                return commit.getOid();
            }

            addParent(commit, f);
        }
        return null;
    }

    /**
     * Adds the parent commit of the specified commit to the processing queue,
     * while updating its associated flags. This method verifies the parent commit's
     * ObjectID and updates flags to ensure all inherited flags are accounted for.
     * It avoids it re-adding commits that already have the required flags.
     *
     * @param commit         the commit whose parent is to be processed
     * @param inheritedFlags the set of flags to be inherited by the parent commit
     * @throws IOException              if an error occurs while accessing the repository
     * @throws NoSuchAlgorithmException if the required cryptographic algorithm is unavailable
     */
    private void addParent(Commit commit, EnumSet<Flag> inheritedFlags) throws IOException, NoSuchAlgorithmException {
        if (commit.getParentId() == null) return;
        ObjectID parentObjectId = new ObjectID(commit.getParentId());
        Commit parentCommit = (Commit) database.loadObject(parentObjectId, ObjectType.COMMIT);
        EnumSet<Flag> parentFlags =
                flags.computeIfAbsent(parentCommit.getOid(), k -> EnumSet.noneOf(Flag.class));

        if (parentFlags.containsAll(inheritedFlags)) return;

        parentFlags.addAll(inheritedFlags);
        queue.add(parentCommit);
    }

    /**
     * Marks the specified ObjectID with the given flag. If the ObjectID does not
     * already have an associated set of flags, a new empty set is created and the
     * flag is added to it. This method ensures that the ObjectID's flags are
     * updated to include the new flag.
     *
     * @param oid  the {@code ObjectID} to be marked with the given flag
     * @param flag the {@code Flag} to associate with the specified {@code ObjectID}
     */
    private void mark(ObjectID oid, Flag flag) {
        flags.computeIfAbsent(oid, k -> EnumSet.noneOf(Flag.class)).add(flag);
    }


}
