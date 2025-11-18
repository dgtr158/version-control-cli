package duongtran.vctrl.index;

import java.nio.ByteBuffer;

import duongtran.vctrl.concurrency.Lockfile;
import duongtran.vctrl.storage.ObjectStorage;
import duongtran.vctrl.utils.DirectoryNames;
import duongtran.vctrl.utils.FileUtil;
import duongtran.vctrl.utils.HexUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class Index {

    private static final Logger log = LoggerFactory.getLogger(Index.class);

    public static final int REGULAR_MODE = 0100644;   // normal file mode
    public static final int EXECUTABLE_MODE = 0100755; // executable file mode
    public static final int MAX_PATH_SIZE = 0xfff;
    public static final int VERSION = 2;

    private final Path indexPath;

    private int sizeInBytes;
    private Map<Path, IndexEntry> entryMap;
    private String indexId;

    public Index() {
        File gitPath = new File(DirectoryNames.WORKING_DIRECTORY, DirectoryNames.ROOT_DIR_NAME);
        File indexPath = new File(gitPath, DirectoryNames.INDEX);
        this.indexPath = indexPath.toPath();
        this.entryMap = new HashMap<>();
        this.sizeInBytes = IndexHeader.HEADER_SIZE + ObjectStorage.OID_SIZE;
    }

    public Path getIndexPath() {
        return indexPath;
    }

    public int getSizeInBytes() {
        return sizeInBytes;
    }

    public Map<Path, IndexEntry> getEntryMap() {
        return entryMap;
    }

    public String getIndexId() {
        return indexId;
    }

    public void setSizeInBytes(int sizeInBytes) {
        this.sizeInBytes = sizeInBytes;
    }

    public void setEntryMap(Map<Path, IndexEntry> entryMap) {
        this.entryMap = entryMap;
    }

    public void setIndexId(String indexId) {
        if (this.indexId != null) {
            this.indexId = indexId;
        }
    }

    public void addEntry(Path path, String blobId) throws IOException {
        FileStat stat;
        if (FileUtil.isWindows()) stat = new WindowFileStat(path);
        else stat = new UnixFileStat(path);

        IndexEntry entry = createIndexEntry(path, blobId, stat);
        entryMap.put(path, entry);
        sizeInBytes += entry.getSizeInBytes();

    }

    private IndexEntry createIndexEntry(Path path, String blobId, FileStat stat) {
        /*
            TODO: modify flags 16-bit
                16-bit flags (high to low) contains:
                   1-bit assume-valid flag (0 in version2)
                   1-bit extended flag (0 in version 2)
                   2-bit stage (0-normal, 1-ours, 2-theirs, 3-base)
                   12-bit name length MIN(actual_path_length.countBytes(), 0xFFF)
         */
        short flags = (short) Math.min(path.toString().getBytes(StandardCharsets.UTF_8).length, MAX_PATH_SIZE);
        return new IndexEntry(
                stat.getCtimeSeconds()
                , stat.getCtimeNanos()
                , stat.getMtimeSeconds()
                , stat.getMtimeNanos()
                , stat.getDev()
                , stat.getIno()
                , stat.getMode()
                , stat.getUid()
                , stat.getGid()
                , stat.getSize()
                , blobId
                , flags
                , path.toString()
        );
    }

    public void write() {
        try (Lockfile out = new Lockfile(this.indexPath)) {

            // Convert index file into bytes
            byte[] bytes = new byte[sizeInBytes];
            ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
            toBytes(buf);

            // Change buffer to write mode
            buf.flip();

            // Flush content into disk
            int writtenBytes = out.write(buf);
            log.info("Written bytes: {}, Total bytes: {}", writtenBytes, sizeInBytes);

        } catch (NoSuchAlgorithmException e) {
            log.error("Cannot create index object ID: {}\n", e.getMessage());
        } catch (IOException e) {
            log.warn("Failed to write to file: {}\n Retry later", e.getMessage());
        }

    }

    public void toBytes(ByteBuffer buf) throws NoSuchAlgorithmException {
        // Header
        IndexHeader header = new IndexHeader(VERSION, this.entryMap.size());
        header.toBytes(buf);

        // Index Entries
        for (Map.Entry<Path, IndexEntry> mapEntry : this.entryMap.entrySet()) {
            IndexEntry indexEntry = mapEntry.getValue();
            indexEntry.toBytes(buf);
        }

        // Calculate index's sha-1 hash
        int dataLen = buf.position();
        byte[] hashInput = new byte[dataLen];
        buf.rewind();
        buf.get(hashInput, 0, dataLen);
        String hashID = calculateOid(hashInput);
        byte[] checksum = HexUtil.hexStringToByteArray(hashID);
        buf.put(checksum);

        // Update the index's ID
        this.indexId = hashID;
    }

    public static Index fromBytes(ByteBuffer buf) throws Exception {
        int size = 0;
        Map<Path, IndexEntry> entryMap = new HashMap<>();
        Index index = new Index();

        // Header
        IndexHeader indexHeader = IndexHeader.fromBytes(buf);
        size += IndexHeader.HEADER_SIZE;

        // Entries
        int numEntries = indexHeader.getEntryCount();
        for (int i = 0; i < numEntries; i++) {
            // Create index's entries
            IndexEntry indexEntry = IndexEntry.fromBytes(buf);
            Path path = Paths.get(indexEntry.getPath());
            entryMap.put(path, indexEntry);

            // Update index's size
            size += indexEntry.getSizeInBytes();
        }

        // Index's ID
        byte[] idBytes = new byte[20];
        buf.get(idBytes);
        String id = HexUtil.bytesToHex(idBytes);

        // Set index's attributes
        index.setEntryMap(entryMap);
        index.setSizeInBytes(size);
        index.setIndexId(id);

        return index;
    }

    /**
     * Calculates the object ID using SHA-1 hash.
     *
     * @return SHA1 hash ID
     * @throws NoSuchAlgorithmException If SHA-1 is not available
     */
    private String calculateOid(byte[] content) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(ObjectStorage.HASH_ALGORITHM);
        return HexUtil.bytesToHex(digest.digest(content));
    }

}
