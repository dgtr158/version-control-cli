package duongtran.vctrl.index;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IndexHeaderTest {

    private static final int HEADER_SIZE = 12;

    @Test
    void testSerializeDeserialize() throws Exception {
        int version = 2;
        int entryCount = 8;
        IndexHeader expected = new IndexHeader(version, entryCount);

        ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE);
        IndexHeader header = new IndexHeader(version, entryCount);
        header.toBytes(buffer);
        buffer.flip();
        IndexHeader actual = IndexHeader.fromBytes(buffer);

        assertEquals(expected, actual);

    }

}
