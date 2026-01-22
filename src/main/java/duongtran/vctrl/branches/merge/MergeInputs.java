package duongtran.vctrl.branches.merge;

import duongtran.vctrl.branches.Revision;
import duongtran.vctrl.references.Refs;
import duongtran.vctrl.storage.ObjectID;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.Set;

/**
 * Represents the inputs required for a merge operation. The class identifies
 * the three key points necessary for performing a three-way merge: the current
 * head of the branch, the other branch to merge, and their common ancestor (base).
 */
public class MergeInputs {

    private final ObjectID head;
    private final ObjectID other;
    private final ObjectID base;

    public MergeInputs(Refs refs, String branchName, int revision)
            throws IOException, NoSuchAlgorithmException {

        this.head = new ObjectID(refs.readHead());

        Revision resolver = new Revision(refs, branchName, revision);
        this.other = new ObjectID(resolver.resolveAncestor());

        CommonAncestors ancestors = new CommonAncestors(head, other);
        Set<ObjectID> bases = ancestors.find();
        this.base = bases.iterator().next(); // best common ancestor
    }

    public ObjectID getHead() {
        return head;
    }

    public ObjectID getOther() {
        return other;
    }

    public ObjectID getBase() {
        return base;
    }

    public boolean isAlreadyMerged() {
        return Objects.equals(this.base, this.other);
    }

    public boolean isFastForward() {
        return Objects.equals(this.base, this.head);
    }

}
