package lexek.wschat.chat.handlers;

import com.google.common.collect.ImmutableSet;
import lexek.wschat.chat.Connection;
import lexek.wschat.chat.Room;
import lexek.wschat.chat.model.*;
import lexek.wschat.chat.processing.AbstractRoomMessageHandler;

public abstract class AbstractModerationHandler extends AbstractRoomMessageHandler {
    private final boolean fetchChatterIfOffline;
    private final String deniedErrorName;

    AbstractModerationHandler(MessageType type, boolean fetchChatterIfOffline, String deniedErrorName) {
        super(
            ImmutableSet.of(
                MessageProperty.ROOM,
                MessageProperty.NAME
            ),
            type,
            LocalRole.MOD,
            false
        );
        this.fetchChatterIfOffline = fetchChatterIfOffline;
        this.deniedErrorName = deniedErrorName;
    }

    private static boolean canBan(Chatter modChatter, Chatter userChatter) {
        User user = userChatter.getUser();
        return !userChatter.hasRole(LocalRole.MOD) &&
            (
                modChatter.hasRole(LocalRole.MOD) &&
                modChatter.hasGreaterRole(userChatter.getRole()) &&
                !user.hasRole(GlobalRole.MOD)
            );
    }

    @Override
    public void handle(Connection connection, User user, Room room, Chatter modChatter, Message message) {
        Chatter userChatter;
        if (fetchChatterIfOffline) {
            userChatter = room.getChatter(message.get(MessageProperty.NAME));
        } else {
            userChatter = room.getOnlineChatterByName(message.get(MessageProperty.NAME));
        }
        if (userChatter != null && userChatter.getId() != null) {
            if (canBan(modChatter, userChatter)) {
                if (performOperation(room, modChatter, userChatter)) {
                    success(connection, room, modChatter, userChatter);
                } else {
                    connection.send(Message.errorMessage("INTERNAL_ERROR"));
                }
            } else {
                connection.send(Message.errorMessage(deniedErrorName));
            }
        } else {
            connection.send(Message.errorMessage("UNKNOWN_USER"));
        }
    }

    protected abstract boolean performOperation(Room room, Chatter mod, Chatter userChatter);

    protected abstract void success(Connection connection, Room room, Chatter modChatter, Chatter userChatter);
}
