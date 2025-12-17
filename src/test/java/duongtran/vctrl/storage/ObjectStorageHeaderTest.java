package duongtran.vctrl.storage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ObjectStorageHeaderTest {

    @Test
    void testSerializeDeserialize() {
        int size = 1024;
        ObjectType type = ObjectType.BLOB;
        ObjectStorageHeader expected = new ObjectStorageHeader(type, size);

        byte[] bytes = expected.toBytes();
        ObjectStorageHeader actual = ObjectStorageHeader.fromBytes(bytes);
        assertEquals(expected, actual);
    }

    @Test
    void testSize() {
        int objectSize = 1024;

        for (ObjectType type : ObjectType.values()) {
            ObjectStorageHeader header = new ObjectStorageHeader(type, objectSize);
            assertEquals(objectSize, header.getContentLength());
        }
    }

}
