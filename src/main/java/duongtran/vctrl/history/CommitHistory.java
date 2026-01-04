package duongtran.vctrl.history;

import duongtran.vctrl.references.Refs;
import duongtran.vctrl.storage.Database;
import duongtran.vctrl.storage.ObjectID;
import duongtran.vctrl.storage.ObjectType;
import duongtran.vctrl.storage.objects.Commit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.List;

public class CommitHistory implements Iterable<Commit> {

    private static final Logger log = LoggerFactory.getLogger(CommitHistory.class);

    @Override
    public Iterator<Commit> iterator() {
        return new CommitIterator();
    }


    private static class CommitIterator implements Iterator<Commit> {

        ObjectID nextOid = null;
        Database database;

        public CommitIterator() {
            this.database = Database.getInstance();
            Refs refs = new Refs();
            this.nextOid = new ObjectID(refs.readHead());
        }

        @Override
        public boolean hasNext() {
            return nextOid != null;
        }

        // TODO: right now get only the first parent if there're multiple parents
        @Override
        public Commit next() {
            try {
                Commit commit = (Commit) database.loadObject(this.nextOid, ObjectType.COMMIT);

                List<ObjectID> parentIds = commit.getParentIds();
                if (parentIds.isEmpty()) this.nextOid = null;
                else this.nextOid = commit.getParentIds().get(0);
                return commit;
            } catch (IOException | NoSuchAlgorithmException e) {
                log.error("Cannot load commit with oid: {}", this.nextOid);
                this.nextOid = null;
                return null;
            }

        }
    }
}
