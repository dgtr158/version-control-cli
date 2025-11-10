package duongtran.vctrl.storage.objects;


import duongtran.vctrl.storage.ObjectStorage;
import duongtran.vctrl.storage.ObjectType;

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
