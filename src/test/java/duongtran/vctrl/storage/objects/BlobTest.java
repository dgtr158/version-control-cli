package duongtran.vctrl.storage.objects;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BlobTest {

    @Test
    void testSerializeDeserialize() {
        String content = "The content of a Blob object";
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        Blob expected = new Blob(contentBytes);

        Blob actual = Blob.fromBytes(expected.toBytes());
        assertEquals(expected, actual);

    }

}
