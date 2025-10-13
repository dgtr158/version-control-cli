package duongtran.example.storage.objects;

import duongtran.example.metadata.Workspace;
import duongtran.example.storage.Database;
import duongtran.example.storage.ObjectStorage;
import duongtran.example.storage.ObjectType;
import duongtran.example.utils.DirectoryNames;
import duongtran.example.utils.HexUtil;

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
    private final List<Entry> entries;

    public Tree(List<Entry> entries) {
        this.entries = entries;
        entries.sort(Comparator.comparing(Entry::getName));
    }

    public ObjectType getType() {
        return ObjectType.TREE;
    }

    @Override
    protected byte[] toBytes() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            for (Entry entry : entries) {
                String entryHeader = String.format("%s %s\0", entry.getMode(), entry.getName());
                byte[] entryData = entryHeader.getBytes(StandardCharsets.ISO_8859_1);
                out.write(entryData);

                byte[] objectID = HexUtil.hexStringToByteArray(entry.getOid());
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
        List<Entry> entryList = new ArrayList<>();
        for (Path p : paths) {
            if (Files.isDirectory(p)) {
                Tree subTree = buildTree(p, database);
                entryList.add(
                        new Entry(p.getFileName().toString(), subTree.getOid(), false)
                );
            } else {
                Blob blob = new Blob(Files.readAllBytes(p));
                String blobId = database.store(blob);
                entryList.add(
                        new Entry(p.getFileName().toString(), blobId, Files.isExecutable(p))
                );
            }
        }

        // Store the tree and return its object ID
        Tree tree = new Tree(entryList);
        database.store(tree);

        return tree;
    }

}
