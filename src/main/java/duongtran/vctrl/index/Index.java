package duongtran.vctrl.index;

import duongtran.vctrl.concurrency.Lockfile;
import duongtran.vctrl.Workspace;
import duongtran.vctrl.storage.ObjectStorage;
import duongtran.vctrl.storage.objects.ObjectID;
import duongtran.vctrl.utils.DirectoryNames;
import duongtran.vctrl.utils.FileUtil;
import duongtran.vctrl.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
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

public class Index implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(Index.class);

    public static final int REGULAR_MODE = 0100644;   // normal file mode
    public static final int EXECUTABLE_MODE = 0100755; // executable file mode
    public static final int MAX_PATH_SIZE = 0xfff;
    public static final int VERSION = 2;

    private transient final Path indexPath;
    private transient int sizeInBytes;
    private transient boolean isChanged;

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
        this.isChanged = false;
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

    public boolean isChanged() {
        return isChanged;
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

    /**
     * Adds an entry to the index.
     * If the entry does not already exist, it is created.
     * If the entry exists but has changed, it is updated with the new data.
     * The method also updates the index size and marks the index as changed if any modifications occur.
     *
     * @param path The file path of the entry to be added.
     * @param blobId The identifier of the blob associated with the entry.
     * @throws IOException If an I/O error occurs while processing the file.
     */
    public void addEntry(Path path, String blobId) throws IOException {
        // Get file stat
        FileStat stat;
        if (FileUtil.isWindows()) stat = new WindowFileStat(path);
        else stat = new UnixFileStat(path);

        // Create the index entry
        IndexEntry entry = createIndexEntry(path, blobId, stat);

        // Create or update the index entry
        Path entryPath = Paths.get(entry.getPath());
        IndexEntry existingEntry = entryMap.get(entryPath);
        if (existingEntry == null) {
            entryMap.put(path, entry);
            sizeInBytes += entry.getSize();
            this.header.incrementEntryCount();
            this.isChanged = true;
        } else if (!existingEntry.equals(entry)){
            entryMap.put(path, entry);
            this.isChanged = true;
        }

    }

    /**
     * Creates an index entry based on the provided path, blob ID, and file statistics.
     *
     * @param path the file path for the index entry
     * @param blobId the identifier of the blob associated with the file
     * @param stat the file statistics providing metadata such as modification time, size, and permissions
     * @return an IndexEntry object representing the data and metadata for the given file
     */
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
                , IndexEntry.computeEntrySize(path.toString())
        );
    }

    /**
     * Writes the current state of the index to the disk in a thread-safe and consistent manner.
     *
     * The method performs the following steps:
     * - Acquires a lock on the target file to ensure no other process modifies it concurrently.
     * - Converts the index data into a byte array using the associated {@code toBytes} method.
     * - Writes the byte array into the locked file.
     * - Logs the number of bytes written for verification.
     *
     * If any errors occur during the conversion or file write process, appropriate exceptions are caught and handled:
     * - {@link NoSuchAlgorithmException} is logged in case of an issue with creating index object IDs.
     * - {@link IOException} is logged when the file write operation fails, and a retry attempt is suggested.
     *
     * The lock and resources are properly released after the operation completes.
     */
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

    /**
     * Converts the index and its components into bytes and writes them into the provided buffer.
     *
     * This method serializes the index header and its associated index entries into the given buffer.
     * It also computes and updates the SHA-1 checksum of the index, storing it as the index ID.
     *
     * @param buf the buffer into which the index and its components should be written
     * @throws NoSuchAlgorithmException if the algorithm used for checksum computation is not available
     */
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

    /**
     * Computes a checksum for the content stored in the provided buffer.
     * The method processes the buffer content, calculates a hash, and updates the buffer with the checksum.
     *
     * @param buf the buffer containing the data for which the checksum is to be computed
     * @return an ObjectID constructed from the computed checksum
     * @throws NoSuchAlgorithmException if the specified hashing algorithm is not available
     */
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

    /**
     * Creates an Index object by deserializing it from the provided ByteBuffer.
     *
     * This method reads and constructs the IndexHeader, IndexEntries, and Index ID
     * from the buffer to create an Index instance.
     *
     * @param buf the ByteBuffer containing the serialized index data
     * @return a new Index instance constructed from the deserialized data
     */
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
