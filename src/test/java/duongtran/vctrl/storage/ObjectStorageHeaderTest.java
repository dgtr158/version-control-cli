package duongtran.vctrl.storage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ObjectStorageHeaderTest {

    @Test
    void testSerializeDeserialize() {
        long size = 1024L;
        ObjectType type = ObjectType.BLOB;
        ObjectStorageHeader expected = new ObjectStorageHeader(type, size);

        byte[] bytes = expected.toBytes();
        ObjectStorageHeader actual = ObjectStorageHeader.fromBytes(bytes);
        assertEquals(expected, actual);
    }

    @Test
    void testSize() {
        int fixedSize = Long.BYTES + 2;
        long objectSize = 1024L;

        for (ObjectType type : ObjectType.values()) {
            ObjectStorageHeader header = new ObjectStorageHeader(type, objectSize);
            int typeSize;
            switch (type) {
                case BLOB, TREE -> typeSize = 4;
                case COMMIT -> typeSize = 6;
                case TAG -> typeSize = 3;
                default -> typeSize = 0;
            }
            int expectedSize = typeSize + fixedSize;
            assertEquals(expectedSize, header.size());
        }
    }

}
