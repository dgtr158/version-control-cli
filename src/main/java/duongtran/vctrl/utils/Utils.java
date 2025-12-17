package duongtran.vctrl.utils;

import duongtran.vctrl.index.IndexEntry;

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

    public static boolean mapsEqual(Map<Path, IndexEntry> m1, Map<Path, IndexEntry> m2) {
        if (m1.size() != m2.size()) return false;
        for (Path key : m1.keySet()) {
            if (!m2.containsKey(key)) return false;
            if (!Objects.equals(m1.get(key), m2.get(key))) return false;
        }
        return true;
    }

    public static String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }

}
