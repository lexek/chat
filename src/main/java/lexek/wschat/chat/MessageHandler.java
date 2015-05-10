package lexek.wschat.chat;

import java.util.List;

public interface MessageHandler {
    MessageType getType();

    GlobalRole getRole();

    int getArgCount();

    void handle(List<String> args, Connection connection);

    boolean isNeedsInterval();

    boolean isNeedsLogging();
}
