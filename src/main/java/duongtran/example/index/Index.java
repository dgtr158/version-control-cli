package duongtran.example.index;

import duongtran.example.utils.DirectoryNames;
import duongtran.example.utils.FileUtil;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Index {

    public static final int REGULAR_MODE = 0100644;   // normal file mode
    public static final int EXECUTABLE_MODE = 0100755; // executable file mode
    public static final int MAX_PATH_SIZE = 0xfff;
    public static final String SIGNATURE = "DIRC";

    private final Path indexPath;
    private int version;
    private int numEntries;
    private List<IndexEntry> entries;

    public Index(int version){
        File gitPath = new File(DirectoryNames.WORKING_DIRECTORY, DirectoryNames.ROOT_DIR_NAME);
        File indexPath = new File(gitPath, DirectoryNames.INDEX);
        this.indexPath = indexPath.toPath();

        this.version = version;
        this.numEntries = 0;
        this.entries = new ArrayList<>();
    }

    public void addEntry(Path path, String blobId) throws IOException {
        FileStat stat;
        if (FileUtil.isWindows()) stat = new WindowFileStat(path);
        else stat = new UnixFileStat(path);

        /*
            TODO: modify flags 16-bit
                16-bit flags (high to low) contains:
                   1-bit assume-valid flag (0 in version2)
                   1-bit extended flag (0 in version 2)
                   2-bit stage (0-normal, 1-ours, 2-theirs, 3-base)
                   12-bit name length MIN(actual_path_length.countBytes(), 0xFFF)
         */
        int flags = Math.min(path.toString().getBytes(StandardCharsets.UTF_8).length, MAX_PATH_SIZE);
        entries.add (
                new IndexEntry(
                        stat.getCtimeSeconds()
                        ,stat.getCtimeNanos()
                        ,stat.getMtimeSeconds()
                        ,stat.getMtimeNanos()
                        ,stat.getDev()
                        ,stat.getIno()
                        ,stat.getMode()
                        ,stat.getUid()
                        ,stat.getGid()
                        ,stat.getSize()
                        ,blobId
                        ,flags
                        ,path.toString()
                )
        );
        numEntries++;

    }

}
