package lexek.httpserver;

import java.io.IOException;

public interface StaticHandlerContext {
    boolean exists();

    String getContentType();

    long lastModified();

    byte[] read() throws IOException;
}
