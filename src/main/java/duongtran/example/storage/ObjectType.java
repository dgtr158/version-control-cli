package duongtran.example.storage;

public enum ObjectType {
    BLOB,
    TREE,
    COMMIT,
    TAG;

    /**
     * @return string representation of the object type.
     */
    @Override
    public String toString() {
        return switch (this) {
            case BLOB -> "blob";
            case TREE -> "tree";
            case COMMIT -> "commit";
            case TAG -> "tag";
        };
    }
}
