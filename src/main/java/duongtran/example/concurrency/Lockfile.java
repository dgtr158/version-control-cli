package duongtran.example.concurrency;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class Lockfile implements AutoCloseable {

    private final Path targetFile;
    private final Path lockFile;
    private OutputStream lockStream;

    public Lockfile(Path targetFile) {
        this.targetFile = targetFile;
        this.lockFile = targetFile.resolveSibling(targetFile.getFileName() + ".lock");
    }

    public void acquire() throws IOException {
        // Avoid a process calling acquire() multiple times
        if (lockStream != null) return;

        try {
            lockStream = Files.newOutputStream(
                    lockFile
                    , StandardOpenOption.CREATE_NEW
                    , StandardOpenOption.WRITE
            );
        } catch (FileAlreadyExistsException ex) {
            throw new IllegalStateException("Lock are already held by another process: " + lockFile);
        } catch (NoSuchFileException ex) {
            throw new IOException("Directory does not exist: " + lockFile.getParent());
        } catch (AccessDeniedException ex) {
            throw new IOException("Permission denied for: " + lockFile);
        }
    }

    public void write(String content) throws IOException {
        ensureLockHeld();
        lockStream.write(content.getBytes(StandardCharsets.UTF_8));
    }

    public void commit() throws IOException {
        ensureLockHeld();
        lockStream.close();
        Files.move(lockFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
        lockStream = null;
    }

    public void rollback() throws IOException {
        if (lockStream != null) {
            lockStream.close();
            Files.deleteIfExists(lockFile);
            lockStream = null;
        }
    }

    private void ensureLockHeld() {
        if (lockStream == null) {
            throw new IllegalStateException("Lock not acquired on: " + lockFile);
        }
    }

    @Override
    public void close() throws Exception {
        rollback();
    }
}
