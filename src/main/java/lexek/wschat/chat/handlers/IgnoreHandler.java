package lexek.wschat.chat.handlers;

import com.google.common.collect.ImmutableSet;
import lexek.wschat.chat.Connection;
import lexek.wschat.chat.e.EntityNotFoundException;
import lexek.wschat.chat.e.InvalidInputException;
import lexek.wschat.chat.e.LimitExceededException;
import lexek.wschat.chat.model.*;
import lexek.wschat.chat.processing.AbstractGlobalMessageHandler;
import lexek.wschat.services.IgnoreService;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;

@Service
public class IgnoreHandler extends AbstractGlobalMessageHandler {
    private final IgnoreService ignoreService;

    @Inject
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
        } catch (EntityNotFoundException e) {
            if (e.getMessage().equals("user")) {
                connection.send(Message.errorMessage("UNKNOWN_USER"));
            }
        } catch (InvalidInputException e) {
            if (e.name().equals("name")) {
                connection.send(Message.errorMessage(e.message()));
            }
        }
    }
}
