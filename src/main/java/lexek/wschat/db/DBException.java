package lexek.wschat.db;

public class DBException extends RuntimeException {
    public DBException(Throwable t) {
        super(t);
    }

    public static class DuplicateKeyException extends DBException {
        public DuplicateKeyException(Throwable t) {
            super(t);
        }
    }
}
