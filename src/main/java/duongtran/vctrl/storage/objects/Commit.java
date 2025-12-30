package duongtran.vctrl.storage.objects;

import duongtran.vctrl.storage.*;
import duongtran.vctrl.utils.Utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a commit object in a version control system. A commit includes
 * information about the author, associated tree object, a message, and optionally
 * a parent commit.
 */
public class Commit extends ObjectStorage {

    private final CommitAuthor author;
    private final ObjectID treeOid;
    private final String message;
    private final List<ObjectID> parentIds;

    public Commit(CommitAuthor author, ObjectID treeOid, String message, List<ObjectID> parentIds) {
        this.author = author;
        this.treeOid = treeOid;
        this.message = message;
        this.parentIds = parentIds;
    }

    public CommitAuthor getAuthor() {
        return author;
    }

    public ObjectID getTreeOid() {
        return treeOid;
    }

    public String getMessage() {
        return message;
    }

    public List<ObjectID> getParentIds() {
        return parentIds;
    }

    @Override
    public byte[] getContent() {
        StringBuilder bodyBuilder = new StringBuilder();
        bodyBuilder.append("tree ").append(treeOid.getValue()).append("\n");
        for (ObjectID parentId : parentIds) {
            bodyBuilder.append("parent ").append(parentId.getValue()).append("\n");
        }
        bodyBuilder.append("author ").append(author.toString()).append("\n");
        bodyBuilder.append("committer ").append(author.toString()).append("\n");
        bodyBuilder.append("\n");
        bodyBuilder.append(message);

        return bodyBuilder.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Loads and returns a {@link Commit} object corresponding to the given {@code ObjectID}.
     *
     * @param oid the identifier of the commit object to be loaded
     * @return the {@link Commit} object associated with the specified {@code ObjectID}
     * @throws IOException if there is an I/O error while retrieving the commit object
     * @throws NoSuchAlgorithmException if the required algorithm for object retrieval is not available
     */
    public static Commit loadCommit(ObjectID oid) throws IOException, NoSuchAlgorithmException {
        Database database = Database.getInstance();
        return (Commit) database.loadObject(oid, ObjectType.COMMIT);
    }

    /**
     * Constructs a {@code Commit} object by parsing its serialized byte array representation.
     *
     * The input byte array is expected to contain the serialized representation of a commit
     * object, including its headers and message. The method validates the necessary fields,
     * such as the tree object ID and author, and throws an exception if they are missing or invalid.
     *
     * @param bytes the byte array containing the serialized commit object
     * @return a {@code Commit} instance representing the details parsed from the input byte array
     * @throws IllegalArgumentException if the byte array is incomplete, or the commit object
     *                                  is missing required fields
     */
    public static Commit fromBytes(byte[] bytes) {
        ByteBuffer buf = ByteBuffer.wrap(bytes);

        // 1. Parse header
        byte[] headerBytes = new byte[ObjectType.COMMIT.getObjectHeaderSize()];
        buf.get(headerBytes);
        ObjectStorageHeader.fromBytes(headerBytes);

        // 2. Remaining bytes = commit content
        byte[] contentBytes = new byte[buf.remaining()];
        buf.get(contentBytes);
        String content = new String(contentBytes, StandardCharsets.UTF_8);

        // 3. Split headers and message
        String[] parts = content.split("\n\n", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid commit object: missing message separator");
        }

        String headerPart = parts[0];
        String message = parts[1];

        String treeOid = null;
        List<ObjectID> parentIds = new ArrayList<>();
        CommitAuthor author = null;

        // 4. Parse header lines
        for (String line : headerPart.split("\n")) {
            if (line.startsWith("tree ")) {
                treeOid = line.substring(5);
            } else if (line.startsWith("parent ")) {
                ObjectID parentId = new ObjectID(line.substring(7));
                parentIds.add(parentId);
            } else if (line.startsWith("author ")) {
                author = CommitAuthor.fromString(line.substring(7));
            }
        }

        if (treeOid == null || author == null) {
            throw new IllegalArgumentException("Invalid commit object: missing required fields");
        }

        return new Commit(
                author,
                new ObjectID(treeOid),
                message,
                parentIds
        );
    }

    @Override
    public ObjectType getType() {
        return ObjectType.COMMIT;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Commit commit)) return false;
        return Objects.equals(author, commit.author) && Objects.equals(treeOid, commit.treeOid) && Objects.equals(message, commit.message) && Utils.listsEqual(parentIds, commit.parentIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(author, treeOid, message, parentIds);
    }
}
