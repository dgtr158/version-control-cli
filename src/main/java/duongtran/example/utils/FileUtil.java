package duongtran.example.utils;

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

}
