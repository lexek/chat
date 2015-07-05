package lexek.httpserver;

public class HttpException extends RuntimeException {
    private final int status;
    private final String reason;

    public HttpException(int status, String reason) {
        this.status = status;
        this.reason = reason;
    }
}
