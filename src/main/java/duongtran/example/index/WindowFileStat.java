package duongtran.example.index;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
            String drive = getPath().toAbsolutePath().getRoot().toString(); // e.g., "C:\"
            String driveLetter = drive.length() >= 2 ? drive.substring(0, 2) : "C:";
            String[] cmd = {"cmd", "/c", "vol", driveLetter};
            String out = runAndReadStdout(cmd);
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
            String[] cmd = {"cmd", "/c", "fsutil", "file", "queryfileid", getPath().toAbsolutePath().toString()};
            String out = runAndReadStdout(cmd);
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

    private String runAndReadStdout(String[] cmd) {
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
