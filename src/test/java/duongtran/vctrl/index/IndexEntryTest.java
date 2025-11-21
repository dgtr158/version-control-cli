package duongtran.vctrl.index;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class IndexEntryTest {

    @Test
    void testSerializeDeserialize() throws Exception {

        Instant now = Instant.now();
        int ctimeSeconds = (int) now.getEpochSecond();
        int ctimeNanos = now.getNano();
        int mtimeSeconds  = (int) now.getEpochSecond();
        int mtimeNanos = now.getNano();
        int dev = 2048;
        int ino = 62000161;
        int mode = Index.REGULAR_MODE;
        int uid = 1000;
        int gid = 1000;
        int fileSize = 1024;
        String oid = "e69de29bb2d1d6434b8b29ae775ad8c2e48c5391";
        int flags = 0x0A5B; // 16-bit
        String path = "test.txt";

        IndexEntry expected = new IndexEntry(
                ctimeSeconds,ctimeNanos
                ,mtimeSeconds,mtimeNanos
                ,dev,ino,mode,uid
                ,gid,fileSize
                ,oid,flags,path
        );
        int expectedSize = 72;

        ByteBuffer buffer = ByteBuffer.allocate(expected.getSize());
        expected.toBytes(buffer);
        buffer.flip();

        IndexEntry actual = IndexEntry.fromBytes(buffer);
        assertFalse(buffer.hasRemaining());
        assertEquals(expectedSize, actual.getSize());
        assertEquals(expected, actual);

    }

}
