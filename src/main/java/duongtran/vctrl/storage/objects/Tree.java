package duongtran.vctrl.storage.objects;

import duongtran.vctrl.Workspace;
import duongtran.vctrl.storage.Database;
import duongtran.vctrl.storage.ObjectStorage;
import duongtran.vctrl.storage.ObjectType;
import duongtran.vctrl.utils.DirectoryNames;
import duongtran.vctrl.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Tree extends ObjectStorage {
    private final List<TreeEntry> entries;

    public Tree(List<TreeEntry> entries) {
        this.entries = entries;
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
     * Build a tree rooted at the path
     *
     * @param curPath the root path of the tree
     * @param database the database to store a blob object
     * @return tree object id
     */
    public static Tree buildTree(Path curPath, Database database) throws IOException, NoSuchAlgorithmException {
        Path rootPath = Workspace.getInstance().getRootPath();
        Path vctrlPath = rootPath.resolve(".vctrl");
        List<Path> paths = Files.list(curPath)
                .filter(path -> !path.equals(vctrlPath))
                .filter(path -> !path.getFileName().toString().contains(File.separator + DirectoryNames.ROOT_DIR_NAME))
                .sorted()
                .toList();
        List<TreeEntry> treeEntryList = new ArrayList<>();
        for (Path p : paths) {
            if (Files.isDirectory(p)) {
                Tree subTree = buildTree(p, database);
                treeEntryList.add(
                        new TreeEntry(p.getFileName().toString(), subTree.getOid(), false)
                );
            } else {
                Blob blob = new Blob(Files.readAllBytes(p));
                String blobId = database.store(blob);
                treeEntryList.add(
                        new TreeEntry(p.getFileName().toString(), blobId, Files.isExecutable(p))
                );
            }
        }

        // Store the tree and return its object ID
        Tree tree = new Tree(treeEntryList);
        database.store(tree);

        return tree;
    }

}
