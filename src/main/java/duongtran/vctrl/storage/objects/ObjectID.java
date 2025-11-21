package duongtran.vctrl.storage.objects;

import duongtran.vctrl.storage.ObjectStorage;
import duongtran.vctrl.utils.Utils;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

public class ObjectID {

    public static final int SIZE_IN_BYTES = 20;

    private final String value;

    public ObjectID(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    public void toBytes(ByteBuffer buf) {
        byte[] bytes = Utils.hexStringToByteArray(this.value);
        buf.put(bytes);
    }

    public static ObjectID fromBytes(ByteBuffer buf) {
        byte[] bytes = new byte[SIZE_IN_BYTES];
        buf.get(bytes);
        return new ObjectID(Utils.bytesToHex(bytes));
    }

    public static ObjectID fromBytes(byte[] content) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(ObjectStorage.HASH_ALGORITHM);
        return new ObjectID(Utils.bytesToHex(digest.digest(content)));
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ObjectID objectID = (ObjectID) o;
        return Objects.equals(value, objectID.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }
}
