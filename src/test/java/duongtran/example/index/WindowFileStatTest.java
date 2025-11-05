package duongtran.example.index;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WindowFileStatTest {

    private Path tmp1;
    private Path tmp2;

    private static boolean isWindows() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("win");
    }

    private static Path projectTempDir() throws IOException {
        Path dir = Path.of("tempFolder", "test-tmp");
        Files.createDirectories(dir);
        return dir;
    }

    private static Path newProjectTempFile(String prefix, String suffix) throws IOException {
        Path dir = projectTempDir();
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
        Assumptions.assumeTrue(isWindows(), "Windows-only test");

        tmp1 = newProjectTempFile("winfilestat-", ".tmp");
        WindowFileStat stat = new WindowFileStat(tmp1);

        int dev = stat.getDev(); // expected to map to volume serial or similar
        int ino = stat.getIno(); // expected to map to low 32 bits of file index or similar

        assertTrue(dev >= 0, "dev should be non-negative");
        assertTrue(ino >= 0, "ino should be non-negative");
    }

    @Test
    void devAndIno_stable_forSameFileInstance() throws IOException {
        Assumptions.assumeTrue(isWindows(), "Windows-only test");

        tmp1 = newProjectTempFile("winfilestat-", ".tmp");
        WindowFileStat stat1 = new WindowFileStat(tmp1);
        WindowFileStat stat2 = new WindowFileStat(tmp1);

        assertEquals(stat1.getDev(), stat2.getDev(), "dev should be stable for the same path");
        assertEquals(stat1.getIno(), stat2.getIno(), "ino should be stable for the same path");
    }

    @Test
    void inoOrDev_differs_betweenDifferentFiles() throws IOException {
        Assumptions.assumeTrue(isWindows(), "Windows-only test");

        tmp1 = newProjectTempFile("winfilestat-a-", ".tmp");
        tmp2 = newProjectTempFile("winfilestat-b-", ".tmp");

        WindowFileStat a = new WindowFileStat(tmp1);
        WindowFileStat b = new WindowFileStat(tmp2);

        boolean differs = a.getIno() != b.getIno() || a.getDev() != b.getDev();
        assertTrue(differs, "At least ino or dev should differ between two distinct files");
    }

    @Test
    void rename_keeps_devAndIno_withinSameVolume() throws IOException {
        Assumptions.assumeTrue(isWindows(), "Windows-only test");

        tmp1 = newProjectTempFile("winfilestat-", ".tmp");
        WindowFileStat before = new WindowFileStat(tmp1);

        Path renamed = tmp1.resolveSibling(tmp1.getFileName().toString() + ".renamed");
        Files.move(tmp1, renamed);
        tmp1 = renamed; // update for cleanup

        WindowFileStat after = new WindowFileStat(renamed);

        assertEquals(before.getDev(), after.getDev(), "dev should remain the same after rename on same volume");
        assertEquals(before.getIno(), after.getIno(), "ino should remain the same after rename on same volume");
    }
}