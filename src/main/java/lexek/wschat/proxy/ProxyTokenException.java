package lexek.wschat.proxy;

import org.jetbrains.annotations.NonNls;

public class ProxyTokenException extends RuntimeException {
    public ProxyTokenException() {
        super();
    }

    public ProxyTokenException(@NonNls String message) {
        super(message);
    }

    public ProxyTokenException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProxyTokenException(Throwable cause) {
        super(cause);
    }

    protected ProxyTokenException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
