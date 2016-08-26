package lexek.wschat.proxy.cybergame;

import com.fasterxml.jackson.databind.JsonNode;

public class CybergameTvEvent {
    private final String type;
    private final JsonNode data;

    public CybergameTvEvent(String type, JsonNode data) {
        this.type = type;
        this.data = data;
    }

    public String getType() {
        return type;
    }

    public JsonNode getData() {
        return data;
    }
}
