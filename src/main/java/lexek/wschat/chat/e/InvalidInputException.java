package lexek.wschat.chat.e;

import java.util.Map;

public class InvalidInputException extends DomainException {
    private final Map<String, String> data;

    public InvalidInputException(Map<String, String> data) {
        super();
        this.data = data;
    }

    public Map<String, String> getData() {
        return data;
    }
}
