package duongtran.example.index;

import duongtran.example.utils.DirectoryNames;

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

    private final Path indexPath;
    private final String signature = "DIRC";
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
        FileStat stat = new FileStatImpl(path);
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
