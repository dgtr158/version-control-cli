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
    private final List<String> branches;

    public RevList(Refs refs, List<String> branches) throws VctrlException {
        this.refs = refs;
        this.branches = branches;
    }

    @Override
    public Iterator<Commit> iterator() {
        return new CommitIterator(this.refs, this.branches);
    }


    static class CommitIterator implements Iterator<Commit> {

        private final Database database;
        private final Map<String, ObjectID> branchTable;
        private final PriorityQueue<CommitQueueItem> commitQueue;

        static class CommitQueueItem {
            String branch;
            Commit commit;

            public CommitQueueItem(String branch, Commit commit) {
                this.branch = branch;
                this.commit = commit;
            }

            public String getBranch() {
                return this.branch;
            }

            public Commit getCommit() {
                return this.commit;
            }

            @Override
            public boolean equals(Object o) {
                if (!(o instanceof CommitQueueItem that)) return false;
                return Objects.equals(commit.getOid(), that.commit.getOid());
            }

            @Override
            public int hashCode() {
                return Objects.hash(branch, commit.getOid());
            }
        }

        public CommitIterator(Refs refs, List<String> branches) {
            this.database = Database.getInstance();
            this.branchTable = new HashMap<>();
            this.commitQueue = new PriorityQueue<>(
                    (item1, item2) -> {
                        Instant t1 = item1.getCommit().getAuthor().getTime();
                        Instant t2 = item2.getCommit().getAuthor().getTime();
                        return t2.compareTo(t1);
                    }
            );

            try {
                // Build a branch table
                RefHead refHeads = refs.getRefHead();
                for (String branch : branches) {
                    String branchHeadCommitID = refHeads.getBranchHeadContent(branch);
                    branchTable.put(branch, new ObjectID(branchHeadCommitID));
                }

                // Init the commit queue
                for (Map.Entry<String, ObjectID> branchTableEntry : branchTable.entrySet()) {
                    String branchName = branchTableEntry.getKey();
                    ObjectID headCommitID = branchTableEntry.getValue();
                    this.loadNewCommit(branchName, headCommitID);
                }

            } catch (Exception e) {
                throw new VctrlException(e);
            }
        }

        @Override
        public boolean hasNext() {
            return !commitQueue.isEmpty();
        }

        // TODO: right now get only the first parent if there're multiple parents
        @Override
        public Commit next() {
            if (commitQueue.isEmpty()) {
                throw new VctrlException("There no more commit");
            }
            CommitQueueItem queueItem = commitQueue.poll();
            String branch = queueItem.getBranch();
            Commit commit = queueItem.getCommit();

            try {
                ObjectID branchHeadCommitID = branchTable.get(branch);
                if (branchHeadCommitID != null) {
                    this.loadNewCommit(branch, branchHeadCommitID);
                }
            } catch (Exception e) {
                throw new VctrlException(e);
            }

            return commit;

        }

        private void loadNewCommit(String branch, ObjectID commitID) throws IOException, NoSuchAlgorithmException {
            if (commitID == null) {
                branchTable.put(branch, null);
                return;
            }

            Commit commit = (Commit) database.loadObject(commitID, ObjectType.COMMIT);
            CommitQueueItem queueItem = new CommitQueueItem(branch, commit);
            if (!commitQueue.contains(queueItem)) {
                commitQueue.add(queueItem);
            }

            // Update the branch table
            // TODO: Need to deal with the commit has multiple parents
            ObjectID parent = commit.getParentIds().isEmpty() ? null : commit.getParentIds().get(0);
            branchTable.put(branch, parent);
        }

    }
}
