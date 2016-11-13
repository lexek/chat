package lexek.wschat.proxy.sc2tv;

import com.fasterxml.jackson.databind.JsonNode;

public class Sc2tvMessage {
    private final Long id;
    private final String scope;
    private final JsonNode data;

    public Sc2tvMessage(Long id, String scope, JsonNode data) {
        this.id = id;
        this.scope = scope;
        this.data = data;
    }

    public String getScope() {
        return scope;
    }

    public JsonNode getData() {
        return data;
    }

    public Long getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Sc2tvMessage{" +
            "id=" + id +
            ", scope='" + scope + '\'' +
            ", data=" + data +
            '}';
    }
}
