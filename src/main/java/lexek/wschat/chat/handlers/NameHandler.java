package lexek.wschat.chat.handlers;

import com.google.common.collect.ImmutableSet;
import lexek.wschat.chat.Connection;
import lexek.wschat.chat.model.*;
import lexek.wschat.chat.processing.AbstractGlobalMessageHandler;
import lexek.wschat.services.UserService;
import lexek.wschat.util.Names;

public class NameHandler extends AbstractGlobalMessageHandler {
    private final UserService userService;

    public NameHandler(UserService userService) {
        super(
            ImmutableSet.of(
                MessageProperty.NAME
            ),
            MessageType.NAME,
            GlobalRole.USER,
            false);

        this.userService = userService;
    }

    @Override
    public void handle(Connection connection, User user, Message message) {
        String newName = message.get(MessageProperty.NAME).trim().toLowerCase();
        if (connection.getUser().isRenameAvailable()) {
            if (Names.USERNAME_PATTERN.matcher(newName).matches()) {
                if (!userService.changeName(user.getWrappedObject(), newName)) {
                    connection.send(Message.errorMessage("NAME_TAKEN"));
                }
            } else {
                connection.send(Message.errorMessage("NAME_BAD_FORMAT"));
            }
        } else if (user.getRole() == GlobalRole.SUPERADMIN) {
            if (!newName.isEmpty()) {
                if (!userService.changeName(user.getWrappedObject(), newName)) {
                    connection.send(Message.errorMessage("NAME_TAKEN"));
                }
            } else {
                connection.send(Message.errorMessage("NAME_BAD_FORMAT"));
            }
        } else {
            connection.send(Message.errorMessage("NAME_DENIED"));
        }
    }
}
