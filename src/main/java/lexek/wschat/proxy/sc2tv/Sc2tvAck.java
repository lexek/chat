package lexek.wschat.proxy.sc2tv;

import com.fasterxml.jackson.databind.JsonNode;

public class Sc2tvAck {
    private final long id;
    private final JsonNode data;

    public Sc2tvAck(long id, JsonNode data) {
        this.id = id;
        this.data = data;
    }

    public long getId() {
        return id;
    }

    public JsonNode getData() {
        return data;
    }

    @Override
    public String toString() {
        return "Sc2tvAck{" +
            "id=" + id +
            ", data=" + data +
            '}';
    }
}
