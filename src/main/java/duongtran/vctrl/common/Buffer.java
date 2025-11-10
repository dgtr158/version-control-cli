package duongtran.vctrl.common;

import java.nio.ByteOrder;

public interface Buffer {

    Buffer order(ByteOrder bo);

    Buffer put(byte b);
    Buffer putInt(int value);

}
