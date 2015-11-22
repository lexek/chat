package lexek.wschat.chat.handlers;

import com.google.common.collect.ImmutableSet;
import lexek.wschat.chat.*;
import lexek.wschat.chat.e.InvalidInputException;
import lexek.wschat.chat.e.LimitExceededException;
import lexek.wschat.chat.processing.AbstractGlobalMessageHandler;
import lexek.wschat.services.IgnoreService;

public class IgnoreHandler extends AbstractGlobalMessageHandler {
    private final IgnoreService ignoreService;

    public IgnoreHandler(IgnoreService ignoreService) {
        super(ImmutableSet.of(MessageProperty.NAME), MessageType.IGNORE, GlobalRole.USER, true);
        this.ignoreService = ignoreService;
    }

    @Override
    public void handle(Connection connection, User user, Message message) {
        try {
            String name = message.get(MessageProperty.NAME);
            ignoreService.ignore(user, name);
            connection.send(Message.ignoredMessage(ignoreService.getIgnoredNames(user)));
            connection.send(Message.ignoreMessage(MessageType.IGNORE, name));
        } catch (LimitExceededException e) {
            connection.send(Message.errorMessage("IGNORE_LIMIT_REACHED"));
        } catch (InvalidInputException e) {
            if (e.name().equals("name")) {
                connection.send(Message.errorMessage(e.message()));
            }
        }
    }
}
