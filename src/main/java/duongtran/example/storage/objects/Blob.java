package duongtran.example.storage.objects;


import duongtran.example.storage.ObjectStorage;
import duongtran.example.storage.ObjectType;

public class Blob extends ObjectStorage {

    private final byte[] data;

    public Blob(byte[] data) {
        this.data = data;
    }

    @Override
    protected byte[] toBytes() {
        return data;
    }

    @Override
    public ObjectType getType() {
        return ObjectType.BLOB;
    }
}
