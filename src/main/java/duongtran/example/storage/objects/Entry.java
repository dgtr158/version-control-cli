package duongtran.example.storage.objects;

import duongtran.example.storage.ObjectStorage;

public class Entry {
    public static final String REGULAR_MODE = "100644";
    public static final String EXECUTABLE_MODE = "100644";
    private final String name;
    private final String oid;
    private final String mode;

    public Entry(String name, String oid, boolean isExecutable) {
        this.name = name;
        this.oid = oid;
        mode = isExecutable ? EXECUTABLE_MODE : REGULAR_MODE;
    }

    public String getName() {
        return name;
    }

    public String getOid() {
        return oid;
    }
    public String getMode() {
        return mode;
    }
}
