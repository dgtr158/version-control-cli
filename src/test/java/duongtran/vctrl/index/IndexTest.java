package duongtran.vctrl.index;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IndexTest {

    @Test
    void testSerializeDeserialize() {

        int numEntries = 2;
        Instant now = Instant.now();
        Map<Path, IndexEntry> indexEntries = createIndexEntryMap(numEntries, now);

        int size = IndexHeader.HEADER_SIZE;


    }

    private Map<Path, IndexEntry> createIndexEntryMap(int num, Instant time) {
        Map<Path, IndexEntry> indexEntries = new HashMap<>();
        for (int i = 0; i < num; i++) {
            IndexEntry entry = createIndexEntry(time);
            indexEntries.put(Paths.get(entry.getPath()), entry);
        }
        return indexEntries;
    }

    private IndexEntry createIndexEntry(Instant time) {
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
        String path = "test.txt";

        return new IndexEntry(
                ctimeSeconds
                ,ctimeNanos
                ,mtimeSeconds
                ,mtimeNanos
                ,dev
                ,ino
                ,mode
                ,uid
                ,gid
                ,fileSize
                ,oid
                ,flags
                ,path
        );
    }



}
