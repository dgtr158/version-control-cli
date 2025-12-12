package duongtran.vctrl.index;

import duongtran.vctrl.TestUtils;
import duongtran.vctrl.actions.AddAction;
import duongtran.vctrl.Workspace;
import duongtran.vctrl.storage.Database;
import duongtran.vctrl.storage.ObjectStorage;
import duongtran.vctrl.utils.DirectoryNames;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class IndexTest {

    private static final Logger log = LoggerFactory.getLogger(IndexTest.class);

    // Vctrl instances
    Workspace workspace;
    Database database;
    Path rootPath;
    Path indexPath;

    // Mock directories
    Path testFile11;
    Path testFile12;
    Path testFile21;
    Path firstDir;
    Path secondDir;


    @BeforeEach
    void setup() throws IOException {
        TestUtils.createTestWorkspace();
        workspace = Workspace.getInstance();
        database = Database.getInstance();

        // Root path
        rootPath = workspace.getRootPath();
        indexPath = rootPath.resolve(DirectoryNames.INDEX);

        // Initialize test directories and files
        firstDir = rootPath.resolve("firstDir");
        secondDir = rootPath.resolve("secondDir");

        testFile11 = firstDir.resolve("file11.txt");
        testFile12 = firstDir.resolve("file12.txt");
        testFile21 = secondDir.resolve("file21.txt");

        // Create folders
        Files.createDirectories(firstDir);
        Files.createDirectories(secondDir);

    }

    @AfterEach
    void tearDown() {
        TestUtils.removeWorkspace();
        workspace = null;
    }

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
            assertTrue(actual.isChanged());

            // serialized object and deserialized object are identical
            assertEquals(expected, actual);

        } catch (NoSuchAlgorithmException ex) {
            log.error("Error when creating object ID: {}", ex.getMessage());
            fail();
        } catch (Exception ex) {
            log.error("Error when convert the index into bytes: {}", ex.getMessage());
            fail();
        }

    }

    @Test
    void testLoadFromDiskSuccessfully() {

        AddAction addAction;

        try {

            // Create files in the firstDir
            TestUtils.writeText(testFile11, "Test content 11");
            TestUtils.writeText(testFile12, "Test content 12");

            // Create a file in the secondDir
            TestUtils.writeText(testFile21, "Test content 21");

            // Execute action
            addAction = new AddAction();
            addAction.execute(rootPath);

            // Validate that index contains the correct entries
            Path indexPath = rootPath.resolve(DirectoryNames.INDEX);
            assertTrue(Files.exists(indexPath));

            // Load index from disk
            Index actual = Index.loadFromDisk();

            // Validate entries
            List<Path> expectedEntries = Arrays.asList(
                    testFile11
                    ,testFile12
                    ,testFile21
            );
            assertEquals(3, actual.getHeader().getEntryCount());
            Map<Path, IndexEntry> entryMap = actual.getEntryMap();
            List<Path> actualEntries = entryMap.keySet().stream().toList();
            assertIterableEquals(expectedEntries, actualEntries);

        } catch (Exception e) {
            log.error("Failed to load index from disk");
            fail();
        }
    }

    @Test
    void testLoadFromDiskFailedChecksum() {

        AddAction addAction;

        try {

            // Create files in the firstDir
            TestUtils.writeText(testFile11, "Test content 11");
            TestUtils.writeText(testFile12, "Test content 12");

            // Create a file in the secondDir
            TestUtils.writeText(testFile21, "Test content 21");

            // Execute action
            addAction = new AddAction();
            addAction.execute(rootPath);

            // Append some content into the index file
            String newContent = "new content for checksum fail";
            Files.writeString(
                    indexPath
                    , newContent
                    , StandardOpenOption.CREATE
                    , StandardOpenOption.APPEND
            );

            // Execute the load the index file from disk
            Index.loadFromDisk();

            log.error("Checksum failed, need to throw Exception");
            fail();

        } catch (Exception ex) {
            assertEquals("Failed to load index file, checksum failed", ex.getMessage());
        }

    }

    @Test
    void testIndexNotChange() {

        AddAction addAction;
        Index index;

        try {

            // Create files in the firstDir
            TestUtils.writeText(testFile11, "Test content 11");

            // Create a file in the secondDir
            TestUtils.writeText(testFile21, "Test content 21");

            // Execute action
            addAction = new AddAction();
            index = addAction.execute(rootPath);

            // Add the same index entry
            index = addAction.execute(testFile11);
            assertFalse(index.isChanged());

            // Add a new index entry n times
            int n = 10;
            for (int i = 0; i < n; i++) {
                TestUtils.writeText(testFile12, "Test content 12");
                index = addAction.execute(testFile12);
                assertTrue(index.isChanged());
            }

            // Modify a file, then add again
            TestUtils.writeText(testFile12, "Test content 12 Overwrite");
            index = addAction.execute(testFile12);
            assertTrue(index.isChanged());

        } catch (Exception e) {
            log.error("Failed to load index from disk");
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
        int mtimeSeconds = (int) time.getEpochSecond();
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
        int size = 72;

        return new IndexEntry(
                ctimeSeconds, ctimeNanos
                , mtimeSeconds, mtimeNanos, dev, ino
                , mode, uid, gid, fileSize
                , oid, flags, path, size
        );
    }


}
