package duongtran.vctrl.index;

import duongtran.vctrl.concurrency.Lockfile;
import duongtran.vctrl.metadata.Workspace;
import duongtran.vctrl.storage.ObjectStorage;
import duongtran.vctrl.storage.objects.ObjectID;
import duongtran.vctrl.utils.DirectoryNames;
import duongtran.vctrl.utils.FileUtil;
import duongtran.vctrl.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class Index {

    private static final Logger log = LoggerFactory.getLogger(Index.class);

    public static final int REGULAR_MODE = 0100644;   // normal file mode
    public static final int EXECUTABLE_MODE = 0100755; // executable file mode
    public static final int MAX_PATH_SIZE = 0xfff;
    public static final int VERSION = 2;

    private final Path indexPath;

    private int sizeInBytes;
    private final IndexHeader header;
    private Map<Path, IndexEntry> entryMap;
    private ObjectID indexId;

    public Index() {
        File rootPath = new File(Workspace.getInstance().getRootPath().toString());
        File indexPath = new File(rootPath, DirectoryNames.INDEX);
        this.indexPath = indexPath.toPath();

        this.header = new IndexHeader(VERSION, 0);
        this.entryMap = new TreeMap<>();
        this.sizeInBytes = IndexHeader.HEADER_SIZE + ObjectStorage.OID_SIZE;
    }

    public IndexHeader getHeader() {
        return this.header;
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

    public ObjectID getIndexId() {
        return indexId;
    }

    public void setSizeInBytes(int sizeInBytes) {
        this.sizeInBytes = sizeInBytes;
    }

    public void setEntryMap(Map<Path, IndexEntry> entryMap) {
        this.entryMap = entryMap;
        this.header.setEntryCount(entryMap.size());
    }

    public void setIndexId(ObjectID indexId) {
        if (this.indexId == null) {
            this.indexId = indexId;
        }
    }

    public void addEntry(Path path, String blobId) throws IOException {
        // Get file stat
        FileStat stat;
        if (FileUtil.isWindows()) stat = new WindowFileStat(path);
        else stat = new UnixFileStat(path);

        // Create the index entry
        IndexEntry entry = createIndexEntry(path, blobId, stat);

        // If the new entry is already in the index, do nothing
        if (!isChanged(entry)) return;

        entryMap.put(path, entry);
        sizeInBytes += entry.getSize();
        this.header.incrementEntryCount();

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
        this.header.toBytes(buf);

        // Index Entries
        for (Map.Entry<Path, IndexEntry> mapEntry : this.entryMap.entrySet()) {
            IndexEntry indexEntry = mapEntry.getValue();
            indexEntry.toBytes(buf);
        }

        // Calculate index's sha-1 hash
        ObjectID objectID = computeChecksum(buf);

        // Update the index's ID
        this.indexId = new ObjectID(objectID.getValue());
    }

    private ObjectID computeChecksum(ByteBuffer buf) throws NoSuchAlgorithmException {
        int dataLen = buf.position();
        byte[] hashInput = new byte[dataLen];
        buf.rewind();
        buf.get(hashInput, 0, dataLen);
        ObjectID objectID = ObjectID.fromBytes(hashInput);
        byte[] checksum = Utils.hexStringToByteArray(objectID.getValue());
        buf.put(checksum);
        return objectID;
    }

    public static Index fromBytes(ByteBuffer buf) {
        int size = 0;
        Map<Path, IndexEntry> entryMap = new TreeMap<>();
        Index index = new Index();

        // Header
        IndexHeader indexHeader = IndexHeader.fromBytes(buf);
        size += IndexHeader.HEADER_SIZE;

        // Entries
        int numEntries = indexHeader.getEntryCount();
        for (int i = 0; i < numEntries; i++) {
            // Create index's entries
            IndexEntry indexEntry = IndexEntry.fromBytes(buf);
            Path path = Paths.get(indexEntry.getPath()).normalize();
            entryMap.put(path, indexEntry);

            // Update index's size
            size += indexEntry.getSize();
        }

        // Index's ID
        ObjectID id = ObjectID.fromBytes(buf);
        size += ObjectID.SIZE_IN_BYTES;

        // Set index's attributes
        index.setEntryMap(entryMap);
        index.setSizeInBytes(size);
        index.setIndexId(id);

        return index;
    }

    /**
     * Load the index from the disk.
     *
     * @return the loaded index
     */
    public static Index loadFromDisk() throws IOException, NoSuchAlgorithmException {
        Path indexPath = Workspace.getInstance().getRootPath().resolve(DirectoryNames.INDEX);
        try (Lockfile in = new Lockfile(indexPath)) {
            in.acquire();
            byte[] indexAllBytes = in.read(indexPath);
            in.close();
            
            // Verify checksum
            int separator = indexAllBytes.length - ObjectID.SIZE_IN_BYTES;
            byte[] contentBytes = Arrays.copyOfRange(indexAllBytes, 0, separator);
            byte[] checksumBytes = Arrays.copyOfRange(indexAllBytes, separator, indexAllBytes.length);

            ObjectID content = ObjectID.fromBytes(contentBytes);
            ObjectID checksum = ObjectID.fromBytes(ByteBuffer.wrap(checksumBytes));
            if (!content.equals(checksum)) {
                throw new IOException("Failed to load index file, checksum failed");
            }
            ByteBuffer byteBuffer = ByteBuffer.wrap(indexAllBytes);
            return Index.fromBytes(byteBuffer);
        }
    }

    /**
     * Checks if the given {@code IndexEntry} is already present in the index and is identical
     * to the corresponding entry.
     *
     * @param entry the {@code IndexEntry} to be checked
     * @return {@code true} if the entry exists and is identical to the given entry;
     *         {@code false} otherwise
     */
    private boolean isChanged(IndexEntry entry) {
        Path entryPath = Paths.get(entry.getPath());
        IndexEntry existingEntry = entryMap.get(entryPath);

        if (existingEntry == null) return true;
        return !existingEntry.equals(entry);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Index index = (Index) o;
        return sizeInBytes == index.sizeInBytes && Objects.equals(indexPath, index.indexPath) && Utils.mapsEqual(entryMap, index.entryMap) && Objects.equals(indexId, index.indexId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(indexPath, sizeInBytes, entryMap, indexId);
    }

}
