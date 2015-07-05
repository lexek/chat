package lexek.wschat.util;

import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.io.InputStream;

public class BufferInputStream extends InputStream {
    private final ByteBuf buffer;

    public BufferInputStream(ByteBuf buffer) {
        this.buffer = buffer;
    }

    @Override
    public int read() throws IOException {
        if (buffer.readableBytes() > 0) {
            return buffer.readByte() & 0xFF;
        } else {
            return -1;
        }
    }
}
