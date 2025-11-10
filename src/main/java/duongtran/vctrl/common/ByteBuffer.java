package duongtran.vctrl.common;

import java.nio.ByteOrder;

public class ByteBuffer implements Buffer {

    private java.nio.ByteBuffer buf;

    private ByteBuffer(java.nio.ByteBuffer buf) {
        this.buf = buf;
    }

    public static Buffer wrap(byte[] array, int offset, int length) {
        return new ByteBuffer(java.nio.ByteBuffer.wrap(array, offset, length));
    }

    public static Buffer wrap(byte[] array) {
        return new ByteBuffer(java.nio.ByteBuffer.wrap(array));
    }

    @Override
    public Buffer order(ByteOrder bo) {
        buf.order(ByteOrder.BIG_ENDIAN);
        return this;
    }

    @Override
    public Buffer put(byte b) {
        buf.put(b);
        return this;
    }

    @Override
    public Buffer putInt(int value) {
        buf.putInt(value);
        return this;
    }
}
