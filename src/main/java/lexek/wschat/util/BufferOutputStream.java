package lexek.wschat.util;


import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.io.OutputStream;

public class BufferOutputStream extends OutputStream {
    private final ByteBuf buffer;

    public BufferOutputStream(ByteBuf buffer) {
        this.buffer = buffer;
    }

    @Override
    public void write(int b) throws IOException {
        buffer.writeByte(b);
    }
}
