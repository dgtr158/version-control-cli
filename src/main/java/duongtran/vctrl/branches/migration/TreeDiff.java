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
        detectDeletionsAndChanges(leftTreeEntries, rightTreeEntries, prefix);
        detectAdditions(leftTreeEntries, rightTreeEntries, prefix);
    }

    /**
     * Identifies and records deletions by comparing entries between two directory trees.
     * The method iterates through the entries in the left tree, compares them with the right tree,
     * and records changes, including mismatched or missing entries, while accommodating
     * subtree comparison if either entry represents a directory.
     *
     * @param leftTreeEntries  a map representing the entries of the left tree, where keys are file names
     *                         and values are {@code TreeEntry} objects.
     * @param rightTreeEntries a map representing the entries of the right tree, where keys are file names
     *                         and values are {@code TreeEntry} objects.
     * @param prefix           the base path that is used as a prefix when identifying the full path of tree entries.
     * @throws IOException              if an I/O error occurs while processing the directory structure.
     * @throws NoSuchAlgorithmException if the hashing algorithm required for comparison is not available.
     */
    private void detectDeletionsAndChanges(
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

    /**
     * Loads and retrieves the entries of a tree object identified by the specified tree object ID.
     * The method fetches the tree from the database, extracts its stored entries,
     * and returns them as a map.
     *
     * @param treeObjectID the {@code ObjectID} representing the tree object to be loaded.
     * @return a map of tree entries where the keys are file or directory names
     * and the values are {@code TreeEntry} objects describing each entry.
     * Returns an empty map if the tree object is null.
     * @throws IOException              if an I/O error occurs while loading the tree object from the database.
     * @throws NoSuchAlgorithmException if a required hashing algorithm is not available.
     */
    private Map<String, TreeEntry> loadCompareTreeEntries(ObjectID treeObjectID) throws IOException, NoSuchAlgorithmException {
        Map<String, TreeEntry> entries = new TreeMap<>();
        Tree tree = (Tree) database.loadObject(treeObjectID, ObjectType.TREE);
        if (tree != null) {
            entries = tree.getStoredEntries();
        }
        return entries;
    }

    /**
     * Detects and records additions by identifying entries present in the right tree but not in the left tree.
     * This method iterates through the entries of the right tree. For each entry that is absent in the left
     * tree, it calculates its path, checks if it is a subtree or a regular file, and either recursively processes
     * subtrees or records the addition of regular files.
     *
     * @param leftTreeEntries  a map representing the entries of the left tree, where keys are file names
     *                         and values are {@code TreeEntry} objects that describe the contents of the left tree.
     * @param rightTreeEntries a map representing the entries of the right tree, where keys are file names
     *                         and values are {@code TreeEntry} objects that describe the contents of the right tree.
     * @param prefix           the base path that is used as a prefix for constructing the full path of tree entries.
     * @throws IOException              if an I/O error occurs during processing of the tree structures.
     * @throws NoSuchAlgorithmException if the required hashing algorithm for comparing tree contents is not available.
     */
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

    /**
     * Retrieves the tree object ID (ObjectID) associated with the specified commit object ID.
     * Loads the commit object from the database using the given commit object ID,
     * checks if it is non-null, and returns its tree object ID.
     *
     * @param commitOid the {@code ObjectID} representing the commit whose tree object ID is to be retrieved.
     * @return the {@code ObjectID} of the tree associated with the specified commit,
     * or {@code null} if the commit object does not exist or is invalid.
     * @throws IOException              if an I/O error occurs while accessing the database.
     * @throws NoSuchAlgorithmException if the required hashing algorithm is not available.
     */
    private ObjectID commitToTree(ObjectID commitOid) throws IOException, NoSuchAlgorithmException {
        Commit commit = (Commit) database.loadObject(commitOid, ObjectType.COMMIT);
        if (commit == null) return null;
        return commit.getTreeOid();
    }
}
