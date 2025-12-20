package duongtran.vctrl.index;

import duongtran.vctrl.storage.FileMode;
import duongtran.vctrl.utils.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UnixFileStat extends AbstractFileStat {

    private static final Logger logger = LoggerFactory.getLogger(UnixFileStat.class);

    private static final Pattern UNIX_FILEKEY_PATTERN = Pattern.compile("\\(.*?dev=([^,\\s)]+).*?ino=([^,\\s)]+).*?\\)");
    private static final int DEFAULT_VALUE = 0;
    private final PosixFileAttributes posixAttrs;

    // File Key in Unix has a pattern (dev=#dev, ino=#ino)
    private int dev; // device node
    private int ino; // inode

    public UnixFileStat(Path path) throws IOException {
        super(path);

        PosixFileAttributes posixAttrs = null;
        try {
            posixAttrs = Files.readAttributes(path, PosixFileAttributes.class);
        } catch (UnsupportedOperationException ex) {
            logger.warn("Cannot support posix attributes: {}", ex.getMessage());
        }
        this.posixAttrs = posixAttrs;

        if (!getFromFileKey() && !getFromUnixStatCommand()) {
            this.dev = DEFAULT_VALUE;
            this.ino = DEFAULT_VALUE;
        }

    }

    @Override
    public int getDev() {
        return dev;
    }

    @Override
    public int getIno() {
        return ino;
    }

    @Override
    public FileMode getMode() {
        if (posixAttrs != null) {
            return PosixFilePermissions.toString(posixAttrs.permissions()).contains("x")
                    ? FileMode.EXECUTABLE_FILE
                    : FileMode.REGULAR_FILE;
        } else {
            return super.getMode();
        }
    }

    @Override
    public int getGid() {
        if (posixAttrs != null) {
            return posixAttrs.group().getName().hashCode();
        }
        return 0;
    }

    private boolean getFromFileKey() {
        Object fileKey = attrs.fileKey();
        if (fileKey == null) {
            this.dev = DEFAULT_VALUE;
            this.ino = DEFAULT_VALUE;
            return false;
        }
        String s = String.valueOf(fileKey);

        Matcher m = UNIX_FILEKEY_PATTERN.matcher(s);
        if (m.find()) {
            try {
                this.dev = (int) parseNumberMaybeHex(m.group(1));
                this.ino = (int) parseNumberMaybeHex(m.group(2));
            } catch (Exception ignored) {
                logger.error("Cannot get dev and ino in Unix");
                return false;
            }
        }

        return true;
    }

    private long parseNumberMaybeHex(String text) {
        String t = Objects.requireNonNull(text).trim();
        if (t.startsWith("0x") || t.startsWith("0X")) {
            return Long.parseUnsignedLong(t.substring(2), 16);
        }
        try {
            return Long.parseLong(t);
        } catch (NumberFormatException nfe) {
            return Long.parseUnsignedLong(t, 16);
        }
    }

    private boolean getFromUnixStatCommand() {
        // Try GNU coreutils: stat -c %d:%i
        String[] cmdLinux = {"bash", "-lc", "stat -c %d:%i -- " + escapeShell(path.toString())};
        String out = FileUtil.runAndReadStdout(cmdLinux);
        boolean gnuRes = parseDevInoColon(out);
        if (gnuRes) return true;

        // Try BSD/macOS: stat -f %d:%i
        String[] cmdBsd = {"bash", "-lc", "stat -f %d:%i -- " + escapeShell(path.toString())};
        out = FileUtil.runAndReadStdout(cmdBsd);
        return parseDevInoColon(out);
    }

    private boolean parseDevInoColon(String out) {
        if (out == null) return false;
        String s = out.trim();
        int idx = s.indexOf(':');
        if (idx <= 0 || idx >= s.length() - 1) return false;
        try {
            this.dev = (int) Long.parseLong(s.substring(0, idx).trim());
            this.ino = (int) Long.parseLong(s.substring(idx + 1).trim());
            return true;
        } catch (NumberFormatException ignored) {
            logger.error("Failed to get dev and ino from stat command");
            return false;
        }
    }

    private String escapeShell(String p) {
        // Minimal robust escaping by single-quoting and escaping single quotes within
        return "'" + p.replace("'", "'\"'\"'") + "'";
    }

}
