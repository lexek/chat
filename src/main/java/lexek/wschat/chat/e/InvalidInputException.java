package lexek.wschat.chat.e;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

public class InvalidInputException extends DomainException {
    private final String name;
    private final String message;

    public InvalidInputException(String name, String message) {
        this.name = name;
        this.message = message;
    }

    public String name() {
        return name;
    }

    public String message() {
        return message;
    }

    public Map<String, String> getData() {
        return ImmutableMap.of("name", name, "message", message);
    }
}
