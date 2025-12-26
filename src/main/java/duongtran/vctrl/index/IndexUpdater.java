package duongtran.vctrl.index;

import duongtran.vctrl.Workspace;
import duongtran.vctrl.storage.Database;
import duongtran.vctrl.storage.ObjectID;
import duongtran.vctrl.storage.ObjectType;
import duongtran.vctrl.storage.objects.Commit;
import duongtran.vctrl.storage.objects.Tree;
import duongtran.vctrl.storage.objects.TreeEntry;

import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

public class IndexUpdater {

    private final Database database;
    private final Index index;
    private final Path rootPath;

    public IndexUpdater(Index index) {
        this.index = index;
        this.database = Database.getInstance();
        this.rootPath = Workspace.getInstance().getRootPath();
    }

    /**
     * Updates the index based on the state of the tree associated with the provided commit ID.
     * This method retrieves the commit object corresponding to the given {@code commitId},
     * extracts the root tree object from the commit, and updates the index to reflect the structure
     * and contents of the tree.
     *
     * @param commitId the {@code ObjectID} of the commit from which the index should be updated
     * @throws IOException if an I/O error occurs while loading objects from the database or writing to the index
     * @throws NoSuchAlgorithmException if the required cryptographic algorithm cannot be found during processing
     */
    public void updateFromCommitId(ObjectID commitId) throws IOException, NoSuchAlgorithmException {
        Commit commit = (Commit) this.database.loadObject(commitId, ObjectType.COMMIT);
        ObjectID treeOid = commit.getTreeOid();

        index.clear();
        walkTree(treeOid, Path.of(""));
        index.write();
    }

    /**
     * Recursively traverses a tree structure and updates the index with the entries it encounters.
     * If a tree entry is a tree itself, this method calls itself recursively;
     * otherwise, it adds the file entry to the index.
     *
     * @param treeOid the ObjectID of the tree object to start traversal from
     * @param currentPath the current path relative to the root, representing
     *                    the traversal progress within the tree structure
     * @throws IOException if an I/O error occurs during object loading or indexing
     * @throws NoSuchAlgorithmException if the required cryptographic algorithm is not available
     */
    private void walkTree(ObjectID treeOid, Path currentPath) throws IOException, NoSuchAlgorithmException {
        Tree tree = (Tree) this.database.loadObject(treeOid, ObjectType.TREE);
        for (TreeEntry treeEntry : tree.getStoredEntries().values()) {
            Path entryPath = currentPath.resolve(treeEntry.getFileName());
            if (treeEntry.isTree()) {
                walkTree(treeEntry.getOid(), entryPath);
            } else {
                index.addEntry(rootPath.resolve(entryPath), treeEntry.getOid().getValue());
            }
        }
    }

}
