package lexek.wschat.services;

public class PollOption {
    private final int optionId;
    private final String text;

    public PollOption(int optionId, String text) {
        this.optionId = optionId;
        this.text = text;
    }

    public int getOptionId() {
        return optionId;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return "PollOption{" +
                "optionId=" + optionId +
                ", text='" + text + '\'' +
                '}';
    }
}
