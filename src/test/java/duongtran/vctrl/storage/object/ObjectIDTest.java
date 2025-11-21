package duongtran.vctrl.storage.object;

import java.nio.ByteBuffer;

import duongtran.vctrl.storage.objects.ObjectID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ObjectIDTest {

    @Test
    public void testSerializeDeserialize() {
        // Create object id
        ByteBuffer buf = ByteBuffer.allocate(ObjectID.SIZE_IN_BYTES);
        String value = "e69de29bb2d1d6434b8b29ae775ad8c2e48c5391";
        ObjectID expected = new ObjectID(value);
        expected.toBytes(buf);

        // change buffer into read mode
        buf.flip();

        ObjectID actual = ObjectID.fromBytes(buf);
        assertEquals(expected, actual);
    }

}
