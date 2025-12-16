package duongtran.vctrl.storage.objects;

import duongtran.vctrl.Workspace;
import duongtran.vctrl.index.IndexEntry;
import duongtran.vctrl.storage.*;
import duongtran.vctrl.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

public class Tree extends ObjectStorage {
    private final List<TreeEntry> entries;
    private final List<Tree> subTrees;

    public Tree(List<TreeEntry> entries, List<Tree> subTrees) {
        this.entries = entries;
        this.subTrees = subTrees;
        entries.sort(Comparator.comparing(TreeEntry::getFileName));
    }

    public ObjectType getType() {
        return ObjectType.TREE;
    }

    /**
     * Converts the tree and its entries into a byte array representation.
     * The representation includes the mode, file name, and object ID of each entry.
     *      <mode> <fileName>\0<oid>
     *
     * @return a byte array containing the serialized tree with its entries.
     * @throws RuntimeException if an I/O error occurs during the conversion process.
     */
    @Override
    protected byte[] getContent() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            for (TreeEntry treeEntry : entries) {
                String entryHeader = String.format("%s %s\0", treeEntry.getMode(), treeEntry.getFileName());
                byte[] entryData = entryHeader.getBytes(StandardCharsets.ISO_8859_1);
                out.write(entryData);

                byte[] objectID = Utils.hexStringToByteArray(treeEntry.getOid());
                out.write(objectID);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return out.toByteArray();
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
     *         paths and index entries.
     */
    public static Tree buildTree(Map<Path, IndexEntry> indexEntries) {
        Map<Path, IndexEntry> relativeEntryPath = normalizePath(indexEntries);
        return buildWithNormalize(relativeEntryPath);
    }

    public static ObjectID store(Tree root, Database database) {
        // TODO
        return null;
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
    private static Tree buildWithNormalize(Map<Path, IndexEntry> normalizedEntryPath) {
        List<TreeEntry> files = new ArrayList<>();
        Map<String, Map<Path, IndexEntry>> children = new TreeMap<>();

        for (Map.Entry<Path, IndexEntry> e : normalizedEntryPath.entrySet()) {
            Path path = e.getKey();

            if (path.getNameCount() == 1) {

                files.add(new TreeEntry(
                        path.getFileName().toString()
                        ,e.getValue().getOid()
                        ,FileMode.REGULAR_FILE
                ));
            } else {
                String dir = path.getName(0).toString();
                Path rest = path.subpath(1, path.getNameCount());

                children
                        .computeIfAbsent(dir, k -> new TreeMap<>())
                        .put(rest, e.getValue());
            }
        }

        List<Tree> subTrees = new ArrayList<>();
        for (Map.Entry<String, Map<Path, IndexEntry>> child : children.entrySet()) {
            Tree subTree = buildWithNormalize(child.getValue());
            subTrees.add(subTree);
        }

        return new Tree(files, subTrees);
    }

    /**
     * Normalizes the paths in the given map by converting them to relative paths
     * with respect to the workspace root path.
     *
     * @param map a map containing absolute paths as keys and their associated index entries as values
     * @return a new map with keys as relative paths to the workspace root and their original index entries as values
     */
    private static Map<Path, IndexEntry> normalizePath(Map<Path, IndexEntry> map) {
        Map<Path, IndexEntry> relativeMap = new TreeMap<>();
        for (Map.Entry<Path, IndexEntry> e : map.entrySet()) {
            relativeMap.put(Workspace.getInstance().getRootPath().relativize(e.getKey()), e.getValue());
        }
        return relativeMap;
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
