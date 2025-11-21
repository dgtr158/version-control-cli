package duongtran.vctrl.index;

import duongtran.vctrl.storage.ObjectStorage;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

public class IndexTest {

    private static final Logger log = LoggerFactory.getLogger(IndexTest.class);

    @Test
    void testSerializeDeserialize() {

        int numEntries = 2;
        Instant now = Instant.now();
        Map<Path, IndexEntry> entryMap = createIndexEntryMap(numEntries, now);

        // Get the size of the index
        int size = IndexHeader.HEADER_SIZE;
        for (Map.Entry<Path, IndexEntry> entry : entryMap.entrySet()) {
            size += entry.getValue().getSize();
        }
        size += ObjectStorage.OID_SIZE;

        // Create the expected index
        Index expected = new Index();
        expected.setEntryMap(entryMap);
        expected.setSizeInBytes(size);

        ByteBuffer buf = ByteBuffer.allocate(expected.getSizeInBytes());
        try {
            expected.toBytes(buf);
            buf.flip();

            Index actual = Index.fromBytes(buf);

            // The buffer is exhausted
            assertFalse(buf.hasRemaining());

            // Index's entry must be ascending order of its path
            List<Path> keys = new ArrayList<>(actual.getEntryMap().keySet());
            List<Path> sorted = new ArrayList<>(keys);
            sorted.sort(null);
            assertEquals(sorted, keys);

            // serialized object and deserialized object are identical
            assertEquals(expected, actual);

        } catch (NoSuchAlgorithmException ex) {
            log.error("Error when creating object ID: {}" , ex.getMessage());
            fail();
        } catch (Exception ex) {
            log.error("Error when convert the index into bytes: {}" , ex.getMessage());
            fail();
        }

    }

    private Map<Path, IndexEntry> createIndexEntryMap(int num, Instant time) {
        Map<Path, IndexEntry> indexEntries = new TreeMap<>();
        for (int i = 0; i < num; i++) {
            IndexEntry entry = createIndexEntry(time, i + 1);
            indexEntries.put(Paths.get(entry.getPath()), entry);
        }
        return indexEntries;
    }

    private IndexEntry createIndexEntry(Instant time, int i) {
        int ctimeSeconds = (int) time.getEpochSecond();
        int ctimeNanos = time.getNano();
        int mtimeSeconds  = (int) time.getEpochSecond();
        int mtimeNanos = time.getNano();
        int dev = 2048;
        int ino = 62000161;
        int mode = Index.REGULAR_MODE;
        int uid = 1000;
        int gid = 1000;
        int fileSize = 1024;
        String oid = "e69de29bb2d1d6434b8b29ae775ad8c2e48c5391";
        int flags = 0x0A5B; // 16-bit
        String path = "test" + i + ".txt";

        return new IndexEntry(
                ctimeSeconds,ctimeNanos
                ,mtimeSeconds,mtimeNanos,dev,ino
                ,mode,uid,gid,fileSize
                ,oid,flags,path
        );
    }



}
