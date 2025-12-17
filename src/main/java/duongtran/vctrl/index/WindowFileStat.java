package duongtran.vctrl.index;

import duongtran.vctrl.utils.FileUtil;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WindowFileStat extends AbstractFileStat {

    private static final Pattern VOL_SERIAL_PATTERN = Pattern.compile("Volume Serial Number is ([0-9A-Fa-f-]+)");

    private final int fileIndex;
    private final int volumeSerialNumber;
    private static final int DEFAULT_VALUE = 0;

    public WindowFileStat(Path path) throws IOException {
        super(path);

        Integer volumeSerialNumber = getVolumeSerialNumber();
        if (volumeSerialNumber != null) this.volumeSerialNumber = volumeSerialNumber;
        else this.volumeSerialNumber = DEFAULT_VALUE;

        Integer fileIndex = getFileIndex();
        if (fileIndex != null) this.fileIndex = fileIndex;
        else this.fileIndex = DEFAULT_VALUE;

    }

    @Override
    public int getDev() {
        return volumeSerialNumber;
    }

    @Override
    public int getIno() {
        return fileIndex;
    }

    private Integer getVolumeSerialNumber() {
        try {
            String drive = path.toAbsolutePath().getRoot().toString(); // e.g., "C:\"
            String driveLetter = drive.length() >= 2 ? drive.substring(0, 2) : "C:";
            String[] cmd = {"cmd", "/c", "vol", driveLetter};
            String out = FileUtil.runAndReadStdout(cmd);
            if (out == null) return null;
            Matcher m = VOL_SERIAL_PATTERN.matcher(out);
            if (m.find()) {
                String hex = m.group(1).replace("-", "");
                long val = Long.parseLong(hex, 16);
                return (int) val;
            }
        } catch (Exception ignored) {
        }
        return null;
    }


    private Integer getFileIndex() {
        try {
            // fsutil file queryfileid outputs e.g.: "File ID is 0x000000120000002B"
            String[] cmd = {"cmd", "/c", "fsutil", "file", "queryfileid", path.toAbsolutePath().toString()};
            String out = FileUtil.runAndReadStdout(cmd);
            if (out == null) return null;
            String s = out.trim();
            int idx = s.toLowerCase(Locale.ROOT).lastIndexOf("0x");
            if (idx >= 0) {
                String hex = s.substring(idx + 2).replaceAll("[^0-9A-Fa-f]", "");
                if (!hex.isEmpty()) {
                    // Use the lower 32 bits to fit interface return type (int)
                    long id = Long.parseUnsignedLong(hex, 16);
                    return (int) (id & 0xFFFF_FFFFL);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }



}
