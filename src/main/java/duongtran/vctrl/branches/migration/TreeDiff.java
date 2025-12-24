package duongtran.vctrl.branches.migration;

import duongtran.vctrl.storage.Database;
import duongtran.vctrl.storage.ObjectID;
import duongtran.vctrl.storage.ObjectType;
import duongtran.vctrl.storage.objects.Commit;
import duongtran.vctrl.storage.objects.Tree;
import duongtran.vctrl.storage.objects.TreeEntry;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public final class TreeDiff {

    private final Database database;
    private final Map<Path, TreeChanges> changes = new HashMap<>();

    public TreeDiff() {
        this.database = Database.getInstance();
    }

    public Map<Path, TreeChanges> detectTreeDiff(ObjectID leftCommitOid, ObjectID rightCommitOid) throws IOException, NoSuchAlgorithmException {
        ObjectID leftTreeOid = commitToTree(leftCommitOid);
        ObjectID rightTreeOid = commitToTree(rightCommitOid);
        compareTrees(leftTreeOid, rightTreeOid, Paths.get(""));
        return changes;
    }

    private void compareTrees(ObjectID leftTreeOid, ObjectID rightTreeOid, Path prefix) throws IOException, NoSuchAlgorithmException {
        if (Objects.equals(leftTreeOid, rightTreeOid)) {
            return;
        }

        // Load the left and right trees
        Map<String, TreeEntry> leftTreeEntries = loadCompareTreeEntries(leftTreeOid);
        Map<String, TreeEntry> rightTreeEntries = loadCompareTreeEntries(rightTreeOid);

        // Detect changes
        detectDeletions(leftTreeEntries, rightTreeEntries, prefix);
        detectAdditions(leftTreeEntries, rightTreeEntries, prefix);
    }

    private void detectDeletions(
            Map<String, TreeEntry> leftTreeEntries,
            Map<String, TreeEntry> rightTreeEntries,
            Path prefix) throws IOException, NoSuchAlgorithmException {

        for (Map.Entry<String, TreeEntry> e : leftTreeEntries.entrySet()) {
            String name = e.getKey();
            TreeEntry leftTreeEntry = e.getValue();

            Path path = prefix.resolve(name);
            TreeEntry rightTreeEntry = rightTreeEntries.get(name);

            if (Objects.equals(leftTreeEntry, rightTreeEntry)) {
                continue;
            }

            ObjectID leftSubTree = leftTreeEntry.isTree() ? leftTreeEntry.getOid() : null;
            ObjectID rightSubTree = (rightTreeEntry != null && rightTreeEntry.isTree()) ? rightTreeEntry.getOid() : null;

            if (leftSubTree != null || rightSubTree != null) {
                compareTrees(leftSubTree, rightSubTree, path);
            }

            TreeEntry leftChangeEntry = leftTreeEntry.isTree() ? null : leftTreeEntry;
            TreeEntry rightChangeEntry = (rightTreeEntry != null && rightTreeEntry.isTree()) ? null : rightTreeEntry;

            if (leftChangeEntry != null || rightChangeEntry != null) {
                changes.put(path, new TreeChanges(leftChangeEntry, rightChangeEntry));
            }
        }
    }

    private Map<String, TreeEntry> loadCompareTreeEntries(ObjectID treeObjectID) throws IOException, NoSuchAlgorithmException {
        Map<String, TreeEntry> entries = new TreeMap<>();
        Tree tree = (Tree) database.loadObject(treeObjectID, ObjectType.TREE);
        if (tree != null) {
            entries = tree.getStoredEntries();
        }
        return entries;
    }

    private void detectAdditions(
            Map<String, TreeEntry> leftTreeEntries,
            Map<String, TreeEntry> rightTreeEntries,
            Path prefix) throws IOException, NoSuchAlgorithmException {

        for (Map.Entry<String, TreeEntry> e : rightTreeEntries.entrySet()) {
            String name = e.getKey();
            TreeEntry rightTreeEntry = e.getValue();

            if (leftTreeEntries.containsKey(name)) {
                continue;
            }

            Path path = prefix.resolve(name);
            if (rightTreeEntry.isTree()) {
                compareTrees(null, rightTreeEntry.getOid(), path);
            } else {
                changes.put(path, new TreeChanges(null, rightTreeEntry));
            }
        }
    }

    private ObjectID commitToTree(ObjectID commitOid) throws IOException, NoSuchAlgorithmException {
        Commit commit = (Commit) database.loadObject(commitOid, ObjectType.COMMIT);
        if (commit == null) return null;
        return commit.getTreeOid();
    }
}
