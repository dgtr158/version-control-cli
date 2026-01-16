package duongtran.vctrl.branches;

import duongtran.vctrl.VctrlException;
import duongtran.vctrl.references.RefHead;
import duongtran.vctrl.references.Refs;
import duongtran.vctrl.storage.Database;
import duongtran.vctrl.storage.ObjectID;
import duongtran.vctrl.storage.ObjectType;
import duongtran.vctrl.storage.objects.Commit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;

public class RevList implements Iterable<Commit> {

    private static final Logger log = LoggerFactory.getLogger(RevList.class);

    private final Refs refs;
    private final List<String> includes;
    private final List<String> excludes;

    public RevList(Refs refs, List<String> includes, List<String> excludes) throws VctrlException {
        this.refs = refs;
        this.includes = includes;
        this.excludes = excludes;
    }

    @Override
    public Iterator<Commit> iterator() {
        return new CommitIterator();
    }


    class CommitIterator implements Iterator<Commit> {

        private final Database database;
        private final Set<ObjectID> seenCommits;
        private final Set<ObjectID> uninterestingCommits;
        private final PriorityQueue<Commit> commitQueue;

        public CommitIterator() throws VctrlException {
            this.database = Database.getInstance();
            this.seenCommits = new HashSet<>();
            this.uninterestingCommits = new HashSet<>();
            this.commitQueue = new PriorityQueue<>(
                    (c1, c2) -> {
                        Instant t1 = c1.getAuthor().getTime();
                        Instant t2 = c2.getAuthor().getTime();
                        return t2.compareTo(t1);
                    }
            );

            try {
                // Add each branch's head commit into the commit queue
                RefHead refHeads = refs.getRefHead();
                for (String branch : includes) {
                    String branchHeadCommitID = refHeads.getBranchHeadContent(branch);
                    if (branchHeadCommitID != null) {
                        enqueueCommit(new ObjectID(branchHeadCommitID));
                    }
                }

                // Mard uninteresting commits
                for (String branch : excludes) {
                    String branchHeadCommitID = refHeads.getBranchHeadContent(branch);
                    if (branchHeadCommitID != null) {
                        markUninteresting(new ObjectID(branchHeadCommitID));
                    }
                }

            } catch (Exception e) {
                throw new VctrlException(e);
            }
        }

        @Override
        public boolean hasNext() {
            return !commitQueue.isEmpty();
        }

        @Override
        public Commit next() {
            if (commitQueue.isEmpty()) {
                throw new VctrlException("There no more commit");
            }
            Commit commit = commitQueue.poll();

            try {
                for (ObjectID parentId : commit.getParentIds()) {
                    enqueueCommit(parentId);
                }
            } catch (Exception e) {
                throw new VctrlException(e);
            }

            return commit;

        }

        private void enqueueCommit(ObjectID commitID) throws IOException, NoSuchAlgorithmException {
            if (commitID == null || seenCommits.contains(commitID) || uninterestingCommits.contains(commitID)) return;

            Commit commit = (Commit) database.loadObject(commitID, ObjectType.COMMIT);
            if (commit == null) return;
            seenCommits.add(commit.getOid());
            commitQueue.add(commit);
        }

        private void markUninteresting(ObjectID commitID) throws IOException, NoSuchAlgorithmException {
            if (commitID == null || uninterestingCommits.contains(commitID)) return;

            uninterestingCommits.add(commitID);
            Commit commit = (Commit) database.loadObject(commitID, ObjectType.COMMIT);
            for (ObjectID parentID : commit.getParentIds()) {
                markUninteresting(parentID);
            }
        }

    }
}
