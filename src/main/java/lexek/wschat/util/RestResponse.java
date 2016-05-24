package lexek.wschat.util;

import com.fasterxml.jackson.databind.JsonNode;

public class RestResponse {
    private final int status;
    private final boolean success;
    private final JsonNode rootNode;

    public RestResponse(int status, boolean success, JsonNode rootNode) {
        this.status = status;
        this.success = success;
        this.rootNode = rootNode;
    }

    public int getStatus() {
        return status;
    }

    public boolean isSuccess() {
        return success;
    }

    public JsonNode getRootNode() {
        return rootNode;
    }
}
