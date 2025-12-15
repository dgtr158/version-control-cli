package duongtran.vctrl.storage;

public enum FileMode {

    REGULAR_FILE(100644),
    EXECUTABLE_FILE(100755),
    SYMBOLIC_LINK(120000),
    DIRECTORY(040000),
    GIT_LINK(160000)
    ;

    private final int value;

    FileMode(int value) {
        this.value = value;
    }
}
