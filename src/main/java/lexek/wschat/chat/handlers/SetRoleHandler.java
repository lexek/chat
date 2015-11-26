package lexek.wschat.chat.handlers;

import com.google.common.collect.ImmutableSet;
import lexek.wschat.chat.Connection;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.model.*;
import lexek.wschat.chat.processing.AbstractRoomMessageHandler;
import lexek.wschat.services.ChatterService;

public class SetRoleHandler extends AbstractRoomMessageHandler {
    private final ChatterService chatterService;

    public SetRoleHandler(ChatterService chatterService) {
        super(
            ImmutableSet.of(
                MessageProperty.ROOM,
                MessageProperty.NAME,
                MessageProperty.LOCAL_ROLE
            ),
            MessageType.ROLE,
            LocalRole.ADMIN,
            false);
        this.chatterService = chatterService;
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
        Chatter userChatter = room.getChatter(message.get(MessageProperty.NAME));
        if (userChatter != null && userChatter.getId() != null) {
            if (canChangeRole(adminChatter, userChatter)) {
                if (chatterService.setRole(room, userChatter, adminChatter, newRole)) {
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
