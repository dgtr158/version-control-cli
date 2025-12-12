package duongtran.vctrl.storage.objects;

public class TreeEntry {
    public static final String REGULAR_MODE = "100644";
    public static final String EXECUTABLE_MODE = "100644";
    private final String fileName;
    private final String oid;
    private final String mode;

    public TreeEntry(String fileName, String oid, boolean isExecutable) {
        this.fileName = fileName;
        this.oid = oid;
        mode = isExecutable ? EXECUTABLE_MODE : REGULAR_MODE;
    }

    public String getFileName() {
        return fileName;
    }

    public String getOid() {
        return oid;
    }
    public String getMode() {
        return mode;
    }
}
