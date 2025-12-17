package duongtran.vctrl.storage.objects;

import duongtran.vctrl.storage.FileMode;
import duongtran.vctrl.storage.ObjectID;
import duongtran.vctrl.storage.ObjectStorage;
import duongtran.vctrl.utils.Utils;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TreeEntryTest {

    @Test
    void testSerializeDeserialize() throws NoSuchAlgorithmException {

        String content = "Test Content";
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);

        MessageDigest digest = MessageDigest.getInstance(ObjectStorage.HASH_ALGORITHM);
        ObjectID objectID = new ObjectID(Utils.bytesToHex(digest.digest(contentBytes)));

        TreeEntry expected = new TreeEntry(
                "TestTreeEntry"
                , objectID
                , FileMode.REGULAR_FILE
        );

        byte[] bytes = expected.toBytes();
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        TreeEntry actual = TreeEntry.fromBytes(buf);
        assertEquals(expected, actual);

    }

}
