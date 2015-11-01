package lexek.wschat.chat.e;

public class LimitExceededException extends DomainException {
    private final String name;

    public LimitExceededException(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
