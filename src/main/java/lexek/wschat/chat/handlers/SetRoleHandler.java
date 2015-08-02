package lexek.wschat.chat.handlers;

import com.google.common.collect.ImmutableSet;
import lexek.wschat.chat.*;
import lexek.wschat.chat.processing.AbstractRoomMessageHandler;

public class SetRoleHandler extends AbstractRoomMessageHandler {

    public SetRoleHandler() {
        super(
            ImmutableSet.of(
                MessageProperty.ROOM,
                MessageProperty.NAME,
                MessageProperty.LOCAL_ROLE
            ),
            MessageType.ROLE,
            LocalRole.ADMIN,
            false);
    }

    private static boolean canChangeRole(Chatter modChatter, Chatter userChatter) {
        User user = userChatter.getUser();
        User modUser = modChatter.getUser();
        return
            modChatter.hasRole(LocalRole.ADMIN) &&
                (
                    modChatter.hasGreaterRole(userChatter.getRole()) ||
                        modUser.hasGreaterRole(user.getRole())
                );
    }

    @Override
    public void handle(Connection connection, User user, Room room, Chatter adminChatter, Message message) {
        LocalRole newRole = message.get(MessageProperty.LOCAL_ROLE);
        if (newRole == null) {
            connection.send(Message.errorMessage("UNKNOWN_ROLE"));
            return;
        }
        Chatter userChatter = room.getChatter(message.get(MessageProperty.NAME));
        if (userChatter != null && userChatter.getId() != null) {
            if (canChangeRole(adminChatter, userChatter)) {
                if (room.setRole(userChatter, adminChatter, newRole)) {
                    connection.send(Message.infoMessage("OK"));
                } else {
                    connection.send(Message.errorMessage("INTERNAL_ERROR"));
                }
            } else {
                connection.send(Message.errorMessage("ROLE_DENIED"));
            }
        } else {
            connection.send(Message.errorMessage("UNKNOWN_USER"));
        }
    }
}
