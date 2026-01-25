package duongtran.vctrl.storage.objects;

import duongtran.vctrl.Workspace;
import duongtran.vctrl.index.IndexEntry;
import duongtran.vctrl.index.IndexKey;
import duongtran.vctrl.index.StagEnum;
import duongtran.vctrl.storage.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Represents a hierarchical tree structure with entries and subtrees.
 * A tree consists of file entries and subtrees, allowing for a directory-like structure.
 * <p>
 * Tree object format:
 * tree<space><contentLength><null><listOfTreeEntry>
 * See {@code TreeEntry} for tree's entry format.
 *
 */
public class Tree extends ObjectStorage {
    private final List<TreeEntry> entries;
    private final List<Tree> subTrees;
    private final TreeMap<String, TreeEntry> storedEntries; // <name, entry>
    private final Path path;

    public Tree(List<TreeEntry> entries, List<Tree> subTrees, Path path) {
        this.entries = entries;
        this.subTrees = subTrees;
        this.path = path;
        this.storedEntries = new TreeMap<>();
        for (TreeEntry entry : entries) {
            storedEntries.put(entry.getFileName(), entry);
        }
    }

    public Tree(TreeMap<String, TreeEntry> storedEntries, Path path) {
        this.entries = new ArrayList<>();
        this.subTrees = new ArrayList<>();
        this.storedEntries = storedEntries;
        this.path = path;
    }

    @Override
    public ObjectType getType() {
        return ObjectType.TREE;
    }

    /**
     * Converts the tree and its entries into a byte array representation.
     * The representation includes the mode, file name, and object ID of each entry.
     * <mode> <fileName>\0<oid>
     *
     * @return a byte array containing the serialized tree with its entries.
     * @throws RuntimeException if an I/O error occurs during the conversion process.
     */
    @Override
    public byte[] getContent() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            for (Map.Entry<String, TreeEntry> entryMap : storedEntries.entrySet()) {
                TreeEntry entry = entryMap.getValue();
                String entryHeader = String.format("%s %s\0", entry.getMode(), entry.getFileName());
                byte[] entryData = entryHeader.getBytes(StandardCharsets.ISO_8859_1);
                out.write(entryData);

                byte[] objectID = entry.getOid().toBytes();
                out.write(objectID);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return out.toByteArray();
    }

    /**
     * Retrieves the path associated with the current tree instance.
     *
     * @return the Path object representing the location or identifier of the tree.
     */
    public Path getPath() {
        return path;
    }

    /**
     * Retrieves the stored entries of the tree.
     * The stored entries are represented as a map where the key is the file name
     * and the value is the corresponding {@code TreeEntry}.
     *
     * @return a {@code TreeMap<String, TreeEntry>} containing the stored entries of the tree.
     */
    public TreeMap<String, TreeEntry> getStoredEntries() {
        return storedEntries;
    }

    /**
     * Adds a {@code TreeEntry} object to the stored entries map in the current tree instance.
     * The entry is associated with its file name as the key in the map.
     *
     * @param entry the {@code TreeEntry} object to be added to the map. Must not be null.
     */
    public void addStoreEntry(TreeEntry entry) {
        this.storedEntries.put(entry.getFileName(), entry);
    }

    /**
     * Builds a tree structure from a map of paths and associated index entries.
     * The method normalizes the given paths before constructing the tree by
     * organizing paths into a hierarchical structure of entries and subtrees.
     *
     * @param indexEntries a map containing absolute paths as keys and their
     *                     associated index entries as values. Each path represents
     *                     a file or directory within the workspace.
     * @return a Tree object representing the hierarchical structure of the given
     * paths and index entries.
     */
    public static Tree buildTree(Map<IndexKey, IndexEntry> indexEntries) {
        Map<IndexKey, IndexEntry> normalStageEntries = filterNormalStage(indexEntries);
        Map<IndexKey, IndexEntry> relativeEntryPath = normalizePath(normalStageEntries);
        return buildWithNormalize(Path.of(""), relativeEntryPath);
    }

    /**
     * Stores the specified tree into the database. The method recursively processes
     * all subtrees of the given tree, stores them into the database, and then constructs and
     * stores the root tree in the database. Each subtree is represented as a directory entry
     * in the root tree.
     *
     * @param root     the root {@code Tree} object to be stored. Must not be null and should contain
     *                 its associated subtrees and entries.
     * @param database the {@code Database} instance where the tree and its contents will be stored.
     *                 Must not be null.
     * @return the {@code ObjectID} representing the stored tree in the database.
     * @throws IOException              if an I/O error occurs during the storage process.
     * @throws NoSuchAlgorithmException if the hash algorithm used for generating the
     *                                  {@code ObjectID} is not available.
     */
    public static ObjectID store(Tree root, Database database) throws IOException, NoSuchAlgorithmException {
        // Build the subtree
        for (Tree subTree : root.subTrees) {
            ObjectID subTreeOID = store(subTree, database);
            root.addStoreEntry(new TreeEntry(
                    subTree.getPath().getFileName().toString()
                    , subTreeOID
                    , FileMode.DIRECTORY
            ));
        }
        return database.store(root);
    }

    /**
     * Loads a {@code Tree} object from the database using its specified {@code ObjectID}.
     * The method retrieves the object from the database and casts it to a {@code Tree}.
     *
     * @param oid the {@code ObjectID} of the {@code Tree} to be loaded. Must not be null.
     * @return the {@code Tree} object corresponding to the provided {@code ObjectID}.
     * @throws IOException              if an I/O error occurs while loading the object from the database.
     * @throws NoSuchAlgorithmException if the hashing algorithm used during the loading process is unavailable.
     */
    public static Tree loadTree(ObjectID oid) throws IOException, NoSuchAlgorithmException, IllegalArgumentException {
        Database database = Database.getInstance();
        return (Tree) database.loadObject(oid, ObjectType.TREE);
    }

    /**
     * Constructs a {@code Tree} object from the provided byte array representation.
     * The byte array should encode the header and the stored entries of the tree.
     *
     * @param bytes the byte array containing the serialized representation of a {@code Tree}.
     *              Must not be null and must include both the header and entry data.
     * @return a {@code Tree} object constructed from the provided byte array.
     * @throws NoSuchAlgorithmException if the algorithm used for processing the byte array is unavailable.
     */
    public static Tree fromBytes(byte[] bytes) throws NoSuchAlgorithmException, IllegalArgumentException {
        ByteBuffer buf = ByteBuffer.wrap(bytes);

        // Header
        byte[] headerBytes = new byte[ObjectType.TREE.getObjectHeaderSize()];
        buf.get(headerBytes);
        ObjectStorageHeader header = ObjectStorageHeader.fromBytes(headerBytes);
        if (!header.getType().equals(ObjectType.TREE)) {
            throw new IllegalArgumentException("Not a tree object");
        }

        // Stored Entries
        TreeMap<String, TreeEntry> storedEntryMap = new TreeMap<>();
        Path rootPath = Path.of("");
        while (buf.hasRemaining()) {
            TreeEntry treeEntry = TreeEntry.fromBytes(buf);
            Path treePath = Path.of(treeEntry.getFileName());
            storedEntryMap.put(treePath.toString(), treeEntry);
        }

        return new Tree(storedEntryMap, rootPath);
    }


    /**
     * Recursively lists all the files in a tree structure starting from a given object ID.
     * For each non-directory entry, the method adds an entry to the file map with the
     * corresponding path as the key and the file's data as the value.
     *
     * @param objectID the unique identifier of the root tree from which to start listing files.
     *                 Must not be null and correspond to a valid tree object in the storage.
     * @param parent   the parent path to which the entries are relative. This path is used
     *                 to build full paths for each entry in the tree.
     * @param fileMap  a map that stores the resulting file paths and their associated data
     *                 entries. The keys are the full paths of the files, and the values
     *                 contain information about the files such as mode and object ID.
     *                 This map will be populated with the listed files.
     * @throws IOException              if an I/O error occurs while processing the tree or its entries.
     * @throws NoSuchAlgorithmException if the algorithm required to handle the object ID or entries is unavailable.
     * @throws IllegalArgumentException if the provided object ID is invalid or any unexpected
     *                                  argument is encountered.
     */
    public static void listAllFiles(ObjectID objectID, Path parent, Map<Path, DataEntry> fileMap) throws IOException, NoSuchAlgorithmException, IllegalArgumentException {
        Tree tree = Tree.loadTree(objectID);
        TreeMap<String, TreeEntry> storedEntryMap = tree.getStoredEntries();
        for (TreeEntry treeEntry : storedEntryMap.values()) {
            Path currentPath = parent.resolve(Path.of(treeEntry.getFileName()));
            // If the entry is not a tree, put it into the file map
            if (!treeEntry.getMode().equals(FileMode.DIRECTORY)) {
                fileMap.put(currentPath, new DataEntry(
                        treeEntry.getMode()
                        , treeEntry.getOid()
                        , currentPath
                ));
            } else {
                listAllFiles(treeEntry.getOid(), currentPath, fileMap);
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Tree tree)) return false;
        return deepEquals(tree);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entries, subTrees);
    }

    /**
     * Builds a tree structure by normalizing the given map of paths and index entries.
     * The method processes the provided map to create file entries and subtree structures,
     * grouping paths based on their directory hierarchy.
     *
     * @param normalizedEntryPath a map containing paths as keys and their associated index entries as values.
     *                            The paths are expected to be normalized relative paths.
     * @return a Tree object representing the hierarchical structure of the given paths and index entries.
     */
    private static Tree buildWithNormalize(Path currentPath, Map<IndexKey, IndexEntry> normalizedEntryPath) {
        List<TreeEntry> files = new ArrayList<>();
        Map<String, Map<IndexKey, IndexEntry>> children = new TreeMap<>();

        for (Map.Entry<IndexKey, IndexEntry> e : normalizedEntryPath.entrySet()) {
            Path path = e.getKey().getPath();

            if (path.getNameCount() == 1) {

                files.add(new TreeEntry(
                        path.getFileName().toString()
                        , new ObjectID(e.getValue().getOid())
                        , FileMode.REGULAR_FILE
                ));
            } else {
                String dir = path.getName(0).toString();
                Path restPath = path.subpath(1, path.getNameCount());

                children
                        .computeIfAbsent(dir, k -> new TreeMap<>())
                        .put(new IndexKey(restPath, e.getKey().getStage()), e.getValue());
            }
        }

        List<Tree> subTrees = new ArrayList<>();
        for (Map.Entry<String, Map<IndexKey, IndexEntry>> child : children.entrySet()) {
            Path childPath = currentPath.resolve(child.getKey());
            Tree subTree = buildWithNormalize(childPath, child.getValue());
            subTrees.add(subTree);
        }

        return new Tree(files, subTrees, currentPath);
    }

    /**
     * Normalizes the paths in the given map by converting them to relative paths
     * with respect to the workspace root path.
     *
     * @param map a map containing absolute paths as keys and their associated index entries as values
     * @return a new map with keys as relative paths to the workspace root and their original index entries as values
     */
    private static Map<IndexKey, IndexEntry> normalizePath(Map<IndexKey, IndexEntry> map) {
        Map<IndexKey, IndexEntry> relativeMap = new TreeMap<>();
        for (Map.Entry<IndexKey, IndexEntry> e : map.entrySet()) {
            Path indexPath = Workspace.getInstance()
                    .getRootPath()
                    .relativize(e.getKey().getPath());

            IndexKey indexKey = new IndexKey(indexPath, e.getKey().getStage());
            relativeMap.put(indexKey, e.getValue());
        }
        return relativeMap;
    }

    private static Map<IndexKey, IndexEntry> filterNormalStage(Map<IndexKey, IndexEntry> indexEntries) {
        Map<IndexKey, IndexEntry> result = new TreeMap<>();
        for (Map.Entry<IndexKey, IndexEntry> e : indexEntries.entrySet()) {
            if (e.getKey().getStage() == StagEnum.STAGE_NORMAL.toValue()) {
                result.put(e.getKey(), e.getValue());
            }
        }
        return result;
    }

    /**
     * Compares this tree with another tree to check for deep structural equality.
     * The comparison includes the entries and the hierarchical structure of the subtrees.
     *
     * @param other the other tree to compare with this tree. Can be null.
     * @return true if this tree and the other tree are deeply equal, false otherwise.
     */
    private boolean deepEquals(Tree other) {
        if (other == null) return false;
        if (this == other) return true;

        // compare files
        if (!entries.equals(other.entries)) {
            return false;
        }

        // compare subtree count
        if (subTrees.size() != other.subTrees.size()) {
            return false;
        }

        // compare subtrees recursively
        for (int i = 0; i < subTrees.size(); i++) {
            if (!subTrees.get(i).deepEquals(other.subTrees.get(i))) {
                return false;
            }
        }

        return true;
    }

}
