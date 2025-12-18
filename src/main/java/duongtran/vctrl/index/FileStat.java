package duongtran.vctrl.index;

import java.nio.file.Path;

public interface FileStat {
    boolean isExecutable();
    int getCtimeSeconds();
    int getCtimeNanos();
    int getMtimeSeconds();
    int getMtimeNanos();
    int getDev();
    int getIno();
    int getMode ();
    int getUid();
    int getGid();
    int getSize();
    Path getPath();
    boolean isDirectory();
}
