package lexek.wschat.db.model.rest;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BooleanValueContainer {
    private final boolean value;

    public BooleanValueContainer(@JsonProperty("value") boolean value) {
        this.value = value;
    }

    public boolean getValue() {
        return value;
    }
}
