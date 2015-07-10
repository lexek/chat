package lexek.wschat.db.model.rest;

public class ErrorModel {
    private final String message;

    public ErrorModel(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
