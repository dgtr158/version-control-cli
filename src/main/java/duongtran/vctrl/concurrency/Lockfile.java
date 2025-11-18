package duongtran.vctrl.concurrency;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class Lockfile implements AutoCloseable {

    private final Path targetFile;
    private final Path lockFile;
    private OutputStream lockStream;
    private FileChannel fileChannel;

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

    public int write(ByteBuffer buf) throws IOException {
        int bytes = 0;
        try {
            fileChannel = FileChannel.open(
                    lockFile
                    , StandardOpenOption.CREATE
                    , StandardOpenOption.WRITE
                    , StandardOpenOption.TRUNCATE_EXISTING
            );
            while (buf.hasRemaining()) {
                bytes += fileChannel.write(buf);
            }
            fileChannel.force(true);
            commit();

        } catch (FileAlreadyExistsException ex) {
            rollback();
            throw new IllegalStateException("Lock are already held by another process: " + lockFile);
        } catch (NoSuchFileException ex) {
            rollback();
            throw new IOException("Directory does not exist: " + lockFile.getParent());
        } catch (AccessDeniedException ex) {
            rollback();
            throw new IOException("Permission denied for: " + lockFile);
        }

        return bytes;
    }

    public void commit() throws IOException {
        // Move file to target
        Files.move(lockFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
        close();
    }

    public void rollback() throws IOException {
        if (lockFile != null) {
            Files.deleteIfExists(lockFile);
        }
        close();
    }

    private void ensureLockHeld() {
        if (lockStream == null) {
            throw new IllegalStateException("Lock not acquired on: " + lockFile);
        }
    }

    @Override
    public void close() throws IOException {
        if (lockStream != null) {
            lockStream.close();
            lockStream = null;
        }
        if (fileChannel != null && fileChannel.isOpen()) {
            fileChannel.close();
            fileChannel = null;
        }
    }
}
