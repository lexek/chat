package lexek.wschat.db.model.rest;

public class RoomRestModel {
    private final long id;
    private final String name;
    private final String topic;
    private final long online;

    public RoomRestModel(long id, String name, String topic, long online) {
        this.id = id;
        this.name = name;
        this.topic = topic;
        this.online = online;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTopic() {
        return topic;
    }

    public long getOnline() {
        return online;
    }
}
