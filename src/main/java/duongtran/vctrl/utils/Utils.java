package duongtran.vctrl.utils;

import duongtran.vctrl.index.FileStat;
import duongtran.vctrl.index.UnixFileStat;
import duongtran.vctrl.index.WindowFileStat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

public final class Utils {

    // Convert byte[] to hex string
    public static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hexString.append(String.format("%02x", b & 0xFF));
        }
        return hexString.toString();
    }

    // Convert hex string to byte[]
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        if (len % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have even length");
        }
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int high = Character.digit(s.charAt(i), 16);
            int low = Character.digit(s.charAt(i + 1), 16);
            if (high < 0 || low < 0) {
                throw new IllegalArgumentException("Invalid hex character at position " + i);
            }
            data[i / 2] = (byte) ((high << 4) + low);
        }
        return data;
    }

    public static <K, V> boolean mapsEqual(Map<K, V> m1, Map<K, V> m2) {
        if (m1 == m2) return true;
        if (m1 == null || m2 == null) return false;
        if (m1.size() != m2.size()) return false;

        for (Map.Entry<K, V> e : m1.entrySet()) {
            K key = e.getKey();
            if (!m2.containsKey(key)) return false;
            if (!Objects.equals(e.getValue(), m2.get(key))) return false;
        }
        return true;
    }

    public static String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }

    public static FileStat getFileStat(Path path) throws IOException {
        return FileUtil.isWindows() ? new WindowFileStat(path) : new UnixFileStat(path);
    }

}
