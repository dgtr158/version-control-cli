package duongtran.vctrl.index;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

public abstract class AbstractFileStat implements FileStat {

    private static final Logger logger = LoggerFactory.getLogger(AbstractFileStat.class);

    protected final Path path;
    protected final BasicFileAttributes attrs;
     // For Unix file

    public AbstractFileStat(Path path) throws IOException {
        this.path = path;
        this.attrs = Files.readAttributes(path, BasicFileAttributes.class);
    }

    @Override
    public boolean isExecutable() {
        return Files.isExecutable(path);
    }

    @Override
    public int getCtimeSeconds() {
        return (int) (this.attrs.creationTime().toMillis() / 1000);
    }

    @Override
    public int getCtimeNanos() {
        return (int) (this.attrs.creationTime().toMillis() % 1000);
    }

    @Override
    public int getMtimeSeconds() {
        return (int) (this.attrs.lastModifiedTime().toMillis() / 1000);
    }

    @Override
    public int getMtimeNanos() {
        return (int) (this.attrs.creationTime().toMillis() % 1000);
    }

    @Override
    public abstract int getDev();

    @Override
    public abstract int getIno();

    @Override
    public int getMode() {
        return Files.isExecutable(path) ? Index.EXECUTABLE_MODE : Index.REGULAR_MODE;
    }

    @Override
    public int getUid() {
        return 0;
    }

    @Override
    public int getGid() {
        return 0;
    }

    @Override
    public int getSize() {
        return (int) attrs.size();
    }

}
