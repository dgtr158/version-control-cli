package duongtran.example.index;

import duongtran.example.utils.FileUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UnixFileStatTest {

    private Path tmp1;
    private Path tmp2;

    private static Path projectTempDir() throws IOException {
        Path dir = Path.of("tempFolder", "test-tmp");
        Files.createDirectories(dir);
        return dir;
    }

    private static Path newProjectTempFile(String prefix, String suffix) throws IOException {
        Path dir = projectTempDir();
        // Create a unique file name to avoid conflicts when tests run in parallel
        Path file = dir.resolve(prefix + UUID.randomUUID() + suffix);
        return Files.createFile(file);
    }

    @AfterEach
    void cleanup() throws IOException {
        if (tmp1 != null) Files.deleteIfExists(tmp1);
        if (tmp2 != null) Files.deleteIfExists(tmp2);
    }


    @Test
    void devAndIno_exist_forRealFile() throws IOException {
        Assumptions.assumeTrue(FileUtil.isUnix(), "Unix-only test");

        tmp1 = newProjectTempFile("unixfilestat-", ".tmp");
        UnixFileStat stat = new UnixFileStat(tmp1);

        int dev = stat.getDev();
        int ino = stat.getIno();

        assertTrue(dev >= 0, "dev should be non-negative");
        assertTrue(ino >= 0, "ino should be non-negative");
    }

    @Test
    void devAndIno_stable_forSameFileInstance() throws IOException {
        Assumptions.assumeTrue(FileUtil.isUnix(), "Unix-only test");

        tmp1 = newProjectTempFile("unixfilestat-", ".tmp");
        UnixFileStat stat1 = new UnixFileStat(tmp1);
        UnixFileStat stat2 = new UnixFileStat(tmp1);

        assertEquals(stat1.getDev(), stat2.getDev(), "dev should be stable for the same path");
        assertEquals(stat1.getIno(), stat2.getIno(), "ino should be stable for the same path");
    }

    @Test
    void inoOrDev_differs_betweenDifferentFiles() throws IOException {
        Assumptions.assumeTrue(FileUtil.isUnix(), "Unix-only test");

        tmp1 = newProjectTempFile("unixfilestat-a-", ".tmp");
        tmp2 = newProjectTempFile("unixfilestat-b-", ".tmp");

        UnixFileStat a = new UnixFileStat(tmp1);
        UnixFileStat b = new UnixFileStat(tmp2);

        boolean differs = a.getIno() != b.getIno() || a.getDev() != b.getDev();
        assertTrue(differs, "At least ino or dev should differ between two distinct files");
    }

    @Test
    void rename_keeps_devAndIno_withinSameFs() throws IOException {
        Assumptions.assumeTrue(FileUtil.isUnix(), "Unix-only test");

        tmp1 = newProjectTempFile("unixfilestat-", ".tmp");
        UnixFileStat before = new UnixFileStat(tmp1);

        Path renamed = tmp1.resolveSibling(tmp1.getFileName().toString() + ".renamed");
        Files.move(tmp1, renamed);
        tmp1 = renamed; // update for cleanup

        UnixFileStat after = new UnixFileStat(renamed);

        assertEquals(before.getDev(), after.getDev(), "dev should remain the same after rename");
        assertEquals(before.getIno(), after.getIno(), "ino should remain the same after rename");
    }

}