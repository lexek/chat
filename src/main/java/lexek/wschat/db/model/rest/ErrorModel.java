package lexek.wschat.db.model.rest;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

public class ErrorModel {
    private final String message;
    private final Map errors;

    public ErrorModel(String message) {
        this.message = message;
        this.errors = ImmutableMap.of();
    }

    public ErrorModel(String message, Map errors) {
        this.message = message;
        this.errors = errors;
    }

    public String getMessage() {
        return message;
    }

    public Map getErrors() {
        return errors;
    }
}
