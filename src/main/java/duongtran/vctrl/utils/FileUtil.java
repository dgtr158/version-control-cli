package duongtran.vctrl.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

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

}
