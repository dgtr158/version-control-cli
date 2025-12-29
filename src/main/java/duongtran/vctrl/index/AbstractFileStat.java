package duongtran.vctrl.index;

import duongtran.vctrl.storage.FileMode;
import duongtran.vctrl.utils.FileUtil;
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

    public AbstractFileStat(Path path) throws IOException {
        this.path = path;
        this.attrs = Files.readAttributes(path, BasicFileAttributes.class);
    }

    @Override
    public boolean isExecutable() throws IOException {
        return FileUtil.isExecutable(path);
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
    public FileMode getMode() throws IOException {
        return FileUtil.isExecutable(path) ? FileMode.EXECUTABLE_FILE : FileMode.REGULAR_FILE;
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

    @Override
    public Path getPath() {
        return path;
    }

    @Override
    public boolean isDirectory() {
        if (path == null) return false;
        return Files.isDirectory(path);
    }
    
    @Override
    public boolean isFile() {
        if (path == null) return false;

        try {
            FileMode mode = getMode();
            return mode == FileMode.REGULAR_FILE || mode == FileMode.EXECUTABLE_FILE;
        } catch (Exception ex) {
            return false;
        }
    }

}
