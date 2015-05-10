package lexek.wschat.chat;

public class MessageDataKey<T> {
    private final String name;

    private MessageDataKey(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Name is null");
        }
        this.name = name;
    }

    public static <T> MessageDataKey<T> valueOf(String name) {
        return new MessageDataKey<>(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
