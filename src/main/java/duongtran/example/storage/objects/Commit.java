package duongtran.example.storage.objects;

import duongtran.example.storage.CommitAuthor;
import duongtran.example.storage.ObjectStorage;
import duongtran.example.storage.ObjectType;

import java.nio.charset.StandardCharsets;

public class Commit extends ObjectStorage {

    private final CommitAuthor author;
    private final Tree tree;
    private final String message;
    private final String parentId;

    public Commit(CommitAuthor author, Tree tree, String message, String parentId) {
        this.author = author;
        this.tree = tree;
        this.message = message;
        this.parentId = parentId;
    }

    @Override
    protected byte[] toBytes() {
        StringBuilder bodyBuilder = new StringBuilder();
        bodyBuilder.append("tree ").append(tree.getOid()).append("\n");
        if (parentId != null) {
            bodyBuilder.append("parent ").append(parentId).append("\n");
        }
        bodyBuilder.append("author ").append(author.toString()).append("\n");
        bodyBuilder.append("committer ").append(author.toString()).append("\n");
        bodyBuilder.append("\n");
        bodyBuilder.append(message);

        return bodyBuilder.toString().getBytes(StandardCharsets.ISO_8859_1);
    }

    @Override
    public ObjectType getType() {
        return ObjectType.COMMIT;
    }
}
