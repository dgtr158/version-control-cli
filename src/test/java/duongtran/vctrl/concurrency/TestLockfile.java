package duongtran.vctrl.concurrency;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class TestLockfile {

    @TempDir
    Path tempDir;

    @Test
    void testAcquireCreatesLockFileSuccessfully() throws IOException {
        Path targetFile = Files.createTempFile("test", "file");
        try {
            Lockfile lockfile = new Lockfile(targetFile);
            lockfile.acquire();

            assertTrue(Files.exists(targetFile.resolveSibling(targetFile.getFileName() + ".lock")));
        } finally {
            Files.deleteIfExists(targetFile.resolveSibling(targetFile.getFileName() + ".lock"));
            Files.deleteIfExists(targetFile);
        }
    }

    @Test
    void testAcquireThrowsExceptionWhenLockAlreadyHeld() throws IOException {
        Path targetFile = Files.createTempFile("test", "file");
        Path lockFile = targetFile.resolveSibling(targetFile.getFileName() + ".lock");
        try {
            Files.createFile(lockFile);

            Lockfile lockfile = new Lockfile(targetFile);
            IllegalStateException exception = assertThrows(IllegalStateException.class, lockfile::acquire);

            assertEquals("Lock are already held by another process: " + lockFile, exception.getMessage());
        } finally {
            Files.deleteIfExists(lockFile);
            Files.deleteIfExists(targetFile);
        }
    }

    @Test
    void testAcquireThrowsIOExceptionWhenParentDirectoryDoesNotExist() {
        Path nonExistentDir = Paths.get("nonexistent_directory");
        Path targetFile = nonExistentDir.resolve("file");
        Lockfile lockfile = new Lockfile(targetFile);

        IOException exception = assertThrows(IOException.class, lockfile::acquire);

        assertTrue(Objects.requireNonNull(exception.getMessage())
                .contains("Directory does not exist: " + nonExistentDir));
    }

    @Test
    void testAcquireThrowsIOExceptionWhenPermissionDenied() throws IOException {
        Path targetFile = Files.createTempFile("test", "file");
        Path lockFile = targetFile.resolveSibling(targetFile.getFileName() + ".lock");
        try {
            Files.createFile(lockFile);
            lockFile.toFile().setWritable(false);

            Lockfile lockfile = new Lockfile(targetFile);
            IOException exception = assertThrows(IOException.class, lockfile::acquire);

            assertTrue(exception.getMessage().contains("Permission denied for: " + lockFile));
        } finally {
            lockFile.toFile().setWritable(true);
            Files.deleteIfExists(lockFile);
            Files.deleteIfExists(targetFile);
        }
    }

    @Test
    void testAcquireDoesNothingIfAlreadyAcquired() throws IOException {
        Path targetFile = Files.createTempFile("test", "file");
        try {
            Lockfile lockfile = new Lockfile(targetFile);
            lockfile.acquire();

            assertDoesNotThrow(lockfile::acquire);
        } finally {
            Files.deleteIfExists(targetFile.resolveSibling(targetFile.getFileName() + ".lock"));
            Files.deleteIfExists(targetFile);
        }
    }

    @Test
    void testMoveContentToTarget() throws Exception {
        Path target = tempDir.resolve("index");
        Lockfile lockFile = new Lockfile(target);

        String content = "Hello, world";
        byte[] data = content.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buf = ByteBuffer.wrap(data);
        int writtenBytes = lockFile.write(buf);

        assertEquals(data.length, writtenBytes);
        assertTrue(Files.exists(target));
        assertArrayEquals(data, Files.readAllBytes(target));
        assertFalse(Files.exists(target.resolveSibling("index.lock")));

        // Override existing content
        String newContent = "Hello, Duong";
        buf = ByteBuffer.wrap(newContent.getBytes(StandardCharsets.UTF_8));
        lockFile.write(buf);
        assertEquals(newContent, Files.readString(target));

    }
}