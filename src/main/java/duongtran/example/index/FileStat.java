package duongtran.example.index;

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
}
