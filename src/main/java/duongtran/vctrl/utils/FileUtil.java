package duongtran.vctrl.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Locale;
import java.util.Set;

public class FileUtil {

    public static boolean isWindows() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        return os.contains("win");
    }

    // TODO: right now support only window or unix, refactor this if support more OS
    public static boolean isUnix() {
        return !isWindows();
    }

    public static String runAndReadStdout(String[] cmd) {
        Process proc = null;
        try {
            proc = new ProcessBuilder(cmd)
                    .redirectErrorStream(true)
                    .start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) {
                    if (sb.length() > 0) sb.append('\n');
                    sb.append(line);
                }
                int code = proc.waitFor();
                if (code == 0) {
                    return sb.toString();
                }
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (proc != null) proc.destroy();
        }
        return null;
    }

    public static boolean isExecutable(Path path) throws IOException {
        try {
            Set<PosixFilePermission> perms =
                    Files.getPosixFilePermissions(path);
            return perms.contains(PosixFilePermission.OWNER_EXECUTE);
        } catch (UnsupportedOperationException e) {
            // Non-POSIX FS (Windows)
            return false;
        }
    }

    public static boolean isDeeplyContained(Path parent, Path child) throws IOException {
        Path parentReal = parent.toRealPath();
        Path childReal = child.toRealPath();
        return childReal.startsWith(parentReal);
    }

}
