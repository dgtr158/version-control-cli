package duongtran.vctrl.storage.objects;

import duongtran.vctrl.storage.CommitAuthor;
import duongtran.vctrl.storage.ObjectID;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CommitTest {

    @Test
    void testCommitSerializeDeserialize() throws Exception {
        Instant time = Instant.ofEpochSecond(1716030000);
        CommitAuthor author = new CommitAuthor(
                "Alice",
                "alice@example.com",
                time
        );

        ObjectID treeId = new ObjectID("0123456789abcdef0123456789abcdef01234567");
        String parentId = "abcdefabcdefabcdefabcdefabcdefabcdefabcd";
        String message = "Initial commit";

        Commit original = new Commit(author, treeId, message, parentId);

        // Serialize (header + content)
        byte[] raw = original.toBytes();

        // Act
        Commit parsed = Commit.fromBytes(raw);

        // Assert
        assertNotNull(parsed);

        assertEquals(treeId.getValue(), parsed.getTreeOid().getValue());
        assertEquals(parentId, parsed.getParentId());
        assertEquals(message, parsed.getMessage());

        CommitAuthor parsedAuthor = parsed.getAuthor();
        assertEquals(author.getName(), parsedAuthor.getName());
        assertEquals(author.getEmail(), parsedAuthor.getEmail());
        assertEquals(author.getTime(), parsedAuthor.getTime());
    }
}
