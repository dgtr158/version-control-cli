package duongtran.vctrl.index;

import duongtran.vctrl.common.Buffer;
import duongtran.vctrl.common.ByteBuffer;
import duongtran.vctrl.concurrency.Lockfile;
import duongtran.vctrl.utils.DirectoryNames;
import duongtran.vctrl.utils.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;


public class Index {

    private static final Logger log = LoggerFactory.getLogger(Index.class);

    public static final int REGULAR_MODE = 0100644;   // normal file mode
    public static final int EXECUTABLE_MODE = 0100755; // executable file mode
    public static final int MAX_PATH_SIZE = 0xfff;

    private final Path indexPath;
    private final int version;
    private int sizeInBytes;
    private Map<Path, IndexEntry> entryMap;

    public Index(int version){
        File gitPath = new File(DirectoryNames.WORKING_DIRECTORY, DirectoryNames.ROOT_DIR_NAME);
        File indexPath = new File(gitPath, DirectoryNames.INDEX);
        this.indexPath = indexPath.toPath();

        this.version = version;
        this.entryMap = new HashMap<>();
        sizeInBytes = IndexHeader.HEADER_SIZE;
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
        int flags = Math.min(path.toString().getBytes(StandardCharsets.UTF_8).length, MAX_PATH_SIZE);
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
                ,flags
                , path.toString()
        );
    }

    public void writeUpdate() {


        try (Lockfile lockfile = new Lockfile(indexPath)) {
            // hold index file for update
            lockfile.acquire();

            // Convert index file into bytes
            byte[] bytes = new byte[sizeInBytes];
            Buffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);

            IndexHeader header = new IndexHeader(version, entryMap.size());
            header.toBytes(buf);

            // Convert the index object into byte
//            lockfile.write(objectId + "\n");

            // Flush the bytes into the disk

            // Commit the change
//            lockfile.commit();

        } catch (Exception e) {
            log.warn("Failed to acquire lock: {}\n Retry later", e.getMessage());
        }

        // convert index object into byte

        // flush the bytes into the disk
    }



}
