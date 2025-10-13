package duongtran.example.index;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;

public class FileStatImpl implements FileStat {

    private static final Logger logger = LoggerFactory.getLogger(FileStatImpl.class);

    private final Path path;
    private final BasicFileAttributes attrs;
    private final PosixFileAttributes posixAttrs; // For Unix file

    public FileStatImpl(Path path) throws IOException {
        this.path = path;
        this.attrs = Files.readAttributes(path, BasicFileAttributes.class);
        PosixFileAttributes posixAttrs = null;
        try {
            posixAttrs = Files.readAttributes(path, PosixFileAttributes.class);
        } catch (UnsupportedOperationException ex) {
            logger.warn("Cannot support posix attributes: {}", ex.getMessage());
        }
        this.posixAttrs = posixAttrs;
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
    public int getDev() {
        return this.path.hashCode();
    }

    @Override
    public int getIno() {
        Object fileKey = attrs.fileKey();
        return fileKey != null ? fileKey.hashCode() : 0;
    }

    @Override
    public int getMode() {
        if (posixAttrs != null) {
            return PosixFilePermissions.toString(posixAttrs.permissions()).contains("x")
                    ? Index.EXECUTABLE_MODE
                    : Index.REGULAR_MODE;
        } else {
            return Files.isExecutable(path) ? Index.EXECUTABLE_MODE : Index.REGULAR_MODE;
        }
    }

    @Override
    public int getUid() {
        return 0;
    }

    @Override
    public int getGid() {
        if (posixAttrs != null) {
            return posixAttrs.group().getName().hashCode();
        }
        return 0;
    }

    @Override
    public int getSize() {
        return (int) attrs.size();
    }
}
