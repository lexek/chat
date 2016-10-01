package lexek.wschat.db.model.rest;

public class BasicResponse {
    private final boolean success;
    private final String error;

    public BasicResponse(boolean success, String error) {
        this.success = success;
        this.error = error;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getError() {
        return error;
    }
}
