package duongtran.vctrl.branches;

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

public class RevList implements Iterable<Commit> {

    private static final Logger log = LoggerFactory.getLogger(RevList.class);

    private final Refs refs;
    private final List<String> branches;

    public RevList(Refs refs, List<String> branches) {
        this.refs = refs;
        this.branches = branches;
    }

    @Override
    public Iterator<Commit> iterator() {
        return new CommitIterator();
    }


    class CommitIterator implements Iterator<Commit> {

        ObjectID nextOid;
        Database database;

        public CommitIterator() {
            this.database = Database.getInstance();
            this.nextOid = null;
//            Revision revision = new Revision(refs, branches);
//            try {
//                this.nextOid = new ObjectID(revision.resolveAncestor());
//            } catch (IOException | NoSuchAlgorithmException e) {
//                log.error("Cannot get the next object Id");
//                throw new RuntimeException(e);
//            }
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
