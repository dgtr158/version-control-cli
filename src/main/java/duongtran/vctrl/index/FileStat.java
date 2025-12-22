package duongtran.vctrl.index;

import duongtran.vctrl.storage.FileMode;

import java.io.IOException;
import java.nio.file.Path;

public interface FileStat {
    boolean isExecutable() throws IOException;
    int getCtimeSeconds();
    int getCtimeNanos();
    int getMtimeSeconds();
    int getMtimeNanos();
    int getDev();
    int getIno();
    FileMode getMode() throws IOException;
    int getUid();
    int getGid();
    int getSize();
    Path getPath();
    boolean isDirectory();
}
